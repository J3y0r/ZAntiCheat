package cn.jeyor1337.zanticheat.util.velocity;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.util.config.ConfigManager;
import cn.jeyor1337.zanticheat.util.logger.LogType;
import cn.jeyor1337.zanticheat.util.logger.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;

import java.io.Closeable;

public class RedisVelocityMessageBridge implements VelocityMessageBridge, Closeable {

    private volatile Thread subscriberThread;
    private volatile JedisPubSub jedisPubSub;
    private volatile boolean closed = true;

    @Override
    public boolean initialize() {
        closed = true;
        if (!"redis".equalsIgnoreCase(ConfigManager.Config.VelocitySupport.MessageBridge.type)) {
            Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") Unsupported velocity message bridge type: "
                    + ConfigManager.Config.VelocitySupport.MessageBridge.type);
            return false;
        }
        try (Jedis jedis = createJedis()) {
            jedis.ping();
            closed = false;
            return true;
        } catch (JedisException exception) {
            Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") Failed to initialize velocity sync bridge: " + exception.getMessage());
            return false;
        }
    }

    @Override
    public boolean publish(VelocityBanSyncMessage message) {
        if (closed)
            return false;
        try (Jedis jedis = createJedis()) {
            jedis.publish(ConfigManager.Config.VelocitySupport.MessageBridge.channel, serialize(message));
            return true;
        } catch (JedisException exception) {
            Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") Failed to publish velocity sync message: " + exception.getMessage());
            return false;
        }
    }

    @Override
    public void subscribe(VelocityMessageConsumer consumer) {
        if (closed)
            return;
        if (subscriberThread != null && subscriberThread.isAlive())
            return;
        subscriberThread = new Thread(() -> {
            while (!closed && !Thread.currentThread().isInterrupted()) {
                JedisPubSub pubSub = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        VelocityBanSyncMessage syncMessage = deserialize(message);
                        if (syncMessage != null)
                            consumer.accept(syncMessage);
                    }
                };
                jedisPubSub = pubSub;
                try (Jedis jedis = createJedis()) {
                    jedis.subscribe(pubSub, ConfigManager.Config.VelocitySupport.MessageBridge.channel);
                    break;
                } catch (JedisException exception) {
                    if (closed || Thread.currentThread().isInterrupted())
                        break;
                    Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") Failed to subscribe velocity sync bridge: " + exception.getMessage());
                    try {
                        Thread.sleep(Math.max(ConfigManager.Config.VelocitySupport.MessageBridge.connectTimeoutMs, 1000));
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } finally {
                    jedisPubSub = null;
                }
            }
        }, "zac-velocity-sync-subscriber");
        subscriberThread.setDaemon(true);
        subscriberThread.start();
    }

    @Override
    public void close() {
        closed = true;
        JedisPubSub pubSub = jedisPubSub;
        if (pubSub != null)
            pubSub.unsubscribe();
        Thread thread = subscriberThread;
        subscriberThread = null;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(1000L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private Jedis createJedis() {
        Jedis jedis = new Jedis(ConfigManager.Config.VelocitySupport.MessageBridge.host,
                ConfigManager.Config.VelocitySupport.MessageBridge.port,
                ConfigManager.Config.VelocitySupport.MessageBridge.connectTimeoutMs,
                ConfigManager.Config.VelocitySupport.MessageBridge.publishTimeoutMs);
        String password = ConfigManager.Config.VelocitySupport.MessageBridge.password;
        if (password != null && !password.isEmpty())
            jedis.auth(password);
        jedis.select(ConfigManager.Config.VelocitySupport.MessageBridge.database);
        return jedis;
    }

    private String serialize(VelocityBanSyncMessage message) {
        return sanitize(message.serverId) + "|"
                + sanitize(message.playerName) + "|"
                + sanitize(message.uuid) + "|"
                + sanitize(message.checkName) + "|"
                + sanitize(message.command) + "|"
                + message.timestamp;
    }

    private VelocityBanSyncMessage deserialize(String raw) {
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 5 && parts.length != 6) {
            Logger.logConsole(LogType.WARN, "(" + Main.getInstance().getName() + ") Ignored malformed velocity sync message");
            return null;
        }
        try {
            if (parts.length == 5)
                return new VelocityBanSyncMessage(parts[0], parts[1], parts[2], parts[3], Long.parseLong(parts[4]));
            return new VelocityBanSyncMessage(parts[0], parts[1], parts[2], parts[3], parts[4], Long.parseLong(parts[5]));
        } catch (NumberFormatException exception) {
            Logger.logConsole(LogType.WARN, "(" + Main.getInstance().getName() + ") Ignored malformed velocity sync timestamp");
            return null;
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace("|", "/").replace("\n", " ").replace("\r", " ");
    }

}
