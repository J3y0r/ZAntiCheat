package cn.jeyor1337.zanticheat.util.player.brand;

import cn.jeyor1337.zanticheat.util.hook.server.paper.PaperUtil;
import cn.jeyor1337.zanticheat.util.reflection.ReflectionException;
import cn.jeyor1337.zanticheat.util.reflection.ReflectionUtil;
import org.bukkit.entity.Player;

public class ClientBrandRecognizer {

    public static String getClientBrand(Player player) {
        Object clientBrandObject = null;
        if (PaperUtil.isPaper()) {
            try {
                clientBrandObject = ReflectionUtil.runDeclaredMethod(player, "getClientBrandName");
            } catch (ReflectionException ignored) {
            }
        }
        String clientBrand = clientBrandObject instanceof String ? clientBrandObject.toString() : "unknown";
        if (clientBrand.length() >= 1)
            clientBrand = clientBrand.substring(0, 1).toUpperCase() + clientBrand.substring(1);
        return clientBrand;
    }

}
