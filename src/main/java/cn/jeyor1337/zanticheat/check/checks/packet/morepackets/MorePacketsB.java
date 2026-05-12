package cn.jeyor1337.zanticheat.check.checks.packet.morepackets;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.packet.PacketCheck;
import cn.jeyor1337.zanticheat.event.packetrecive.ZACAsyncPacketReceiveEvent;
import cn.jeyor1337.zanticheat.event.packetrecive.packettype.PacketType;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Nuker hack
 */
public class MorePacketsB extends PacketCheck implements Listener {
    public MorePacketsB() {
        super(CheckName.MOREPACKETS_B);
    }

    @EventHandler
    public void onAsyncPacketReceive(ZACAsyncPacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.BLOCK_DIG)
            return;

        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (!limitPackets('A', buffer, 667L, 400, 3))
            return;

        Scheduler.runTask(true, () -> {
            callViolationEvent(player, zacPlayer, null);
        });
    }

}
