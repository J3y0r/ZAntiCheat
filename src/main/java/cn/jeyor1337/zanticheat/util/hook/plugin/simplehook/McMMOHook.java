package cn.jeyor1337.zanticheat.util.hook.plugin.simplehook;

import cn.jeyor1337.zanticheat.util.hook.plugin.HookUtil;
import org.bukkit.Material;

public class McMMOHook extends HookUtil {

    private static final String PLUGIN_NAME = "mcMMO";

    public static boolean isPrevented(Material material) {
        if (!isPlugin(PLUGIN_NAME))
            return false;
        String name = material.name();
        if (name.endsWith("_LOG") || name.endsWith("_LEAVES"))
            return true;
        return false;
    }

}
