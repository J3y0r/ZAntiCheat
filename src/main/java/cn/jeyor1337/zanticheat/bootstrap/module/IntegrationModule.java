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
        unregisterBanRecordPersistenceListener(context);
        shutdownVelocitySupportSubsystem(context);
        shutdownDatabaseSubsystem(context);
    }

    public void reloadDatabaseSubsystem(PluginContext context) {
        shutdownDatabaseSubsystem(context);
        if (!ConfigManager.Config.Database.enabled) {
            refreshBanRecordPersistenceListener(context);
            return;
        }

        mySqlBanRecordService = new MySqlBanRecordService();
        if (!mySqlBanRecordService.initialize()) {
            mySqlBanRecordService = null;
            refreshBanRecordPersistenceListener(context);
            return;
        }

        context.putService(MySqlBanRecordService.class, mySqlBanRecordService);
        refreshBanRecordPersistenceListener(context);
    }

    public void reloadVelocitySupportSubsystem(PluginContext context) {
        shutdownVelocitySupportSubsystem(context);
        if (!ConfigManager.Config.VelocitySupport.enabled) {
            refreshBanRecordPersistenceListener(context);
            return;
        }

        velocitySupportService = new VelocitySupportService();
        if (!velocitySupportService.initialize()) {
            velocitySupportService = null;
            refreshBanRecordPersistenceListener(context);
            return;
        }

        context.putService(VelocitySupportService.class, velocitySupportService);
        refreshBanRecordPersistenceListener(context);
    }

    public VelocitySupportService getVelocitySupportService() {
        return velocitySupportService;
    }

    private void shutdownDatabaseSubsystem(PluginContext context) {
        if (mySqlBanRecordService != null) {
            mySqlBanRecordService.close();
            mySqlBanRecordService = null;
            context.putService(MySqlBanRecordService.class, null);
        }
    }

    private void shutdownVelocitySupportSubsystem(PluginContext context) {
        if (velocitySupportService == null) {
            return;
        }
        velocitySupportService.shutdown();
        velocitySupportService = null;
        context.putService(VelocitySupportService.class, null);
    }

    private void refreshBanRecordPersistenceListener(PluginContext context) {
        unregisterBanRecordPersistenceListener(context);
        if (mySqlBanRecordService == null && velocitySupportService == null) {
            return;
        }
        banRecordPersistenceListener = new BanRecordPersistenceListener(mySqlBanRecordService);
        context.registerListener(banRecordPersistenceListener);
    }

    private void unregisterBanRecordPersistenceListener(PluginContext context) {
        if (banRecordPersistenceListener == null) {
            return;
        }
        context.unregisterListener(banRecordPersistenceListener);
        banRecordPersistenceListener = null;
    }
}
