package cn.jeyor1337.zanticheat.util.hook.plugin.simplehook;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.util.hook.plugin.HookUtil;
import cn.jeyor1337.zanticheat.version.VerPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ExecutableItemsHook extends HookUtil {

    private static final String PLUGIN_NAME = "ExecutableItems";

    public static boolean isPrevented(CheckName checkName, Player player) {
        if (!isPlugin(PLUGIN_NAME))
            return false;

        ItemStack itemStack = VerPlayer.getItemInMainHand(player);
        if (itemStack == null || itemStack.getAmount() == 0 || itemStack.getItemMeta() == null)
            return false;

        String itemName = itemStack.getType().name();

        boolean flag = false;
        if (checkName.type == CheckName.CheckType.COMBAT &&
                (itemName.endsWith("_SWORD") || itemName.endsWith("_AXE")))
            flag = true;
        if (checkName.type == CheckName.CheckType.INTERACTION &&
                (itemName.endsWith("_PICKAXE") || itemName.endsWith("_SHOVEL") || itemName.endsWith("_AXE")))
            flag = true;
        if (!flag)
            return false;

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta.hasDisplayName() && itemMeta.getDisplayName().contains("§"))
            return true;
        if (itemMeta.hasLore() && itemMeta.getLore() != null) {
            for (String line : itemMeta.getLore())
                if (!line.isEmpty())
                    return true;
        }
        return false;
    }

}
