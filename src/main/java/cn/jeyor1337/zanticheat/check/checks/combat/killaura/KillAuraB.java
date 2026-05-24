package cn.jeyor1337.zanticheat.check.checks.combat.killaura;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.combat.CombatCheck;
import cn.jeyor1337.zanticheat.event.playerattack.ZACAsyncPlayerAttackEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
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

/**
 * Hitbox
 */
public class KillAuraB extends CombatCheck implements Listener {
    public KillAuraB() {
        super(CheckName.KILLAURA_B);
    }

    @EventHandler
    public void onAsyncHit(ZACAsyncPlayerAttackEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();

        if (FloodgateHook.isProbablyPocketEditionPlayer(player, true))
            return;

        Location eyeLocation = player.getEyeLocation().clone();
        double yawChange = getYawChange(eyeLocation, zacPlayer);

        if (!isCheckAllowed(player, zacPlayer))
            return;

        if (zacPlayer.isGliding() || zacPlayer.isRiptiding())
            return;

        Buffer buffer = getBuffer(player, true);
        Entity entity = null;
        int entityId = event.getEntityId();
        boolean exists = buffer.isExists(String.valueOf(entityId));

        if (exists) {
            entity = buffer.getEntity(String.valueOf(entityId));
        } else {
            for (Entity entity1 : CooldownUtil.getAllEntitiesAsync(zacPlayer.cooldown, player)) {
                if (entity1.getEntityId() != entityId)
                    continue;
                entity = entity1;
                break;
            }
        }
        if (entity == null)
            return;
        if (!exists)
            buffer.put(String.valueOf(entityId), entity);

        if (VerUtil.getWidth(entity) > 2.0)
            return;
        if (VerUtil.getWidth(entity) == 10 && VerUtil.getHeight(entity) == 10)
            return;

        if (distanceToHitbox(player, entity) != -1)
            return;

        boolean bedrockPlayer = FloodgateHook.isBedrockPlayer(player, true);

        Location entityLocation = entity.getLocation();
        double entityHalfWidth = VerUtil.getWidth(entity) / 2.0;
        float angle = Float.MAX_VALUE;

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location location = entityLocation.add(x * entityHalfWidth, 0, z * entityHalfWidth);
                Vector vector = location.toVector().setY(0.0D).subtract(eyeLocation.toVector().setY(0.0D));
                angle = Math.min(angle, eyeLocation.getDirection().setY(0.0D).angle(vector) * 57.2958F);
            }
        }

        double halfDiagonal = Math.sqrt(Math.pow(entityHalfWidth, 2) * 2);
        double distance = distanceHorizontal(eyeLocation, entityLocation) - halfDiagonal;
        double extraOffset = 0.0;
        if (distance < 1.0)
            return;
        PlayerCacheHistory<Location> eventHistory = zacPlayer.cache.history.onEvent.location;
        PlayerCacheHistory<Location> packetHistory = zacPlayer.cache.history.onPacket.location;
        Location location = player.getLocation();
        if (distanceHorizontal(eventHistory.get(HistoryElement.FROM), entityLocation) - distanceHorizontal(location, entityLocation) > 0.2 ||
                distanceHorizontal(packetHistory.get(HistoryElement.FROM), entityLocation) - distanceHorizontal(location, entityLocation) > 0.2 ||
                distance(eventHistory.get(HistoryElement.FROM), entityLocation) - distance(location, entityLocation) > 0.3 ||
                distance(packetHistory.get(HistoryElement.FROM), entityLocation) - distance(location, entityLocation) > 0.3) {
            if (distance <= 1.2)
                return;
            if (distance <= 1.3)
                extraOffset += 15.0;
            else if (distance <= 1.4)
                extraOffset += 10.0;
            else if (distance <= 1.5)
                extraOffset += 5.0;
        } else {
            if (distance <= 1.1)
                extraOffset += 9.0;
            else if (distance <= 1.2)
                extraOffset += 6.0;
            else if (distance <= 1.3)
                extraOffset += 3.0;
        }


        double maxAngle = Math.atan(halfDiagonal / distance) * 57.2958F;

        if (distanceHorizontal(eventHistory.get(HistoryElement.FIRST), eventHistory.get(HistoryElement.FROM)) >
                (0.21585 + 0.28061) / 2)
            maxAngle += 25;
        if (bedrockPlayer) {
            maxAngle += 25;
            extraOffset += 5.0;
        }

        double yawChange1 = getYawChange(eyeLocation, zacPlayer);
        float finalAngle = angle;
        Entity finalEntity = entity;
        double finalExtraOffset = extraOffset;
        double finalMaxAngle = maxAngle;
        Scheduler.runTaskLater(player, () -> {
            if (distanceToHitbox(player, finalEntity) != -1)
                return;

            double yawChange2 = getYawChange(eyeLocation, zacPlayer);
            double yawChange3 = getYawChange(player.getEyeLocation(), zacPlayer);
            float resultAngle = finalAngle - (float) max(yawChange, yawChange1, yawChange2, yawChange3);
            if (resultAngle <= finalMaxAngle * 1.1 + 5 + finalExtraOffset)
                return;

            if (System.currentTimeMillis() - buffer.getLong("lastFlagTime") <= 200) return;
            else buffer.put("lastFlagTime", System.currentTimeMillis());

            if (AccuracyUtil.isViolationCancel(getCheckSetting(), buffer))
                return;

            if (getItemStackAttributes(player, "PLAYER_SWEEPING_DAMAGE_RATIO") != 0 ||
                    getPlayerAttributes(player).getOrDefault("PLAYER_SWEEPING_DAMAGE_RATIO", 0.0) > 0.01)
                buffer.put("attribute", System.currentTimeMillis());
            if (System.currentTimeMillis() - buffer.getLong("attribute") < 3500)
                return;

            callViolationEventIfRepeat(player, zacPlayer, null, buffer, Main.getBufferDurationMils() - 1000L);
        }, 1);
    }

    private static double getYawChange(Location eyeLocation, ZACPlayer zacPlayer) {
        float yaw = yaw(eyeLocation.getYaw());
        PlayerCache.History history = zacPlayer.cache.history;
        return Math.min(Math.abs(yaw - yaw(history.onEvent.location.get(HistoryElement.FROM).getYaw())),
                Math.abs(yaw - yaw(history.onPacket.location.get(HistoryElement.FROM).getYaw())));
    }

    private static float yaw(float yaw) {
        yaw = yaw % 360;
        return yaw >= 0 ? yaw : 360 - yaw;
    }

    private static double max(double first, double second, double third, double fourth) {
        return Math.max(
                Math.max(first, second),
                Math.max(third, fourth)
        );
    }

}
