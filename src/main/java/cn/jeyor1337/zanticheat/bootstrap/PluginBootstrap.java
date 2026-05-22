package cn.jeyor1337.zanticheat.bootstrap;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.bootstrap.module.CheckModule;
import cn.jeyor1337.zanticheat.bootstrap.module.CommandModule;
import cn.jeyor1337.zanticheat.bootstrap.module.CoreModule;
import cn.jeyor1337.zanticheat.bootstrap.module.IntegrationModule;
import cn.jeyor1337.zanticheat.bootstrap.module.PluginModule;
import cn.jeyor1337.zanticheat.util.velocity.VelocitySupportService;

import java.util.Arrays;
import java.util.List;

public final class PluginBootstrap {

    private final PluginContext context;
    private final IntegrationModule integrationModule;
    private final List<PluginModule> modules;
    private int enabledModules;

    public PluginBootstrap(Main plugin) {
        this.context = new PluginContext(plugin);
        this.integrationModule = new IntegrationModule();
        this.modules = Arrays.asList(
                new CoreModule(),
                integrationModule,
                new CommandModule(),
                new CheckModule()
        );
    }

    public void enable() {
        enabledModules = 0;
        try {
            for (PluginModule module : modules) {
                module.enable(context);
                enabledModules++;
            }
        } catch (RuntimeException exception) {
            disable();
            throw exception;
        }
    }

    public void disable() {
        for (int i = enabledModules - 1; i >= 0; i--) {
            modules.get(i).disable(context);
        }
        enabledModules = 0;
    }

    public void reloadDatabaseSubsystem() {
        integrationModule.reloadDatabaseSubsystem(context);
    }

    public void reloadVelocitySupportSubsystem() {
        integrationModule.reloadVelocitySupportSubsystem(context);
    }

    public VelocitySupportService getVelocitySupportService() {
        return integrationModule.getVelocitySupportService();
    }

    public PluginContext getContext() {
        return context;
    }
}
