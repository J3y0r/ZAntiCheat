package cn.jeyor1337.zanticheat.util.logger;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.check.CheckSetting;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.config.ConfigManager;
import cn.jeyor1337.zanticheat.util.config.placeholder.Placeholder;
import cn.jeyor1337.zanticheat.util.config.placeholder.PlaceholderConvertor;
import cn.jeyor1337.zanticheat.util.logger.text.ColorUtil;
import cn.jeyor1337.zanticheat.util.logger.text.ComponentUtil;
import cn.jeyor1337.zanticheat.util.permission.ACPermission;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.VerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;

public class Logger {

    public static void logConsole(LogType type, String message) {
        switch (type) {
            case INFO:
                Bukkit.getLogger().log(Level.INFO, message);
                break;
            case WARN:
                Bukkit.getLogger().log(Level.WARNING, message);
                break;
            case ERROR:
                Bukkit.getLogger().log(Level.SEVERE, message);
                break;
        }
    }

    public static void logAlert(String message, CheckSetting checkSetting, Player violator, ZACPlayer violatorZacPlayer) {
        Bukkit.getConsoleSender().sendMessage(
                ColorUtil.colorize(PlaceholderConvertor.swapAll(message, checkSetting, violator, violatorZacPlayer),
                        false));
        List<String> lines = ComponentUtil.generateLines(message, checkSetting, violator, violatorZacPlayer);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission(ACPermission.ALERTS_NOTIFY) && !player.hasPermission(ACPermission.ALERTS))
                continue;
            ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
            if (!zacPlayer.cache.alerts)
                continue;
            VerPlayer.sendHoverMessage(player, lines, ConfigManager.Config.Messages.hexColorCodes);
        }
    }

    public static void logFile(String message) {
        String logFilePath = Main.getInstance().getDataFolder().getPath() + "/" +
                PlaceholderConvertor.swapSome(ConfigManager.Config.Log.file, Placeholder.DATA);

        String[] filePathParts = logFilePath.split("/");
        Scheduler.runTaskAsynchronously(false, () -> {
            if (filePathParts.length != 1)
                new File(logFilePath.substring(0, logFilePath.length() - 1 - filePathParts[filePathParts.length - 1].length())).mkdirs();
            File logFile = new File(logFilePath);
            if (!logFile.exists()) {
                try {
                    logFile.createNewFile();
                } catch (IOException e) {
                    Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") " + e.getMessage());
                }
            }
            if (message == null || message.isEmpty())
                return;
            String finalMessage = ColorUtil.removeColors(message);
            try {
                FileWriter fileWriter = new FileWriter(logFile, true);
                fileWriter.write(finalMessage + "\n");
                fileWriter.close();
            } catch (IOException e) {
                Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") " + e.getMessage());
            }
        });
    }

    public static void logDiscord(String message, boolean punishment) {
        String webhookUrl = punishment ? ConfigManager.Config.DiscordWebhook.SendPunishments.webhookUrl :
                ConfigManager.Config.DiscordWebhook.SendViolations.webhookUrl;
        if (!webhookUrl.startsWith("https://discord.com/api/webhooks/"))
            return;
        message = ColorUtil.removeColors(message);
        message = "{\"content\":\"" + message + "\"}";
        byte[] bytes = message.getBytes();
        Scheduler.runTaskAsynchronously(false, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.addRequestProperty("Content-Type", "application/json");
                connection.addRequestProperty("User-Agent", "Java-DiscordWebhook");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestMethod("POST");

                OutputStream stream = connection.getOutputStream();
                stream.write(bytes);
                stream.flush();
                stream.close();

                connection.getInputStream().close();
                connection.disconnect();
            } catch (IOException e) {
                if (e.getMessage() == null || !e.getMessage().contains("429"))
                    Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") " + e.getMessage());
            }
        });
    }

}
