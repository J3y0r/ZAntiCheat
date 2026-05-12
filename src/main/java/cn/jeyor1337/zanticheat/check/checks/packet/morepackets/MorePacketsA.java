package cn.jeyor1337.zanticheat.check.checks.packet.morepackets;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.packet.PacketCheck;
import cn.jeyor1337.zanticheat.event.packetrecive.ZACAsyncPacketReceiveEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Packet limiter
 */
public class MorePacketsA extends PacketCheck implements Listener {
    public MorePacketsA() {
        super(CheckName.MOREPACKETS_A);
    }

    private static final int LONG_LIMIT = (int) Math.ceil(20 * 1.6 * 5.5);
    private static final int SHORT_LIMIT = (int) Math.ceil(20 * 0.8 * 4.5);

    //      Long interval:

    @EventHandler
    public void onAsyncPacketReceiveA(ZACAsyncPacketReceiveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (!limitPackets('A', buffer, 1600L, (int) Math.ceil(LONG_LIMIT * 1.9), 3))
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
    public void onAsyncPacketReceiveB(ZACAsyncPacketReceiveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (!limitPackets('B', buffer, 800L, (int) Math.ceil(SHORT_LIMIT * 1.9), 5))
            return;

        if (System.currentTimeMillis() - buffer.getLong("lastFlagTime") < 700)
            return;
        buffer.put("lastFlagTime", System.currentTimeMillis());

        Scheduler.runTask(true, () -> {
            callViolationEvent(player, zacPlayer, null);
        });
    }

}
