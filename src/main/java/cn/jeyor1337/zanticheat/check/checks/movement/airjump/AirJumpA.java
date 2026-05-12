package cn.jeyor1337.zanticheat.check.checks.movement.airjump;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.movement.MovementCheck;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.event.playerplaceblock.ZACAsyncPlayerPlaceBlockEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.util.hook.plugin.FloodgateHook;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.VerUtil;
import cn.jeyor1337.zanticheat.version.identifier.ZACVersion;
import cn.jeyor1337.zanticheat.version.identifier.VerIdentifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

public class AirJumpA extends MovementCheck implements Listener {
    public AirJumpA() {
        super(CheckName.AIRJUMP_A);
    }

    @Override
    public boolean isConditionAllowed(Player player, ZACPlayer zacPlayer, PlayerCache cache, boolean isClimbing, boolean isInWater,
                                      boolean isFlying, boolean isInsideVehicle, boolean isGliding, boolean isRiptiding) {
        if (isFlying || isInsideVehicle || isClimbing || isGliding || isRiptiding || isInWater)
            return false;
        if (cache.flyingTicks >= -15 || cache.climbingTicks >= -2 ||
                cache.glidingTicks >= -3 || cache.riptidingTicks >= -5)
            return false;
        long time = System.currentTimeMillis();
        return time - cache.lastInsideVehicle > 150 && time - cache.lastInWater > 150 &&
                time - cache.lastKnockback > 750 && time - cache.lastKnockbackNotVanilla > 3000 &&
                time - cache.lastWasFished > 4000 && time - cache.lastTeleport > 700 &&
                time - cache.lastRespawn > 500 && time - cache.lastEntityVeryNearby > 700 &&
                time - cache.lastBlockExplosion > 5000 && time - cache.lastEntityExplosion > 3000 &&
                time - cache.lastSlimeBlockVertical > 4000 && time - cache.lastSlimeBlockHorizontal > 2500 &&
                time - cache.lastHoneyBlockVertical > 2500 && time - cache.lastHoneyBlockHorizontal > 1500 &&
                time - cache.lastPowderSnowWalk > 750 &&
                time - cache.lastWasHit > 350 && time - cache.lastWasDamaged > 150 &&
                time - cache.lastKbVelocity > 1000 && time - cache.lastAirKbVelocity > 2000 &&
                time - cache.lastStrongKbVelocity > 5000 && time - cache.lastStrongAirKbVelocity > 15 * 1000 &&
                time - cache.lastFlight > 750 &&
                time - cache.lastGliding > 2000 && time - cache.lastRiptiding > 3500 &&
                time - cache.lastWindCharge > 1000 && time - cache.lastWindChargeReceive > 500 &&
                time - cache.lastWindBurst > 1500 && time - cache.lastWindBurstNotVanilla > 4000;
    }

    @EventHandler
    public void onAsyncMovement(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        PlayerCache cache = zacPlayer.cache;
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true)) {
            buffer.put("airJump", 0);
            buffer.put("airJumpTicks", 0);
            return;
        }

        if (!isConditionAllowed(player, zacPlayer, event)) {
            buffer.put("airJumpTicks", 0);
            if (System.currentTimeMillis() - cache.lastTeleport > 700)
                buffer.put("airJump", 0);
            return;
        }

        if (!event.isToWithinBlocksPassable() || !event.isFromWithinBlocksPassable()) {
            buffer.put("airJump", 0);
            buffer.put("airJumpTicks", 0);
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - cache.lastEntityNearby <= 1000 ||
                currentTime - buffer.getLong("effectTime") <= 2000 ||
                currentTime - buffer.getLong("lastScaffoldPlace") <= 400L) {
            buffer.put("airJump", 0);
            buffer.put("airJumpTicks", 0);
            return;
        }

        for (int i = 0; i < 3 && i < HistoryElement.values().length; i++) {
            if (cache.history.onEvent.onGround.get(HistoryElement.values()[i]).towardsTrue ||
                    cache.history.onPacket.onGround.get(HistoryElement.values()[i]).towardsTrue) {
                buffer.put("airJump", 0);
                buffer.put("airJumpTicks", 0);
                return;
            }
        }

        if (event.getFrom().getBlockY() > event.getTo().getBlockY() ||
                event.getFrom().getY() > event.getTo().getY() && getBlockY(event.getTo().getY()) == 0) {
            if (!event.isToDownBlocksPassable()) {
                buffer.put("airJump", 0);
                buffer.put("airJumpTicks", 0);
                return;
            }
            for (Block block : event.getToDownBlocks()) {
                if (!isActuallyPassable(block.getRelative(BlockFace.DOWN))) {
                    buffer.put("airJump", 0);
                    buffer.put("airJumpTicks", 0);
                    return;
                }
            }
        }

        buffer.put("airJumpTicks", buffer.getInt("airJumpTicks") + 1);
        if (buffer.getInt("airJumpTicks") <= 2)
            return;

        double verticalDistance = distanceVertical(event.getFrom(), event.getTo());
        if (buffer.getInt("airJump") == 0) {
            if (verticalDistance > 0.05)
                buffer.put("airJump", 1);
            return;
        }

        if (buffer.getInt("airJump") == 1) {
            if (verticalDistance < -0.125)
                buffer.put("airJump", 2);
            return;
        }

        if (buffer.getInt("airJump") != 2 || VerIdentifier.getVersion().isOlderThan(ZACVersion.V1_9))
            return;

        boolean isBedrockPlayer = FloodgateHook.isBedrockPlayer(player, true);
        double secondJumpThreshold = !isBedrockPlayer ? 0.125 : 0.19;
        if (FloodgateHook.isProbablyPocketEditionPlayer(player, true))
            secondJumpThreshold += 0.04;
        if (verticalDistance <= secondJumpThreshold)
            return;

        buffer.put("airJump", 1);
        Set<Player> players = getPlayersForEnchantsSquared(zacPlayer, player);
        Scheduler.runTask(true, () -> {
            if (currentTime - buffer.getLong("lastScaffoldPlace") <= 400L ||
                    zacPlayer.isGliding() || zacPlayer.isRiptiding()) {
                buffer.put("airJump", 0);
                buffer.put("airJumpTicks", 0);
                return;
            }
            if (isEnchantsSquaredImpact(players))
                return;
            if (getItemStackAttributes(player, "GENERIC_JUMP_STRENGTH") > 0.15 ||
                    getPlayerAttributes(player).getOrDefault("GENERIC_JUMP_STRENGTH", 0.42) > 0.43)
                return;
            if (isLagGlidingPossible(player, buffer) || isPingGlidingPossible(player, cache))
                return;

            long repeatWindow = !isBedrockPlayer ? Main.getBufferDurationMils() - 1000L : 2500L;
            if (FloodgateHook.isProbablyPocketEditionPlayer(player, true))
                repeatWindow += 500L;
            callViolationEventIfRepeat(player, zacPlayer, event, buffer, repeatWindow);
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void beforeMovement(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (getEffectAmplifier(zacPlayer.cache, VerUtil.potions.get("LEVITATION")) > 0 ||
                getEffectAmplifier(zacPlayer.cache, VerUtil.potions.get("SLOW_FALLING")) > 1 ||
                getEffectAmplifier(zacPlayer.cache, PotionEffectType.JUMP) > 6) {
            Buffer buffer = getBuffer(player, true);
            buffer.put("effectTime", System.currentTimeMillis());
        }
    }

    @EventHandler
    public void scaffoldAsyncBlockPlace(ZACAsyncPlayerPlaceBlockEvent event) {
        if (isActuallyPassable(event.getBlock()))
            return;
        Block placedBlock = event.getBlock();
        boolean within = false;
        for (Block block : getWithinBlocks(event.getPlayer())) {
            if (!equals(placedBlock, block) &&
                    !equals(placedBlock, block.getRelative(BlockFace.DOWN)))
                continue;
            within = true;
            break;
        }
        if (!within)
            return;
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("lastScaffoldPlace", System.currentTimeMillis());
    }

    private static boolean equals(Block block1, Block block2) {
        return block1.getX() == block2.getX() &&
                block1.getY() == block2.getY() &&
                block1.getZ() == block2.getZ();
    }
}
