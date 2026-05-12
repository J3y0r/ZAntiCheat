package cn.jeyor1337.zanticheat.check.checks.movement.trident;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.movement.MovementCheck;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;

/**
 * TridentBoost hack
 */
public class TridentA extends MovementCheck implements Listener {
    public TridentA() {
        super(CheckName.TRIDENT_A);
    }

    @Override
    public boolean isConditionAllowed(Player player, ZACPlayer zacPlayer, PlayerCache cache, boolean isClimbing, boolean isInWater,
                                      boolean isFlying, boolean isInsideVehicle, boolean isGliding, boolean isRiptiding) {
        if (isFlying || isInsideVehicle || isClimbing)
            return false;
        if (cache.flyingTicks >= -4 || cache.climbingTicks >= -2)
            return false;
        long time = System.currentTimeMillis();
        return time - cache.lastInsideVehicle > 150 &&
                time - cache.lastKnockback > 500 && time - cache.lastKnockbackNotVanilla > 2000 &&
                time - cache.lastWasFished > 3000 && time - cache.lastTeleport > 500 &&
                time - cache.lastRespawn > 500 && time - cache.lastEntityVeryNearby > 700 &&
                time - cache.lastBlockExplosion > 4000 && time - cache.lastEntityExplosion > 2500 &&
                time - cache.lastSlimeBlockVertical > 4000 && time - cache.lastSlimeBlockHorizontal > 3500 &&
                time - cache.lastHoneyBlockVertical > 3000 && time - cache.lastHoneyBlockHorizontal > 3000 &&
                time - cache.lastFireworkBoost > 3500 && time - cache.lastFireworkBoostNotVanilla > 6000 &&
                time - cache.lastWasHit > 300 && time - cache.lastWasDamaged > 150 &&
                time - cache.lastKbVelocity > 500 && time - cache.lastAirKbVelocity > 1000 &&
                time - cache.lastStrongKbVelocity > 2500 && time - cache.lastStrongAirKbVelocity > 5000 &&
                time - cache.lastFlight > 750;
    }

    @EventHandler
    public void onAsyncMovement(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        PlayerCache cache = zacPlayer.cache;
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);
        long currentTime = System.currentTimeMillis();

        if (cache.glidingTicks > 5) {
            buffer.put("longGlidingTime", currentTime);
            if (cache.riptidingTicks > 5)
                buffer.put("longRiptidingTime", currentTime);
            return;
        }

        if (cache.riptidingTicks > 5) {
            buffer.put("longRiptidingTime", currentTime);
            return;
        }

        if (!isCheckAllowed(player, zacPlayer, true)) {
            buffer.put("cancelTime", currentTime);
            return;
        }

        if (!isConditionAllowed(player, zacPlayer, event)) {
            buffer.put("cancelTime", currentTime);
            return;
        }

        if (currentTime - buffer.getLong("effectTime") < 500) {
            buffer.put("cancelTime", currentTime);
            return;
        }

        for (Block block : event.getToWithinBlocks())
            if (!isActuallyPassable(block) && block.getType() != Material.WATER) {
                buffer.put("cancelTime", currentTime);
                return;
            }

        if (!zacPlayer.isRiptiding())
            return;

        ItemStack main = zacPlayer.getItemInMainHand();
        if (main != null && main.getType() == VerUtil.material.get("TRIDENT") &&
                main.getEnchantmentLevel(VerUtil.enchantment.get("RIPTIDE")) > 3) {
            return;
        }
        ItemStack off = zacPlayer.getItemInOffHand();
        if (off != null && off.getType() == VerUtil.material.get("TRIDENT") &&
                main.getEnchantmentLevel(VerUtil.enchantment.get("RIPTIDE")) > 3) {
            return;
        }

        if (buffer.isExists("cancelTime") && currentTime - buffer.getLong("cancelTime") < 1000 ||
                buffer.isExists("longGlidingTime") && currentTime - buffer.getLong("longGlidingTime") < 3750 ||
                buffer.isExists("longRiptidingTime") && currentTime - buffer.getLong("longRiptidingTime") < 7500)
            return;

        double hSpeed = distanceHorizontal(event.getFrom(), event.getTo());
        double maxSpeed = 3.5;
        maxSpeed *= 1.25;

        if (hSpeed < maxSpeed)
            return;

        Scheduler.runTask(true, () -> {
            callViolationEvent(player, zacPlayer, event);
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void beforeMovement(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (getEffectAmplifier(zacPlayer.cache, PotionEffectType.SPEED) > 2 ||
                getEffectAmplifier(zacPlayer.cache, VerUtil.potions.get("DOLPHINS_GRACE")) > 1) {
            Buffer buffer = getBuffer(player, true);
            buffer.put("effectTime", System.currentTimeMillis());
        }
    }

}
