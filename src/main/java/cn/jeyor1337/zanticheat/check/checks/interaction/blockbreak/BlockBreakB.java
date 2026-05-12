package cn.jeyor1337.zanticheat.check.checks.interaction.blockbreak;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.interaction.InteractionCheck;
import cn.jeyor1337.zanticheat.event.playerbreakblock.ZACAsyncPlayerBreakBlockEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.util.hook.plugin.simplehook.AureliumSkillsHook;
import cn.jeyor1337.zanticheat.util.hook.plugin.simplehook.EnchantsSquaredHook;
import cn.jeyor1337.zanticheat.util.hook.plugin.simplehook.McMMOHook;
import cn.jeyor1337.zanticheat.util.hook.plugin.simplehook.VeinMinerHook;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Horizontal block break reach
 */
public class BlockBreakB extends InteractionCheck implements Listener {
    public BlockBreakB() {
        super(CheckName.BLOCKBREAK_B);
    }

    @EventHandler
    public void onAsyncBlockBreak(ZACAsyncPlayerBreakBlockEvent event) {
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        Block block = event.getBlock();
        Location blockLocation = block.getLocation();
        double distance = distanceHorizontal(event.getEyeLocation(), blockLocation);
        distance -= 0.707107;

        double maxDistance = 6.0;

        PlayerCache cache = zacPlayer.cache;
        double eventBackwardsDistance = 0;
        if (distanceHorizontal(cache.history.onEvent.location.get(HistoryElement.FROM), blockLocation) <
                distanceHorizontal(event.getLocation(), blockLocation))
            eventBackwardsDistance = distanceHorizontal(cache.history.onEvent.location.get(HistoryElement.FROM), event.getLocation());
        double packetBackwardsDistance = 0;
        if (distanceHorizontal(cache.history.onPacket.location.get(HistoryElement.FROM), blockLocation) <
                distanceHorizontal(event.getLocation(), blockLocation))
            packetBackwardsDistance = distanceHorizontal(cache.history.onPacket.location.get(HistoryElement.FROM), event.getLocation());
        double backwardsDistance = Math.max(eventBackwardsDistance, packetBackwardsDistance);
        maxDistance += backwardsDistance;
        maxDistance += backwardsDistance * (zacPlayer.getPing() / 1000.0 * 20.0);
        maxDistance = Math.min(maxDistance, 8.5);

        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE)
            maxDistance += 1.5;

        if (distance < maxDistance)
            return;

        Buffer buffer = getBuffer(player, true);
        buffer.put("flags", buffer.getInt("flags") + 1);
        if (buffer.getInt("flags") <= 2)
            return;

        if (getItemStackAttributes(player, "PLAYER_BLOCK_INTERACTION_RANGE") != 0 ||
                getPlayerAttributes(player).getOrDefault("PLAYER_BLOCK_INTERACTION_RANGE", 0.0) > 0.01)
            buffer.put("attribute", System.currentTimeMillis());
        if (System.currentTimeMillis() - buffer.getLong("attribute") < 2500)
            return;

        Scheduler.runTask(true, () -> {
            if (AureliumSkillsHook.isPrevented(player) ||
                    VeinMinerHook.isPrevented(player) ||
                    McMMOHook.isPrevented(block.getType()))
                return;

            if (EnchantsSquaredHook.hasEnchantment(player, "Excavation", "Deforestation", "Harvesting"))
                return;

            callViolationEvent(player, zacPlayer, null);
        });
    }

}
