package cn.jeyor1337.zanticheat.event.playerattack;

import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.hook.server.folia.FoliaUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ZACAsyncPlayerAttackEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final ZACPlayer zacPlayer;
    private final int entityId;

    public ZACAsyncPlayerAttackEvent(Player player, ZACPlayer zacPlayer, int entityId) {
        super(!FoliaUtil.isFolia());

        this.player = player;
        this.zacPlayer = zacPlayer;
        this.entityId = entityId;
    }

    public Player getPlayer() {
        return player;
    }

    public ZACPlayer getZacPlayer() {
        return zacPlayer;
    }

    public int getEntityId() {
        return entityId;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
