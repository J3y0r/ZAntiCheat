package cn.jeyor1337.zanticheat.player;

import cn.jeyor1337.zanticheat.player.cache.PlayerCache;
import cn.jeyor1337.zanticheat.player.cooldown.PlayerCooldown;
import cn.jeyor1337.zanticheat.player.violation.PlayerViolations;
import cn.jeyor1337.zanticheat.util.hook.server.folia.FoliaUtil;
import cn.jeyor1337.zanticheat.version.VerPlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ZACPlayer extends VerPlayer {

    private ZACPlayer(Player player) {
        super(player);
        this.uuid = player.getUniqueId();
        this.joinTime = System.currentTimeMillis();
        this.cache = new PlayerCache(player);
        this.cooldown = new PlayerCooldown();
        this.violations = new PlayerViolations();
        PLAYERS.put(uuid, this);
    }

    static {
        PLAYERS = !FoliaUtil.isFolia() ? new HashMap<>() : new ConcurrentHashMap<>();
    }

    protected static final Map<UUID, ZACPlayer> PLAYERS;

    public UUID uuid;
    public long joinTime;
    public long leaveTime;

    public PlayerCache cache;
    public PlayerCooldown cooldown;
    public PlayerViolations violations;

    public static ZACPlayer getZacPlayer(UUID uuid) {
        return PLAYERS.getOrDefault(uuid, null);
    }

    public static ZACPlayer getZacPlayer(Player player) {
        return getZacPlayer(player.getUniqueId());
    }

    protected static void createZacPlayer(Player player) {
        new ZACPlayer(player);
    }

    protected static void removeZacPlayer(UUID uuid) {
        PLAYERS.remove(uuid);
    }

}
