package cn.jeyor1337.zanticheat.check.checks.movement.nofall;


import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.movement.MovementCheck;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;

/**
 * Spoof of Entity.isOnGround()
 */
public class NoFallB extends MovementCheck implements Listener {
    public NoFallB() {
        super(CheckName.NOFALL_B);
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
                time - cache.lastWasFished > 4000 && time - cache.lastTeleport > 900 &&
                time - cache.lastRespawn > 500 && time - cache.lastEntityVeryNearby > 700 &&
                time - cache.lastBlockExplosion > 4000 && time - cache.lastEntityExplosion > 2000 &&
                time - cache.lastSlimeBlockVertical > 2500 && time - cache.lastSlimeBlockHorizontal > 2500 &&
                time - cache.lastHoneyBlockVertical > 2500 && time - cache.lastHoneyBlockHorizontal > 2500 &&
                time - cache.lastWasHit > 350 && time - cache.lastWasDamaged > 150 &&
                time - cache.lastFlight > 750;
    }

    @EventHandler
    public void onAsyncMovement(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        PlayerCache cache = zacPlayer.cache;
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, zacPlayer, true)) {
            buffer.put("fallEvents", 0);
            return;
        }

        if (!isConditionAllowed(player, zacPlayer, event)) {
            buffer.put("fallEvents", 0);
            return;
        }

        if (!event.isToWithinBlocksPassable() || !event.isFromWithinBlocksPassable()) {
            buffer.put("fallEvents", 0);
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - cache.lastEntityNearby <= 3000) {
            buffer.put("fallEvents", 0);
            return;
        }

        if (currentTime - buffer.getLong("effectTime") < 1000) {
            buffer.put("fallEvents", 0);
            return;
        }

        if (!event.isToDownBlocksPassable()) {
            buffer.put("fallEvents", 0);
            return;
        }

        if (event.getFrom().getBlockY() > event.getTo().getBlockY() ||
                event.getFrom().getY() > event.getTo().getY() && getBlockY(event.getTo().getY()) == 0) {
            for (Block block : getDownBlocks(player, event.getTo(), Double.MIN_VALUE * 100)) {
                if (!isActuallyPassable(block.getRelative(BlockFace.DOWN))) {
                    buffer.put("fallEvents", 0);
                    return;
                }
            }
        }

        for (int i = 0; i < 3 && i < HistoryElement.values().length; i++)
            if (cache.history.onEvent.onGround.get(HistoryElement.values()[i]).towardsTrue ||
                    cache.history.onPacket.onGround.get(HistoryElement.values()[i]).towardsTrue) {
                buffer.put("fallEvents", 0);
                return;
            }

        Location newerLocation = event.getTo();
        for (int i = 0; i < 3 && i < HistoryElement.values().length; i++) {
            Location location = cache.history.onEvent.location.get(HistoryElement.values()[i]);
            double vSpeed = distanceVertical(location, newerLocation);
            newerLocation = location;
            if (vSpeed > -0.00001) {
                buffer.put("fallEvents", 0);
                return;
            }
        }

        if (event.getTo().getY() % 0.5 == 0) {
            buffer.put("fallEvents", 0);
            return;
        }

        buffer.put("fallEvents", buffer.getInt("fallEvents") + 1);

        if (buffer.getInt("fallEvents") <= 3)
            return;

        if (!((LivingEntity) player).isOnGround())
            return;

        updateDownBlocks(player, zacPlayer, event.getToDownBlocks());
        int jumpEffectAmplifier = getEffectAmplifier(zacPlayer.cache, PotionEffectType.JUMP);
        Scheduler.runTask(true, () -> {
            if (jumpEffectAmplifier <= 2) callViolationEventIfRepeat(player, zacPlayer, null, buffer, 3000);
            else callViolationEventIfRepeat(player, zacPlayer, null, buffer, 600);
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void beforeMovement(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (getEffectAmplifier(zacPlayer.cache, VerUtil.potions.get("LEVITATION")) > 0 ||
                getEffectAmplifier(zacPlayer.cache, VerUtil.potions.get("SLOW_FALLING")) > 0) {
            Buffer buffer = getBuffer(player, true);
            long currentTime = System.currentTimeMillis();
            buffer.put("effectTime", currentTime);
        }
    }

}
