package cn.jeyor1337.zanticheat.check.checks.interaction.fastbreak;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.interaction.InteractionCheck;
import cn.jeyor1337.zanticheat.event.playerbreakblock.ZACPlayerBreakBlockEvent;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.hook.plugin.simplehook.AureliumSkillsHook;
import cn.jeyor1337.zanticheat.util.hook.plugin.simplehook.EnchantsSquaredHook;
import cn.jeyor1337.zanticheat.util.hook.plugin.simplehook.McMMOHook;
import cn.jeyor1337.zanticheat.util.hook.plugin.simplehook.VeinMinerHook;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

/**
 * Mining with a pickaxe using Timer or FastBreak hack
 */
public class FastBreakA extends InteractionCheck implements Listener {

    static class Duration {
        public Duration(int stone, int deepslate) {
            DURATIONS.put(Material.STONE, stone);
            DURATIONS.put(VerUtil.material.get("DEEPSLATE"), deepslate);
        }

        private final Map<Material, Integer> DURATIONS = new HashMap<>();

        public int getDuration(Material material) {
            return DURATIONS.getOrDefault(material, 0);
        }
    }

    private static final Map<Material, Duration> DURATIONS = new HashMap<>();
    private static final Map<Material, Duration> ENCHANTED_DURATIONS = new HashMap<>();

    static {
        DURATIONS.put(VerUtil.material.get("WOODEN_PICKAXE"), new Duration(1150, 2250));
        DURATIONS.put(Material.STONE_PICKAXE, new Duration(600, 1150));
        DURATIONS.put(Material.IRON_PICKAXE, new Duration(400, 750));
        DURATIONS.put(Material.DIAMOND_PICKAXE, new Duration(300, 600));
        DURATIONS.put(VerUtil.material.get("NETHERITE_PICKAXE"), new Duration(250, 500));

        ENCHANTED_DURATIONS.put(VerUtil.material.get("WOODEN_PICKAXE"), new Duration(100, 200));
        ENCHANTED_DURATIONS.put(Material.STONE_PICKAXE, new Duration(100, 150));
        ENCHANTED_DURATIONS.put(Material.IRON_PICKAXE, new Duration(100, 150));
        ENCHANTED_DURATIONS.put(Material.DIAMOND_PICKAXE, new Duration(100, 150));
        ENCHANTED_DURATIONS.put(VerUtil.material.get("NETHERITE_PICKAXE"), new Duration(100, 150));
    }

    public FastBreakA() {
        super(CheckName.FASTBREAK_A);
    }

    @EventHandler
    public void onBlockBreak(ZACPlayerBreakBlockEvent event) {
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();

        if (!isCheckAllowed(player, zacPlayer))
            return;
        if (player.getGameMode() != GameMode.SURVIVAL)
            return;
        Block block = event.getBlock();
        if (AureliumSkillsHook.isPrevented(player) ||
                VeinMinerHook.isPrevented(player) ||
                McMMOHook.isPrevented(block.getType()))
            return;

        Buffer buffer = getBuffer(player);
        ItemStack tool = zacPlayer.getItemInMainHand();
        if (tool == null) return;
        int efficiencyLevel = tool.getEnchantmentLevel(VerUtil.enchantment.get("EFFICIENCY"));
        if (efficiencyLevel > 5)
            return;
        boolean enchantedTool = efficiencyLevel != 0;

        if (block.getType() != Material.STONE && block.getType() != VerUtil.material.get("DEEPSLATE")) {
            if (buffer.getInt("flags") > 0)
                buffer.put("flags", buffer.getInt("flags") - 1);
            return;
        }
        if (!DURATIONS.containsKey(tool.getType()) || !ENCHANTED_DURATIONS.containsKey(tool.getType())) {
            if (buffer.getInt("flags") > 0)
                buffer.put("flags", buffer.getInt("flags") - 1);
            return;
        }

        if (!buffer.isExists("lastInteraction")) {
            if (buffer.getInt("flags") > 0)
                buffer.put("flags", buffer.getInt("flags") - 1);
            return;
        }

        if (!buffer.isExists("blockType") || buffer.getMaterial("blockType") != block.getType()) {
            buffer.put("blockType", block.getType());
            if (buffer.getInt("flags") > 0)
                buffer.put("flags", buffer.getInt("flags") - 1);
            return;
        }

        if (!buffer.isExists("tool") || buffer.getMaterial("tool") != tool.getType()) {
            buffer.put("tool", tool.getType());
            if (buffer.getInt("flags") > 0)
                buffer.put("flags", buffer.getInt("flags") - 1);
            return;
        }

        if (!buffer.isExists("enchantedTool") || buffer.getBoolean("enchantedTool") != enchantedTool) {
            buffer.put("enchantedTool", enchantedTool);
            if (buffer.getInt("flags") > 0)
                buffer.put("flags", buffer.getInt("flags") - 1);
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - buffer.getLong("effectTime") <= 10 * 1000) {
            buffer.put("flags", 0);
            return;
        }

        long interval = System.currentTimeMillis() - buffer.getLong("lastInteraction");

        long maxDuration = !enchantedTool ?
                DURATIONS.get(tool.getType()).getDuration(block.getType()) :
                ENCHANTED_DURATIONS.get(tool.getType()).getDuration(block.getType());

        boolean flag = interval < maxDuration / 1.45;

        if (flag) {
            if (buffer.getInt("flags") < 6)
                buffer.put("flags", buffer.getInt("flags") + 1);
        } else {
            if (buffer.getInt("flags") > 0)
                buffer.put("flags", buffer.getInt("flags") - 1);
            if (buffer.getInt("flags") > 0)
                buffer.put("flags", buffer.getInt("flags") - 1);
        }

        if (buffer.getInt("flags") < 6)
            return;
        if (buffer.getInt("flags") > 0)
            buffer.put("flags", buffer.getInt("flags") - 1);
        if (buffer.getInt("flags") > 0)
            buffer.put("flags", buffer.getInt("flags") - 1);

        if (EnchantsSquaredHook.hasEnchantment(player, "Excavation", "Deforestation", "Harvesting"))
            return;

        Map<String, Double> attributes = getPlayerAttributes(player);
        if (getItemStackAttributes(player, "PLAYER_BLOCK_BREAK_SPEED",
                "PLAYER_MINING_EFFICIENCY", "PLAYER_SUBMERGED_MINING_SPEED") != 0 ||
                attributes.getOrDefault("PLAYER_BLOCK_BREAK_SPEED", 0.0) > 0.01 ||
                attributes.getOrDefault("PLAYER_MINING_EFFICIENCY", 0.0) > 0.01 ||
                attributes.getOrDefault("PLAYER_SUBMERGED_MINING_SPEED", 0.0) > 0.01)
            buffer.put("attribute", System.currentTimeMillis());
        if (System.currentTimeMillis() - buffer.getLong("attribute") < 3500)
            return;

        callViolationEvent(player, zacPlayer, event.getEvent());
    }

    @EventHandler
    public void onInteraction(PlayerInteractEvent event) {
        if (isExternalNPC(event)) return;
        if (event.getAction() != Action.LEFT_CLICK_BLOCK)
            return;

        Buffer buffer = getBuffer(event.getPlayer());
        buffer.put("lastInteraction", System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void beforeBlockBreak(ZACPlayerBreakBlockEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, zacPlayer))
            return;

        if (getEffectAmplifier(player, PotionEffectType.FAST_DIGGING) > 0) {
            Buffer buffer = getBuffer(player);
            buffer.put("effectTime", System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onMovement(ZACAsyncPlayerMoveEvent event) {
        ZACPlayer zacPlayer = event.getZacPlayer();
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        if (getEffectAmplifier(zacPlayer.cache, PotionEffectType.FAST_DIGGING) > 0) {
            Scheduler.runTask(true, () -> {
                Buffer buffer = getBuffer(player);
                buffer.put("effectTime", System.currentTimeMillis());
            });
        }
    }

}
