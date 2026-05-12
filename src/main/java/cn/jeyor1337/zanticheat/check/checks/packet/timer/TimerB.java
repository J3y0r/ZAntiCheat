package cn.jeyor1337.zanticheat.check.checks.packet.timer;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.packet.PacketCheck;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Event counter
 */
public class TimerB extends PacketCheck implements Listener {
    public TimerB() {
        super(CheckName.TIMER_B);
    }

    //      Long interval:

    @EventHandler
    public void onAsyncMovementA(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (!limitPackets('A', buffer, 1600L, (int) (20 * 1.6 * 5.5), 3))
            return;

        if (System.currentTimeMillis() - buffer.getLong("lastFlagTime") < 1500)
            return;
        buffer.put("lastFlagTime", System.currentTimeMillis());

        Scheduler.runTask(true, () -> {
            callViolationEvent(player, zacPlayer, null);
        });
    }

    @EventHandler
    public void onAsyncMovementB(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (!limitPackets('B', buffer, 1200L, (int) (20 * 1.2 * 5.0), 4))
            return;

        if (System.currentTimeMillis() - buffer.getLong("lastFlagTime") < 1500)
            return;
        buffer.put("lastFlagTime", System.currentTimeMillis());

        Scheduler.runTask(true, () -> {
            callViolationEvent(player, zacPlayer, null);
        });
    }

    @EventHandler
    public void onAsyncMovementC(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (!limitPackets('C', buffer, 800L, (int) (20 * 0.8 * 4.5), 5))
            return;

        if (System.currentTimeMillis() - buffer.getLong("lastFlagTime") < 1500)
            return;
        buffer.put("lastFlagTime", System.currentTimeMillis());

        Scheduler.runTask(true, () -> {
            callViolationEvent(player, zacPlayer, null);
        });
    }

    @EventHandler
    public void onAsyncMovementD(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (!limitPackets('D', buffer, 571L, (int) (20 * 0.571 * 4.1), 7))
            return;

        if (System.currentTimeMillis() - buffer.getLong("lastFlagTime") < 1500)
            return;
        buffer.put("lastFlagTime", System.currentTimeMillis());

        Scheduler.runTask(true, () -> {
            callViolationEvent(player, zacPlayer, null);
        });
    }

    //      Short interval:

    @EventHandler
    public void onAsyncMovementE(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (!limitPackets('E', buffer, 960L, (int) (20 * 1.6 * 5.5), 3))
            return;

        if (System.currentTimeMillis() - buffer.getLong("lastFlagTime") < 900)
            return;
        buffer.put("lastFlagTime", System.currentTimeMillis());

        Scheduler.runTask(true, () -> {
            callViolationEventIfRepeat(player, zacPlayer, null, buffer, Main.getBufferDurationMils() - 1000L);
        });
    }

    @EventHandler
    public void onAsyncMovementF(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (!limitPackets('F', buffer, 480L, (int) (20 * 0.8 * 4.5), 5))
            return;

        if (System.currentTimeMillis() - buffer.getLong("lastFlagTime") < 900)
            return;
        buffer.put("lastFlagTime", System.currentTimeMillis());

        Scheduler.runTask(true, () -> {
            callViolationEventIfRepeat(player, zacPlayer, null, buffer, Main.getBufferDurationMils() - 1000L);
        });
    }

}
