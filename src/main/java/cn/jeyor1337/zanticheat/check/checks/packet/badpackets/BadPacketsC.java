package cn.jeyor1337.zanticheat.check.checks.packet.badpackets;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.checks.packet.PacketCheck;
import cn.jeyor1337.zanticheat.event.packetrecive.ZACAsyncPacketReceiveEvent;
import cn.jeyor1337.zanticheat.event.packetrecive.packettype.PacketType;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.player.cooldown.element.EntityDistance;
import cn.jeyor1337.zanticheat.util.cooldown.CooldownUtil;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Impassible SteerVehicle packet
 */
public class BadPacketsC extends PacketCheck implements Listener {
    public BadPacketsC() {
        super(CheckName.BADPACKETS_C);
    }

    @EventHandler
    public void onAsyncPacketReceive(ZACAsyncPacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.STEER_VEHICLE)
            return;
        if (!"PacketPlayInSteerVehicle".equals(event.getPacketClassName()))
            return;

        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();
        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (System.currentTimeMillis() - zacPlayer.joinTime < 2000)
            return;

        if (player.isInsideVehicle() || System.currentTimeMillis() - zacPlayer.cache.lastInsideVehicle < 500)
            return;
        if (!CooldownUtil.getNearbyEntitiesAsync(zacPlayer.cooldown, event.getPlayer(), EntityDistance.NEARBY).isEmpty() ||
                distance(zacPlayer.cache.history.onEvent.location.get(HistoryElement.FIRST), zacPlayer.cache.history.onEvent.location.get(HistoryElement.FROM)) == 0)
            return;

        Scheduler.runTaskLaterAsynchronously(() -> {
            if (!player.isOnline() || zacPlayer.leaveTime != 0)
                return;
            if (player.isInsideVehicle() || System.currentTimeMillis() - zacPlayer.cache.lastInsideVehicle < 500)
                return;
            if (!CooldownUtil.getNearbyEntitiesAsync(zacPlayer.cooldown, event.getPlayer(), EntityDistance.NEARBY).isEmpty() ||
                    distance(zacPlayer.cache.history.onEvent.location.get(HistoryElement.FIRST), zacPlayer.cache.history.onEvent.location.get(HistoryElement.FROM)) == 0)
                return;

            Scheduler.runTaskLaterAsynchronously(() -> {
                if (!player.isOnline() || zacPlayer.leaveTime != 0)
                    return;
                if (player.isInsideVehicle() || System.currentTimeMillis() - zacPlayer.cache.lastInsideVehicle < 500)
                    return;
                if (!CooldownUtil.getNearbyEntitiesAsync(zacPlayer.cooldown, event.getPlayer(), EntityDistance.NEARBY).isEmpty() ||
                        distance(zacPlayer.cache.history.onEvent.location.get(HistoryElement.FIRST), zacPlayer.cache.history.onEvent.location.get(HistoryElement.FROM)) == 0)
                    return;

                Scheduler.runTask(false, () -> {
                    if (!player.isOnline() || zacPlayer.leaveTime != 0)
                        return;
                    flag(player, zacPlayer);
                });
            }, 1);
        }, 1);
    }

}
