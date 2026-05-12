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

public class BadPacketsF extends PacketCheck implements Listener {
    public BadPacketsF() {
        super(CheckName.BADPACKETS_F);
    }

    @EventHandler
    public void onAsyncPacketReceive(ZACAsyncPacketReceiveEvent event) {
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();
        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        Buffer buffer = getBuffer(player, true);

        if (event.getPacketType() == PacketType.FLYING) {
            buffer.put("lastPacketType", PacketType.FLYING.ordinal());
            return;
        }

        if (event.getPacketType() != PacketType.HELD_ITEM_SLOT)
            return;

        int lastPacketType = buffer.getInt("lastPacketType");
        buffer.put("lastPacketType", PacketType.HELD_ITEM_SLOT.ordinal());

        if (lastPacketType == PacketType.HELD_ITEM_SLOT.ordinal()) {
            flag(player, zacPlayer);
        }
    }
}
