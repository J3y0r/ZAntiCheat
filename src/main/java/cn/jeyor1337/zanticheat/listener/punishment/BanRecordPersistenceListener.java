package cn.jeyor1337.zanticheat.listener.punishment;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.api.event.LACPunishmentEvent;
import cn.jeyor1337.zanticheat.storage.mysql.MySqlBanRecordService;
import cn.jeyor1337.zanticheat.util.config.ConfigManager;
import cn.jeyor1337.zanticheat.util.logger.LogType;
import cn.jeyor1337.zanticheat.util.logger.Logger;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.util.velocity.VelocitySupportService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class BanRecordPersistenceListener implements Listener {

    private final MySqlBanRecordService mySqlBanRecordService;

    public BanRecordPersistenceListener(MySqlBanRecordService mySqlBanRecordService) {
        this.mySqlBanRecordService = mySqlBanRecordService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPunishment(LACPunishmentEvent event) {
        if (event.isCancelled())
            return;
        if (!MySqlBanRecordService.shouldStorePunishment(event.getCheckSettings().punishmentCommands))
            return;
        Scheduler.runTaskAsynchronously(false, () -> {
            boolean saved = !ConfigManager.Config.Database.enabled ||
                    mySqlBanRecordService != null && mySqlBanRecordService.saveBannedPlayer(event.getPlayer().getUniqueId());
            if (!saved)
                return;
            Main main = Main.getInstance();
            VelocitySupportService velocitySupportService = main.getVelocitySupportService();
            if (velocitySupportService == null)
                return;
            boolean published = velocitySupportService.publishBan(event.getPlayer().getName(),
                    event.getPlayer().getUniqueId(), event.getCheckSettings());
            if (!published && ConfigManager.Config.VelocitySupport.enabled) {
                Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") Failed to publish velocity ban sync for "
                        + event.getPlayer().getUniqueId());
            }
        });
    }
}
