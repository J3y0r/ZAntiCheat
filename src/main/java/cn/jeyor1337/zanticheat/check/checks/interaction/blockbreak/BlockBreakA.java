package cn.jeyor1337.zanticheat.check.checks.interaction.blockbreak;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.interaction.InteractionCheck;
import cn.jeyor1337.zanticheat.event.playerbreakblock.ZACAsyncPlayerBreakBlockEvent;
import cn.jeyor1337.zanticheat.event.playerbreakblock.ZACPlayerBreakBlockEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.player.cache.history.PlayerCacheHistory;
import cn.jeyor1337.zanticheat.util.hook.plugin.simplehook.AureliumSkillsHook;
import cn.jeyor1337.zanticheat.util.hook.plugin.simplehook.EnchantsSquaredHook;
import cn.jeyor1337.zanticheat.util.hook.plugin.simplehook.McMMOHook;
import cn.jeyor1337.zanticheat.util.hook.plugin.simplehook.VeinMinerHook;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Head rotation
 */
public class BlockBreakA extends InteractionCheck implements Listener {
    public BlockBreakA() {
        super(CheckName.BLOCKBREAK_A);
    }

    @EventHandler
    public void onAsyncBlockBreak(ZACAsyncPlayerBreakBlockEvent event) {
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("lastAsyncResult", flag(event.getPlayer(), event.getZacPlayer(),
                event.getBlock(), event.getEyeLocation(), true));
    }

    @EventHandler
    public void onBlockBreak(ZACPlayerBreakBlockEvent event) {
        Buffer buffer = getBuffer(event.getPlayer(), true);
        if (!buffer.getBoolean("lastAsyncResult"))
            return;
        if (!flag(event.getPlayer(), event.getZacPlayer(),
                event.getBlock(), event.getPlayer().getEyeLocation(), false))
            return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - buffer.getLong("lastFlag") < 500)
            return;
        buffer.put("lastFlag", currentTime);

        buffer.put("flags", buffer.getInt("flags") + 1);
        if (buffer.getInt("flags") <= 1)
            return;

        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();
        Block block = event.getBlock();

        Scheduler.runTaskLater(player, () -> {
            if (getYawChange(player.getEyeLocation(), zacPlayer) > 30.0)
                return;

            if (AureliumSkillsHook.isPrevented(player) ||
                    VeinMinerHook.isPrevented(player) ||
                    McMMOHook.isPrevented(block.getType()))
                return;

            if (EnchantsSquaredHook.hasEnchantment(player, "Excavation", "Deforestation", "Harvesting"))
                return;

            callViolationEvent(player, zacPlayer, null);
        }, 1);
    }

    private boolean flag(Player player, ZACPlayer zacPlayer, Block block, Location eyeLocation, boolean async) {
        if (!isCheckAllowed(player, zacPlayer, async))
            return false;

        boolean flag = true;
        Location blockLocation = block.getLocation();
        Block targetBlock = zacPlayer.getTargetBlockExact(10);
        if (targetBlock != null)
            if (distanceHorizontal(blockLocation, targetBlock.getLocation()) <= 3.5)
                flag = false;

        if (flag) {
            Set<Material> transparent = new HashSet<>();
            transparent.add(Material.AIR);
            transparent.add(Material.WATER);
            transparent.add(Material.LAVA);
            transparent.add(block.getType());
            if (targetBlock != null)
                transparent.add(targetBlock.getType());

            List<Block> lineOfSight = player.getLineOfSight(transparent, 10);
            for (Block block1 : lineOfSight) {
                if (distanceHorizontal(blockLocation, block1.getLocation()) <= 3.0) {
                    flag = false;
                    break;
                }
            }
        }

        Vector vector = blockLocation.toVector().setY(0.0D).subtract(eyeLocation.toVector().setY(0.0D));
        float angle = eyeLocation.getDirection().setY(0.0D).angle(vector) * 57.2958F;
        if (angle > 120 && eyeLocation.getPitch() < 60 && eyeLocation.getPitch() > -40)
            flag = true;
        return flag;
    }

    private static double getYawChange(Location eyeLocation, ZACPlayer zacPlayer) {
        float yaw = yaw(eyeLocation.getYaw());
        PlayerCacheHistory<Location> eventHistory = zacPlayer.cache.history.onEvent.location;
        PlayerCacheHistory<Location> packetHistory = zacPlayer.cache.history.onPacket.location;
        return Math.max(
                Math.min(Math.abs(yaw - yaw(eventHistory.get(HistoryElement.FROM).getYaw())),
                        Math.abs(yaw - yaw(packetHistory.get(HistoryElement.FROM).getYaw()))),
                Math.min(Math.abs(yaw - yaw(eventHistory.get(HistoryElement.FIRST).getYaw())),
                        Math.abs(yaw - yaw(packetHistory.get(HistoryElement.FIRST).getYaw())))
        );
    }

    private static float yaw(float yaw) {
        yaw = yaw % 360;
        return yaw >= 0 ? yaw : 360 - yaw;
    }

}
