package cn.jeyor1337.zanticheat.check.checks.packet.timer;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.packet.PacketCheck;
import cn.jeyor1337.zanticheat.event.packetrecive.ZACAsyncPacketReceiveEvent;
import cn.jeyor1337.zanticheat.event.packetrecive.packettype.PacketType;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.hook.plugin.FloodgateHook;
import cn.jeyor1337.zanticheat.version.identifier.ZACVersion;
import cn.jeyor1337.zanticheat.version.identifier.VerIdentifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Timer hack
 */
public class TimerA extends PacketCheck implements Listener {
    public TimerA() {
        super(CheckName.TIMER_A);
    }

    @EventHandler
    public void onAsyncPacketReceive(ZACAsyncPacketReceiveEvent event) {
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();

        if (event.getPacketType() != PacketType.FLYING)
            return;

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (FloodgateHook.isProbablyPocketEditionPlayer(player, true))
            return;

        Buffer buffer = getBuffer(player, true);
        long currentTime = System.currentTimeMillis();

        if (!buffer.getBoolean("moved") || currentTime - zacPlayer.joinTime < 2000)
            return;

        if (VerIdentifier.getVersion().isOlderOrEqualsTo(ZACVersion.V1_8) && currentTime - zacPlayer.joinTime < 12000)
            return;

        if (player.isInsideVehicle()) {
            buffer.put("skipVehiclePacket", !buffer.getBoolean("skipVehiclePacket"));
            if (!buffer.getBoolean("skipVehiclePacket"))
                return;
        }

        if (System.currentTimeMillis() - zacPlayer.cache.lastWindCharge < 3000 ||
                System.currentTimeMillis() - zacPlayer.cache.lastWindChargeReceive < 1000) {
            buffer.put("skipVehiclePacket", !buffer.getBoolean("skipVehiclePacket"));
            if (!buffer.getBoolean("skipVehiclePacket"))
                return;
        }

        if (!buffer.isExists("lastTime") || !buffer.isExists("packets") ||
                !buffer.isExists("packetsBalancer") || !buffer.isExists("balancerTime"))
            buffer.put("lastNonExistingFieldTime", currentTime);

        long difference = currentTime - buffer.getLong("lastTime");
        buffer.put("packets", buffer.getInt("packets") + 1);
        buffer.put("packetsBalancer", buffer.getInt("packetsBalancer") + 1);
        if (difference >= 1000) {
            buffer.put("lastTime", currentTime);
            buffer.put("balancerTime", buffer.getInt("balancerTime") + (21 - buffer.getInt("packetsBalancer")));
            buffer.put("packets", 0);
            buffer.put("packetsBalancer", 0);
            return;
        }
        if (buffer.getInt("balancerTime") > 0) {
            buffer.put("balancerTime", buffer.getInt("balancerTime") - 1);
            buffer.put("packets", buffer.getInt("packets") - 1);
            return;
        }
        if (buffer.getInt("packets") > (VerIdentifier.getVersion().isNewerThan(ZACVersion.V1_8) ? 28 : 35)) {
            if (currentTime - zacPlayer.joinTime > 10 * 1000)
                localFlag(buffer, player, zacPlayer);
            buffer.put("packets", buffer.getInt("packets") - 2);
        }
    }

    @EventHandler
    public void onAsyncMovement(ZACAsyncPlayerMoveEvent event) {
        if (distance(event.getFrom(), event.getTo()) == 0)
            return;
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("moved", true);
    }

    private void localFlag(Buffer buffer, Player player, ZACPlayer zacPlayer) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - buffer.getLong("lastNonExistingFieldTime") <= 2000)
            return;

        if (currentTime - buffer.getLong("localFlagTime") > 2000) {
            buffer.put("localFlagTime", currentTime);
            buffer.put("localFlags", 0);
        }
        buffer.put("localFlags", buffer.getInt("localFlags") + 1);
        if (buffer.getInt("localFlags") <= 2)
            return;

        if (currentTime - buffer.getLong("lastFlagTime") <= 2000)
            return;

        flag(player, zacPlayer);
        buffer.put("lastFlagTime", currentTime);
    }

}
