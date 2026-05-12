package cn.jeyor1337.zanticheat.check.checks.movement.jump;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.movement.MovementCheck;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.player.cache.history.PlayerCacheHistory;
import cn.jeyor1337.zanticheat.util.detection.CheckUtil;
import cn.jeyor1337.zanticheat.util.hook.server.folia.FoliaUtil;
import cn.jeyor1337.zanticheat.util.hook.plugin.simplehook.EnchantsSquaredHook;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;

/**
 * Jump height
 */
public class JumpB extends MovementCheck implements Listener {
    public JumpB() {
        super(CheckName.JUMP_B);
    }

    @Override
    public boolean isConditionAllowed(Player player, ZACPlayer zacPlayer, PlayerCache cache, boolean isClimbing, boolean isInWater,
                                      boolean isFlying, boolean isInsideVehicle, boolean isGliding, boolean isRiptiding) {
        if (isFlying || isInsideVehicle || isClimbing || isGliding || isRiptiding || isInWater)
            return false;
        if (cache.flyingTicks >= -5 || cache.climbingTicks >= -4 ||
                cache.glidingTicks >= -6 || cache.riptidingTicks >= -10)
            return false;
        long time = System.currentTimeMillis();
        return time - cache.lastInsideVehicle > 300 && time - cache.lastInWater > 300 &&
                time - cache.lastKnockback > 750 && time - cache.lastKnockbackNotVanilla > 3000 &&
                time - cache.lastWasFished > 5000 && time - cache.lastTeleport > 700 &&
                time - cache.lastRespawn > 500 && time - cache.lastEntityVeryNearby > 700 &&
                time - cache.lastBlockExplosion > 5000 && time - cache.lastEntityExplosion > 3000 &&
                time - cache.lastSlimeBlockVertical > 4000 && time - cache.lastSlimeBlockHorizontal > 2500 &&
                time - cache.lastHoneyBlockVertical > 2500 && time - cache.lastHoneyBlockHorizontal > 1500 &&
                time - cache.lastPowderSnowWalk > 750 &&
                time - cache.lastWasHit > 350 && time - cache.lastWasDamaged > 150 &&
                time - cache.lastStrongKbVelocity > 5000 && time - cache.lastStrongAirKbVelocity > 10 * 1000 &&
                time - cache.lastFlight > 1200 &&
                time - cache.lastWindCharge > 1000 && time - cache.lastWindChargeReceive > 500 &&
                time - cache.lastWindBurst > 1500 && time - cache.lastWindBurstNotVanilla > 4000;
    }

    @EventHandler
    public void onAsyncMovement(ZACAsyncPlayerMoveEvent event) {
        if (FoliaUtil.isFolia())
            return;

        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true)) {
            buffer.put("jumpHeight", 0.0);
            return;
        }
        if (!isConditionAllowed(player, zacPlayer, event)) {
            buffer.put("jumpHeight", 0.0);
            return;
        }

        if (System.currentTimeMillis() - buffer.getLong("lastVelocity") < 1750) {
            buffer.put("jumpHeight", 0.0);
            return;
        }

        PlayerCache cache = zacPlayer.cache;
        PlayerCache.History history = cache.history;
        if (isBlockHeight((float) getBlockY(event.getTo().getY())) ||
                history.onEvent.onGround.get(HistoryElement.FROM).towardsTrue ||
                history.onPacket.onGround.get(HistoryElement.FROM).towardsTrue) {
            buffer.put("jumpHeight", 0.0);
            return;
        }

        if (!event.isToWithinBlocksPassable() || !event.isFromWithinBlocksPassable()) {
            buffer.put("jumpHeight", 0.0);
            return;
        }

        int jumpEffectAmplifier = getEffectAmplifier(cache, PotionEffectType.JUMP);
        if (getEffectAmplifier(cache, VerUtil.potions.get("LEVITATION")) > 0 ||
                jumpEffectAmplifier > 5) {
            buffer.put("jumpHeight", 0.0);
            return;
        }

        if (buffer.isExists("lastJumpEffect") && buffer.getInt("lastJumpEffect") != jumpEffectAmplifier) {
            buffer.put("lastJumpEffect", jumpEffectAmplifier);
            buffer.put("jumpHeight", 0.0);
            return;
        }
        buffer.put("lastJumpEffect", jumpEffectAmplifier);

        Set<Material> interactiveMaterials = new HashSet<>();
        getInteractiveBlocks(player, event.getFrom()).forEach(block -> {
            interactiveMaterials.add(block.getType());
            interactiveMaterials.add(block.getRelative(BlockFace.UP).getType());
        });
        getInteractiveBlocks(player, event.getTo()).forEach(block -> {
            interactiveMaterials.add(block.getType());
            interactiveMaterials.add(block.getRelative(BlockFace.UP).getType());
        });
        if (interactiveMaterials.contains(Material.SLIME_BLOCK) || interactiveMaterials.contains(VerUtil.material.get("HONEY_BLOCK"))) {
            buffer.put("jumpHeight", 0.0);
            return;
        }

        Set<Material> downMaterials = new HashSet<>();
        getDownBlocks(player, event.getTo(), 0.45).forEach(block -> downMaterials.add(block.getType()));
        getDownBlocks(player, event.getFrom(), 0.45).forEach(block -> {
            downMaterials.add(block.getType());
            downMaterials.add(block.getRelative(BlockFace.DOWN).getType());
        });
        getDownBlocks(player, cache.history.onEvent.location.get(HistoryElement.FIRST), 0.40)
                .forEach(block -> downMaterials.add(block.getType()));
        if (downMaterials.contains(Material.SLIME_BLOCK) || downMaterials.contains(VerUtil.material.get("HONEY_BLOCK"))) {
            buffer.put("jumpHeight", 0.0);
            return;
        }

        PlayerCacheHistory<Location> eventHistory = history.onEvent.location;
        PlayerCacheHistory<Location> packetHistory = history.onEvent.location;
        double previousEventSpeed = distanceVertical(eventHistory.get(HistoryElement.FIRST), eventHistory.get(HistoryElement.FROM));
        double eventSpeed = distanceVertical(eventHistory.get(HistoryElement.FROM), event.getTo());
        double previousPacketSpeed = distanceVertical(packetHistory.get(HistoryElement.SECOND), packetHistory.get(HistoryElement.FIRST));
        double packetSpeed = distanceVertical(packetHistory.get(HistoryElement.FIRST), packetHistory.get(HistoryElement.FROM));

        if (previousEventSpeed <= 0 || eventSpeed <= 0 || previousEventSpeed < eventSpeed ||
                previousPacketSpeed <= 0 || packetSpeed <= 0 || previousPacketSpeed < packetSpeed) {
            buffer.put("jumpHeight", 0.0);
            return;
        }

        buffer.put("jumpHeight", buffer.getDouble("jumpHeight") + eventSpeed);

        double maxJumpHeight = Double.MAX_VALUE;
        if (jumpEffectAmplifier == 0) maxJumpHeight = 0.499;
        else if (jumpEffectAmplifier == 1) maxJumpHeight = 0.885;
        else if (jumpEffectAmplifier == 2) maxJumpHeight = 1.368;
        else if (jumpEffectAmplifier == 3) maxJumpHeight = 1.944;
        else if (jumpEffectAmplifier == 4) maxJumpHeight = 2.609;
        else if (jumpEffectAmplifier == 5) maxJumpHeight = 3.360;
        maxJumpHeight = maxJumpHeight * 1.2 + 0.25;

        double attributeAmount = Math.max(
                getItemStackAttributes(player, "GENERIC_JUMP_STRENGTH"),
                getPlayerAttributes(player).getOrDefault("GENERIC_JUMP_STRENGTH", 0.42) - 0.42
        );
        if (attributeAmount != 0)
            buffer.put("attribute", System.currentTimeMillis());
        else if (System.currentTimeMillis() - buffer.getLong("attribute") < 4000)
            return;
        if (attributeAmount != 0) {
            if (attributeAmount <= 0.5)
                maxJumpHeight += 20.0;
            else if (attributeAmount <= 1.0)
                maxJumpHeight += 40.0;
            else
                maxJumpHeight += 80.0;
        }

        double jumpHeight = buffer.getDouble("jumpHeight");
        if (jumpHeight <= maxJumpHeight)
            return;

        Set<Player> players = getPlayersForEnchantsSquared(zacPlayer, player);
        double finalMaxJumpHeight = maxJumpHeight;
        Scheduler.runTask(true, () -> {
            if (EnchantsSquaredHook.hasEnchantment(player, "Burden") &&
                    jumpHeight * 0.9 - 0.5 <= finalMaxJumpHeight)
                return;

            if (isEnchantsSquaredImpact(players) && jumpHeight * 0.7 - 2.2 <= finalMaxJumpHeight)
                return;

            callViolationEventIfRepeat(player, zacPlayer, event, buffer, Main.getBufferDurationMils() - 1000L);
        });
    }

    @EventHandler
    public void onVelocity(PlayerVelocityEvent event) {
        if (FoliaUtil.isFolia())
            return;

        if (CheckUtil.isExternalNPC(event)) return;
        double yVelocity = event.getVelocity().getY();
        if (yVelocity < -0.0784000015258789 + 0.005 &&
                yVelocity > -0.0784000015258789 - 0.005)
            return;
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("lastVelocity", System.currentTimeMillis());
    }

}
