package cn.jeyor1337.zanticheat.check.checks.packet.badpackets;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.packet.PacketCheck;
import cn.jeyor1337.zanticheat.event.packetrecive.ZACAsyncPacketReceiveEvent;
import cn.jeyor1337.zanticheat.event.packetrecive.packettype.PacketType;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class BadPacketsE extends PacketCheck implements Listener {
    public BadPacketsE() {
        super(CheckName.BADPACKETS_E);
    }

    @EventHandler
    public void onAsyncPacketReceive(ZACAsyncPacketReceiveEvent event) {
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();
        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        Buffer buffer = getBuffer(player, true);
        if (event.getPacketType() == PacketType.FLYING) {
            buffer.put("attacks", 0);
            return;
        }

        if (event.getPacketType() != PacketType.USE_ENTITY || !event.isAttack())
            return;

        int attacks = buffer.getInt("attacks") + 1;
        buffer.put("attacks", attacks);
        if (attacks <= 1)
            return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - buffer.getLong("lastFlagTime") <= 200)
            return;

        flag(player, zacPlayer);
        buffer.put("lastFlagTime", currentTime);
    }
}
