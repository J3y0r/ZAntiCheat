package cn.jeyor1337.zanticheat.check.checks.interaction.fastplace;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.interaction.InteractionCheck;
import cn.jeyor1337.zanticheat.event.playerplaceblock.ZACAsyncPlayerPlaceBlockEvent;
import cn.jeyor1337.zanticheat.event.playerplaceblock.ZACPlayerPlaceBlockEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Placement interval
 */
public class FastPlaceA extends InteractionCheck implements Listener {
    public FastPlaceA() {
        super(CheckName.FASTPZACE_A);
    }

    @EventHandler
    public void onAsyncBlockPlace(ZACAsyncPlayerPlaceBlockEvent event) {
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true)) {
            buffer.put("asyncFlag", false);
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (!buffer.isExists("lastAsyncPlace")) {
            buffer.put("lastAsyncPlace", currentTime);
            buffer.put("asyncFlag", false);
            return;
        }

        long interval = currentTime - buffer.getLong("lastAsyncPlace");
        if (interval > 4L) {
            buffer.put("lastAsyncPlace", currentTime);
            return;
        }

        buffer.put("lastAsyncPlace", currentTime);
        buffer.put("asyncFlag", true);
    }

    @EventHandler
    public void onBlockPlace(ZACPlayerPlaceBlockEvent event) {
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);
        if (!buffer.getBoolean("asyncFlag"))
            return;

        ZACPlayer zacPlayer = event.getZacPlayer();
        if (!isCheckAllowed(player, zacPlayer))
            return;

        long currentTime = System.currentTimeMillis();
        if (!buffer.isExists("lastPlace")) {
            buffer.put("lastPlace", currentTime);
            return;
        }

        long interval = currentTime - buffer.getLong("lastPlace");
        if (interval > 3L) {
            buffer.put("lastPlace", currentTime);
            return;
        }

        if (currentTime - buffer.getLong("lastFlag1") > 8 * 1000) {
            buffer.put("lastFlag1", System.currentTimeMillis());
            buffer.put("lastPlace1", currentTime);
            return;
        }

        if (currentTime - buffer.getLong("lastFlag2") > 6 * 1000) {
            buffer.put("lastFlag2", System.currentTimeMillis());
            buffer.put("lastPlace2", currentTime);
            return;
        }

        buffer.put("lastPlace", currentTime);

        callViolationEvent(player, zacPlayer, null);
    }

}
