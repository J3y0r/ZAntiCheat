package cn.jeyor1337.zanticheat.event.playerbreakblock;

import cn.jeyor1337.zanticheat.player.ZACPlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;

public class ZACPlayerBreakBlockEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private BlockBreakEvent event;
    private Player player;
    private ZACPlayer zacPlayer;
    private Block block;

    public ZACPlayerBreakBlockEvent(BlockBreakEvent event, Player player, ZACPlayer zacPlayer, Block block) {
        this.event = event;
        this.player = player;
        this.zacPlayer = zacPlayer;
        this.block = block;
    }

    public BlockBreakEvent getEvent() {
        return event;
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

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
