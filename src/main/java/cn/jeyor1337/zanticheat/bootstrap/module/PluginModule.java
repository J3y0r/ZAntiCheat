package cn.jeyor1337.zanticheat.bootstrap.module;

import cn.jeyor1337.zanticheat.bootstrap.PluginContext;

public interface PluginModule {

    void enable(PluginContext context);

    default void disable(PluginContext context) {
    }
}
