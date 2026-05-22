package cn.jeyor1337.zanticheat.bootstrap.module;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.bootstrap.PluginContext;
import cn.jeyor1337.zanticheat.event.ZACEventCaller;
import cn.jeyor1337.zanticheat.listener.invalidping.InvalidPingListener;
import cn.jeyor1337.zanticheat.listener.unloadedchunk.UnloadedChunkListener;
import cn.jeyor1337.zanticheat.player.ZACPlayerListener;
import cn.jeyor1337.zanticheat.util.api.ApiUtil;
import cn.jeyor1337.zanticheat.util.config.ConfigManager;
import cn.jeyor1337.zanticheat.util.hook.server.folia.FoliaUtil;
import cn.jeyor1337.zanticheat.util.logger.Logger;
import cn.jeyor1337.zanticheat.util.npc.ExternalNPCUtil;
import cn.jeyor1337.zanticheat.util.player.connectionstability.ConnectionStabilityListener;
import cn.jeyor1337.zanticheat.util.player.cps.CPSListener;
import cn.jeyor1337.zanticheat.util.tps.TPSCalculator;
import cn.jeyor1337.zanticheat.util.updater.Updater;
import cn.jeyor1337.zanticheat.util.violation.ViolationHandler;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;

public final class CoreModule implements PluginModule {

    private ZACEventCaller eventCaller;

    @Override
    public void enable(PluginContext context) {
        FoliaUtil.loadFoliaUtil();
        ConfigManager.loadConfig();

        Buffer.loadBufferCleaner(Main.getBufferDurationMils());
        TPSCalculator.loadTPSCalculator();
        Logger.logFile("");
        ApiUtil.setApiInstance();

        ZACPlayerListener.loadZACPlayerListener();
        context.registerListener(new ZACPlayerListener());

        ExternalNPCUtil.loadExternalNPCUtil();
        context.registerListener(new ViolationHandler());

        eventCaller = new ZACEventCaller();
        context.registerListener(eventCaller);

        UnloadedChunkListener.handleUnloadedChunks();
        InvalidPingListener.limitMaxPing();

        CPSListener.loadCpsCalculatorOnReload();
        CPSListener.loadCpsCalculator();
        context.registerListener(new CPSListener());

        ConnectionStabilityListener.loadConnectionCalculatorOnReload();
        ConnectionStabilityListener.loadConnectionCalculator();
        context.registerListener(new ConnectionStabilityListener());

        Updater.loadUpdateChecker();
        context.registerListener(new Updater());
    }

    @Override
    public void disable(PluginContext context) {
        if (eventCaller == null) {
            return;
        }
        eventCaller.close();
        eventCaller = null;
    }
}
