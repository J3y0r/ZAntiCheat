package cn.jeyor1337.zanticheat.util.hook.plugin.simplehook;

import cn.jeyor1337.zanticheat.util.hook.plugin.HookUtil;

public class EliteMobsHook extends HookUtil {

    private static final String PLUGIN_NAME = "EliteMobs";

    public static boolean isPluginInstalled() {
        return isPlugin(PLUGIN_NAME);
    }

}
