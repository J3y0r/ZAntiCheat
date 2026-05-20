package cn.jeyor1337.zanticheat.check.checks.movement.speed;

import com.kireiko.utils.math.ScalaredMath;
import com.kireiko.utils.math.client.MoveEngine;
import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.movement.MovementCheck;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.util.hook.plugin.FloodgateHook;
import cn.jeyor1337.zanticheat.util.hook.plugin.simplehook.ValhallaMMOHook;
import cn.jeyor1337.zanticheat.util.precise.AccuracyUtil;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Simulation speed
 */
public class SpeedC extends MovementCheck implements Listener {
    public SpeedC() {
        super(CheckName.SPEED_C);
    }

    @Override
    public boolean isConditionAllowed(Player player, ZACPlayer zacPlayer, PlayerCache cache, boolean isClimbing, boolean isInWater,
                                      boolean isFlying, boolean isInsideVehicle, boolean isGliding, boolean isRiptiding) {
        if (isFlying || isInsideVehicle || isClimbing || isGliding || isRiptiding || isInWater)
            return false;
        if (cache.flyingTicks >= -8 || cache.climbingTicks >= -3 ||
                cache.glidingTicks >= -7 || cache.riptidingTicks >= -8)
            return false;
        long time = System.currentTimeMillis();
        return time - cache.lastInsideVehicle > 200 && time - cache.lastInWater > 200 &&
                time - cache.lastKnockback > 1700 && time - cache.lastKnockbackNotVanilla > 6000 &&
                time - cache.lastWasFished > 4000 && time - cache.lastTeleport > 1000 &&
                time - cache.lastRespawn > 600 && time - cache.lastEntityVeryNearby > 800 &&
                time - cache.lastBlockExplosion > 5000 && time - cache.lastEntityExplosion > 3000 &&
                time - cache.lastSlimeBlockVertical > 2500 && time - cache.lastSlimeBlockHorizontal > 3500 &&
                time - cache.lastHoneyBlockVertical > 2000 && time - cache.lastHoneyBlockHorizontal > 2000 &&
                time - cache.lastWasHit > 700 && time - cache.lastWasDamaged > 300 &&
                time - cache.lastKbVelocity > 900 && time - cache.lastAirKbVelocity > 1800 &&
                time - cache.lastStrongKbVelocity > 5000 && time - cache.lastStrongAirKbVelocity > 15 * 1000 &&
                time - cache.lastFlight > 1000 &&
                time - cache.lastGliding > 2000 && time - cache.lastRiptiding > 3500 &&
                time - cache.lastWindCharge > 1000 && time - cache.lastWindChargeReceive > 1000 &&
                time - cache.lastWindBurst > 500 && time - cache.lastWindBurstNotVanilla > 1000;
    }

    @EventHandler
    public void onAsyncMovement(ZACAsyncPlayerMoveEvent event) {
        if (ValhallaMMOHook.isPluginInstalled()) return;
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true)) {
            buffer.put("speedTicks", 0);
            return;
        }

        PlayerCache.History history = zacPlayer.cache.history;
        if (history.onEvent.onGround.get(HistoryElement.FROM).towardsTrue ||
                history.onPacket.onGround.get(HistoryElement.FROM).towardsTrue) {
            buffer.put("localAirTicker", 0);
        } else {
            buffer.put("localAirTicker", buffer.getInt("localAirTicker") + 1);
        }

        if (!isConditionAllowed(player, zacPlayer, event)) {
            buffer.put("speedTicks", 0);
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - buffer.getLong("effectTime") <= 2500) {
            buffer.put("speedTicks", 0);
            return;
        }
        if (currentTime - buffer.getLong("impassableTime") < 700) {
            buffer.put("speedTicks", 0);
            return;
        }
        if (currentTime - buffer.getLong("iceTime") < 2000) {
            buffer.put("speedTicks", 0);
            return;
        }
        if (currentTime - buffer.getLong("soulSpeedTime") < 2000) {
            buffer.put("speedTicks", 0);
            return;
        }

        buffer.put("speedTicks", buffer.getInt("speedTicks") + 1);
        if (buffer.getInt("speedTicks") <= 2)
            return;

        /*
        Smart speed calculating via 0.01 scale
         */
        Location from = event.getFrom();
        Location to = event.getTo();

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double speed = ScalaredMath.scaleVal(Math.sqrt(dx * dx + dz * dz), 2);

        double maxSpeed = 1.0;
        int speedEffectAmplifier = getEffectAmplifier(zacPlayer.cache, PotionEffectType.SPEED);
        if (speedEffectAmplifier > 0) {
            maxSpeed *= speedEffectAmplifier * 0.35 + 1;
            if (speedEffectAmplifier > 2)
                maxSpeed *= 1.35;
        }

        boolean isBedrockPlayer = FloodgateHook.isBedrockPlayer(player, true);
        boolean isPocketPlayer = FloodgateHook.isProbablyPocketEditionPlayer(player, true);

        double targetSpeed = ScalaredMath.scaleVal(speed, 2);
        double finalSpeedLimit = ScalaredMath.scaleVal(
                MoveEngine.getSpeedByTick(buffer.getInt("localAirTicker")) * maxSpeed + 0.01, 2);

        if (isBedrockPlayer)
            finalSpeedLimit = finalSpeedLimit * (isPocketPlayer ? 1.35 : 1.2) + (isPocketPlayer ? 0.08 : 0.04);

        Map<String, Double> attributes = getPlayerAttributes(player);
        if (getItemStackAttributes(player, "GENERIC_MOVEMENT_SPEED", "PLAYER_SNEAKING_SPEED") != 0 ||
                attributes.getOrDefault("GENERIC_MOVEMENT_SPEED", 0.13) > 0.14 ||
                attributes.getOrDefault("PLAYER_SNEAKING_SPEED", 0.0) > 0.1)
            return;

        int reportThreshold = isBedrockPlayer ? (isPocketPlayer ? 43 : 40) : 30;
        int requiredFlags = isBedrockPlayer ? (isPocketPlayer ? 6 : 5) : 3;

        if (targetSpeed < finalSpeedLimit) {
            if (buffer.getInt("localPlayerRaport") <= 0) return;
            buffer.put("localPlayerRaport", buffer.getInt("localPlayerRaport") - 1);
            return;
        } else {
            buffer.put("localPlayerRaport", buffer.getInt("localPlayerRaport") + 3);
        }

        if (buffer.getInt("localPlayerRaport") <= reportThreshold) return;

        buffer.put("flags", buffer.getInt("flags") + 1);
        if (buffer.getInt("flags") <= requiredFlags - 1 && currentTime - zacPlayer.cache.lastEntityNearby > 1000 ||
                buffer.getInt("flags") <= requiredFlags)
            return;

        Set<Player> players = getPlayersForEnchantsSquared(zacPlayer, player);
        Scheduler.runTask(true, () -> {
            if (zacPlayer.isGliding() || zacPlayer.isRiptiding()) {
                buffer.put("speedTicks", 0);
                return;
            }

            if (isEnchantsSquaredImpact(players))
                return;

            if (AccuracyUtil.isViolationCancel(getCheckSetting(), buffer))
                return;
            callViolationEventIfRepeat(player, zacPlayer, event, buffer, 5000);
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void beforeMovement(ZACAsyncPlayerMoveEvent event) {
        if (ValhallaMMOHook.isPluginInstalled()) return;
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (getEffectAmplifier(zacPlayer.cache, PotionEffectType.SPEED) > 5) {
            Buffer buffer = getBuffer(player, true);
            buffer.put("effectTime", System.currentTimeMillis());
        }

        for (Block block : event.getToWithinBlocks()) {
            if (isActuallyPassable(block))
                continue;
            if (block.getType().name().endsWith("_SLAB") && !VerUtil.isWatterLoggedSlab(block))
                continue;
            return;
        }

        Set<Material> downMaterials = new HashSet<>();
        downMaterials.addAll(event.getToDownMaterials());
        downMaterials.addAll(event.getFromDownMaterials());

        if (downMaterials.contains(Material.ICE) || downMaterials.contains(Material.PACKED_ICE) ||
                downMaterials.contains(VerUtil.material.get("BLUE_ICE"))) {
            Buffer buffer = getBuffer(player, true);
            buffer.put("iceTime", System.currentTimeMillis());
        }

        if (downMaterials.contains(Material.SOUL_SAND) || downMaterials.contains(VerUtil.material.get("SOUL_SOIL"))) {
            ItemStack boots = zacPlayer.getArmorPiece(EquipmentSlot.FEET);
            if (boots != null && boots.getEnchantmentLevel(VerUtil.enchantment.get("SOUL_SPEED")) != 0) {
                Buffer buffer = getBuffer(player, true);
                buffer.put("soulSpeedTime", System.currentTimeMillis());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onTeleport(PlayerTeleportEvent event) {
        if (isExternalNPC(event)) return;
        if (ValhallaMMOHook.isPluginInstalled()) return;
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("flags", 0);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (isExternalNPC(event)) return;
        if (ValhallaMMOHook.isPluginInstalled()) return;
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("flags", 0);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onRespawn(PlayerRespawnEvent event) {
        if (isExternalNPC(event)) return;
        if (ValhallaMMOHook.isPluginInstalled()) return;
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("flags", 0);
    }

}
