package cn.jeyor1337.zanticheat.util.detection;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.api.DetectionStatus;
import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.CheckSetting;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.util.annotation.SecureAsync;
import cn.jeyor1337.zanticheat.util.api.ApiUtil;
import cn.jeyor1337.zanticheat.util.async.AsyncUtil;
import cn.jeyor1337.zanticheat.util.config.ConfigManager;
import cn.jeyor1337.zanticheat.util.cooldown.CooldownUtil;
import cn.jeyor1337.zanticheat.util.detection.specific.PassableUtil;
import cn.jeyor1337.zanticheat.util.hook.plugin.FloodgateHook;
import cn.jeyor1337.zanticheat.util.hook.plugin.simplehook.ExecutableItemsHook;
import cn.jeyor1337.zanticheat.util.logger.LogType;
import cn.jeyor1337.zanticheat.util.logger.Logger;
import cn.jeyor1337.zanticheat.util.npc.ExternalNPCUtil;
import cn.jeyor1337.zanticheat.util.permission.ACPermission;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.util.tps.TPSCalculator;
import cn.jeyor1337.zanticheat.version.VerPlayer;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CheckUtil extends PassableUtil {

    @SecureAsync
    public static boolean isCheckAllowed(CheckSetting checkSetting, Player player, ZACPlayer zacPlayer, boolean async) {
        if (checkSetting == null || player == null || zacPlayer == null)
            return false;

        if (!checkSetting.enabled) {
            Scheduler.runTask(true, () -> {
                Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") Something went wrong! " +
                        checkSetting.name.title + " check is supposed to be disabled!");
            });
            return false;
        }

        if (ConfigManager.Config.silentMode && !ApiUtil.isReported(player))
            return false;

        if (!ConfigManager.Config.Permission.disableAllBypassPermissions) {
            if (CooldownUtil.hasPermission(zacPlayer.cooldown, player, ACPermission.BYPASS, async))
                return false;

            if (ConfigManager.Config.Permission.perCheckBypassPermission &&
                    CooldownUtil.hasPermission(zacPlayer.cooldown, player,
                            ACPermission.BYPASS + "." + checkSetting.name.name().toLowerCase(), async))
                return false;
        }

        DetectionStatus detectionStatus = ApiUtil.getCheckStatus(player, checkSetting.name.name().toLowerCase());
        if (ConfigManager.Config.Api.enabled && detectionStatus != DetectionStatus.ENABLED)
            return false;

        if (TPSCalculator.getTickDurationInMs() >= ConfigManager.Config.LagProtection.tickThreshold)
            return false;
        if (TPSCalculator.getTPS() < checkSetting.minTps)
            return false;

        boolean bedrock = FloodgateHook.isBedrockPlayer(player, async);
        if (!bedrock && !checkSetting.detectJava || bedrock && !checkSetting.detectBedrock)
            return false;
        if (FloodgateHook.isCancelledCombat(checkSetting.name, player, async))
            return false;

        CheckName checkName = checkSetting.name;
        if (!async) {
            if (ExecutableItemsHook.isPrevented(checkName, player))
                return false;
        }

        if (VerPlayer.getPing(player, async) > checkSetting.maxPing)
            return false;

        long currentTime = System.currentTimeMillis();
        if (currentTime - zacPlayer.joinTime < ConfigManager.Config.LagProtection.ignoreTimeOnJoin ||
                currentTime - zacPlayer.cache.lastWorldChange < ConfigManager.Config.LagProtection.ignoreTimeOnTeleport)
            return false;
        return true;
    }

    public static boolean isCheckAllowed(CheckSetting checkSetting, Player player, ZACPlayer zacPlayer) {
        return isCheckAllowed(checkSetting, player, zacPlayer, false);
    }

    public static double distance(Location from, Location to) {
        if (from == null || to == null || AsyncUtil.getWorld(from) != AsyncUtil.getWorld(to))
            return 0;
        return from.distance(to);
    }

    public static double distanceAbsVertical(Location from, Location to) {
        if (from == null || to == null || AsyncUtil.getWorld(from) != AsyncUtil.getWorld(to))
            return 0;
        return Math.abs(to.getY() - from.getY());
    }

    public static double distanceVertical(Location from, Location to) {
        if (from == null || to == null || AsyncUtil.getWorld(from) != AsyncUtil.getWorld(to))
            return 0;
        return to.getY() - from.getY();
    }

    public static double distanceHorizontal(Location from, Location to) {
        if (from == null || to == null || AsyncUtil.getWorld(from) != AsyncUtil.getWorld(to))
            return 0;
        return Math.sqrt(Math.pow(to.getX() - from.getX(), 2) + Math.pow(to.getZ() - from.getZ(), 2));
    }

    @SecureAsync
    public static int getEffectAmplifier(Player player, PotionEffectType type) {
        return VerUtil.getPotionLevel(player, type);
    }

    @SecureAsync
    public static int getEffectAmplifier(PlayerCache cache, PotionEffectType type) {
        PotionEffect effect = cache.potionEffects.getOrDefault(type, null);
        if (effect == null) return 0;
        return effect.getAmplifier() + 1;
    }

    @SecureAsync
    public static double getItemStackAttributes(Player player, String... names) {
        Set<ItemStack> itemStacks = new HashSet<>();
        ItemStack itemInMainHand = VerPlayer.getItemInMainHand(player);
        if (itemInMainHand != null)
            itemStacks.add(itemInMainHand);
        ItemStack itemInOffHand = VerPlayer.getItemInOffHand(player);
        if (itemInOffHand != null)
            itemStacks.add(itemInOffHand);
        for (ItemStack itemStack : player.getInventory().getArmorContents())
            if (itemStack != null)
                itemStacks.add(itemStack);
        double result = 0;
        for (ItemStack itemStack : itemStacks) {
            Map<String, Double> attributes = VerUtil.getAttributes(itemStack);
            for (String name : names)
                if (attributes.containsKey(name))
                    result = Math.max(result, attributes.get(name));
        }
        return result;
    }

    @SecureAsync
    public static Map<String, Double> getPlayerAttributes(Player player) {
        return VerUtil.getAttributes(player);
    }

    @SecureAsync
    public static boolean isExternalNPC(Player player, boolean async) {
        return ExternalNPCUtil.isExternalNPC(player, async);
    }

    public static boolean isExternalNPC(Player player) {
        return isExternalNPC(player, false);
    }

    public static boolean isExternalNPC(PlayerEvent playerEvent) {
        return isExternalNPC(playerEvent.getPlayer(), false);
    }

    @SecureAsync
    public static boolean isExternalNPC(Entity entity, boolean async) {
        return ExternalNPCUtil.isExternalNPC(entity, async);
    }

    public static boolean isExternalNPC(Entity entity) {
        return isExternalNPC(entity, false);
    }

}
