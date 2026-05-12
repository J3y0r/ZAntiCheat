package cn.jeyor1337.zanticheat.check.checks.interaction.scaffold;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.interaction.InteractionCheck;
import cn.jeyor1337.zanticheat.event.playerplaceblock.ZACAsyncPlayerPlaceBlockEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;


/**
 * Head rotation
 */
public class ScaffoldA extends InteractionCheck implements Listener {
    public ScaffoldA() {
        super(CheckName.SCAFFOLD_A);
    }

    @EventHandler
    public void onAsyncBlockPlace(ZACAsyncPlayerPlaceBlockEvent event) {
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();
        PlayerCache cache = zacPlayer.cache;

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (!isScaffoldPlacement(player, event.getBlock(), event.getBlockAgainst()))
            return;

        for (Block withinBlock : getWithinBlocks(player)) {
            if (withinBlock.getType() != Material.AIR)
                return;
        }

        if (getEffectAmplifier(player, VerUtil.potions.get("LEVITATION")) > 0 ||
                getEffectAmplifier(player, PotionEffectType.SPEED) > 5)
            return;

        boolean flag = event.getLocation().getPitch() <= 34;

        if (!flag) {
            Location from = cache.history.onEvent.location.get(HistoryElement.FROM);
            Location first = cache.history.onEvent.location.get(HistoryElement.FIRST);
            if (rotation(from, event.getLocation()) && !rotation(from, first)) {
                flag = true;
            }
        }

        if (!flag)
            return;

        Buffer buffer = getBuffer(player, true);
        buffer.put("flags", buffer.getInt("flags") + 1);
        if (buffer.getInt("flags") <= 2)
            return;

        Scheduler.runTask(true, () -> {
            callViolationEventIfRepeat(player, zacPlayer, null, buffer, 1500);
        });
    }

    private static boolean rotation(Location from, Location to) {
        return from.getYaw() != to.getYaw() && from.getPitch() != to.getPitch();
    }

}
