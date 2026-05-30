package cn.jeyor1337.zanticheat.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.nio.file.Path;

@Plugin(
        id = "zanticheat-velocity",
        name = "ZAntiCheat Velocity Sync",
        version = "0.0.3",
        authors = {"Jeyor1337"}
)
public final class ZACVelocitySyncPlugin {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private RedisBanMessageSubscriber subscriber;
    private VelocitySyncConfig config;

    @Inject
    public ZACVelocitySyncPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        config = VelocitySyncConfig.load(dataDirectory, logger);
        if (!config.enabled) {
            logger.info("ZAntiCheat Velocity sync is disabled.");
            return;
        }

        subscriber = new RedisBanMessageSubscriber(config, logger, this::handleBanMessage);
        if (subscriber.start()) {
            logger.info("ZAntiCheat Velocity sync is listening on Redis channel {}.", config.redisChannel);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (subscriber != null) {
            subscriber.close();
            subscriber = null;
        }
    }

    private void handleBanMessage(VelocityBanSyncMessage message) {
        if (message.serverId == null || message.serverId.trim().isEmpty()) {
            return;
        }
        if (config.ignoreOwnServerId && message.serverId.equalsIgnoreCase(config.serverId)) {
            return;
        }

        proxy.getScheduler().buildTask(this, () -> executeBanCommand(message)).schedule();
    }

    private void executeBanCommand(VelocityBanSyncMessage message) {
        String command = safe(message.command).trim();
        if (command.isEmpty()) {
            command = config.command
                    .replace("%name%", safe(message.playerName))
                    .replace("%uuid%", safe(message.uuid))
                    .replace("%check%", safe(message.checkName))
                    .replace("%source-server%", safe(message.serverId))
                    .replace("%timestamp%", Long.toString(message.timestamp))
                    .trim();
        }

        if (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        if (command.isEmpty()) {
            logger.warn("Ignored empty ZAntiCheat Velocity sync command for {}.", message.playerName);
            return;
        }

        if (config.logReceivedMessages) {
            logger.info("Executing synced ZAntiCheat ban command from {} for {} ({})",
                    message.serverId, message.playerName, message.checkName);
        }

        final String commandLine = command;
        proxy.getCommandManager().executeAsync(proxy.getConsoleCommandSource(), commandLine)
                .whenComplete((success, throwable) -> {
                    if (throwable != null) {
                        logger.error("Failed to execute synced ZAntiCheat command: {}", commandLine, throwable);
                        return;
                    }
                    if (!success) {
                        logger.warn("Synced ZAntiCheat command was not handled: {}", commandLine);
                    }
                });
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
