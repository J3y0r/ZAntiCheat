package cn.jeyor1337.zanticheat.check.checks.movement.vehicle;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.movement.MovementCheck;
import cn.jeyor1337.zanticheat.event.packetrecive.ZACAsyncPacketReceiveEvent;
import cn.jeyor1337.zanticheat.event.packetrecive.packettype.PacketType;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.util.detection.LeanTowards;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Horizontal speed limiter
 */
public class VehicleA extends MovementCheck implements Listener {
    public VehicleA() {
        super(CheckName.VEHICLE_A);
    }

    @Override
    public boolean isConditionAllowed(Player player, ZACPlayer zacPlayer, PlayerCache cache, boolean isClimbing, boolean isInWater,
                                      boolean isFlying, boolean isInsideVehicle, boolean isGliding, boolean isRiptiding) {
        if (isFlying || !isInsideVehicle || isGliding || isRiptiding)
            return false;
        if (cache.flyingTicks >= -5 || cache.climbingTicks >= -2 ||
                cache.glidingTicks >= -3 || cache.riptidingTicks >= -5)
            return false;
        long time = System.currentTimeMillis();
        return time - cache.lastKnockback > 500 && time - cache.lastKnockbackNotVanilla > 2000 &&
                time - cache.lastWasFished > 3000 && time - cache.lastTeleport > 500 &&
                time - cache.lastRespawn > 500 &&
                time - cache.lastBlockExplosion > 5500 && time - cache.lastEntityExplosion > 2000 &&
                time - cache.lastSlimeBlockVertical > 3500 && time - cache.lastSlimeBlockHorizontal > 3000 &&
                time - cache.lastHoneyBlockVertical > 2000 && time - cache.lastHoneyBlockHorizontal > 2000 &&
                time - cache.lastWasHit > 300 && time - cache.lastWasDamaged > 150 &&
                time - cache.lastKbVelocity > 250 && time - cache.lastAirKbVelocity > 500 &&
                time - cache.lastStrongKbVelocity > 1250 && time - cache.lastStrongAirKbVelocity > 2500 &&
                time - cache.lastFlight > 750;
    }

    @EventHandler
    public void vehicleSpeedAndFlight(ZACAsyncPacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.STEER_VEHICLE)
            return;

        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true)) {
            buffer.put("vehicleSpeedEvents", 0);
            buffer.put("fromLocation", null);
            buffer.put("previousLocation", null);
            return;
        }

        if (!isConditionAllowed(player, zacPlayer, zacPlayer.cache, false, false, player.isFlying(),
                player.isInsideVehicle() && player.getVehicle() != null, zacPlayer.isGliding(), zacPlayer.isRiptiding())) {
            buffer.put("vehicleSpeedEvents", 0);
            buffer.put("fromLocation", null);
            buffer.put("previousLocation", null);
            return;
        }

        Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            buffer.put("vehicleSpeedEvents", 0);
            buffer.put("fromLocation", null);
            buffer.put("previousLocation", null);
            return;
        }

        if (vehicle.getType() != EntityType.HORSE &&
                vehicle.getType() != VerUtil.entityTypes.get("MULE") &&
                vehicle.getType() != EntityType.PIG) {
            buffer.put("vehicleSpeedEvents", 0);
            buffer.put("fromLocation", null);
            buffer.put("previousLocation", null);
            return;
        }

        Location to = vehicle.getLocation();
        Location from = buffer.getLocation("fromLocation");
        Location previous = buffer.getLocation("previousLocation");
        buffer.put("previousLocation", from);
        buffer.put("fromLocation", to);
        if (from == null || previous == null) {
            buffer.put("vehicleSpeedEvents", 0);
            return;
        }

        for (Block block : getWithinBlocks(vehicle, to))
            if (!isActuallyPassable(block)) {
                buffer.put("vehicleSpeedEvents", 0);
                buffer.put("previousVerticalSpeed", distanceVertical(from, to));
                return;
            }
        for (Block block : getWithinBlocks(vehicle, from))
            if (!isActuallyPassable(block)) {
                buffer.put("vehicleSpeedEvents", 0);
                buffer.put("previousVerticalSpeed", distanceVertical(from, to));
                return;
            }

        buffer.put("vehicleSpeedEvents", buffer.getInt("vehicleSpeedEvents") + 1);
        if (buffer.getInt("vehicleSpeedEvents") <= 2) {
            buffer.put("previousVerticalSpeed", distanceVertical(from, to));
            return;
        }

        boolean flag = false;

        double horizontalSpeed = Math.min(
                distanceHorizontal(from, to),
                distanceHorizontal(previous, to) / 2.0
        );
        if (horizontalSpeed > 3.65 * 1.35)
            flag = true;

        double verticalSpeed = distanceVertical(from, to);
        if (!flag && buffer.getInt("vehicleSpeedEvents") >= 4 && buffer.isExists("previousVerticalSpeed")) {
            if (!isBlockHeight((float) getBlockY(from.getY())) &&
                    !isBlockHeight((float) getBlockY(to.getY())) &&
                    !isOnGround(vehicle, 0.5, LeanTowards.TRUE, true)) {
                double previousVerticalSpeed = buffer.getDouble("previousVerticalSpeed");
                if (previousVerticalSpeed != 0) {
                    if (previousVerticalSpeed + 0.002 >= verticalSpeed)
                        buffer.put("verticalFlags", Math.min(buffer.getInt("verticalFlags") + 1, 4));
                    else
                        buffer.put("verticalFlags", Math.max(buffer.getInt("verticalFlags") - 2, 0));
                    if (buffer.getInt("verticalFlags") >= 4)
                        flag = true;
                }
            }
            for (Block block : getDownBlocks(vehicle, 0.4)) {
                if (isActuallyPassable(block))
                    continue;
                flag = false;
                break;
            }
        }

        buffer.put("previousVerticalSpeed", verticalSpeed);
        if (!flag)
            return;

        Scheduler.runTask(true, () -> {
            callViolationEventIfRepeat(player, zacPlayer, null, buffer, 2000);
        });
    }

}
