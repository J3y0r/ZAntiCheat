package com.fren_gor.lightInjector;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class LightInjector {

    private static final String COMPLETE_VERSION;
    private static final int VERSION;
    private static final Class<?> SERVER_CLASS;
    private static final Class<?> SERVER_CONNECTION_CLASS;
    private static final Class<?> NETWORK_MANAGER_CLASS;
    private static final Class<?> ENTITY_PLAYER_CLASS;
    private static final Class<?> PLAYER_CONNECTION_CLASS;
    private static final Class<?> PACKET_LOGIN_OUT_SUCCESS_CLASS;
    private static final Field NMS_SERVER;
    private static final Field NMS_SERVER_CONNECTION;
    private static final Field NMS_NETWORK_MANAGERS_LIST;
    private static final Field NMS_PENDING_NETWORK_MANAGERS;
    private static final Field NMS_CHANNEL_FROM_NM;
    private static final Field GAME_PROFILE_FROM_PACKET;
    private static final Field GET_PLAYER_CONNECTION;
    private static final Field GET_NETWORK_MANAGER;
    private static final Method GET_PLAYER_HANDLE;
    private static int ID;

    private final Plugin plugin;
    private final String identifier;
    private final List<?> networkManagers;
    private final Iterable<?> pendingNetworkManagers;
    private final EventListener listener = new EventListener();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Map<UUID, Player> playerCache = Collections.synchronizedMap(new HashMap<>());
    private final Set<Channel> injectedChannels = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    public LightInjector(Plugin plugin) {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("LightInjector must be constructed on the main thread.");
        this.plugin = Objects.requireNonNull(plugin, "Plugin is null.");
        if (!plugin.isEnabled())
            throw new IllegalArgumentException("Plugin " + plugin.getName() + " is not enabled");
        identifier = Objects.requireNonNull(getIdentifier(), "getIdentifier() returned a null value.") + "-" + ID++;

        try {
            Object serverConnection = NMS_SERVER_CONNECTION.get(NMS_SERVER.get(Bukkit.getServer()));
            if (serverConnection == null)
                throw new RuntimeException("[LightInjector] ServerConnection is null.");
            networkManagers = (List<?>) NMS_NETWORK_MANAGERS_LIST.get(serverConnection);
            pendingNetworkManagers = NMS_PENDING_NETWORK_MANAGERS == null ? null :
                    (Iterable<?>) NMS_PENDING_NETWORK_MANAGERS.get(serverConnection);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("[LightInjector] An error occurred while injecting.", exception);
        }

        Bukkit.getPluginManager().registerEvents(listener, plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                injectPlayer(player);
            } catch (Exception ignored) {
            }
        }
    }

    protected abstract Object onPacketReceiveAsync(Player player, Channel channel, Object packet);

    protected abstract Object onPacketSendAsync(Player player, Channel channel, Object packet);

    public final void sendPacket(Player player, Object packet) {
        Objects.requireNonNull(player, "Player is null.");
        sendPacket(getChannel(player), Objects.requireNonNull(packet, "Packet is null."));
    }

    public final void sendPacket(Channel channel, Object packet) {
        Objects.requireNonNull(channel, "Channel is null.").pipeline()
                .writeAndFlush(Objects.requireNonNull(packet, "Packet is null."));
    }

    public final void receivePacket(Player player, Object packet) {
        Objects.requireNonNull(player, "Player is null.");
        receivePacket(getChannel(player), Objects.requireNonNull(packet, "Packet is null."));
    }

    public final void receivePacket(Channel channel, Object packet) {
        Objects.requireNonNull(channel, "Channel is null.").pipeline().context("encoder")
                .fireChannelRead(Objects.requireNonNull(packet, "Packet is null."));
    }

    protected String getIdentifier() {
        return "light-injector-" + plugin.getName();
    }

    public final void close() {
        if (closed.getAndSet(true))
            return;
        listener.unregister();
        synchronized (networkManagers) {
            for (Object networkManager : networkManagers) {
                try {
                    Channel channel = getChannel(networkManager);
                    channel.eventLoop().submit(() -> channel.pipeline().remove(identifier));
                } catch (Exception ignored) {
                }
            }
        }
        playerCache.clear();
        injectedChannels.clear();
    }

    public final boolean isClosed() {
        return closed.get();
    }

    public final Plugin getPlugin() {
        return plugin;
    }

    private void injectPlayer(Player player) {
        Object networkManager = getNetworkManager(player);
        PacketHandler handler = injectChannel(getChannel(networkManager));
        handler.player = player;
    }

    private PacketHandler injectChannel(Channel channel) {
        PacketHandler handler = new PacketHandler();
        channel.eventLoop().submit(() -> {
            if (isClosed())
                return;
            if (!injectedChannels.add(channel))
                return;
            try {
                channel.pipeline().addBefore("packet_handler", identifier, handler);
            } catch (IllegalArgumentException ignored) {
            }
        });
        return handler;
    }

    private void injectNetworkManager(Object networkManager) {
        injectChannel(getChannel(networkManager));
    }

    private Object getNetworkManager(Player player) {
        try {
            Object handle = GET_PLAYER_HANDLE.invoke(player);
            Object connection = GET_PLAYER_CONNECTION.get(handle);
            return GET_NETWORK_MANAGER.get(connection);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("[LightInjector] Cannot get player's NetworkManager.", exception);
        }
    }

    private Channel getChannel(Player player) {
        return getChannel(getNetworkManager(player));
    }

    private Channel getChannel(Object networkManager) {
        try {
            return (Channel) NMS_CHANNEL_FROM_NM.get(networkManager);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("[LightInjector] Cannot get player's Channel.", exception);
        }
    }

    private static Class<?> getNMSClass(String legacyName, String modernPackage) {
        String name = "net.minecraft." + (VERSION >= 17 ? modernPackage : "server." + COMPLETE_VERSION) + "." + legacyName;
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            throw new RuntimeException("[LightInjector] Cannot find NMS Class! (" + name + ")", exception);
        }
    }

    private static Class<?> getCBClass(String name) {
        String className = "org.bukkit.craftbukkit." + (COMPLETE_VERSION == null ? "" : COMPLETE_VERSION + ".") + name;
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new RuntimeException("[LightInjector] Cannot find CB Class! (" + className + ")", exception);
        }
    }

    private static Field getField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new RuntimeException("[LightInjector] Cannot find field! (" + clazz.getName() + "." + name + ")");
    }

    private static Field getField(Class<?> clazz, Class<?> type, int index) {
        return getField(clazz, type, index, 0);
    }

    private static Field getField(Class<?> clazz, Class<?> type, int index, int skipAssignable) {
        Field[] fields = getDeclaredFields(clazz);
        int remaining = index;
        for (Field field : fields) {
            if (field.getType().equals(type) && --remaining <= 0) {
                field.setAccessible(true);
                return field;
            }
        }
        remaining = index + skipAssignable;
        for (Field field : fields) {
            if (type.isAssignableFrom(field.getType()) && --remaining <= 0) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new RuntimeException("[LightInjector] Cannot find field! (" + index + getOrdinal(index)
                + " returning " + type.getName() + " in " + clazz.getName() + ")");
    }

    private static Field[] getDeclaredFields(Class<?> clazz) {
        ArrayList<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            Collections.addAll(fields, current.getDeclaredFields());
            current = current.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }

    private static Field getPendingNetworkManagersFieldOrNull(Class<?> clazz) {
        for (Field field : getDeclaredFields(clazz)) {
            if (!Iterable.class.isAssignableFrom(field.getType()) || List.class.isAssignableFrom(field.getType()))
                continue;
            field.setAccessible(true);
            return field;
        }
        return null;
    }

    private static Method getMethod(Class<?> clazz, String name, Class<?>... parameters) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                Method method = current.getDeclaredMethod(name, parameters);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (Class<?> parameter : parameters)
            joiner.add(parameter.getName());
        throw new RuntimeException("[LightInjector] Cannot find method! (" + clazz.getName() + "." + name
                + "(" + joiner + "))");
    }

    private static String getOrdinal(int number) {
        if (number == 1)
            return "st";
        if (number == 2)
            return "nd";
        if (number == 3)
            return "rd";
        return "th";
    }

    private final class PacketHandler extends ChannelDuplexHandler {
        private volatile Player player;

        @Override
        public void channelUnregistered(ChannelHandlerContext context) throws Exception {
            injectedChannels.remove(context.channel());
            super.channelUnregistered(context);
        }

        @Override
        public void write(ChannelHandlerContext context, Object packet, ChannelPromise promise) throws Exception {
            if (player == null && PACKET_LOGIN_OUT_SUCCESS_CLASS.isInstance(packet)) {
                try {
                    Object profile = GAME_PROFILE_FROM_PACKET.get(packet);
                    UUID uuid = getProfileId(profile);
                    if (uuid != null) {
                        Player cachedPlayer = playerCache.remove(uuid);
                        if (cachedPlayer != null)
                            player = cachedPlayer;
                    }
                } catch (Throwable ignored) {
                }
            }

            Object newPacket;
            try {
                newPacket = onPacketSendAsync(player, context.channel(), packet);
            } catch (OutOfMemoryError error) {
                throw error;
            } catch (Throwable throwable) {
                super.write(context, packet, promise);
                return;
            }
            if (newPacket != null)
                super.write(context, newPacket, promise);
        }

        @Override
        public void channelRead(ChannelHandlerContext context, Object packet) throws Exception {
            Object newPacket;
            try {
                newPacket = onPacketReceiveAsync(player, context.channel(), packet);
            } catch (OutOfMemoryError error) {
                throw error;
            } catch (Throwable throwable) {
                super.channelRead(context, packet);
                return;
            }
            if (newPacket != null)
                super.channelRead(context, newPacket);
        }
    }

    private final class EventListener implements Listener {

        @EventHandler
        public void onAsyncPlayerPreLoginEvent(AsyncPlayerPreLoginEvent event) {
            if (isClosed())
                return;
            synchronized (networkManagers) {
                if (networkManagers instanceof RandomAccess) {
                    for (int i = networkManagers.size() - 1; i >= 0; i--)
                        injectNetworkManager(networkManagers.get(i));
                } else {
                    for (Object networkManager : networkManagers)
                        injectNetworkManager(networkManager);
                }
                if (pendingNetworkManagers != null) {
                    synchronized (pendingNetworkManagers) {
                        for (Object networkManager : pendingNetworkManagers)
                            injectNetworkManager(networkManager);
                    }
                }
            }
        }

        @EventHandler
        public void onPlayerLoginEvent(PlayerLoginEvent event) {
            if (isClosed())
                return;
            playerCache.put(event.getPlayer().getUniqueId(), event.getPlayer());
        }

        @EventHandler
        public void onPlayerJoinEvent(PlayerJoinEvent event) {
            if (isClosed())
                return;
            Player player = event.getPlayer();
            Object networkManager = getNetworkManager(player);
            Channel channel = getChannel(networkManager);
            ChannelHandler handler = channel.pipeline().get(identifier);
            if (handler instanceof PacketHandler) {
                ((PacketHandler) handler).player = player;
                playerCache.remove(player.getUniqueId());
                return;
            }
            injectChannel(channel).player = player;
        }

        @EventHandler
        public void onPluginDisableEvent(PluginDisableEvent event) {
            if (plugin.equals(event.getPlugin()))
                close();
        }

        private void unregister() {
            AsyncPlayerPreLoginEvent.getHandlerList().unregister(this);
            PlayerLoginEvent.getHandlerList().unregister(this);
            PlayerJoinEvent.getHandlerList().unregister(this);
            PluginDisableEvent.getHandlerList().unregister(this);
        }
    }

    private static UUID getProfileId(Object profile) {
        if (profile == null)
            return null;
        UUID uuid = getProfileIdByMethod(profile, "getId");
        if (uuid != null)
            return uuid;
        uuid = getProfileIdByMethod(profile, "id");
        if (uuid != null)
            return uuid;
        try {
            Field field = profile.getClass().getDeclaredField("id");
            field.setAccessible(true);
            return toUuid(field.get(profile));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static UUID getProfileIdByMethod(Object profile, String methodName) {
        try {
            Method method = profile.getClass().getMethod(methodName);
            return toUuid(method.invoke(profile));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static UUID toUuid(Object value) {
        if (value instanceof UUID)
            return (UUID) value;
        if (!(value instanceof String))
            return null;
        String raw = ((String) value).trim();
        if (raw.isEmpty())
            return null;
        try {
            if (raw.length() == 32) {
                raw = raw.substring(0, 8) + "-" + raw.substring(8, 12) + "-"
                        + raw.substring(12, 16) + "-" + raw.substring(16, 20) + "-"
                        + raw.substring(20);
            }
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    static {
        String craftServerName = Bukkit.getServer().getClass().getName();
        String[] parts = craftServerName.split("\\.");
        if (parts.length >= 5) {
            COMPLETE_VERSION = parts[3];
            VERSION = Integer.parseInt(COMPLETE_VERSION.split("_")[1]);
        } else {
            COMPLETE_VERSION = null;
            VERSION = 17;
        }

        SERVER_CLASS = getNMSClass("MinecraftServer", "server");
        SERVER_CONNECTION_CLASS = getNMSClass("ServerConnection", "server.network");
        NETWORK_MANAGER_CLASS = getNMSClass("NetworkManager", "network");
        ENTITY_PLAYER_CLASS = getNMSClass("EntityPlayer", "server.level");
        PLAYER_CONNECTION_CLASS = getNMSClass("PlayerConnection", "server.network");
        PACKET_LOGIN_OUT_SUCCESS_CLASS = getNMSClass("PacketLoginOutSuccess", "network.protocol.login");
        NMS_SERVER = getField(getCBClass("CraftServer"), SERVER_CLASS, 1);
        NMS_SERVER_CONNECTION = getField(SERVER_CLASS, SERVER_CONNECTION_CLASS, 1);
        NMS_NETWORK_MANAGERS_LIST = getField(SERVER_CONNECTION_CLASS, List.class, 2);
        NMS_PENDING_NETWORK_MANAGERS = getPendingNetworkManagersFieldOrNull(SERVER_CONNECTION_CLASS);
        NMS_CHANNEL_FROM_NM = getField(NETWORK_MANAGER_CLASS, Channel.class, 1);
        GAME_PROFILE_FROM_PACKET = getField(PACKET_LOGIN_OUT_SUCCESS_CLASS,
                getClassOrThrow("com.mojang.authlib.GameProfile"), 1);
        GET_PLAYER_CONNECTION = getField(ENTITY_PLAYER_CLASS, PLAYER_CONNECTION_CLASS, 1);
        GET_NETWORK_MANAGER = getField(PLAYER_CONNECTION_CLASS, NETWORK_MANAGER_CLASS, 1, 1);
        GET_PLAYER_HANDLE = getMethod(getCBClass("entity.CraftPlayer"), "getHandle");
        ID = 0;
    }

    private static Class<?> getClassOrThrow(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            throw new RuntimeException("[LightInjector] Cannot find class! (" + name + ")", exception);
        }
    }
}
