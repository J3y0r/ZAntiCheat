package cn.jeyor1337.zanticheat.player;

import cn.jeyor1337.zanticheat.event.packetrecive.ZACAsyncPacketReceiveEvent;
import cn.jeyor1337.zanticheat.event.packetrecive.packettype.PacketType;
import cn.jeyor1337.zanticheat.event.playerbreakblock.ZACPlayerBreakBlockEvent;
import cn.jeyor1337.zanticheat.event.playermove.ZACAsyncPlayerMoveEvent;
import cn.jeyor1337.zanticheat.event.playermove.ZACPlayerMoveEvent;
import cn.jeyor1337.zanticheat.event.playerplaceblock.ZACPlayerPlaceBlockEvent;
import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.player.cache.entity.CachedEntity;
import cn.jeyor1337.zanticheat.player.cache.history.HistoryElement;
import cn.jeyor1337.zanticheat.player.cooldown.PlayerCooldown;
import cn.jeyor1337.zanticheat.player.cooldown.element.EntityDistance;
import cn.jeyor1337.zanticheat.player.violation.PlayerViolations;
import cn.jeyor1337.zanticheat.util.api.ApiUtil;
import cn.jeyor1337.zanticheat.util.async.AsyncUtil;
import cn.jeyor1337.zanticheat.util.config.ConfigManager;
import cn.jeyor1337.zanticheat.util.cooldown.CooldownUtil;
import cn.jeyor1337.zanticheat.util.detection.CheckUtil;
import cn.jeyor1337.zanticheat.util.detection.LeanTowards;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import cn.jeyor1337.zanticheat.version.VerPlayer;
import cn.jeyor1337.zanticheat.version.VerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ZACPlayerListener implements Listener {

    private static final Set<ZACPlayer> LEFT_PLAYERS = new HashSet<>();
    private static Map<UUID, ZACPlayer> asyncPlayers = new ConcurrentHashMap<>();

    public static void loadZACPlayerListener() {
        ApiUtil.clearReportedPlayers();
        loadZacPlayersOnReload();
        loadZacPlayerCleaner();
        loadSchedulerCacheUpdated();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Scheduler.entityThread(event.getPlayer(), true, () -> {
            ApiUtil.unreportPlayer(event.getPlayer());
            loadZacPlayer(event.getPlayer());
        });
    }

    private static void loadZacPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        if (!ZACPlayer.PLAYERS.containsKey(uuid)) {
            ZACPlayer.createZacPlayer(player);
            return;
        }

        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(uuid);
        zacPlayer.joinTime = System.currentTimeMillis();
        zacPlayer.leaveTime = 0;
        zacPlayer.cache = new PlayerCache(player);
        zacPlayer.cooldown = new PlayerCooldown();
    }

    private static void loadZacPlayersOnReload() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scheduler.entityThread(player, true, () -> {
                ApiUtil.unreportPlayer(player);
                loadZacPlayer(player);
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Scheduler.entityThread(event.getPlayer(), true, () -> {
            ApiUtil.unreportPlayer(event.getPlayer());
            ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(event.getPlayer());
            if (zacPlayer == null) return;
            zacPlayer.leaveTime = System.currentTimeMillis();
            zacPlayer.cache = new PlayerCache(event.getPlayer());
            LEFT_PLAYERS.add(zacPlayer);
            getAsyncPlayers().remove(event.getPlayer().getUniqueId());
        });
    }

    private static void loadZacPlayerCleaner() {
        Scheduler.runTaskTimer(() -> {
                    for (ZACPlayer zacPlayer : ZACPlayer.PLAYERS.values())
                        zacPlayer.violations = new PlayerViolations();
                }, 20L * ConfigManager.Config.Violation.Reset.resetInterval,
                20L * ConfigManager.Config.Violation.Reset.resetInterval);

        Scheduler.runTaskTimer(() -> {
            long currentTime = System.currentTimeMillis();
            long wait = ConfigManager.Config.Violation.Cache.enabled ?
                    ((long) ConfigManager.Config.Violation.Cache.cacheDuration * 1000) : (0);

            LEFT_PLAYERS.removeIf(acPlayer -> {
                if (acPlayer.leaveTime == 0)
                    return true;
                if (currentTime - acPlayer.leaveTime >= wait) {
                    ZACPlayer.removeZacPlayer(acPlayer.uuid);
                    return true;
                }
                return false;
            });
        }, 7, 7);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void eventHistory(ZACAsyncPlayerMoveEvent event) {
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();
        zacPlayer.cache.history.onEvent.location.add(event.getFrom());
        Set<Block> downBlocks = CheckUtil.getDownBlocks(player, 0.15);
        zacPlayer.cache.history.onEvent.onGround
                .add(new PlayerCache.OnGround(CheckUtil.isOnGround(player, downBlocks, zacPlayer.cache, LeanTowards.FALSE),
                        CheckUtil.isOnGround(player, downBlocks, zacPlayer.cache, LeanTowards.TRUE)));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void packetHistory(ZACAsyncPacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.FLYING)
            return;
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();
        zacPlayer.cache.history.onPacket.location.add(event.getPlayer().getLocation());
        Set<Block> downBlocks = CheckUtil.getDownBlocks(player, 0.15);
        zacPlayer.cache.history.onPacket.onGround
                .add(new PlayerCache.OnGround(CheckUtil.isOnGround(player, downBlocks, zacPlayer.cache, LeanTowards.FALSE),
                        CheckUtil.isOnGround(player, downBlocks, zacPlayer.cache, LeanTowards.TRUE)));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void activePotionEffects(ZACAsyncPlayerMoveEvent event) {
        Map<PotionEffectType, PotionEffect> potionEffects = new ConcurrentHashMap<>();
        for (PotionEffect potionEffect : event.getPlayer().getActivePotionEffects())
            potionEffects.put(potionEffect.getType(), potionEffect);
        event.getZacPlayer().cache.potionEffects = potionEffects;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void entitiesAsync(ZACAsyncPlayerMoveEvent event) {
        PlayerCache cache = event.getZacPlayer().cache;
        PlayerCooldown cooldown = event.getZacPlayer().cooldown;

        Set<CachedEntity> entitiesNearby = CooldownUtil.getNearbyEntitiesAsync(cooldown, event.getPlayer(), EntityDistance.NEARBY);
        cache.entitiesNearby = entitiesNearby;
        if (!entitiesNearby.isEmpty()) {
            for (CachedEntity entity : entitiesNearby) {
                if (entity.entityType != EntityType.PLAYER) {
                    cache.lastEntityNearby = System.currentTimeMillis();
                    break;
                }
            }
        }

        if (!entitiesNearby.isEmpty()) {
            Set<CachedEntity> entitiesVeryNearby = CooldownUtil.getNearbyEntitiesAsync(cooldown, event.getPlayer(), EntityDistance.VERY_NEARBY);
            cache.entitiesVeryNearby = entitiesVeryNearby;
            if (!entitiesVeryNearby.isEmpty()) {
                for (CachedEntity entity : entitiesVeryNearby) {
                    if (entity.entityType != EntityType.PLAYER) {
                        cache.lastEntityVeryNearby = System.currentTimeMillis();
                        break;
                    }
                }
            }
        } else {
            cache.entitiesVeryNearby = Collections.synchronizedSet(Collections.emptySet());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void lastGlidingRiptidingFlight(ZACPlayerMoveEvent event) {
        if (event.isPlayerGliding())
            event.getZacPlayer().cache.lastGliding = System.currentTimeMillis();
        if (event.isPlayerRiptiding())
            event.getZacPlayer().cache.lastRiptiding = System.currentTimeMillis();
        if (event.isPlayerFlying())
            event.getZacPlayer().cache.lastFlight = System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void lastInsideVehicle(ZACPlayerMoveEvent event) {
        if (!event.isPlayerInsideVehicle())
            return;
        event.getZacPlayer().cache.lastInsideVehicle = System.currentTimeMillis();
    }

    @EventHandler
    public void lastWasDamaged(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.PLAYER)
            return;
        if (!(event.getEntity() instanceof Player))
            return;
        Player player = (Player) event.getEntity();
        if (CheckUtil.isExternalNPC(player)) return;
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (event.getDamage() < 2.0)
                return;
            if (player.getFallDistance() < 4.0)
                return;
            for (int i = 0; i < 3 && i < HistoryElement.values().length; i++)
                if (zacPlayer.cache.history.onEvent.onGround.get(HistoryElement.values()[i]).towardsTrue ||
                        zacPlayer.cache.history.onPacket.onGround.get(HistoryElement.values()[i]).towardsTrue)
                    return;
            boolean ground = false;
            for (Block block : CheckUtil.getDownBlocks(player, 0.1)) {
                if (VerUtil.isPassable(block))
                    continue;
                ground = true;
                break;
            }
            if (!ground)
                return;
        }
        zacPlayer.cache.lastWasDamaged = System.currentTimeMillis();
    }

    @EventHandler
    public void lastWasHit(EntityDamageByEntityEvent event) {
        if (event.getEntityType() != EntityType.PLAYER)
            return;
        if (!(event.getEntity() instanceof Player))
            return;
        Player player = (Player) event.getEntity();
        if (CheckUtil.isExternalNPC(player)) return;
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        zacPlayer.cache.lastWasHit = System.currentTimeMillis();
    }

    @EventHandler
    public void lastWasFished(PlayerFishEvent event) {
        if (!(event.getCaught() instanceof Player))
            return;
        Player player = (Player) event.getCaught();
        if (CheckUtil.isExternalNPC(player)) return;
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        zacPlayer.cache.lastWasFished = System.currentTimeMillis();
        zacPlayer.cache.vectorOnWasFished = null;
    }

    @EventHandler
    public void lastVelocityChange(PlayerVelocityEvent event) {
        if (CheckUtil.isExternalNPC(event)) return;
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        zacPlayer.cache.lastKbVelocity = System.currentTimeMillis();
        zacPlayer.cache.lastAirKbVelocity = System.currentTimeMillis();
        zacPlayer.cache.vectorOnKbVelocity = null;
        zacPlayer.cache.vectorOnAirKbVelocity = null;
        Vector velocity = player.getVelocity();
        if (Math.abs(velocity.getX()) > 0.5 || Math.abs(velocity.getZ()) > 0.5) {
            zacPlayer.cache.lastStrongKbVelocity = System.currentTimeMillis();
            zacPlayer.cache.lastStrongAirKbVelocity = System.currentTimeMillis();
            zacPlayer.cache.vectorOnStrongKbVelocity = null;
            zacPlayer.cache.vectorOnStrongAirKbVelocity = null;
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void lastVelocityChangeNotGround(ZACAsyncPlayerMoveEvent event) {
        if (event.getZacPlayer().cache.history.onEvent.onGround.get(HistoryElement.FROM).towardsTrue ||
                event.getZacPlayer().cache.history.onPacket.onGround.get(HistoryElement.FROM).towardsTrue) {
            event.getZacPlayer().cache.lastAirKbVelocity = 0;
            event.getZacPlayer().cache.lastStrongAirKbVelocity = 0;
        }
    }

    @EventHandler
    public void lastKnockback(EntityDamageByEntityEvent event) {
        if (event.getEntityType() != EntityType.PLAYER)
            return;
        if (!(event.getEntity() instanceof Player))
            return;
        Player player = (Player) event.getEntity();
        if (CheckUtil.isExternalNPC(player)) return;
        Enchantment enchantment = Enchantment.KNOCKBACK;
        Player damager = null;
        if (event.getDamager().getType() == EntityType.PLAYER) {
            damager = (Player) event.getDamager();
        } else if (event.getDamager().getType() == EntityType.ARROW) {
            Arrow arrow = (Arrow) event.getDamager();
            if (arrow.getShooter() instanceof Player) {
                damager = (Player) arrow.getShooter();
                enchantment = Enchantment.ARROW_KNOCKBACK;
            }
        } else if (event.getDamager().getType() == VerUtil.entityTypes.get("SPECTRAL_ARROW")) {
            ProjectileSource shooter = VerUtil.getSpectralArrowShooter(event.getDamager());
            if (shooter instanceof Player) {
                damager = (Player) shooter;
                enchantment = Enchantment.ARROW_KNOCKBACK;
            }
        }
        if (damager == null)
            return;
        if (CheckUtil.isExternalNPC(damager))
            return;
        ItemStack main = VerPlayer.getItemInMainHand(player);
        ItemStack off = VerPlayer.getItemInOffHand(player);
        ItemStack result = null;
        if (main != null && main.getAmount() == 1 && main.getEnchantmentLevel(enchantment) != 0) {
            result = main;
        } else if (off != null && off.getAmount() == 1 && off.getEnchantmentLevel(enchantment) != 0) {
            result = off;
        }
        if (result == null)
            return;

        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        if (result.getEnchantmentLevel(enchantment) <= 2) {
            zacPlayer.cache.lastKnockback = System.currentTimeMillis();
            zacPlayer.cache.vectorOnKnockback = null;
        } else {
            zacPlayer.cache.lastKnockback = System.currentTimeMillis();
            zacPlayer.cache.lastKnockbackNotVanilla = System.currentTimeMillis();
            zacPlayer.cache.vectorOnKnockback = null;
            zacPlayer.cache.vectorOnKnockbackNotVanilla = null;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void lastBlockPlace(ZACPlayerPlaceBlockEvent event) {
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        zacPlayer.cache.lastBlockPlace = System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void lastBlockBreak(ZACPlayerBreakBlockEvent event) {
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        zacPlayer.cache.lastBlockBreak = System.currentTimeMillis();
    }

    @EventHandler
    public void lastTeleport(PlayerTeleportEvent event) {
        if (CheckUtil.isExternalNPC(event)) return;
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        zacPlayer.cache.lastTeleport = System.currentTimeMillis();
    }

    @EventHandler
    public void lastWorldChange(PlayerChangedWorldEvent event) {
        if (CheckUtil.isExternalNPC(event)) return;
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        zacPlayer.cache.lastWorldChange = System.currentTimeMillis();
    }

    @EventHandler
    public void lastGamemodeChange(PlayerGameModeChangeEvent event) {
        if (CheckUtil.isExternalNPC(event)) return;
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        zacPlayer.cache.lastGamemodeChange = System.currentTimeMillis();
    }

    @EventHandler
    public void lastRespawn(PlayerRespawnEvent event) {
        if (CheckUtil.isExternalNPC(event)) return;
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        zacPlayer.cache.lastRespawn = System.currentTimeMillis();
    }

    @EventHandler
    public void lastFirework(PlayerInteractEvent event) {
        if (CheckUtil.isExternalNPC(event)) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR)
            return;
        Player player = event.getPlayer();
        if (!VerPlayer.isGliding(player))
            return;
        ItemStack main = VerPlayer.getItemInMainHand(player);
        ItemStack off = VerPlayer.getItemInOffHand(player);
        ItemStack firework = null;
        if (main != null && main.getAmount() != 0 && main.getType() == VerUtil.material.get("FIREWORK_ROCKET")) {
            firework = main;
        } else if (off != null && off.getAmount() != 0 && off.getType() == VerUtil.material.get("FIREWORK_ROCKET")) {
            firework = off;
        }
        if (firework == null)
            return;
        FireworkMeta meta = (FireworkMeta) firework.getItemMeta();
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        if (meta == null || meta.getPower() <= 3) {
            zacPlayer.cache.lastFireworkBoost = System.currentTimeMillis();
        } else {
            zacPlayer.cache.lastFireworkBoost = System.currentTimeMillis();
            zacPlayer.cache.lastFireworkBoostNotVanilla = System.currentTimeMillis();
        }
    }

    @EventHandler
    public void lastWindCharge(PlayerInteractEvent event) {
        if (CheckUtil.isExternalNPC(event)) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        Player player = event.getPlayer();
        ItemStack main = VerPlayer.getItemInMainHand(player);
        ItemStack off = VerPlayer.getItemInOffHand(player);
        if (!(main != null && main.getType() == VerUtil.material.get("WIND_CHARGE") ||
                off != null && off.getType() == VerUtil.material.get("WIND_CHARGE")))
            return;

        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        zacPlayer.cache.lastWindCharge = System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void lastHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player))
            return;
        Player player = (Player) event.getDamager();
        if (CheckUtil.isExternalNPC(player)) return;
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        zacPlayer.cache.lastHitTime = System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void lastWindBurst(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player))
            return;
        Player player = (Player) event.getDamager();
        if (CheckUtil.isExternalNPC(player)) return;
        ItemStack main = VerPlayer.getItemInMainHand(player);
        if (main == null || main.getType() != VerUtil.material.get("MACE"))
            return;
        int windBurst = main.getEnchantmentLevel(VerUtil.enchantment.get("WIND_BURST"));
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        if (windBurst > 0 && windBurst <= 3) {
            zacPlayer.cache.lastWindBurst = System.currentTimeMillis();
        } else if (windBurst > 3) {
            zacPlayer.cache.lastWindBurst = System.currentTimeMillis();
            zacPlayer.cache.lastWindBurstNotVanilla = System.currentTimeMillis();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void lastWindBurstReceive(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        if (event.getDamager() == null)
            return;
        if (event.getDamager().getType() != VerUtil.entityTypes.get("WIND_CHARGE"))
            return;
        Player player = (Player) event.getEntity();
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        if (zacPlayer == null) return;
        zacPlayer.cache.lastWindChargeReceive = System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void lastSwing(ZACAsyncPacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.ARM_ANIMATION)
            return;
        event.getZacPlayer().cache.lastSwingTime = System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void lastPowderSnowWalk(ZACAsyncPlayerMoveEvent event) {
        Set<Material> materials = ConcurrentHashMap.newKeySet();
        materials.addAll(event.getToDownMaterials());
        materials.addAll(event.getToWithinMaterials());
        materials.addAll(event.getFromDownMaterials());
        materials.addAll(event.getFromWithinMaterials());
        if (!materials.contains(VerUtil.material.get("POWDER_SNOW")))
            return;

        Scheduler.runTask(true, () -> {
            ItemStack boots = event.getZacPlayer().getArmorPiece(EquipmentSlot.FEET);
            if (boots == null || boots.getType() != Material.LEATHER_BOOTS)
                return;
            event.getZacPlayer().cache.lastPowderSnowWalk = System.currentTimeMillis();
        });
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void lastSlimeHoneyBlock(ZACAsyncPlayerMoveEvent event) {
        if (AsyncUtil.getBlock(event.getFrom()) == AsyncUtil.getBlock(event.getTo()))
            return;

        long currentTime = System.currentTimeMillis();
        Vector vector = getVector(event.getFrom(), event.getTo());

        Material honeyBlockType = VerUtil.material.get("HONEY_BLOCK");
        PlayerCache cache = event.getZacPlayer().cache;
        for (Block block : event.getToDownBlocks()) {
            Block downBlock = block.getRelative(BlockFace.DOWN);
            Block downDownBlock = downBlock.getRelative(BlockFace.DOWN);
            if (block.getType() == Material.SLIME_BLOCK || downBlock.getType() == Material.SLIME_BLOCK ||
                    downDownBlock.getType() == Material.SLIME_BLOCK) {
                cache.lastSlimeBlock = currentTime;
                cache.lastSlimeBlockVertical = currentTime;
                cache.vectorOnSlimeBlock = vector;
                cache.vectorOnSlimeBlockVertical = vector;
            } else if (block.getType() == honeyBlockType || downBlock.getType() == honeyBlockType ||
                    downDownBlock.getType() == honeyBlockType) {
                cache.lastHoneyBlock = currentTime;
                cache.lastHoneyBlockVertical = currentTime;
                cache.vectorOnHoneyBlock = vector;
                cache.vectorOnHoneyBlockVertical = vector;
            }
        }
        Set<Block> interactiveBlocks = new HashSet<>();
        for (Block block : CheckUtil.getInteractiveBlocks(event.getPlayer(), event.getTo())) {
            interactiveBlocks.add(block);
            interactiveBlocks.add(block.getRelative(BlockFace.UP));
        }
        for (Block block : interactiveBlocks) {
            if (block.getType() == Material.SLIME_BLOCK) {
                cache.lastSlimeBlock = currentTime;
                cache.lastSlimeBlockHorizontal = currentTime;
                cache.vectorOnSlimeBlock = vector;
                cache.vectorOnSlimeBlockHorizontal = vector;
            } else if (block.getType() == honeyBlockType) {
                cache.lastHoneyBlock = currentTime;
                cache.lastHoneyBlockHorizontal = currentTime;
                cache.vectorOnHoneyBlock = vector;
                cache.vectorOnHoneyBlockHorizontal = vector;
            }
        }

        boolean eventGround = cache.history.onEvent.onGround.get(HistoryElement.FROM).towardsFalse;
        boolean packetGround = cache.history.onPacket.onGround.get(HistoryElement.FROM).towardsFalse;

        if (cache.lastSlimeBlockVertical != 0 && currentTime - cache.lastSlimeBlockVertical > 100) {
            if (eventGround && packetGround || changedVerticalDirectionStrict(cache.vectorOnSlimeBlockVertical, vector))
                cache.lastSlimeBlockVertical = 0;
        }
        if (cache.lastHoneyBlockVertical != 0 && currentTime - cache.lastHoneyBlockVertical > 100) {
            if (eventGround && packetGround || changedVerticalDirectionStrict(cache.vectorOnHoneyBlockVertical, vector))
                cache.lastHoneyBlockVertical = 0;
        }

        if (cache.lastSlimeBlockHorizontal != 0 && currentTime - cache.lastSlimeBlockHorizontal > 200) {
            if (changedDirection(cache.vectorOnSlimeBlockHorizontal, vector) &&
                    changedHorizontalDirection(cache.vectorOnSlimeBlockHorizontal, vector))
                cache.lastSlimeBlockHorizontal = 0;
        }
        if (cache.lastHoneyBlockHorizontal != 0 && currentTime - cache.lastHoneyBlockHorizontal > 200) {
            if (changedDirection(cache.vectorOnHoneyBlockHorizontal, vector) &&
                    changedHorizontalDirection(cache.vectorOnHoneyBlockHorizontal, vector))
                cache.lastHoneyBlockHorizontal = 0;
        }

        if (cache.lastSlimeBlock != 0 && currentTime - cache.lastSlimeBlock > 300 &&
                changedDirection(cache.vectorOnSlimeBlock, vector)) {
            if (changedHorizontalDirection(cache.vectorOnSlimeBlock, vector) ||
                    changedVerticalDirectionStrict(cache.vectorOnSlimeBlock, vector))
                cache.lastSlimeBlock = 0;
        }
        if (cache.lastHoneyBlock != 0 && currentTime - cache.lastHoneyBlock > 300 &&
                changedDirection(cache.vectorOnHoneyBlock, vector)) {
            if (changedHorizontalDirection(cache.vectorOnHoneyBlock, vector) ||
                    changedVerticalDirectionStrict(cache.vectorOnHoneyBlock, vector))
                cache.lastHoneyBlock = 0;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void stopLongBypassAfterRedirection(ZACAsyncPlayerMoveEvent event) {
        long currentTime = System.currentTimeMillis();
        PlayerCache cache = event.getZacPlayer().cache;
        Vector vector = getVector(event.getFrom(), event.getTo());

        if (currentTime - cache.lastBlockExplosion < 100 && cache.vectorOnBlockExplosion == null)
            cache.vectorOnBlockExplosion = vector;
        if (currentTime - cache.lastEntityExplosion < 100 && cache.vectorOnEntityExplosion == null)
            cache.vectorOnEntityExplosion = vector;
        if (currentTime - cache.lastKnockback < 100 && cache.vectorOnKnockback == null)
            cache.vectorOnKnockback = vector;
        if (currentTime - cache.lastKnockbackNotVanilla < 100 && cache.vectorOnKnockbackNotVanilla == null)
            cache.vectorOnKnockbackNotVanilla = vector;
        if (currentTime - cache.lastKbVelocity < 100 && cache.vectorOnKbVelocity == null)
            cache.vectorOnKbVelocity = vector;
        if (currentTime - cache.lastAirKbVelocity < 100 && cache.vectorOnAirKbVelocity == null)
            cache.vectorOnAirKbVelocity = vector;
        if (currentTime - cache.lastStrongKbVelocity < 100 && cache.vectorOnStrongKbVelocity == null)
            cache.vectorOnStrongKbVelocity = vector;
        if (currentTime - cache.lastStrongAirKbVelocity < 100 && cache.vectorOnStrongAirKbVelocity == null)
            cache.vectorOnStrongAirKbVelocity = vector;
        if (currentTime - cache.lastWasFished < 100 && cache.vectorOnWasFished == null)
            cache.vectorOnWasFished = vector;

        if (resetValue(cache.lastBlockExplosion, cache.vectorOnBlockExplosion, vector, false))
            cache.lastBlockExplosion = 0L;
        if (resetValue(cache.lastEntityExplosion, cache.vectorOnEntityExplosion, vector, false))
            cache.lastEntityExplosion = 0L;
        if (resetValue(cache.lastKnockback, cache.vectorOnKnockback, vector, true))
            cache.lastKnockback = 0L;
        if (resetValue(cache.lastKnockbackNotVanilla, cache.vectorOnKnockbackNotVanilla, vector, false))
            cache.lastKnockbackNotVanilla = 0L;
        if (resetValue(cache.lastKbVelocity, cache.vectorOnKbVelocity, vector, true))
            cache.lastKbVelocity = 0L;
        if (resetValue(cache.lastAirKbVelocity, cache.vectorOnAirKbVelocity, vector, true))
            cache.lastAirKbVelocity = 0L;
        if (resetValue(cache.lastStrongKbVelocity, cache.vectorOnStrongKbVelocity, vector, false))
            cache.lastStrongKbVelocity = 0L;
        if (resetValue(cache.lastStrongAirKbVelocity, cache.vectorOnStrongAirKbVelocity, vector, false))
            cache.lastStrongAirKbVelocity = 0L;
        if (resetValue(cache.lastWasFished, cache.vectorOnWasFished, vector, false))
            cache.lastWasFished = 0L;
    }

    private static boolean resetValue(long start, Vector from, Vector to, boolean strict) {
        if (start == 0L)
            return false;
        if (from == null)
            return false;
        long passedTime = System.currentTimeMillis() - start;
        if (passedTime < 300 || passedTime > 20 * 1000)
            return false;
        if (!changedDirection(from, to))
            return false;
        double horizontalSpeed = Math.sqrt(Math.pow(from.getX(), 2) + Math.pow(from.getZ(), 2));
        double verticalSpeed = Math.abs(from.getY());
        if (horizontalSpeed > verticalSpeed) {
            return changedHorizontalDirection(from, to);
        } else {
            if (!strict) return changedVerticalDirection(from, to);
            else return changedVerticalDirectionStrict(from, to);
        }
    }

    private static Vector getVector(Location from, Location to) {
        return new Vector(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
    }

    private static boolean changedDirection(Vector first, Vector second) {
        Vector difference = first.clone().normalize().subtract(second.clone().normalize());
        return difference.length() > 1.55;
    }

    private static boolean changedHorizontalDirection(Vector first, Vector second) {
        Vector difference = first.clone().setY(0).normalize().subtract(second.clone().setY(0).normalize());
        return difference.length() > 1.35;
    }

    private static boolean changedVerticalDirection(Vector first, Vector second) {
        if (first.getY() <= -0.005)
            return false;
        return first.getY() > 0.005 && second.getY() <= 0 ||
                first.getY() < -0.005 && second.getY() >= 0 ||
                Math.abs(first.getY()) < 0.005 && Math.abs(second.getY()) > 0.25;
    }

    private static boolean changedVerticalDirectionStrict(Vector first, Vector second) {
        if (first.getY() <= -0.005)
            return false;
        return first.getY() > 0.01 && second.getY() < -0.01 ||
                first.getY() < -0.01 && second.getY() > 0.01 ||
                Math.abs(first.getY()) < 0.01 && Math.abs(second.getY()) > 0.25;
    }

    @EventHandler
    public void onExplosionDamage(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.PLAYER)
            return;
        if (!(event.getEntity() instanceof Player))
            return;
        Player player = (Player) event.getEntity();
        if (CheckUtil.isExternalNPC(player)) return;
        ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
        if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            zacPlayer.cache.lastBlockExplosion = System.currentTimeMillis();
            zacPlayer.cache.vectorOnBlockExplosion = null;

        }
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            zacPlayer.cache.lastEntityExplosion = System.currentTimeMillis();
            zacPlayer.cache.vectorOnEntityExplosion = null;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void lastBlockExplosion(ZACAsyncPlayerMoveEvent event) {
        for (CachedEntity cachedEntity : event.getZacPlayer().cache.entitiesNearby)
            if (cachedEntity.entityType == EntityType.PRIMED_TNT) {
                ZACPlayer zacPlayer = event.getZacPlayer();
                zacPlayer.cache.lastBlockExplosion = System.currentTimeMillis();
                zacPlayer.cache.vectorOnBlockExplosion = null;
                return;
            }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void lastInWater(ZACAsyncPlayerMoveEvent event) {
        if (!VerPlayer.isInWater(event.getPlayer()))
            return;
        event.getZacPlayer().cache.lastInWater = System.currentTimeMillis();
    }

    private static void loadSchedulerCacheUpdated() {
        Scheduler.runTaskTimer(() -> {
            Map<Player, ZACPlayer> players = new ConcurrentHashMap<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                ZACPlayer zacPlayer = ZACPlayer.getZacPlayer(player);
                if (zacPlayer == null) continue;
                players.put(player, zacPlayer);
            }
            Scheduler.runTaskAsynchronously(true, () -> {
                Map<UUID, ZACPlayer> asyncPlayers = new ConcurrentHashMap<>();
                players.forEach((player, zacPlayer) -> {
                    asyncPlayers.put(player.getUniqueId(), zacPlayer);
                    PlayerCache cache = zacPlayer.cache;
                    Scheduler.entityThread(player, () -> {
                        cache.sneakingTicks = increase(player.isSneaking(), cache.sneakingTicks);
                        cache.sprintingTicks = increase(player.isSprinting(), cache.sprintingTicks);
                        cache.swimmingTicks = increase(zacPlayer.isSwimming(), cache.swimmingTicks);
                        cache.climbingTicks = increase(zacPlayer.isClimbing(), cache.climbingTicks);
                        cache.glidingTicks = increase(zacPlayer.isGliding(), cache.glidingTicks);
                        cache.riptidingTicks = increase(zacPlayer.isRiptiding(), cache.riptidingTicks);
                        cache.flyingTicks = increase(player.isFlying(), cache.flyingTicks);
                        cache.blockingTicks = increase(player.isBlocking(), cache.blockingTicks);
                        if (player.isInsideVehicle())
                            cache.lastInsideVehicle = System.currentTimeMillis();
                    });
                });
                setAsyncPlayers(asyncPlayers);
            });
        }, 1, 1);
    }

    private static int increase(boolean value, int oldValue) {
        if (value && oldValue < 0 || !value && oldValue >= 0)
            oldValue = 0;
        if (Math.abs(oldValue) >= 20 * 60 * 60)
            return oldValue;
        return value ? oldValue + 1 : oldValue - 1;
    }

    public static Map<UUID, ZACPlayer> getAsyncPlayers() {
        return asyncPlayers;
    }

    private static void setAsyncPlayers(Map<UUID, ZACPlayer> asyncPlayers) {
        ZACPlayerListener.asyncPlayers = asyncPlayers;
    }

}
