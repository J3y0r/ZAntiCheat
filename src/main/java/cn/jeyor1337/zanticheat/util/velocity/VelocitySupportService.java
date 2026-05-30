package cn.jeyor1337.zanticheat.util.velocity;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.check.CheckSetting;
import cn.jeyor1337.zanticheat.util.config.ConfigManager;
import cn.jeyor1337.zanticheat.util.config.placeholder.PlaceholderConvertor;
import cn.jeyor1337.zanticheat.util.logger.LogType;
import cn.jeyor1337.zanticheat.util.logger.Logger;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import org.bukkit.Bukkit;

import java.util.Locale;
import java.util.UUID;

public class VelocitySupportService {

    private VelocityMessageBridge messageBridge;
    private VelocitySyncMode syncMode = VelocitySyncMode.NONE;
    private boolean enabled;

    public boolean initialize() {
        shutdown();
        if (!ConfigManager.Config.VelocitySupport.enabled)
            return false;
        if (!isConfigured())
            return false;
        syncMode = loadSyncMode();
        if (syncMode == VelocitySyncMode.NONE)
            return false;
        messageBridge = new RedisVelocityMessageBridge();
        if (!messageBridge.initialize()) {
            messageBridge = null;
            enabled = false;
            return false;
        }
        enabled = true;
        if (ConfigManager.Config.VelocitySupport.mainServer && syncMode == VelocitySyncMode.PUBLISH_AND_CONSUME)
            messageBridge.subscribe(this::handleIncomingMessage);
        Logger.logConsole(LogType.INFO, "(" + Main.getInstance().getName() + ") Velocity support initialized for server "
                + ConfigManager.Config.VelocitySupport.serverId);
        return true;
    }

    public void shutdown() {
        enabled = false;
        syncMode = VelocitySyncMode.NONE;
        if (messageBridge != null) {
            messageBridge.close();
            messageBridge = null;
        }
    }

    public boolean publishBan(String playerName, UUID uuid, CheckSetting checkSetting) {
        return publishBan(playerName, uuid, checkSetting, "");
    }

    public boolean publishBan(String playerName, UUID uuid, CheckSetting checkSetting, String command) {
        if (!enabled || messageBridge == null || syncMode == VelocitySyncMode.NONE)
            return false;
        VelocityBanSyncMessage message = new VelocityBanSyncMessage(
                ConfigManager.Config.VelocitySupport.serverId,
                playerName,
                uuid != null ? uuid.toString() : "",
                checkSetting.name.title,
                command,
                System.currentTimeMillis()
        );
        return messageBridge.publish(message);
    }

    public boolean isEnabled() {
        return enabled && messageBridge != null && syncMode != VelocitySyncMode.NONE;
    }

    private void handleIncomingMessage(VelocityBanSyncMessage message) {
        if (!enabled || !ConfigManager.Config.VelocitySupport.mainServer)
            return;
        if (message == null || message.serverId == null || message.serverId.isEmpty())
            return;
        if (message.serverId.equalsIgnoreCase(ConfigManager.Config.VelocitySupport.serverId))
            return;
        Scheduler.runTask(false, () -> {
            String command = ConfigManager.Config.VelocitySupport.mainServerBanCommand
                    .replace("%name%", message.playerName)
                    .replace("%uuid%", message.uuid)
                    .replace("%check%", message.checkName)
                    .replace("%source-server%", message.serverId);
            command = PlaceholderConvertor.colorize(command, true);
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } catch (Exception exception) {
                Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") Failed to execute main server ban command: " + exception.getMessage());
            }
        });
    }

    private boolean isConfigured() {
        if (isBlank(ConfigManager.Config.VelocitySupport.serverId)) {
            Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") Velocity support requires velocity-support.server-id");
            return false;
        }
        if (isBlank(ConfigManager.Config.VelocitySupport.MessageBridge.type)
                || isBlank(ConfigManager.Config.VelocitySupport.MessageBridge.channel)
                || isBlank(ConfigManager.Config.VelocitySupport.MessageBridge.host)
                || ConfigManager.Config.VelocitySupport.MessageBridge.port <= 0) {
            Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") Velocity support message bridge configuration is incomplete");
            return false;
        }
        if (ConfigManager.Config.VelocitySupport.mainServer && isBlank(ConfigManager.Config.VelocitySupport.mainServerBanCommand)) {
            Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") Velocity support requires velocity-support.main-server-ban-command on main server");
            return false;
        }
        return true;
    }

    private VelocitySyncMode loadSyncMode() {
        if (ConfigManager.Config.VelocitySupport.syncMode == null)
            return VelocitySyncMode.NONE;
        try {
            return VelocitySyncMode.valueOf(ConfigManager.Config.VelocitySupport.syncMode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") Unsupported velocity support sync mode: "
                    + ConfigManager.Config.VelocitySupport.syncMode);
            return VelocitySyncMode.NONE;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
