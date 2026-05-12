package cn.jeyor1337.zanticheat.check.checks.movement;

import cn.jeyor1337.zanticheat.check.Check;
import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.event.playermove.ZACPlayerMoveEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.util.cooldown.CooldownUtil;
import cn.jeyor1337.zanticheat.util.hook.plugin.simplehook.EnchantsSquaredHook;
import cn.jeyor1337.zanticheat.version.VerPlayer;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MovementCheck extends Check {
    public MovementCheck(CheckName name) {
        super(name);
    }

    public abstract boolean isConditionAllowed(Player player, ZACPlayer zacPlayer, PlayerCache cache,
                                               boolean isClimbing, boolean isInWater,
                                               boolean isFlying, boolean isInsideVehicle, boolean isGliding, boolean isRiptiding);

    public boolean isConditionAllowed(Player player, ZACPlayer zacPlayer, ZACPlayerMoveEvent event,
                                      boolean isClimbing, boolean isInWater) {
        return isConditionAllowed(player, zacPlayer, zacPlayer.cache,
                isClimbing, isInWater,
                player.isFlying(), player.isInsideVehicle(), zacPlayer.isGliding(), zacPlayer.isRiptiding());
    }

    public boolean isConditionAllowed(Player player, ZACPlayer zacPlayer, ZACAsyncPlayerMoveEvent event) {
        return isConditionAllowed(player, zacPlayer, zacPlayer.cache,
                event.isPlayerClimbing(), event.isPlayerInWater(),
                event.isPlayerFlying(), event.isPlayerInsideVehicle(), event.isPlayerGliding(), event.isPlayerRiptiding());
    }


    public boolean isPingGlidingPossible(Player player, PlayerCache cache) {
        long currentTime = System.currentTimeMillis();
        if (VerPlayer.getPing(player) > 350 && (currentTime - cache.lastGliding < 2000 || currentTime - cache.lastRiptiding < 3000))
            return true;
        if (VerPlayer.getPing(player) > 500 && (currentTime - cache.lastGliding < 3000 || currentTime - cache.lastRiptiding < 4000))
            return true;
        return false;
    }

    public boolean isLagGlidingPossible(Player player, Buffer buffer, int requiredAccuracy) {
        if (buffer.getInt("methodAccuracy") >= requiredAccuracy)
            return false;

        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        PlayerInventory inventory = player.getInventory();

        boolean elytra = zacPlayer.getArmorPiece(EquipmentSlot.CHEST).getType() == VerUtil.material.get("ELYTRA");
        boolean trident = false;
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack != null && itemStack.getType() == VerUtil.material.get("TRIDENT") &&
                    itemStack.getEnchantmentLevel(VerUtil.enchantment.get("RIPTIDE")) != 0) {
                trident = true;
                break;
            }
        }

        int increase = 1;
        if (!elytra) increase = 2;
        if (!elytra && !trident) increase = requiredAccuracy;

        buffer.put("methodAccuracy", buffer.getInt("methodAccuracy") + increase);
        return buffer.getInt("methodAccuracy") < requiredAccuracy;
    }

    public boolean isLagGlidingPossible(Player player, Buffer buffer) {
        return isLagGlidingPossible(player, buffer, 9);
    }

    public void updateDownBlocks(Player player, ZACPlayer zacPlayer, Set<Block> downBlocks) {
        if (zacPlayer.violations.getViolations(getCheckSetting(this).name) % 3 != 0)
            return;
        for (Block block : downBlocks)
            zacPlayer.sendBlockDate(block.getLocation(), block);
    }

    public Set<Player> getPlayersForEnchantsSquared(ZACPlayer zacPlayer, Player player) {
        if (!EnchantsSquaredHook.isPluginInstalled())
            return Collections.emptySet();
        Set<Player> players = ConcurrentHashMap.newKeySet();
        for (Entity entity : CooldownUtil.getAllEntitiesAsync(zacPlayer.cooldown, player)) {
            if (entity.getType() == EntityType.PLAYER)
                players.add((Player) entity);
        }
        return players;
    }

    public boolean isEnchantsSquaredImpact(Set<Player> players) {
        if (!EnchantsSquaredHook.isPluginInstalled())
            return false;
        for (Player player : players)
            if (EnchantsSquaredHook.hasEnchantment(player, "Rope Dart", "Shockwave"))
                return true;
        return false;
    }

}
