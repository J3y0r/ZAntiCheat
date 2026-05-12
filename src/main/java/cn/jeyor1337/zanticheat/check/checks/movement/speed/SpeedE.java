package cn.jeyor1337.zanticheat.check.checks.movement.speed;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.movement.MovementCheck;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.util.cooldown.CooldownUtil;
import cn.jeyor1337.zanticheat.util.hook.plugin.FloodgateHook;
import cn.jeyor1337.zanticheat.util.hook.server.folia.FoliaUtil;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

/**
 * The absolute horizontal, vertical and absolute speed limiter
 */
public class SpeedE extends MovementCheck implements Listener {
    private static final double BEDROCK_HORIZONTAL_TELEPORT_LIMIT = 8.0;
    private static final double BEDROCK_VERTICAL_TELEPORT_LIMIT = 14.0;
    private static final double POCKET_HORIZONTAL_TELEPORT_LIMIT = 9.0;
    private static final double POCKET_VERTICAL_TELEPORT_LIMIT = 15.0;

    public SpeedE() {
        super(CheckName.SPEED_E);
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
                time - cache.lastWasFished > 4000 && time - cache.lastTeleport > 600 &&
                time - cache.lastRespawn > 500 && time - cache.lastEntityVeryNearby > 400 &&
                time - cache.lastBlockExplosion > 5000 && time - cache.lastEntityExplosion > 3000 &&
                time - cache.lastSlimeBlockVertical > 4000 && time - cache.lastSlimeBlockHorizontal > 3500 &&
                time - cache.lastHoneyBlockVertical > 2500 && time - cache.lastHoneyBlockHorizontal > 2500 &&
                time - cache.lastWasHit > 350 && time - cache.lastWasDamaged > 150 &&
                time - cache.lastKbVelocity > 250 && time - cache.lastAirKbVelocity > 500 &&
                time - cache.lastStrongKbVelocity > 1250 && time - cache.lastStrongAirKbVelocity > 2500 &&
                time - cache.lastFlight > 750;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void afterMovement(ZACAsyncPlayerMoveEvent event) {
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("lastMovement", System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onTeleport(PlayerTeleportEvent event) {
        if (isExternalNPC(event)) return;
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("flags", 0);
        buffer.put("lastTeleport", System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (isExternalNPC(event)) return;
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("flags", 0);
        buffer.put("lastTeleport", System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onRespawn(PlayerRespawnEvent event) {
        if (isExternalNPC(event)) return;
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("flags", 0);
        buffer.put("lastTeleport", System.currentTimeMillis());
    }

    @EventHandler
    public void onTeleportHorizontal(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        PlayerCache cache = zacPlayer.cache;
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, zacPlayer, true))
            return;
        boolean isBedrockPlayer = FloodgateHook.isBedrockPlayer(player, true);
        boolean isPocketPlayer = FloodgateHook.isProbablyPocketEditionPlayer(player, true);

        if (!isConditionAllowed(player, zacPlayer, event))
            return;

        Buffer buffer = getBuffer(player, true);
        if (System.currentTimeMillis() - buffer.getLong("lastTeleport") < 1000)
            return;

        if (getEffectAmplifier(cache, PotionEffectType.SPEED) > 6 ||
                getEffectAmplifier(cache, VerUtil.potions.get("DOLPHINS_GRACE")) > 3)
            return;

        double maxDistance = 6;
        if (isBedrockPlayer)
            maxDistance = isPocketPlayer ? POCKET_HORIZONTAL_TELEPORT_LIMIT : BEDROCK_HORIZONTAL_TELEPORT_LIMIT;
        if (distanceHorizontal(event.getFrom(), event.getTo()) <= maxDistance)
            return;

        event.setCancelled(true);
        FoliaUtil.teleportPlayer(player, event.getFrom());

        Scheduler.runTaskLater(() -> {
            if (System.currentTimeMillis() - buffer.getLong("lastTeleport") < 1000)
                return;
            if (isBedrockPlayer && buffer.getInt("flags") < (isPocketPlayer ? 2 : 1)) {
                buffer.put("flags", buffer.getInt("flags") + 1);
                return;
            }
            callViolationEvent(player, zacPlayer, event);
        }, 1);
    }

    @EventHandler
    public void onTeleportVertical(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        PlayerCache cache = zacPlayer.cache;
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, zacPlayer, true))
            return;
        boolean isBedrockPlayer = FloodgateHook.isBedrockPlayer(player, true);
        boolean isPocketPlayer = FloodgateHook.isProbablyPocketEditionPlayer(player, true);

        if (!isConditionAllowed(player, zacPlayer, event))
            return;

        Buffer buffer = getBuffer(player, true);
        if (System.currentTimeMillis() - buffer.getLong("lastTeleport") < 1000)
            return;

        if (getEffectAmplifier(cache, PotionEffectType.SPEED) > 6 ||
                getEffectAmplifier(cache, VerUtil.potions.get("DOLPHINS_GRACE")) > 3)
            return;

        double maxDistance = 12;
        if (isBedrockPlayer)
            maxDistance = isPocketPlayer ? POCKET_VERTICAL_TELEPORT_LIMIT : BEDROCK_VERTICAL_TELEPORT_LIMIT;
        if (distanceVertical(event.getFrom(), event.getTo()) <= maxDistance)
            return;

        event.setCancelled(true);
        FoliaUtil.teleportPlayer(player, event.getFrom());

        Scheduler.runTaskLater(() -> {
            if (System.currentTimeMillis() - buffer.getLong("lastTeleport") < 1000)
                return;
            if (isBedrockPlayer && buffer.getInt("flags") < (isPocketPlayer ? 2 : 1)) {
                buffer.put("flags", buffer.getInt("flags") + 1);
                return;
            }
            callViolationEvent(player, zacPlayer, event);
        }, 1);
    }

    @EventHandler
    public void onHorizontal(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        PlayerCache cache = zacPlayer.cache;
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true))
            return;
        boolean isBedrockPlayer = FloodgateHook.isBedrockPlayer(player, true);
        boolean isPocketPlayer = FloodgateHook.isProbablyPocketEditionPlayer(player, true);

        if (!isConditionAllowed(player, zacPlayer, event))
            return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - zacPlayer.joinTime < 7500)
            return;
        if (currentTime - buffer.getLong("lastMovement") > 1000)
            return;

        if (!event.isFromWithinBlocksPassable() || !event.isToWithinBlocksPassable())
            return;

        if (getEffectAmplifier(cache, PotionEffectType.SPEED) > 4 ||
                getEffectAmplifier(cache, VerUtil.potions.get("DOLPHINS_GRACE")) > 2)
            return;

        double preHSpeed1 = distanceHorizontal(cache.history.onEvent.location.get(HistoryElement.SECOND), event.getTo()) / 3.0;
        double preHSpeed2 = distanceHorizontal(cache.history.onEvent.location.get(HistoryElement.FIRST), event.getTo()) / 2.0;
        double preHSpeed3 = distanceHorizontal(cache.history.onEvent.location.get(HistoryElement.SECOND), event.getFrom()) / 2.0;

        double hSpeed = Math.min(preHSpeed1, Math.min(preHSpeed2, preHSpeed3));
        hSpeed /= player.getWalkSpeed() / 0.2;

        double maxSpeed = 3.0;
        if (getEffectAmplifier(cache, PotionEffectType.SPEED) > 3)
            maxSpeed *= 2.5;
        else if (getEffectAmplifier(cache, PotionEffectType.SPEED) > 2)
            maxSpeed *= 2;
        if (getEffectAmplifier(cache, VerUtil.potions.get("DOLPHINS_GRACE")) > 1)
            maxSpeed *= 2.5;

        Map<String, Double> attributes = getPlayerAttributes(player);
        double attributeAmount = Math.max(
                getItemStackAttributes(player,
                        "GENERIC_WATER_MOVEMENT_EFFICIENCY", "PLAYER_SNEAKING_SPEED",
                        "GENERIC_MOVEMENT_SPEED", "GENERIC_MOVEMENT_EFFICIENCY"
                ),
                Math.max(
                        Math.max(
                                attributes.getOrDefault("GENERIC_WATER_MOVEMENT_EFFICIENCY", 0.0),
                                attributes.getOrDefault("PLAYER_SNEAKING_SPEED", 0.0)
                        ),
                        Math.max(
                                attributes.getOrDefault("GENERIC_MOVEMENT_SPEED", 0.13) - 0.13,
                                attributes.getOrDefault("GENERIC_MOVEMENT_EFFICIENCY", 0.0)
                        )
                )
        );
        if (attributeAmount != 0) {
            maxSpeed = (maxSpeed * 1.05 + 0.11) * Math.max(1, attributeAmount * 13);
            buffer.put("attribute", System.currentTimeMillis());
        } else if (System.currentTimeMillis() - buffer.getLong("attribute") < 3000) {
            return;
        }

        if (isBedrockPlayer)
            maxSpeed = maxSpeed * (isPocketPlayer ? 1.3 : 1.18) + (isPocketPlayer ? 0.25 : 0.14);

        if (hSpeed < maxSpeed)
            return;

        buffer.put("flags", buffer.getInt("flags") + 1);
        int requiredFlags = isBedrockPlayer ? (isPocketPlayer ? 6 : 5) : 3;
        if (buffer.getInt("flags") <= requiredFlags)
            return;

        Scheduler.runTask(true, () -> callViolationEvent(player, zacPlayer, event));
    }

    @EventHandler
    public void onVertical(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        PlayerCache cache = zacPlayer.cache;
        Player player = event.getPlayer();

        if (zacPlayer.violations.getViolations(getCheckSetting(this).name) == 0)
            if (CooldownUtil.isSkip(150, zacPlayer.cooldown, this))
                return;

        if (!isCheckAllowed(player, zacPlayer, true))
            return;
        boolean isBedrockPlayer = FloodgateHook.isBedrockPlayer(player, true);
        boolean isPocketPlayer = FloodgateHook.isProbablyPocketEditionPlayer(player, true);

        if (event.isPlayerFlying() || event.isPlayerInsideVehicle() || event.isPlayerClimbing() ||
                event.isPlayerGliding() || event.isPlayerRiptiding() || event.isPlayerInWater())
            return;
        if (cache.flyingTicks >= -5 || cache.climbingTicks >= -2 ||
                cache.glidingTicks >= -3 || cache.riptidingTicks >= -5)
            return;
        long time = System.currentTimeMillis();
        boolean isConditionAllowed = time - cache.lastInsideVehicle > 150 && time - cache.lastInWater > 150 &&
                time - cache.lastKnockback > 750 && time - cache.lastKnockbackNotVanilla > 3000 &&
                time - cache.lastWasFished > 4000 && time - cache.lastTeleport > 2500 &&
                time - cache.lastRespawn > 500 && time - cache.lastEntityVeryNearby > 400 &&
                time - cache.lastBlockExplosion > 5000 && time - cache.lastEntityExplosion > 3000 &&
                time - cache.lastSlimeBlockVertical > 4000 && time - cache.lastSlimeBlockHorizontal > 2500 &&
                time - cache.lastHoneyBlockVertical > 2000 && time - cache.lastHoneyBlockHorizontal > 2000 &&
                time - cache.lastWasHit > 350 && time - cache.lastWasDamaged > 150 &&
                time - cache.lastKbVelocity > 500 && time - cache.lastAirKbVelocity > 1000 &&
                time - cache.lastStrongKbVelocity > 2500 && time - cache.lastStrongAirKbVelocity > 5000 &&
                time - cache.lastFlight > 750;
        if (!isConditionAllowed)
            return;

        if (System.currentTimeMillis() - zacPlayer.joinTime < 2000)
            return;
        if (!event.isToWithinBlocksPassable() || !event.isFromWithinBlocksPassable())
            return;

        if (getEffectAmplifier(cache, VerUtil.potions.get("LEVITATION")) > 1 ||
                getEffectAmplifier(cache, VerUtil.potions.get("SLOW_FALLING")) > 1 ||
                getEffectAmplifier(cache, PotionEffectType.JUMP) > 2)
            return;

        for (int i = 0; i < HistoryElement.values().length; i++) {
            if (cache.history.onEvent.onGround.get(HistoryElement.values()[i]).towardsFalse)
                break;
            if (HistoryElement.values()[i] == HistoryElement.TENTH)
                return;
        }
        for (int i = 0; i < HistoryElement.values().length; i++) {
            if (cache.history.onPacket.onGround.get(HistoryElement.values()[i]).towardsFalse)
                break;
            if (HistoryElement.values()[i] == HistoryElement.TENTH)
                return;
        }

        if (event.getFrom().getBlockY() > event.getTo().getBlockY() ||
                event.getFrom().getY() > event.getTo().getY() && getBlockY(event.getTo().getY()) == 0) {
            if (!event.isToDownBlocksPassable())
                return;
            for (Block block : event.getToDownBlocks()) {
                if (!isActuallyPassable(block.getRelative(BlockFace.DOWN)))
                    return;
            }
        }

        double preVSpeed1 = distanceAbsVertical(event.getFrom(), event.getTo());
        double preVSpeed2 = distanceAbsVertical(cache.history.onEvent.location.get(HistoryElement.FIRST), event.getTo()) / 2.0;
        double vSpeed = Math.min(preVSpeed1, preVSpeed2);
        double maxSpeed = 0.72 * 2.0;

        Buffer buffer = getBuffer(player, true);
        Map<String, Double> attributes = getPlayerAttributes(player);
        double attributeAmount = Math.max(
                getItemStackAttributes(player,
                        "GENERIC_WATER_MOVEMENT_EFFICIENCY", "GENERIC_MOVEMENT_SPEED",
                        "GENERIC_MOVEMENT_EFFICIENCY", "GENERIC_JUMP_STRENGTH"
                ),
                Math.max(
                        Math.max(
                                attributes.getOrDefault("GENERIC_WATER_MOVEMENT_EFFICIENCY", 0.0),
                                attributes.getOrDefault("GENERIC_MOVEMENT_SPEED", 0.13) - 0.13
                        ),
                        Math.max(
                                attributes.getOrDefault("GENERIC_MOVEMENT_EFFICIENCY", 0.0),
                                attributes.getOrDefault("GENERIC_JUMP_STRENGTH", 0.42) - 0.42
                        )
                )
        );
        if (attributeAmount != 0) {
            maxSpeed = (maxSpeed * 1.05 + 0.11) * Math.max(1, attributeAmount * 13);
            buffer.put("attribute", System.currentTimeMillis());
        } else if (System.currentTimeMillis() - buffer.getLong("attribute") < 3000) {
            return;
        }

        if (isBedrockPlayer)
            maxSpeed = maxSpeed * (isPocketPlayer ? 1.35 : 1.2) + (isPocketPlayer ? 0.2 : 0.12);

        if (vSpeed < maxSpeed)
            return;

        Scheduler.runTask(true, () -> {
            if (isPingGlidingPossible(player, cache))
                return;
            callViolationEventIfRepeat(player, zacPlayer, event, buffer, isBedrockPlayer ? 2000 : 1500);
        });
    }
}
