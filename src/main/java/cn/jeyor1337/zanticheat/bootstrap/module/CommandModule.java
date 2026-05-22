package cn.jeyor1337.zanticheat.bootstrap.module;

import cn.jeyor1337.zanticheat.bootstrap.PluginContext;
import cn.jeyor1337.zanticheat.command.ZACCommand;

public final class CommandModule implements PluginModule {

    @Override
    public void enable(PluginContext context) {
        ZACCommand command = new ZACCommand();
        context.registerCommand("zanticheat", command);
        context.registerCommand("report", command);
    }
}
