package cn.jeyor1337.zanticheat.util.hook.plugin.simplehook;

import cn.jeyor1337.zanticheat.util.hook.plugin.HookUtil;
import cn.jeyor1337.zanticheat.version.VerPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class VeinMinerHook extends HookUtil {

    private static final String PLUGIN_NAME = "VeinMiner";

    public static boolean isPrevented(Player player) {
        if (!isPlugin(PLUGIN_NAME))
            return false;
        ItemStack itemStack = VerPlayer.getItemInMainHand(player);
        if (itemStack == null || itemStack.getAmount() == 0)
            return false;
        String itemName = itemStack.getType().name();
        if (!itemName.endsWith("_AXE") && !itemName.endsWith("_HOE") &&
                !itemName.endsWith("_PICKAXE") && !itemName.endsWith("_SHOVEL") &&
                !itemName.equals("SHEARS"))
            return false;
        return true;
    }

}
