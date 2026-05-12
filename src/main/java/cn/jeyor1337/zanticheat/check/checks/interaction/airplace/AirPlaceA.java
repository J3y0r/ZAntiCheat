package cn.jeyor1337.zanticheat.check.checks.interaction.airplace;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.interaction.InteractionCheck;
import cn.jeyor1337.zanticheat.event.playerplaceblock.ZACPlayerPlaceBlockEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.hook.plugin.simplehook.EnchantsSquaredHook;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;

/**
 * AirPlace/LiquidPlace hack
 */
public class AirPlaceA extends InteractionCheck implements Listener {
    public AirPlaceA() {
        super(CheckName.AIRPZACE_A);
    }

    private static final Set<BlockFace> BLOCK_FACES = new HashSet<>();

    static {
        BLOCK_FACES.add(BlockFace.UP);
        BLOCK_FACES.add(BlockFace.DOWN);
        BLOCK_FACES.add(BlockFace.NORTH);
        BLOCK_FACES.add(BlockFace.SOUTH);
        BLOCK_FACES.add(BlockFace.WEST);
        BLOCK_FACES.add(BlockFace.EAST);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void beforeBlockPlace(ZACPlayerPlaceBlockEvent event) {
        Material type = event.getBlockAgainst().getType();
        if (type != Material.AIR && type != Material.WATER && type != Material.LAVA)
            return;
        Player player = event.getPlayer();
        if (!isCheckAllowed(player, event.getZacPlayer()))
            return;

        Buffer buffer = getBuffer(player);
        if (System.currentTimeMillis() - buffer.getLong("lastBlockUpdate") < 5000)
            return;
        buffer.put("lastBlockUpdate", System.currentTimeMillis());

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block relativeBlock = event.getBlockAgainst().getRelative(x, y, z);
                    event.getZacPlayer().sendBlockDate(relativeBlock.getLocation(), relativeBlock);
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(ZACPlayerPlaceBlockEvent event) {
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();

        if (!isCheckAllowed(player, zacPlayer))
            return;

        Material replacedBlockType = event.getBlockReplacedState().getType();
        if (replacedBlockType != Material.AIR &&
                replacedBlockType != Material.WATER && replacedBlockType != Material.LAVA) {
            return;
        }

        Block block = event.getBlock();

        for (BlockFace blockFace : BLOCK_FACES) {
            Block relativeBlock = block.getRelative(blockFace);
            if (relativeBlock.getType() != Material.AIR &&
                    relativeBlock.getType() != Material.WATER && relativeBlock.getType() != Material.LAVA)
                return;
        }

        if (block.getType() == VerUtil.material.get("LILY_PAD") || block.getType().name().contains("COPPER"))
            return;

        Buffer buffer = getBuffer(player);
        if (System.currentTimeMillis() - buffer.getLong("lastBlockUpdate") < 200)
            return;

        if (zacPlayer.getPing() > 400) {
            buffer.put("flags", buffer.getInt("flags") + 1);
            if (buffer.getInt("flags") <= 1)
                return;
        }

        if (EnchantsSquaredHook.hasEnchantment(player, "Illuminated", "Harvesting"))
            return;

        callViolationEvent(player, zacPlayer, event.getEvent());
    }

}
