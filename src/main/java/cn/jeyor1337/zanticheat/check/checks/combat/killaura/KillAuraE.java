package cn.jeyor1337.zanticheat.check.checks.combat.killaura;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.CheckSetting;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.combat.CombatCheck;
import cn.jeyor1337.zanticheat.event.playerattack.ZACAsyncPlayerAttackEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.player.cache.history.PlayerCacheHistory;
import cn.jeyor1337.zanticheat.util.cooldown.CooldownUtil;
import cn.jeyor1337.zanticheat.util.hook.plugin.FloodgateHook;
import cn.jeyor1337.zanticheat.util.precise.AccuracyUtil;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

public class KillAuraE extends CombatCheck implements Listener {

    public KillAuraE() {
        super(CheckName.KILLAURA_E);
    }

    @EventHandler
    public void onAsyncHit(ZACAsyncPlayerAttackEvent event) {
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();
        CheckSetting setting = getCheckSetting();
        Buffer buffer = getBuffer(player, true);
        long currentTime = System.currentTimeMillis();

        if (!isCheckAllowed(player, zacPlayer, true)) {
            resetCombatState(buffer);
            return;
        }

        if (zacPlayer.isGliding() || zacPlayer.isRiptiding() || player.isInsideVehicle()) {
            decay(buffer, currentTime, true, setting.killAuraEDecayTime);
            return;
        }

        if (FloodgateHook.isProbablyPocketEditionPlayer(player, true) || FloodgateHook.isBedrockPlayer(player)) {
            decay(buffer, currentTime, true, setting.killAuraEDecayTime);
            return;
        }

        Entity entity = resolveEntity(player, zacPlayer, event.getEntityId(), buffer);
        if (entity == null) {
            decay(buffer, currentTime, false, setting.killAuraEDecayTime);
            return;
        }

        if (VerUtil.getWidth(entity) > 2.0)
            return;
        if (VerUtil.getWidth(entity) == 10 && VerUtil.getHeight(entity) == 10)
            return;

        if (getItemStackAttributes(player, "PLAYER_SWEEPING_DAMAGE_RATIO") != 0 ||
                getPlayerAttributes(player).getOrDefault("PLAYER_SWEEPING_DAMAGE_RATIO", 0.0) > 0.01) {
            buffer.put("attribute", currentTime);
        }
        if (currentTime - buffer.getLong("attribute") < setting.killAuraEAttributeGracePeriod) {
            decay(buffer, currentTime, false, setting.killAuraEDecayTime);
            return;
        }

        PlayerCacheHistory<Location> history = zacPlayer.cache.history.onEvent.location;
        Location from = history.get(HistoryElement.FROM);
        Location first = history.get(HistoryElement.FIRST);
        Location second = history.get(HistoryElement.SECOND);
        if (from == null || first == null || second == null) {
            decay(buffer, currentTime, false, setting.killAuraEDecayTime);
            return;
        }

        Location eyeLocation = player.getEyeLocation().clone();
        Location targetLocation = getTargetLocation(entity);
        if (targetLocation == null) {
            decay(buffer, currentTime, false, setting.killAuraEDecayTime);
            return;
        }

        double yawStep = getYawStep(from, first);
        double previousYawStep = getYawStep(first, second);
        double pitchStep = getPitchStep(from, first);
        double previousPitchStep = getPitchStep(first, second);
        double horizontalOffset = getHorizontalAimOffset(eyeLocation, targetLocation);
        double verticalOffset = getVerticalAimOffset(eyeLocation, targetLocation);
        double totalTurn = yawStep + previousYawStep + pitchStep + previousPitchStep;
        int score = 0;

        if (yawStep >= setting.killAuraELargeYawStep && horizontalOffset <= setting.killAuraELowError) {
            score += 2;
            buffer.put("snapSamples", buffer.getInt("snapSamples") + 1);
        }

        if (yawStep >= setting.killAuraEMinRepeatStep && previousYawStep >= setting.killAuraEMinRepeatStep &&
                Math.abs(yawStep - previousYawStep) <= setting.killAuraERepeatStepEpsilon) {
            int repeatSteps = buffer.getInt("repeatSteps") + 1;
            buffer.put("repeatSteps", repeatSteps);
            if (repeatSteps >= 2)
                score += 2;
            else
                score += 1;
        } else {
            buffer.put("repeatSteps", 0);
        }

        if (totalTurn >= setting.killAuraETurnForLowError && horizontalOffset <= setting.killAuraELowError) {
            int lowErrorSamples = buffer.getInt("lowErrorSamples") + 1;
            buffer.put("lowErrorSamples", lowErrorSamples);
            score += lowErrorSamples >= 2 ? 2 : 1;
        } else if (horizontalOffset > setting.killAuraELowError * 2) {
            buffer.put("lowErrorSamples", 0);
        }

        if (Math.abs(targetLocation.getY() - eyeLocation.getY()) >= setting.killAuraEVerticalOffsetForPitchCheck &&
                yawStep >= setting.killAuraETurnForLowError &&
                pitchStep <= setting.killAuraEStaticPitchEpsilon &&
                verticalOffset <= setting.killAuraELowError) {
            score += 1;
        }

        if (buffer.getInt("lastEntityId") != 0 && buffer.getInt("lastEntityId") != entity.getEntityId() &&
                yawStep >= setting.killAuraELargeYawStep && horizontalOffset <= setting.killAuraEVeryLowError) {
            score += 1;
        }

        buffer.put("lastEntityId", entity.getEntityId());
        buffer.put("lastYawStep", (float) yawStep);
        buffer.put("lastPitchStep", (float) pitchStep);
        buffer.put("lastAimYaw", (float) horizontalOffset);
        buffer.put("lastAimPitch", (float) verticalOffset);
        buffer.put("lastAttackTime", currentTime);

        if (score <= 0) {
            decay(buffer, currentTime, false, setting.killAuraEDecayTime);
            return;
        }

        if (currentTime - buffer.getLong("combatWindow") > setting.killAuraECombatWindow) {
            buffer.put("vl", 0);
            buffer.put("streak", 0);
            buffer.put("repeatSteps", 0);
            buffer.put("snapSamples", 0);
            buffer.put("lowErrorSamples", 0);
        }

        buffer.put("combatWindow", currentTime);
        buffer.put("lastSuspiciousTime", currentTime);
        buffer.put("streak", buffer.getInt("streak") + 1);
        buffer.put("vl", Math.min(setting.killAuraEViolationCap, buffer.getInt("vl") + score));

        if (buffer.getInt("streak") < 2 || buffer.getInt("vl") < setting.killAuraEViolationLevel)
            return;

        if (currentTime - buffer.getLong("lastFlagTime") <= setting.killAuraEFlagCooldown)
            return;
        buffer.put("lastFlagTime", currentTime);

        if (AccuracyUtil.isViolationCancel(setting, buffer))
            return;

        Scheduler.runTaskLater(player, () -> {
            Entity confirmedEntity = resolveEntity(player, zacPlayer, event.getEntityId(), buffer);
            if (confirmedEntity == null)
                return;

            Location confirmedTarget = getTargetLocation(confirmedEntity);
            if (confirmedTarget == null)
                return;

            double confirmedOffset = getHorizontalAimOffset(player.getEyeLocation(), confirmedTarget);
            if (confirmedOffset > setting.killAuraELowError * setting.killAuraEConfirmedOffsetMultiplier)
                return;

            callViolationEventIfRepeat(player, zacPlayer, null, buffer, Main.getBufferDurationMils() - 1000L);
        }, 1);
    }

    private Entity resolveEntity(Player player, ZACPlayer zacPlayer, int entityId, Buffer buffer) {
        String key = String.valueOf(entityId);
        if (buffer.isExists(key)) {
            Entity entity = buffer.getEntity(key);
            if (entity != null)
                return entity;
        }

        for (Entity entity : CooldownUtil.getAllEntitiesAsync(zacPlayer.cooldown, player)) {
            if (entity.getEntityId() != entityId)
                continue;
            buffer.put(key, entity);
            return entity;
        }
        return null;
    }

    private static Location getTargetLocation(Entity entity) {
        if (entity == null)
            return null;
        Location location = entity.getLocation().clone();
        location.add(0.0, Math.min(VerUtil.getHeight(entity) * 0.75, 1.5), 0.0);
        return location;
    }

    private static double getYawStep(Location from, Location to) {
        return Math.abs(getAngleDelta(from.getYaw(), to.getYaw()));
    }

    private static double getPitchStep(Location from, Location to) {
        return Math.abs(to.getPitch() - from.getPitch());
    }

    private static double getHorizontalAimOffset(Location eyeLocation, Location targetLocation) {
        Vector direction = eyeLocation.getDirection().clone().setY(0.0D);
        Vector vector = targetLocation.toVector().setY(0.0D).subtract(eyeLocation.toVector().setY(0.0D));
        if (direction.lengthSquared() == 0.0 || vector.lengthSquared() == 0.0)
            return 0.0;
        return direction.angle(vector) * 57.2958F;
    }

    private static double getVerticalAimOffset(Location eyeLocation, Location targetLocation) {
        double targetPitch = getTargetPitch(eyeLocation, targetLocation);
        return Math.abs(eyeLocation.getPitch() - targetPitch);
    }

    private static double getTargetPitch(Location eyeLocation, Location targetLocation) {
        Vector vector = targetLocation.toVector().subtract(eyeLocation.toVector());
        double xz = Math.sqrt(vector.getX() * vector.getX() + vector.getZ() * vector.getZ());
        return -Math.toDegrees(Math.atan2(vector.getY(), xz));
    }

    private static float getAngleDelta(float from, float to) {
        return wrapAngle(to - from);
    }

    private static float wrapAngle(float angle) {
        angle %= 360.0F;
        if (angle >= 180.0F)
            angle -= 360.0F;
        if (angle < -180.0F)
            angle += 360.0F;
        return angle;
    }

    private static void resetCombatState(Buffer buffer) {
        buffer.put("vl", 0);
        buffer.put("streak", 0);
        buffer.put("repeatSteps", 0);
        buffer.put("snapSamples", 0);
        buffer.put("lowErrorSamples", 0);
        buffer.put("combatWindow", 0L);
        buffer.put("lastSuspiciousTime", 0L);
    }

    private static void decay(Buffer buffer, long currentTime, boolean reset, long decayTime) {
        if (reset || currentTime - buffer.getLong("lastSuspiciousTime") > decayTime) {
            resetCombatState(buffer);
            return;
        }

        int vl = buffer.getInt("vl");
        if (vl > 0)
            buffer.put("vl", vl - 1);
        int streak = buffer.getInt("streak");
        if (streak > 0)
            buffer.put("streak", streak - 1);
    }
}
