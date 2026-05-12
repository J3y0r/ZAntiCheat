package cn.jeyor1337.zanticheat.check.checks.interaction.blockplace;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.interaction.InteractionCheck;
import cn.jeyor1337.zanticheat.event.playerplaceblock.ZACAsyncPlayerPlaceBlockEvent;
import cn.jeyor1337.zanticheat.event.playerplaceblock.ZACPlayerPlaceBlockEvent;
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
public class BlockPlaceA extends InteractionCheck implements Listener {
    public BlockPlaceA() {
        super(CheckName.BLOCKPZACE_A);
    }

    @EventHandler
    public void onAsyncBlockBreak(ZACAsyncPlayerPlaceBlockEvent event) {
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("lastAsyncResult", flag(event.getPlayer(), event.getZacPlayer(),
                event.getBlock(), event.getEyeLocation(), true));
    }

    @EventHandler
    public void onBlockBreak(ZACPlayerPlaceBlockEvent event) {
        Buffer buffer = getBuffer(event.getPlayer(), true);
        if (!buffer.getBoolean("lastAsyncResult"))
            return;
        if (!flag(event.getPlayer(), event.getZacPlayer(),
                event.getBlock(), event.getPlayer().getEyeLocation(), false))
            return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - buffer.getLong("lastFlag") < 550)
            return;
        buffer.put("lastFlag", currentTime);

        buffer.put("flags", buffer.getInt("flags") + 1);
        if (buffer.getInt("flags") <= 1)
            return;

        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();
        Block block = event.getBlock();

        Scheduler.runTaskLater(player, () -> {
            if (getYawChange(player.getEyeLocation(), zacPlayer) > 35.0)
                return;

            if (AureliumSkillsHook.isPrevented(player) ||
                    VeinMinerHook.isPrevented(player) ||
                    McMMOHook.isPrevented(block.getType()))
                return;

            if (EnchantsSquaredHook.hasEnchantment(player, "Illuminated", "Harvesting"))
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
            if (distanceHorizontal(blockLocation, targetBlock.getLocation()) <= 3.0)
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
                if (distanceHorizontal(blockLocation, block1.getLocation()) <= 2.5) {
                    flag = false;
                    break;
                }
            }
        }

        Vector vector = blockLocation.toVector().setY(0.0D).subtract(eyeLocation.toVector().setY(0.0D));
        float angle = eyeLocation.getDirection().setY(0.0D).angle(vector) * 57.2958F;
        if (angle > 110 && eyeLocation.getPitch() < 60 && eyeLocation.getPitch() > -40)
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
