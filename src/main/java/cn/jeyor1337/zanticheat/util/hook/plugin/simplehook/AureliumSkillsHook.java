package cn.jeyor1337.zanticheat.util.hook.plugin.simplehook;

import cn.jeyor1337.zanticheat.util.hook.plugin.HookUtil;
import cn.jeyor1337.zanticheat.version.VerPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AureliumSkillsHook extends HookUtil {

    private static final String PLUGIN_NAME = "AureliumSkills";

    public static boolean isPrevented(Player player) {
        if (!isPlugin(PLUGIN_NAME))
            return false;
        ItemStack itemStack = VerPlayer.getItemInMainHand(player);
        if (itemStack == null || itemStack.getAmount() == 0)
            return false;
        if (!itemStack.getType().name().endsWith("_SHOVEL"))
            return false;
        return true;
    }

}
