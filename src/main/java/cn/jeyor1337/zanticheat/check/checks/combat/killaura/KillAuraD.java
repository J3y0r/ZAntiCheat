package cn.jeyor1337.zanticheat.check.checks.combat.killaura;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.combat.CombatCheck;
import cn.jeyor1337.zanticheat.event.playerattack.ZACAsyncPlayerAttackEvent;
import cn.jeyor1337.zanticheat.event.playerattack.ZACPlayerAttackEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.hook.plugin.FloodgateHook;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * 1. Hitting more than one target per tick
 * 2. Hit while using a shield
 */
public class KillAuraD extends CombatCheck implements Listener {

    public KillAuraD() {
        super(CheckName.KILLAURA_D);
    }

    @EventHandler
    public void multiAuraAsync(ZACAsyncPlayerAttackEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Buffer buffer = getBuffer(event.getPlayer(), true);
        long currentTime = System.currentTimeMillis();

        if (currentTime - buffer.getLong("lastAsyncHit") > 35 - Math.min(zacPlayer.getPing() / 40, 10)) {
            buffer.put("lastAsyncHit", currentTime);
            return;
        }

        buffer.put("lastAsyncFlag", System.currentTimeMillis());
    }

    @EventHandler
    public void multiAura(ZACPlayerAttackEvent event) {
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);
        long currentTime = System.currentTimeMillis();

        if (currentTime - buffer.getLong("lastAsyncFlag") > Main.getBufferDurationMils() - 1000)
            return;

        ZACPlayer zacPlayer = event.getZacPlayer();
        if (currentTime - buffer.getLong("lastHit") > 35 - Math.min(zacPlayer.getPing() / 40, 10)) {
            buffer.put("lastHit", currentTime);
            return;
        }

        if (!isCheckAllowed(player, zacPlayer))
            return;

        if (FloodgateHook.isProbablyPocketEditionPlayer(player))
            return;

        if (!(event.getEntity() instanceof LivingEntity))
            return;

        if (currentTime - buffer.getLong("lastFlag") <= 750) return;
        else buffer.put("lastFlag", currentTime);

        if (getItemStackAttributes(player, "PLAYER_SWEEPING_DAMAGE_RATIO") != 0 ||
                getPlayerAttributes(player).getOrDefault("PLAYER_SWEEPING_DAMAGE_RATIO", 0.0) > 0.01)
            buffer.put("attribute", System.currentTimeMillis());
        if (System.currentTimeMillis() - buffer.getLong("attribute") < 2000)
            return;

        Scheduler.runTask(true, () -> {
            callViolationEventIfRepeat(player, zacPlayer, null, buffer, 5000);
        });
    }

    @EventHandler
    public void shieldAsync(ZACAsyncPlayerAttackEvent event) {
        Player player = event.getPlayer();
        if (!player.isBlocking() && !player.isSleeping() && !player.isDead())
            return;

        Buffer buffer = getBuffer(player, true);
        buffer.put("lastShieldAsyncFlag", System.currentTimeMillis());
    }

    @EventHandler
    public void shield(ZACPlayerAttackEvent event) {
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);
        if (System.currentTimeMillis() - buffer.getLong("lastShieldAsyncFlag") > Main.getBufferDurationMils() - 1000)
            return;

        ZACPlayer zacPlayer = event.getZacPlayer();
        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (!player.isBlocking() && !player.isSleeping() && !player.isDead())
            return;

        if (player.isBlocking() && zacPlayer.cache.blockingTicks < 2)
            return;

        Scheduler.runTask(true, () -> {
            callViolationEventIfRepeat(player, zacPlayer, null, getBuffer(player, true),
                    Main.getBufferDurationMils() - 1000);
        });
    }

}
