package cn.jeyor1337.zanticheat.util.violation;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.api.event.LACPunishmentEvent;
import cn.jeyor1337.zanticheat.api.event.LACViolationEvent;
import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.CheckSetting;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.player.violation.PlayerViolations;
import cn.jeyor1337.zanticheat.storage.mysql.MySqlBanRecordService;
import cn.jeyor1337.zanticheat.util.async.AsyncUtil;
import cn.jeyor1337.zanticheat.util.config.ConfigManager;
import cn.jeyor1337.zanticheat.util.config.placeholder.PlaceholderConvertor;
import cn.jeyor1337.zanticheat.util.detection.CheckUtil;
import cn.jeyor1337.zanticheat.util.detection.LeanTowards;
import cn.jeyor1337.zanticheat.util.hook.server.folia.FoliaUtil;
import cn.jeyor1337.zanticheat.util.logger.Logger;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.util.velocity.VelocitySupportService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ViolationHandler implements Listener {

    private static boolean isVerticalSetback(Player player, ZACPlayer zacPlayer, CheckSetting checkSetting) {
        if (checkSetting.name.type != CheckName.CheckType.MOVEMENT)
            return false;
        if (CheckUtil.isOnGround(player, 0.2, zacPlayer.cache, LeanTowards.TRUE))
            return false;
        if (zacPlayer.cache.history.onEvent.onGround.get(HistoryElement.FIRST).towardsTrue)
            return false;

        Set<CheckName> checks = new HashSet<>(Arrays.asList(
                CheckName.FLIGHT_A, CheckName.FLIGHT_B, CheckName.FLIGHT_C, CheckName.AIRJUMP_A
        ));
        Set<CheckName> additionalChecks = new HashSet<>(Arrays.asList(
                CheckName.SPEED_A, CheckName.SPEED_B, CheckName.SPEED_C, CheckName.JUMP_A, CheckName.JUMP_B
        ));

        boolean vSetback = checks.contains(checkSetting.name);
        boolean afterVSetback = false;
        if (additionalChecks.contains(checkSetting.name)) {
            for (CheckName checkName : checks) {
                if (zacPlayer.violations.getViolations(checkName) < Math.min(5, checkSetting.punishmentVio / 2))
                    continue;
                afterVSetback = true;
                break;
            }
        }

        return vSetback || afterVSetback;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFlag(LACViolationEvent event) {
        if (!event.getPlayer().isOnline() || event.getAcPlayer().leaveTime != 0L)
            return;
        if (ConfigManager.Config.Api.enabled && event.isCancelled())
            return;

        ZACPlayer zacPlayer = event.getAcPlayer();
        CheckSetting checkSetting = event.getCheckSettings();

        if (checkSetting.punishmentVio == zacPlayer.violations.getViolations(checkSetting.name)) {
            Bukkit.getServer().getPluginManager().callEvent(new LACPunishmentEvent(event));
            return;
        }

        if (zacPlayer.violations.getViolations(checkSetting.name) < checkSetting.punishmentVio)
            zacPlayer.violations.increaseViolations(checkSetting.name, 1);

        long currentTime = System.currentTimeMillis();
        if (ConfigManager.Config.Log.enabled) {
            if (ConfigManager.Config.Log.LogViolations.enabled &&
                    currentTime - zacPlayer.violations.violationLogTime > ConfigManager.Config.Log.LogViolations.cooldown) {
                zacPlayer.violations.violationLogTime = currentTime;
                Logger.logFile(PlaceholderConvertor.swapAll(ConfigManager.Config.Log.LogViolations.message,
                        checkSetting, event.getPlayer(), zacPlayer));
            }
        }

        if (ConfigManager.Config.Alerts.BroadcastViolations.enabled &&
                currentTime - zacPlayer.violations.violationDebugTime > ConfigManager.Config.Alerts.BroadcastViolations.cooldown) {
            zacPlayer.violations.violationDebugTime = currentTime;
            Logger.logAlert(ConfigManager.Config.Alerts.BroadcastViolations.message,
                    checkSetting, event.getPlayer(), zacPlayer);
        }

        if (ConfigManager.Config.DiscordWebhook.enabled) {
            if (ConfigManager.Config.DiscordWebhook.SendViolations.enabled &&
                    currentTime - zacPlayer.violations.violationDiscordTime > ConfigManager.Config.DiscordWebhook.SendViolations.cooldown) {
                zacPlayer.violations.violationDiscordTime = currentTime;
                Logger.logDiscord(PlaceholderConvertor.swapAll(ConfigManager.Config.DiscordWebhook.SendViolations.message,
                        checkSetting, event.getPlayer(), zacPlayer), false);
            }
        }

        if (checkSetting.setback && zacPlayer.violations.getViolations(checkSetting.name) >= checkSetting.setbackVio &&
                event.getCancellable() != null) {
            if (!isVerticalSetback(event.getPlayer(), zacPlayer, checkSetting)) {
                event.getCancellable().setCancelled(true);
            } else {
                Location location = event.getPlayer().getLocation();
                boolean isDownBlocks = true;
                for (int i = 1; i <= 25; i++) {
                    boolean cancel = false;
                    Set<Block> blocks = new HashSet<>();
                    if (isDownBlocks || i == 25) {
                        blocks.addAll(new HashSet<>(CheckUtil.getDownBlocks(event.getPlayer(), location, 0.05)));
                    } else {
                        Block block = AsyncUtil.getBlock(event.getPlayer().getLocation());
                        if (block != null) {
                            blocks.add(block);
                            blocks.add(block.getRelative(BlockFace.DOWN));
                        }
                    }
                    isDownBlocks = !isDownBlocks;
                    for (Block block : blocks)
                        if (!CheckUtil.isActuallyPassable(block) || i == 25) {
                            cancel = true;
                            break;
                        }
                    if (cancel) {
                        for (Block block : CheckUtil.getWithinBlocks(event.getPlayer())) {
                            if (!CheckUtil.isActuallyPassable(block)) {
                                FoliaUtil.teleportPlayer(event.getPlayer(), location.add(0, 1 - (location.getY() % 1), 0));
                                break;
                            }
                        }
                        boolean slab = true;
                        for (Block block : CheckUtil.getDownBlocks(event.getPlayer(), 0.1)) {
                            if (!block.getType().name().endsWith("_SLAB")) {
                                slab = false;
                                break;
                            }
                        }
                        if (slab) FoliaUtil.teleportPlayer(event.getPlayer(), location.subtract(0, 0.5, 0));
                        break;
                    }
                    FoliaUtil.teleportPlayer(event.getPlayer(), location.subtract(0, 1, 0));
                }
            }
        }

        if (checkSetting.punishmentVio == zacPlayer.violations.getViolations(checkSetting.name))
            Bukkit.getServer().getPluginManager().callEvent(new LACPunishmentEvent(event));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPunishment(LACPunishmentEvent event) {
        if (!event.getPlayer().isOnline() || event.getAcPlayer().leaveTime != 0L)
            return;
        if (ConfigManager.Config.Api.enabled && event.isCancelled())
            return;

        ZACPlayer zacPlayer = event.getAcPlayer();
        CheckSetting checkSetting = event.getCheckSettings();
        long currentTime = System.currentTimeMillis();
        if (ConfigManager.Config.Log.enabled) {
            if (ConfigManager.Config.Log.LogPunishments.enabled &&
                    currentTime - zacPlayer.violations.punishmentLogTime > ConfigManager.Config.Log.LogPunishments.cooldown) {
                zacPlayer.violations.punishmentLogTime = currentTime;
                Logger.logFile(PlaceholderConvertor.swapAll(ConfigManager.Config.Log.LogPunishments.message,
                        checkSetting, event.getPlayer(), zacPlayer));
            }
        }

        if (ConfigManager.Config.Alerts.BroadcastPunishments.enabled &&
                currentTime - zacPlayer.violations.punishmentDebugTime > ConfigManager.Config.Alerts.BroadcastPunishments.cooldown) {
            zacPlayer.violations.punishmentDebugTime = currentTime;
            Logger.logAlert(ConfigManager.Config.Alerts.BroadcastPunishments.message,
                    checkSetting, event.getPlayer(), zacPlayer);
        }

        if (ConfigManager.Config.DiscordWebhook.enabled) {
            if (ConfigManager.Config.DiscordWebhook.SendPunishments.enabled &&
                    currentTime - zacPlayer.violations.punishmentDiscordTime > ConfigManager.Config.DiscordWebhook.SendPunishments.cooldown) {
                zacPlayer.violations.punishmentDiscordTime = currentTime;
                Logger.logDiscord(PlaceholderConvertor.swapAll(ConfigManager.Config.DiscordWebhook.SendPunishments.message,
                        checkSetting, event.getPlayer(), zacPlayer), true);
            }
        }

        if (checkSetting.punishable && !shouldRoutePunishmentToVelocity(checkSetting) &&
                checkSetting.punishmentCommands != null && !checkSetting.punishmentCommands.isEmpty()) {
            Scheduler.runTask(false, () -> {
                for (String command : checkSetting.punishmentCommands)
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            PlaceholderConvertor.colorize(PlaceholderConvertor.swapAll(command, checkSetting, event.getPlayer(), zacPlayer), true));
            });
        }

        zacPlayer.violations = new PlayerViolations();
    }

    private static boolean shouldRoutePunishmentToVelocity(CheckSetting checkSetting) {
        if (checkSetting.punishmentCommands == null || checkSetting.punishmentCommands.isEmpty())
            return false;
        if (!MySqlBanRecordService.shouldStorePunishment(checkSetting.punishmentCommands))
            return false;
        VelocitySupportService velocitySupportService = Main.getInstance().getVelocitySupportService();
        return velocitySupportService != null && velocitySupportService.isEnabled();
    }

}
