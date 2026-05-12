package cn.jeyor1337.zanticheat.check.checks.movement.noslow;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.movement.MovementCheck;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;

/**
 * Speed inside a cobweb
 */
public class NoSlowA extends MovementCheck implements Listener {
    public NoSlowA() {
        super(CheckName.NOSLOW_A);
    }

    @Override
    public boolean isConditionAllowed(Player player, ZACPlayer zacPlayer, PlayerCache cache, boolean isClimbing, boolean isInWater,
                                      boolean isFlying, boolean isInsideVehicle, boolean isGliding, boolean isRiptiding) {
        if (isFlying || isInsideVehicle || isClimbing || isGliding || isRiptiding || isInWater)
            return false;
        if (cache.flyingTicks >= -5 || cache.climbingTicks >= -2 ||
                cache.glidingTicks >= -3 || cache.riptidingTicks >= -5)
            return false;
        long time = System.currentTimeMillis();
        return time - cache.lastInsideVehicle > 150 && time - cache.lastInWater > 150 &&
                time - cache.lastKnockback > 750 && time - cache.lastKnockbackNotVanilla > 3000 &&
                time - cache.lastWasFished > 4000 && time - cache.lastTeleport > 500 &&
                time - cache.lastRespawn > 500 && time - cache.lastEntityVeryNearby > 200 &&
                time - cache.lastBlockExplosion > 4500 && time - cache.lastEntityExplosion > 2500 &&
                time - cache.lastSlimeBlockVertical > 3500 && time - cache.lastSlimeBlockHorizontal > 2500 &&
                time - cache.lastHoneyBlockVertical > 2000 && time - cache.lastHoneyBlockHorizontal > 2000 &&
                time - cache.lastWasHit > 350 && time - cache.lastWasDamaged > 150 &&
                time - cache.lastKbVelocity > 500 && time - cache.lastAirKbVelocity > 1000 &&
                time - cache.lastStrongKbVelocity > 2500 && time - cache.lastStrongAirKbVelocity > 5000 &&
                time - cache.lastFlight > 750;
    }

    @EventHandler
    public void onAsyncMovement(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true)) {
            buffer.put("cobwebEvents", 0);
            return;
        }

        if (!isConditionAllowed(player, zacPlayer, event)) {
            buffer.put("cobwebEvents", 0);
            return;
        }

        if (System.currentTimeMillis() - buffer.getLong("effectTime") < 1000) {
            buffer.put("cobwebEvents", 0);
            return;
        }

        if (!event.getToWithinMaterials().contains(VerUtil.material.get("COBWEB")) &&
                !event.getToWithinMaterials().contains(VerUtil.material.get("WEB")) ||
                !event.getFromWithinMaterials().contains(VerUtil.material.get("COBWEB")) &&
                        !event.getFromWithinMaterials().contains(VerUtil.material.get("WEB")))
            return;

        for (Block block : event.getToWithinBlocks()) {
            if (block.getType() == VerUtil.material.get("COBWEB") ||
                    block.getType() == VerUtil.material.get("WEB") ||
                    isActuallyPassable(block))
                continue;
            buffer.put("cobwebEvents", 0);
            return;
        }

        for (Block block : event.getFromWithinBlocks()) {
            if (block.getType() == VerUtil.material.get("COBWEB") ||
                    block.getType() == VerUtil.material.get("WEB") ||
                    isActuallyPassable(block))
                continue;
            buffer.put("cobwebEvents", 0);
            return;
        }

        buffer.put("cobwebEvents", buffer.getInt("cobwebEvents") + 1);
        if (buffer.getInt("cobwebEvents") <= 2)
            return;

        double maxHorizontalSpeed = (0.063701 + 0.063701 + 0.28062) / 3;
        int speedEffectAmplifier = getEffectAmplifier(zacPlayer.cache, PotionEffectType.SPEED);
        if (speedEffectAmplifier > 0) {
            maxHorizontalSpeed *= speedEffectAmplifier * 0.35 + 1;
            if (speedEffectAmplifier > 2)
                maxHorizontalSpeed *= 1.35;
        }
        double maxVerticalSpeed = (0.0039201 + 0.0039201 + 0.30432) / 3;

        double horizontalSpeed = distanceHorizontal(event.getFrom(), event.getTo());
        double verticalSpeed = distanceVertical(event.getFrom(), event.getTo());
        verticalSpeed = verticalSpeed < 0 ? Math.abs(verticalSpeed) : 0.0;

        if (horizontalSpeed <= maxHorizontalSpeed && verticalSpeed <= maxVerticalSpeed)
            return;

        if (getItemStackAttributes(player, "GENERIC_MOVEMENT_EFFICIENCY") != 0 ||
                getPlayerAttributes(player).getOrDefault("GENERIC_MOVEMENT_EFFICIENCY", 0.0) > 0.01)
            buffer.put("attribute", System.currentTimeMillis());
        if (System.currentTimeMillis() - buffer.getLong("attribute") < 4000)
            return;

        Scheduler.runTask(true, () -> {
            callViolationEvent(player, zacPlayer, event);
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void beforeMovement(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (getEffectAmplifier(zacPlayer.cache, VerUtil.potions.get("LEVITATION")) > 0 ||
                getEffectAmplifier(zacPlayer.cache, PotionEffectType.JUMP) > 5) {
            Buffer buffer = getBuffer(player, true);
            long currentTime = System.currentTimeMillis();
            buffer.put("effectTime", currentTime);
        }
    }

}
