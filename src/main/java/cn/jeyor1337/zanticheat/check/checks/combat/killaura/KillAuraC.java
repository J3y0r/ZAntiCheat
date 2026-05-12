package cn.jeyor1337.zanticheat.check.checks.combat.killaura;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.combat.CombatCheck;
import cn.jeyor1337.zanticheat.event.playerattack.ZACAsyncPlayerAttackEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.event.playerattack.ZACPlayerAttackEvent;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Set;

/**
 * Attack through blocks
 */
public class KillAuraC extends CombatCheck implements Listener {
    public KillAuraC() {
        super(CheckName.KILLAURA_C);
    }

    @EventHandler
    public void onAsyncHit(ZACAsyncPlayerAttackEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (zacPlayer.isGliding() || zacPlayer.isRiptiding() || player.isInsideVehicle())
            return;

        boolean playerImmured = true;
        Set<Block> playerWithinBlocks = getWithinBlocks(player);
        for (Block block : playerWithinBlocks) {
            if (isNotHittableThrough(block))
                continue;
            playerImmured = false;
            break;
        }
        if (playerImmured && distance(zacPlayer.cache.history.onEvent.location.get(HistoryElement.FROM),
                zacPlayer.cache.history.onEvent.location.get(HistoryElement.FIRST)) >= 0.175)
            playerImmured = false;
        if (playerImmured && distance(zacPlayer.cache.history.onPacket.location.get(HistoryElement.FROM),
                zacPlayer.cache.history.onPacket.location.get(HistoryElement.FIRST)) >= 0.175)
            playerImmured = false;

        if (!playerImmured)
            return;

        Buffer buffer = getBuffer(player, true);
        Scheduler.runTask(true, () -> {
            callViolationEventIfRepeat(player, zacPlayer, null, buffer, 12 * 1000);
        });
    }

    @EventHandler
    public void onHit(ZACPlayerAttackEvent event) {
        if (!event.isEntityAttackCause())
            return;
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, zacPlayer))
            return;

        if (zacPlayer.isGliding() || zacPlayer.isRiptiding() || player.isInsideVehicle())
            return;

        Entity entity = event.getEntity();
        boolean entityImmured = true;
        Set<Block> entityWithinBlocks = getWithinBlocks(entity);
        for (Block block : entityWithinBlocks) {
            if (isNotHittableThrough(block))
                continue;
            entityImmured = false;
            break;
        }
        if (entityImmured && entity instanceof Player) {
            Player damaged = (Player) entity;
            ZACPlayer lacDamagedPlayer = ZACPlayer.getZacPlayer(damaged);
            if (distance(lacDamagedPlayer.cache.history.onEvent.location.get(HistoryElement.FROM),
                    lacDamagedPlayer.cache.history.onEvent.location.get(HistoryElement.FIRST)) >= 0.175)
                entityImmured = false;
            if (distance(lacDamagedPlayer.cache.history.onPacket.location.get(HistoryElement.FROM),
                    lacDamagedPlayer.cache.history.onPacket.location.get(HistoryElement.FIRST)) >= 0.175)
                entityImmured = false;
        }

        if (!entityImmured)
            return;

        Buffer buffer = getBuffer(player, true);
        callViolationEventIfRepeat(player, zacPlayer, event.getEvent(), buffer, Main.getBufferDurationMils() - 1000L);
    }

    private static boolean isNotHittableThrough(Block block) {
        Material material = block.getType();
        String name = material.name().toLowerCase();
        return material.isOccluding() ||
                material == Material.GLASS || name.endsWith("_glass") ||
                material == VerUtil.material.get("TALL_GRASS") || material == VerUtil.material.get("LARGE_FERN") ||
                material == VerUtil.material.get("SUNFLOWER") || material == VerUtil.material.get("LIZAC") ||
                material == VerUtil.material.get("ROSE_BUSH") || material == VerUtil.material.get("PEONY") ||
                material == Material.SUGAR_CANE || material == VerUtil.material.get("PITCHER_PLANT");
    }

}
