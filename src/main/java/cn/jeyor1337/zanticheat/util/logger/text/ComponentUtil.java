package cn.jeyor1337.zanticheat.util.logger.text;

import cn.jeyor1337.zanticheat.check.CheckSetting;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.async.AsyncUtil;
import cn.jeyor1337.zanticheat.util.config.ConfigManager;
import cn.jeyor1337.zanticheat.util.config.placeholder.PlaceholderConvertor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ComponentUtil {

    public static List<String> generateLines(String text, CheckSetting checkSetting, Player violator, ZACPlayer violatorZacPlayer) {
        List<String> lines = Collections.synchronizedList(new LinkedList<>());
        boolean punishment = checkSetting.punishmentVio == violatorZacPlayer.violations.getViolations(checkSetting.name);
        Location location = violator.getLocation();

        //message
        lines.add(PlaceholderConvertor.swapAll(text, checkSetting, violator, violatorZacPlayer));

        //hover message
        String hoverMessage = punishment ? ConfigManager.Config.Alerts.BroadcastPunishments.onHover :
                ConfigManager.Config.Alerts.BroadcastViolations.onHover;
        if (!hoverMessage.isEmpty()) {
            hoverMessage = PlaceholderConvertor.swapAll(
                    PlaceholderConvertor.swapCoordinates(hoverMessage, violator, "#0"),
                    checkSetting, violator, violatorZacPlayer);
        }
        lines.add(hoverMessage);

        //click message
        String clickMessage = punishment ? ConfigManager.Config.Alerts.BroadcastPunishments.onClick :
                ConfigManager.Config.Alerts.BroadcastViolations.onClick;
        if (!clickMessage.isEmpty()) {
            World world = AsyncUtil.getWorld(violator);
            if (world == null) world = violator.getWorld();
            clickMessage = PlaceholderConvertor.swapPlayer(clickMessage.replaceAll(
                            "%teleport-location%", world.getName() + " " +
                                    location.getX() + " " + location.getY() + " " + location.getZ() + " " +
                                    location.getYaw() + " " + location.getPitch()),
                    violator);
        }
        lines.add(clickMessage);

        return lines;
    }

}
