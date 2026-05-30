package cn.jeyor1337.zanticheat.bootstrap.module;

import cn.jeyor1337.zanticheat.bootstrap.PluginContext;
import cn.jeyor1337.zanticheat.listener.punishment.BanRecordPersistenceListener;
import cn.jeyor1337.zanticheat.storage.mysql.MySqlBanRecordService;
import cn.jeyor1337.zanticheat.util.config.ConfigManager;
import cn.jeyor1337.zanticheat.util.velocity.VelocitySupportService;

public final class IntegrationModule implements PluginModule {

    private MySqlBanRecordService mySqlBanRecordService;
    private BanRecordPersistenceListener banRecordPersistenceListener;
    private VelocitySupportService velocitySupportService;

    @Override
    public void enable(PluginContext context) {
        reloadDatabaseSubsystem(context);
        reloadVelocitySupportSubsystem(context);
    }

    @Override
    public void disable(PluginContext context) {
        shutdownVelocitySupportSubsystem();
        shutdownDatabaseSubsystem(context);
    }

    public void reloadDatabaseSubsystem(PluginContext context) {
        shutdownDatabaseSubsystem(context);
        if (!ConfigManager.Config.Database.enabled) {
            return;
        }

        mySqlBanRecordService = new MySqlBanRecordService();
        if (!mySqlBanRecordService.initialize()) {
            mySqlBanRecordService = null;
            return;
        }

        banRecordPersistenceListener = new BanRecordPersistenceListener(mySqlBanRecordService);
        context.registerListener(banRecordPersistenceListener);
        context.putService(MySqlBanRecordService.class, mySqlBanRecordService);
    }

    public void reloadVelocitySupportSubsystem(PluginContext context) {
        shutdownVelocitySupportSubsystem();
        if (!ConfigManager.Config.VelocitySupport.enabled) {
            return;
        }

        velocitySupportService = new VelocitySupportService();
        if (!velocitySupportService.initialize()) {
            velocitySupportService = null;
            return;
        }

        context.putService(VelocitySupportService.class, velocitySupportService);
    }

    public VelocitySupportService getVelocitySupportService() {
        return velocitySupportService;
    }

    private void shutdownDatabaseSubsystem(PluginContext context) {
        if (banRecordPersistenceListener != null) {
            context.unregisterListener(banRecordPersistenceListener);
            banRecordPersistenceListener = null;
        }
        if (mySqlBanRecordService != null) {
            mySqlBanRecordService.close();
            mySqlBanRecordService = null;
            context.putService(MySqlBanRecordService.class, null);
        }
    }

    private void shutdownVelocitySupportSubsystem() {
        if (velocitySupportService == null) {
            return;
        }
        velocitySupportService.shutdown();
        velocitySupportService = null;
    }
}
