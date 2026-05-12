package cn.jeyor1337.zanticheat.event;

import com.fren_gor.lightInjector.LightInjector;
import io.netty.channel.Channel;
import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.event.packetrecive.ZACAsyncPacketReceiveEvent;
import cn.jeyor1337.zanticheat.event.packetrecive.packettype.PacketType;
import cn.jeyor1337.zanticheat.event.playerattack.ZACAsyncPlayerAttackEvent;
import cn.jeyor1337.zanticheat.event.playerattack.ZACPlayerAttackEvent;
import cn.jeyor1337.zanticheat.event.playerbreakblock.ZACAsyncPlayerBreakBlockEvent;
import cn.jeyor1337.zanticheat.event.playerbreakblock.ZACPlayerBreakBlockEvent;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.event.playermove.ZACPlayerMoveEvent;
import cn.jeyor1337.zanticheat.event.playerplaceblock.ZACAsyncPlayerPlaceBlockEvent;
import cn.jeyor1337.zanticheat.event.playerplaceblock.ZACPlayerPlaceBlockEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.ZACPlayerListener;
import cn.jeyor1337.zanticheat.util.config.ConfigManager;
import cn.jeyor1337.zanticheat.util.detection.CheckUtil;
import cn.jeyor1337.zanticheat.util.hook.server.folia.FoliaUtil;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.identifier.ZACVersion;
import cn.jeyor1337.zanticheat.version.identifier.VerIdentifier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZACEventCaller extends LightInjector implements Listener {

    private static final PluginManager PLUGIN_MANAGER;

    static {
        PLUGIN_MANAGER = Bukkit.getServer().getPluginManager();
    }

    public ZACEventCaller() {
        super(Main.getInstance());
    }

    public static void callMovementEvents(PlayerMoveEvent event) {
        if (CheckUtil.isExternalNPC(event))
            return;
        if (event.getTo() == null)
            return;
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        ZACPlayerMoveEvent zacPlayerMoveEvent = new ZACPlayerMoveEvent(event, player, zacPlayer, event.getFrom(), event.getTo());
        Scheduler.entityThread(player, () -> {
            if (!FoliaUtil.isStable(player))
                return;
            PLUGIN_MANAGER.callEvent(zacPlayerMoveEvent);
            Scheduler.runTaskAsynchronously(true, () -> {
                PLUGIN_MANAGER.callEvent(new ZACAsyncPlayerMoveEvent(zacPlayerMoveEvent));
            });
        });
    }

    public static void callEntityDamageEvent(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player))
            return;
        Player player = (Player) event.getDamager();
        if (CheckUtil.isExternalNPC(player))
            return;
        if (CheckUtil.isExternalNPC(event.getEntity()))
            return;
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        Scheduler.entityThread(player, () -> {
            if (!FoliaUtil.isStable(player))
                return;
            PLUGIN_MANAGER.callEvent(new ZACPlayerAttackEvent(event, player, zacPlayer, event.getEntity()));
            Scheduler.runTaskAsynchronously(true, () -> {
                PLUGIN_MANAGER.callEvent(new ZACAsyncPlayerAttackEvent(player, zacPlayer, event.getEntity().getEntityId()));
            });
        });
    }

    public static void callBlockPlaceEvents(BlockPlaceEvent event) {
        if (CheckUtil.isExternalNPC(event.getPlayer()))
            return;
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        ZACPlayerPlaceBlockEvent zacPlayerPlaceBlockEvent = new ZACPlayerPlaceBlockEvent(event, player, zacPlayer,
                event.getBlock(), event.getBlockAgainst(), event.getBlockReplacedState());
        Scheduler.entityThread(player, () -> {
            if (!FoliaUtil.isStable(player))
                return;
            PLUGIN_MANAGER.callEvent(zacPlayerPlaceBlockEvent);
            Scheduler.runTaskAsynchronously(true, () -> {
                PLUGIN_MANAGER.callEvent(new ZACAsyncPlayerPlaceBlockEvent(zacPlayerPlaceBlockEvent));
            });
        });
    }

    public static void callBlockBreakEvents(BlockBreakEvent event) {
        if (CheckUtil.isExternalNPC(event.getPlayer()))
            return;
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        ZACPlayerBreakBlockEvent zacPlayerBreakBlockEvent = new ZACPlayerBreakBlockEvent(event, player, zacPlayer, event.getBlock());
        Scheduler.entityThread(player, () -> {
            if (!FoliaUtil.isStable(player))
                return;
            PLUGIN_MANAGER.callEvent(zacPlayerBreakBlockEvent);
            Scheduler.runTaskAsynchronously(true, () -> {
                PLUGIN_MANAGER.callEvent(new ZACAsyncPlayerBreakBlockEvent(zacPlayerBreakBlockEvent));
            });
        });
    }

    @Override
    protected @Nullable Object onPacketReceiveAsync(@Nullable Player sender, @NotNull Channel channel, @NotNull Object nmsPacket) {
        if (!ConfigManager.Config.enabled) return nmsPacket;
        if (sender == null) return nmsPacket;
        ZACPlayer zacPlayer = ZACPlayerListener.getAsyncPlayers().getOrDefault(sender.getUniqueId(), null);
        if (zacPlayer == null || zacPlayer.leaveTime != 0L || !sender.isOnline())
            return nmsPacket;
        ZACAsyncPacketReceiveEvent event = new ZACAsyncPacketReceiveEvent(sender, zacPlayer, nmsPacket);
        if (event.getPacketType() == PacketType.USE_ENTITY && VerIdentifier.getVersion().isNewerThan(ZACVersion.V1_8)) {
            PLUGIN_MANAGER.callEvent(new ZACAsyncPlayerAttackEvent(event.getPlayer(), event.getZacPlayer(), event.getEntityId()));
        }
        if (event.getPacketType() == PacketType.FLYING) {
            Scheduler.runTaskAsynchronously(true, () -> {
                PLUGIN_MANAGER.callEvent(event);
            });
        } else {
            PLUGIN_MANAGER.callEvent(event);
        }
        return nmsPacket;
    }

    @Override
    protected @Nullable Object onPacketSendAsync(@Nullable Player receiver, @NotNull Channel channel, @NotNull Object nmsPacket) {
        return nmsPacket;
    }

}
