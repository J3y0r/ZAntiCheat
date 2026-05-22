package cn.jeyor1337.zanticheat;

import cn.jeyor1337.zanticheat.bootstrap.PluginBootstrap;
import cn.jeyor1337.zanticheat.util.velocity.VelocitySupportService;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private static final long BUFFER_DURATION_MILS = 20 * 1000L;
    private static final int PLUGIN_ID = 112053;
    private static final int STATS_ID = 12841;

    private PluginBootstrap bootstrap;

    @Override
    public void onEnable() {
        instance = this;
        bootstrap = new PluginBootstrap(this);
        bootstrap.enable();
    }

    @Override
    public void onDisable() {
        if (bootstrap == null) {
            return;
        }
        bootstrap.disable();
        bootstrap = null;
    }

    public static Main getInstance() {
        return instance;
    }

    public static long getBufferDurationMils() {
        return BUFFER_DURATION_MILS;
    }

    public static int getPluginId() {
        return PLUGIN_ID;
    }

    public static int getStatsId() {
        return STATS_ID;
    }

    public void reloadDatabaseSubsystem() {
        if (bootstrap == null) {
            return;
        }
        bootstrap.reloadDatabaseSubsystem();
    }

    public void reloadVelocitySupportSubsystem() {
        if (bootstrap == null) {
            return;
        }
        bootstrap.reloadVelocitySupportSubsystem();
    }

    public VelocitySupportService getVelocitySupportService() {
        return bootstrap == null ? null : bootstrap.getVelocitySupportService();
    }

}
