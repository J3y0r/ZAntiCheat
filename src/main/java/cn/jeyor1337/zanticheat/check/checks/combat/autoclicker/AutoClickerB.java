package cn.jeyor1337.zanticheat.check.checks.combat.autoclicker;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.combat.CombatCheck;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.player.cps.CPSListener;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Max CPS
 */
public class AutoClickerB extends CombatCheck implements Listener {
    public AutoClickerB() {
        super(CheckName.AUTOCLICKER_B);
    }

    @EventHandler
    public void onHit(PlayerInteractEvent event) {
        if (isExternalNPC(event)) return;
        if (event.getAction() == Action.PHYSICAL)
            return;
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);

        Scheduler.entityThread(player, () -> {
            if (!isCheckAllowed(player, zacPlayer))
                return;

            if (CPSListener.getCurrentCps(player) < 47)
                return;

            Buffer buffer = getBuffer(player);
            long currentTime = System.currentTimeMillis();
            if (buffer.isExists("lastFlag") && currentTime - buffer.getLong("lastFlag") < 2000)
                return;
            buffer.put("lastFlag", currentTime);

            callViolationEvent(player, zacPlayer, event);
        });
    }

}
