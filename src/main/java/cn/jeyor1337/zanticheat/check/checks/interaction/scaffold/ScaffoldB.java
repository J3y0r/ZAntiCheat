package cn.jeyor1337.zanticheat.check.checks.interaction.scaffold;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.interaction.InteractionCheck;
import cn.jeyor1337.zanticheat.event.playerplaceblock.ZACAsyncPlayerPlaceBlockEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.util.hook.plugin.FloodgateHook;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;

/**
 * Sprint
 */
public class ScaffoldB extends InteractionCheck implements Listener {
    public ScaffoldB() {
        super(CheckName.SCAFFOLD_B);
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

        if (FloodgateHook.isBedrockPlayer(player, true))
            return;

        for (Block withinBlock : getWithinBlocks(player)) {
            if (withinBlock.getType() != Material.AIR)
                return;
        }

        if (getEffectAmplifier(player, VerUtil.potions.get("LEVITATION")) > 0 ||
                getEffectAmplifier(player, PotionEffectType.SPEED) > 5)
            return;

        for (int i = 0; i < 3 && i < HistoryElement.values().length; i++) {
            if (!cache.history.onEvent.onGround.get(HistoryElement.values()[i]).towardsFalse ||
                    !cache.history.onPacket.onGround.get(HistoryElement.values()[i]).towardsFalse)
                return;
        }

        if (!player.isSprinting())
            return;

        Buffer buffer = getBuffer(player, true);
        buffer.put("flags", buffer.getInt("flags") + 1);
        if (buffer.getInt("flags") <= 2)
            return;

        Scheduler.runTask(true, () -> {
            callViolationEvent(player, zacPlayer, null);
        });
    }

}
