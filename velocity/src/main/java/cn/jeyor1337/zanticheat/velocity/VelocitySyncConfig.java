package cn.jeyor1337.zanticheat.velocity;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class VelocitySyncConfig {

    private static final String DEFAULT_CONFIG =
            "# ZAntiCheat Velocity Sync\n"
                    + "# Install this jar on the Velocity proxy. Bukkit/Paper sub-servers still publish messages through Redis.\n"
                    + "enabled=true\n"
                    + "server-id=velocity\n"
                    + "ignore-own-server-id=true\n"
                    + "log-received-messages=true\n"
                    + "# Fallback command used only for old 5-field sync messages that do not carry the original sub-server command.\n"
                    + "command=ban %name%\n"
                    + "redis.host=127.0.0.1\n"
                    + "redis.port=6379\n"
                    + "redis.password=\n"
                    + "redis.database=0\n"
                    + "redis.channel=zanticheat:velocity:ban\n"
                    + "redis.connect-timeout-ms=3000\n"
                    + "redis.publish-timeout-ms=3000\n";

    public final boolean enabled;
    public final String serverId;
    public final boolean ignoreOwnServerId;
    public final boolean logReceivedMessages;
    public final String command;
    public final String redisHost;
    public final int redisPort;
    public final String redisPassword;
    public final int redisDatabase;
    public final String redisChannel;
    public final int redisConnectTimeoutMs;
    public final int redisPublishTimeoutMs;

    private VelocitySyncConfig(Properties properties) {
        enabled = getBoolean(properties, "enabled", true);
        serverId = getString(properties, "server-id", "velocity");
        ignoreOwnServerId = getBoolean(properties, "ignore-own-server-id", true);
        logReceivedMessages = getBoolean(properties, "log-received-messages", true);
        command = getString(properties, "command", "ban %name% Cheating detected by ZAC");
        redisHost = getString(properties, "redis.host", "127.0.0.1");
        redisPort = getInt(properties, "redis.port", 6379);
        redisPassword = getString(properties, "redis.password", "");
        redisDatabase = getInt(properties, "redis.database", 0);
        redisChannel = getString(properties, "redis.channel", "zanticheat:velocity:ban");
        redisConnectTimeoutMs = getInt(properties, "redis.connect-timeout-ms", 3000);
        redisPublishTimeoutMs = getInt(properties, "redis.publish-timeout-ms", 3000);
    }

    public static VelocitySyncConfig load(Path dataDirectory, Logger logger) {
        Path configPath = dataDirectory.resolve("config.properties");
        try {
            Files.createDirectories(dataDirectory);
            if (!Files.exists(configPath)) {
                Files.write(configPath, DEFAULT_CONFIG.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException exception) {
            logger.error("Failed to create ZAntiCheat Velocity sync config.", exception);
        }

        Properties properties = new Properties();
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException exception) {
                logger.error("Failed to read ZAntiCheat Velocity sync config, using defaults.", exception);
            }
        }
        return new VelocitySyncConfig(properties);
    }

    private static String getString(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private static boolean getBoolean(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private static int getInt(Properties properties, String key, int fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
