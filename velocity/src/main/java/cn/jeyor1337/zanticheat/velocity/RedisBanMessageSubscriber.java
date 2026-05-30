package cn.jeyor1337.zanticheat.velocity;

import org.slf4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;

import java.io.Closeable;
import java.util.function.Consumer;

public final class RedisBanMessageSubscriber implements Closeable {

    private final VelocitySyncConfig config;
    private final Logger logger;
    private final Consumer<VelocityBanSyncMessage> consumer;
    private volatile boolean closed = true;
    private volatile Thread subscriberThread;
    private volatile JedisPubSub jedisPubSub;

    public RedisBanMessageSubscriber(VelocitySyncConfig config, Logger logger, Consumer<VelocityBanSyncMessage> consumer) {
        this.config = config;
        this.logger = logger;
        this.consumer = consumer;
    }

    public boolean start() {
        close();
        try (Jedis jedis = createJedis()) {
            jedis.ping();
        } catch (JedisException exception) {
            logger.error("Failed to connect ZAntiCheat Velocity sync to Redis: {}", exception.getMessage());
            return false;
        }

        closed = false;
        subscriberThread = new Thread(this::runSubscriber, "zac-velocity-ban-sync");
        subscriberThread.setDaemon(true);
        subscriberThread.start();
        return true;
    }

    private void runSubscriber() {
        while (!closed && !Thread.currentThread().isInterrupted()) {
            JedisPubSub pubSub = new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    VelocityBanSyncMessage syncMessage = VelocityBanSyncMessage.deserialize(message);
                    if (syncMessage == null) {
                        logger.warn("Ignored malformed ZAntiCheat Velocity sync message.");
                        return;
                    }
                    consumer.accept(syncMessage);
                }
            };
            jedisPubSub = pubSub;
            try (Jedis jedis = createJedis()) {
                jedis.subscribe(pubSub, config.redisChannel);
            } catch (JedisException exception) {
                if (!closed) {
                    logger.error("ZAntiCheat Velocity sync Redis subscription failed: {}", exception.getMessage());
                    sleepBeforeReconnect();
                }
            } finally {
                jedisPubSub = null;
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        JedisPubSub pubSub = jedisPubSub;
        if (pubSub != null) {
            pubSub.unsubscribe();
        }
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
        Jedis jedis = new Jedis(config.redisHost, config.redisPort,
                config.redisConnectTimeoutMs, config.redisPublishTimeoutMs);
        if (config.redisPassword != null && !config.redisPassword.isEmpty()) {
            jedis.auth(config.redisPassword);
        }
        jedis.select(config.redisDatabase);
        return jedis;
    }

    private void sleepBeforeReconnect() {
        try {
            Thread.sleep(Math.max(config.redisConnectTimeoutMs, 1000));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
