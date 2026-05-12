package cn.jeyor1337.zanticheat.check.checks.packet.badpackets;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.checks.packet.PacketCheck;
import cn.jeyor1337.zanticheat.event.packetrecive.ZACAsyncPacketReceiveEvent;
import cn.jeyor1337.zanticheat.event.packetrecive.packettype.PacketType;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Impassible SetCreativeSlot packet
 */
public class BadPacketsD extends PacketCheck implements Listener {
    public BadPacketsD() {
        super(CheckName.BADPACKETS_D);
    }

    @EventHandler
    public void onAsyncPacketReceive(ZACAsyncPacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.SET_CREATIVE_SLOT)
            return;

        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();
        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (player.getGameMode() == GameMode.CREATIVE ||
                System.currentTimeMillis() - zacPlayer.cache.lastGamemodeChange < 500)
            return;

        Scheduler.runTaskLater(() -> {
            if (!player.isOnline() || zacPlayer.leaveTime != 0)
                return;

            if (player.getGameMode() == GameMode.CREATIVE ||
                    System.currentTimeMillis() - zacPlayer.cache.lastGamemodeChange < 500)
                return;

            flag(player, zacPlayer);
        }, 1);
    }

}
