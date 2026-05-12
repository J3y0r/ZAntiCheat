package cn.jeyor1337.zanticheat.check.checks.movement.speed;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.movement.MovementCheck;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.util.hook.plugin.FloodgateHook;
import cn.jeyor1337.zanticheat.util.precise.AccuracyUtil;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The horizontal speed while standing on a block
 */
public class SpeedB extends MovementCheck implements Listener {
    public SpeedB() {
        super(CheckName.SPEED_B);
    }

    @Override
    public boolean isConditionAllowed(Player player, ZACPlayer zacPlayer, PlayerCache cache, boolean isClimbing, boolean isInWater,
                                      boolean isFlying, boolean isInsideVehicle, boolean isGliding, boolean isRiptiding) {
        if (isFlying || isInsideVehicle || isClimbing || isGliding || isRiptiding || isInWater)
            return false;
        if (cache.flyingTicks >= -6 || cache.climbingTicks >= -2 ||
                cache.glidingTicks >= -5 || cache.riptidingTicks >= -6)
            return false;
        long time = System.currentTimeMillis();
        return time - cache.lastInsideVehicle > 150 && time - cache.lastInWater > 150 &&
                time - cache.lastKnockback > 1500 && time - cache.lastKnockbackNotVanilla > 5000 &&
                time - cache.lastWasFished > 3000 && time - cache.lastTeleport > 600 &&
                time - cache.lastRespawn > 500 && time - cache.lastEntityVeryNearby > 400 &&
                time - cache.lastBlockExplosion > 4000 && time - cache.lastEntityExplosion > 2500 &&
                time - cache.lastSlimeBlockVertical > 2500 && time - cache.lastSlimeBlockHorizontal > 3500 &&
                time - cache.lastHoneyBlockVertical > 2000 && time - cache.lastHoneyBlockHorizontal > 2000 &&
                time - cache.lastWasHit > 700 && time - cache.lastWasDamaged > 300 &&
                time - cache.lastKbVelocity > 500 && time - cache.lastAirKbVelocity > 1000 &&
                time - cache.lastStrongKbVelocity > 2500 && time - cache.lastStrongAirKbVelocity > 5000 &&
                time - cache.lastFlight > 1000 &&
                time - cache.lastGliding > 750 && time - cache.lastRiptiding > 1500 &&
                time - cache.lastWindCharge > 750 && time - cache.lastWindChargeReceive > 875 &&
                time - cache.lastWindBurst > 500 && time - cache.lastWindBurstNotVanilla > 1000;
    }

    @EventHandler
    public void onAsyncMovement(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        PlayerCache cache = zacPlayer.cache;
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true)) {
            buffer.put("speedTicks", 0);
            return;
        }

        if (!isConditionAllowed(player, zacPlayer, event)) {
            buffer.put("speedTicks", 0);
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - buffer.getLong("effectTime") < 1250) {
            buffer.put("speedTicks", 0);
            return;
        }

        if (FloodgateHook.isCancelledMovement(getCheckSetting().name, player, true))
            return;

        for (Block block : event.getToWithinBlocks()) {
            if (isActuallyPassable(block))
                continue;
            if (block.getType().name().endsWith("_SLAB") && !VerUtil.isWatterLoggedSlab(block))
                continue;
            buffer.put("speedTicks", 0);
            return;
        }

        Set<Material> downMaterials = new HashSet<>();
        downMaterials.addAll(event.getToDownMaterials());
        downMaterials.addAll(event.getFromDownMaterials());

        if (downMaterials.contains(Material.ICE) || downMaterials.contains(Material.PACKED_ICE) ||
                downMaterials.contains(VerUtil.material.get("BLUE_ICE"))) {
            buffer.put("speedTicks", 0);
            return;
        }

        if (downMaterials.contains(Material.SOUL_SAND) || downMaterials.contains(VerUtil.material.get("SOUL_SOIL"))) {
            ItemStack boots = zacPlayer.getArmorPiece(EquipmentSlot.FEET);
            if (boots != null && boots.getEnchantmentLevel(VerUtil.enchantment.get("SOUL_SPEED")) != 0) {
                buffer.put("speedTicks", 0);
                return;
            }
        }

        PlayerCache.OnGround newerOnGround = null;
        for (int i = 0; i < 6 && i < HistoryElement.values().length; i++) {
            PlayerCache.OnGround onGround = cache.history.onEvent.onGround.get(HistoryElement.values()[i]);
            if (newerOnGround == null) {
                newerOnGround = onGround;
                continue;
            }
            if (!onGround.towardsTrue && !newerOnGround.towardsTrue) {
                buffer.put("speedTicks", 0);
                return;
            }
            newerOnGround = onGround;
        }
        newerOnGround = null;
        for (int i = 0; i < 6 && i < HistoryElement.values().length; i++) {
            PlayerCache.OnGround onGround = cache.history.onPacket.onGround.get(HistoryElement.values()[i]);
            if (newerOnGround == null) {
                newerOnGround = onGround;
                continue;
            }
            if (!onGround.towardsTrue && !newerOnGround.towardsTrue) {
                buffer.put("speedTicks", 0);
                return;
            }
            newerOnGround = onGround;
        }

        buffer.put("speedTicks", buffer.getInt("speedTicks") + 1);
        if (buffer.getInt("speedTicks") <= 2)
            return;

        double preHSpeed1 = distanceHorizontal(event.getFrom(), event.getTo());
        preHSpeed1 -= distanceAbsVertical(event.getFrom(), event.getTo()) / 1.8;
        double preHSpeed2 = distanceHorizontal(cache.history.onEvent.location.get(HistoryElement.FIRST), event.getTo()) / 2.0;
        preHSpeed2 -= distanceAbsVertical(cache.history.onEvent.location.get(HistoryElement.FIRST), event.getTo()) / 2.0 / 1.8;

        double hSpeed = Math.min(preHSpeed1, preHSpeed2);
        hSpeed /= player.getWalkSpeed() / 0.2;

        double maxSpeed = 0.2806;
        int speedEffectAmplifier = getEffectAmplifier(zacPlayer.cache, PotionEffectType.SPEED);
        if (speedEffectAmplifier > 0) {
            maxSpeed *= speedEffectAmplifier * 0.4 + 1;
            if (speedEffectAmplifier > 2)
                maxSpeed *= 1.35;
        }
        if (!isBlockHeight((float) getBlockY(event.getTo().getY()))) {
            if (getEffectAmplifier(zacPlayer.cache, PotionEffectType.JUMP) > 0 ||
                    getEffectAmplifier(zacPlayer.cache, VerUtil.potions.get("LEVITATION")) > 0)
                return;
            hSpeed -= distanceAbsVertical(event.getFrom(), event.getTo()) * 2.5;
        }
        maxSpeed *= 1.3;

        Map<String, Double> attributes = getPlayerAttributes(player);
        double attributeAmount = Math.max(
                getItemStackAttributes(player, "GENERIC_MOVEMENT_SPEED", "PLAYER_SNEAKING_SPEED"),
                Math.max(attributes.getOrDefault("GENERIC_MOVEMENT_SPEED", 0.13) - 0.13, attributes.getOrDefault("PLAYER_SNEAKING_SPEED", 0.0))
        );
        if (attributeAmount != 0) {
            maxSpeed = (maxSpeed * 1.05 + 0.11) * (1 + Math.max(0, attributeAmount));
            buffer.put("attribute", System.currentTimeMillis());
        } else if (System.currentTimeMillis() - buffer.getLong("attribute") < 3000) {
            return;
        }

        if (hSpeed < maxSpeed)
            return;

        Set<Player> players = getPlayersForEnchantsSquared(zacPlayer, player);
        double finalHSpeed = hSpeed;
        double finalMaxSpeed = maxSpeed;
        Scheduler.runTask(true, () -> {
            if (isLagGlidingPossible(player, buffer, 15)) {
                buffer.put("lastGlidingLagPossibleTime", currentTime);
                return;
            }
            if (isPingGlidingPossible(player, cache))
                return;

            if (zacPlayer.isGliding() || zacPlayer.isRiptiding()) {
                buffer.put("speedTicks", 0);
                return;
            }

            if (isEnchantsSquaredImpact(players) && finalHSpeed / 2.5 < finalMaxSpeed)
                return;

            if (AccuracyUtil.isViolationCancel(getCheckSetting(), buffer))
                return;
            if (currentTime - buffer.getLong("lastGlidingLagPossibleTime") < 7 * 1000)
                callViolationEventIfRepeat(player, zacPlayer, event, buffer, 2000);
            else
                callViolationEvent(player, zacPlayer, event);
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void beforeMovement(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (getEffectAmplifier(zacPlayer.cache, PotionEffectType.SPEED) > 5) {
            Buffer buffer = getBuffer(player, true);
            buffer.put("effectTime", System.currentTimeMillis());
        }
    }

}
