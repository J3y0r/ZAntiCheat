package cn.jeyor1337.zanticheat.event.playerbreakblock;

import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.hook.server.folia.FoliaUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ZACAsyncPlayerBreakBlockEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private ZACPlayer zacPlayer;
    private Block block;
    private Location location;
    private Location eyeLocation;

    public ZACAsyncPlayerBreakBlockEvent(ZACPlayerBreakBlockEvent event) {
        super(!FoliaUtil.isFolia());

        this.player = event.getPlayer();
        this.zacPlayer = event.getZacPlayer();
        this.block = event.getBlock();
        this.location = event.getPlayer().getLocation().clone();
        this.eyeLocation = event.getPlayer().getLocation().clone();
    }

    public Player getPlayer() {
        return player;
    }

    public ZACPlayer getZacPlayer() {
        return zacPlayer;
    }

    public Block getBlock() {
        return block;
    }

    public Location getLocation() {
        return location.clone();
    }

    public Location getEyeLocation() {
        return eyeLocation.clone();
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
