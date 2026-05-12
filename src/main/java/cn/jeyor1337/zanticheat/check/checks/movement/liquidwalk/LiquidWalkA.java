package cn.jeyor1337.zanticheat.check.checks.movement.liquidwalk;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.movement.MovementCheck;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.player.cache.history.PlayerCacheHistory;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;

/**
 * Jesus hack
 */
public class LiquidWalkA extends MovementCheck implements Listener {
    public LiquidWalkA() {
        super(CheckName.LIQUIDWALK_A);
    }

    @Override
    public boolean isConditionAllowed(Player player, ZACPlayer zacPlayer, PlayerCache cache, boolean isClimbing, boolean isInWater,
                                      boolean isFlying, boolean isInsideVehicle, boolean isGliding, boolean isRiptiding) {
        if (isFlying || isInsideVehicle || isClimbing || isGliding || isRiptiding || isInWater)
            return false;
        if (cache.flyingTicks >= -5 || cache.climbingTicks >= -2 ||
                cache.glidingTicks >= -3 || cache.riptidingTicks >= -4)
            return false;
        long time = System.currentTimeMillis();
        return time - cache.lastInsideVehicle > 150 && time - cache.lastInWater > 150 &&
                time - cache.lastKnockback > 250 && time - cache.lastKnockbackNotVanilla > 1000 &&
                time - cache.lastWasFished > 1000 && time - cache.lastTeleport > 500 &&
                time - cache.lastRespawn > 500 && time - cache.lastEntityVeryNearby > 200 &&
                time - cache.lastBlockExplosion > 2000 && time - cache.lastEntityExplosion > 1000 &&
                time - cache.lastSlimeBlockVertical > 3000 && time - cache.lastSlimeBlockHorizontal > 2500 &&
                time - cache.lastHoneyBlockVertical > 3000 && time - cache.lastHoneyBlockHorizontal > 3000 &&
                time - cache.lastWasHit > 150 && time - cache.lastWasDamaged > 50 &&
                time - cache.lastFlight > 750;
    }

    @EventHandler
    public void onAsyncMovement(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        PlayerCache cache = zacPlayer.cache;
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (!isConditionAllowed(player, zacPlayer, event))
            return;

        if (!event.isToWithinBlocksPassable() || !event.isFromWithinBlocksPassable())
            return;

        if (getEffectAmplifier(cache, VerUtil.potions.get("LEVITATION")) != 0)
            return;

        for (HistoryElement element : HistoryElement.values()) {
            if (cache.history.onEvent.onGround.get(element).towardsTrue ||
                    cache.history.onPacket.onGround.get(element).towardsTrue)
                return;
        }

        PlayerCacheHistory<Location> locationHistory = zacPlayer.cache.history.onEvent.location;
        if (distanceAbsVertical(event.getFrom(), event.getTo()) >= LOWEST_BLOCK_HEIGHT ||
                distanceAbsVertical(locationHistory.get(HistoryElement.FIRST), event.getFrom()) >= LOWEST_BLOCK_HEIGHT ||
                distanceAbsVertical(locationHistory.get(HistoryElement.SECOND), locationHistory.get(HistoryElement.FIRST)) >= LOWEST_BLOCK_HEIGHT)
            return;

        if (distanceHorizontal(event.getFrom(), event.getTo()) <= 0.05)
            return;

        boolean downLiquid = true;
        for (Block block : getDownBlocks(player, event.getTo(), 0.15)) {
            if (block.isLiquid())
                continue;
            downLiquid = false;
            break;
        }
        if (!downLiquid) {
            double subtract = event.getTo().getY() % 1;
            if (!(subtract < -0.8 || subtract > 0 && subtract < 0.2))
                return;
            if (event.getTo().getY() < 0) subtract = 1 + subtract;
            for (Block block : getDownBlocks(player, event.getTo().subtract(0, subtract, 0), 0.18)) {
                if (!block.isLiquid())
                    return;
            }
        }

        Material honeyBlock = VerUtil.material.get("HONEY_BLOCK");
        for (Block block : getInteractiveBlocks(player, event.getTo())) {
            Material type = block.getType();
            if (type == Material.SLIME_BLOCK || type == honeyBlock)
                return;
        }

        updateDownBlocks(player, zacPlayer, event.getToDownBlocks());
        Scheduler.runTask(true, () -> {
            Buffer buffer = getBuffer(player);
            callViolationEventIfRepeat(player, zacPlayer, event, buffer, Main.getBufferDurationMils() - 1000L);
        });
    }

}
