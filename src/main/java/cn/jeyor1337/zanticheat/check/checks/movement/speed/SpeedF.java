package cn.jeyor1337.zanticheat.check.checks.movement.speed;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.movement.MovementCheck;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.util.cooldown.CooldownUtil;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Speed while flying legally
 */
public class SpeedF extends MovementCheck implements Listener {
    public SpeedF() {
        super(CheckName.SPEED_F);
    }

    @Override
    public boolean isConditionAllowed(Player player, ZACPlayer zacPlayer, PlayerCache cache, boolean isClimbing, boolean isInWater,
                                      boolean isFlying, boolean isInsideVehicle, boolean isGliding, boolean isRiptiding) {
        if (isFlying || isInsideVehicle || isClimbing || isGliding || isRiptiding)
            return false;
        if (cache.flyingTicks <= 5 || cache.climbingTicks >= -2 ||
                cache.glidingTicks >= -3 || cache.riptidingTicks >= -5)
            return false;
        long time = System.currentTimeMillis();
        return time - cache.lastInsideVehicle > 150 &&
                time - cache.lastKnockback > 750 && time - cache.lastKnockbackNotVanilla > 3000 &&
                time - cache.lastWasFished > 4000 && time - cache.lastTeleport > 600 &&
                time - cache.lastRespawn > 500 && time - cache.lastEntityVeryNearby > 400 &&
                time - cache.lastBlockExplosion > 5000 && time - cache.lastEntityExplosion > 3000 &&
                time - cache.lastSlimeBlockVertical > 4000 && time - cache.lastSlimeBlockHorizontal > 3500 &&
                time - cache.lastHoneyBlockVertical > 2000 && time - cache.lastHoneyBlockHorizontal > 1750 &&
                time - cache.lastWasHit > 350 && time - cache.lastWasDamaged > 150 &&
                time - cache.lastFlight > 1500;
    }

    @EventHandler
    public void onAsyncMovement(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        PlayerCache cache = zacPlayer.cache;
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (zacPlayer.violations.getViolations(getCheckSetting(this).name) == 0 &&
                buffer.getInt("speedTicks") == 0)
            if (CooldownUtil.isSkip(190, zacPlayer.cooldown, this))
                return;

        if (!isCheckAllowed(player, zacPlayer, true)) {
            buffer.put("speedTicks", Math.max(buffer.getInt("speedTicks") - 1, 0));
            return;
        }

        if (!isConditionAllowed(player, zacPlayer, event)) {
            buffer.put("speedTicks", Math.max(buffer.getInt("speedTicks") - 1, 0));
            return;
        }

        double preSpeed1 = distance(event.getFrom(), event.getTo());
        double preSpeed2 = distance(cache.history.onEvent.location.get(HistoryElement.FIRST), event.getTo()) / 2.0;

        double speed = Math.min(preSpeed1, preSpeed2);
        speed /= player.getFlySpeed() / 0.1;

        double maxSpeed = 1.17;
        maxSpeed *= 1.4;

        if (speed < maxSpeed) {
            buffer.put("speedTicks", Math.max(buffer.getInt("speedTicks") - 1, 0));
            return;
        }
        buffer.put("speedTicks", Math.min(buffer.getInt("speedTicks") + 1, 16));

        if (buffer.getInt("speedTicks") <= 15)
            return;

        if (getItemStackAttributes(player, "GENERIC_FLYING_SPEED") != 0 ||
                getPlayerAttributes(player).getOrDefault("GENERIC_FLYING_SPEED", 0.0) > 0.1)
            buffer.put("attribute", System.currentTimeMillis());
        if (System.currentTimeMillis() - buffer.getLong("attribute") < 1000)
            return;

        Scheduler.runTask(true, () -> {
            callViolationEventIfRepeat(player, zacPlayer, event, buffer, 3000);
        });
    }

}
