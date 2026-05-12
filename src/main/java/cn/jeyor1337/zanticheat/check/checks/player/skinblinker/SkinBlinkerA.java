package cn.jeyor1337.zanticheat.check.checks.player.skinblinker;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.player.PlayerCheck;
import cn.jeyor1337.zanticheat.event.packetrecive.ZACAsyncPacketReceiveEvent;
import cn.jeyor1337.zanticheat.event.packetrecive.packettype.PacketType;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.hook.plugin.FloodgateHook;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * SkinBlinker hack
 */
public class SkinBlinkerA extends PlayerCheck implements Listener {
    public SkinBlinkerA() {
        super(CheckName.SKINBLINKER_A);
    }

    @EventHandler
    public void onAsyncPacketReceive(ZACAsyncPacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.CLIENT_INFORMATION)
            return;

        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (FloodgateHook.isBedrockPlayer(player, true))
            return;

        if (System.currentTimeMillis() - buffer.getLong("lastMovement") < 333)
            buffer.put("packets", buffer.getInt("packets") + 1);

        long currentTime = System.currentTimeMillis();
        if (currentTime - buffer.getLong("startTime") <= 2000)
            return;
        buffer.put("startTime", currentTime);

        int packets = buffer.getInt("packets");
        buffer.put("packets", 0);

        if (packets < 12) {
            buffer.put("flags", 0);
            return;
        }
        buffer.put("flags", buffer.getInt("flags") + 1);

        if (buffer.getInt("flags") < 2)
            return;
        buffer.put("flags", 0);

        if (System.currentTimeMillis() - buffer.getLong("lastMovement") >= 1800)
            return;

        Scheduler.runTask(true, () -> {
            callViolationEvent(player, zacPlayer, null);
        });
    }

    @EventHandler
    public void onMovement(ZACAsyncPlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (Math.abs(from.getYaw()) - to.getYaw() <= 5 &&
                Math.abs(from.getPitch()) - to.getPitch() <= 0.5)
            return;

        if (distance(event.getFrom(), event.getTo()) == 0)
            return;

        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("lastMovement", System.currentTimeMillis());
    }

}
