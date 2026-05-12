package cn.jeyor1337.zanticheat.event.playerplaceblock;

import cn.jeyor1337.zanticheat.player.ZACPlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockPlaceEvent;

public class ZACPlayerPlaceBlockEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private BlockPlaceEvent event;
    private Player player;
    private ZACPlayer zacPlayer;
    private Block block;
    private Block blockAgainst;
    private BlockState blockReplacedState;

    public ZACPlayerPlaceBlockEvent(BlockPlaceEvent event, Player player, ZACPlayer zacPlayer,
                                    Block block, Block blockAgainst, BlockState blockReplacedState) {
        this.event = event;
        this.player = player;
        this.zacPlayer = zacPlayer;
        this.block = block;
        this.blockAgainst = blockAgainst;
        this.blockReplacedState = blockReplacedState;
    }

    public BlockPlaceEvent getEvent() {
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

    public Block getBlockAgainst() {
        return blockAgainst;
    }

    public BlockState getBlockReplacedState() {
        return blockReplacedState;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
