package cn.jeyor1337.zanticheat.check.checks.movement.step;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.movement.MovementCheck;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.util.hook.plugin.FloodgateHook;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;

/**
 * Step hack (1.5+ blocks)
 */
public class StepA extends MovementCheck implements Listener {
    public StepA() {
        super(CheckName.STEP_A);
    }

    @Override
    public boolean isConditionAllowed(Player player, ZACPlayer zacPlayer, PlayerCache cache, boolean isClimbing, boolean isInWater,
                                      boolean isFlying, boolean isInsideVehicle, boolean isGliding, boolean isRiptiding) {
        if (isFlying || isInsideVehicle || isClimbing || isGliding || isRiptiding || isInWater)
            return false;
        if (cache.flyingTicks >= -4 || cache.climbingTicks >= -2 ||
                cache.glidingTicks >= -3 || cache.riptidingTicks >= -3)
            return false;
        long time = System.currentTimeMillis();
        return time - cache.lastInsideVehicle > 150 && time - cache.lastInWater > 150 &&
                time - cache.lastKnockback > 750 && time - cache.lastKnockbackNotVanilla > 3000 &&
                time - cache.lastWasFished > 4000 && time - cache.lastTeleport > 700 &&
                time - cache.lastRespawn > 500 && time - cache.lastEntityVeryNearby > 200 &&
                time - cache.lastBlockExplosion > 5000 && time - cache.lastEntityExplosion > 3000 &&
                time - cache.lastSlimeBlockVertical > 4000 && time - cache.lastSlimeBlockHorizontal > 2000 &&
                time - cache.lastHoneyBlockVertical > 2500 && time - cache.lastHoneyBlockHorizontal > 2000 &&
                time - cache.lastWasHit > 350 && time - cache.lastWasDamaged > 150 &&
                time - cache.lastKbVelocity > 250 && time - cache.lastAirKbVelocity > 500 &&
                time - cache.lastStrongKbVelocity > 1250 && time - cache.lastStrongAirKbVelocity > 2500 &&
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

        if (!event.isFromWithinBlocksPassable() || !event.isToWithinBlocksPassable())
            return;

        if (FloodgateHook.isCancelledMovement(getCheckSetting().name, player, true))
            return;

        if (getEffectAmplifier(cache, VerUtil.potions.get("LEVITATION")) > 0 ||
                getEffectAmplifier(cache, VerUtil.potions.get("SLOW_FALLING")) > 1 ||
                getEffectAmplifier(cache, PotionEffectType.JUMP) > 2)
            return;

        double vSpeed = distanceVertical(event.getFrom(), event.getTo());
        if (vSpeed <= 0)
            return;
        if (Math.abs(vSpeed) < 1.5)
            return;

        if (!cache.history.onEvent.onGround.get(HistoryElement.FROM).towardsFalse ||
                !cache.history.onPacket.onGround.get(HistoryElement.FROM).towardsFalse)
            return;
        if (!isBlockHeight((float) getBlockY(event.getFrom().getY())))
            return;

        Buffer buffer = getBuffer(player, true);
        if (getItemStackAttributes(player, "GENERIC_STEP_HEIGHT") != 0)
            buffer.put("attribute", System.currentTimeMillis());
        if (System.currentTimeMillis() - buffer.getLong("attribute") < 4000)
            return;

        Scheduler.runTask(true, () -> {
            callViolationEvent(player, zacPlayer, event);
        });
    }

}
