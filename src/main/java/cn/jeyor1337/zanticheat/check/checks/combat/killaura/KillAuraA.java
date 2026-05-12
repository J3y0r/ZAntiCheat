package cn.jeyor1337.zanticheat.check.checks.combat.killaura;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.combat.CombatCheck;
import cn.jeyor1337.zanticheat.event.playerattack.ZACAsyncPlayerAttackEvent;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.player.cache.history.PlayerCacheHistory;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Head rotation pattern
 */
public class KillAuraA extends CombatCheck implements Listener {

    public KillAuraA() {
        super(CheckName.KILLAURA_A);
    }

    @EventHandler
    public void onAsyncHit(ZACAsyncPlayerAttackEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        PlayerCache cache = zacPlayer.cache;
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true)) {
            buffer.put("listen", 0L);
            return;
        }

        PlayerCacheHistory<Location> history = cache.history.onEvent.location;
        if (getHeadRotationChange(history.get(HistoryElement.SECOND), history.get(HistoryElement.FIRST)) >= 0.01 ||
                getHeadRotationChange(history.get(HistoryElement.FIRST), history.get(HistoryElement.FROM)) >= 0.01) {
            buffer.put("listen", 0L);
            return;
        }

        buffer.put("listen", System.currentTimeMillis());
    }

    @EventHandler
    public void onAsyncMovement(ZACAsyncPlayerMoveEvent event) {
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);
        if (System.currentTimeMillis() - buffer.getLong("listen") > 5000)
            return;
        buffer.put("listen", 0L);

        if (getHeadRotationChange(event.getFrom(), event.getTo()) <= 15) {
            buffer.put("flags", 0);
            return;
        }

        buffer.put("flags", buffer.getInt("flags") + 1);
        if (buffer.getInt("flags") <= 3)
            return;

        Scheduler.runTask(true, () -> {
            callViolationEvent(player, event.getZacPlayer(), null);
        });
    }

    private static double getHeadRotationChange(Location from, Location to) {
        return Math.sqrt(Math.pow(to.getYaw() - from.getYaw(), 2) + Math.pow(to.getPitch() - from.getPitch(), 2));
    }

}
