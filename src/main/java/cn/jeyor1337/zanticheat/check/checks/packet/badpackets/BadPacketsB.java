package cn.jeyor1337.zanticheat.check.checks.packet.badpackets;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.checks.packet.PacketCheck;
import cn.jeyor1337.zanticheat.event.packetrecive.ZACAsyncPacketReceiveEvent;
import cn.jeyor1337.zanticheat.event.packetrecive.packettype.PacketType;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Impossible entity ID
 */
public class BadPacketsB extends PacketCheck implements Listener {
    public BadPacketsB() {
        super(CheckName.BADPACKETS_B);
    }

    @EventHandler
    public void onAsyncPacketReceive(ZACAsyncPacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.USE_ENTITY)
            return;

        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();
        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (event.getEntityId() < 0)
            flag(player, zacPlayer);
    }

}
