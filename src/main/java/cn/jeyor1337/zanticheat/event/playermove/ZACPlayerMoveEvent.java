package cn.jeyor1337.zanticheat.event.playermove;

import cn.jeyor1337.zanticheat.player.ZACPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerMoveEvent;

public class ZACPlayerMoveEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final PlayerMoveEvent event;
    private final Player player;
    private final ZACPlayer zacPlayer;
    private final Location to;
    private final Location from;
    private final Boolean isPlayerFlying;
    private final Boolean isPlayerInsideVehicle;
    private final Boolean isPlayerGliding;
    private final Boolean isPlayerRiptiding;

    public ZACPlayerMoveEvent(PlayerMoveEvent event, Player player,
                              ZACPlayer zacPlayer, Location from, Location to) {
        this.event = event;
        this.player = player;
        this.zacPlayer = zacPlayer;
        this.from = from;
        this.to = to;
        this.isPlayerFlying = player.isFlying();
        this.isPlayerInsideVehicle = player.isInsideVehicle();
        this.isPlayerGliding = zacPlayer.isGliding();
        this.isPlayerRiptiding = zacPlayer.isRiptiding();
    }

    public PlayerMoveEvent getEvent() {
        return event;
    }

    public Player getPlayer() {
        return player;
    }

    public ZACPlayer getZacPlayer() {
        return zacPlayer;
    }

    public Location getFrom() {
        return from;
    }

    public Location getTo() {
        return to;
    }

    public boolean isPlayerFlying() {
        return isPlayerFlying;
    }

    public boolean isPlayerInsideVehicle() {
        return isPlayerInsideVehicle;
    }

    public boolean isPlayerGliding() {
        return isPlayerGliding;
    }

    public boolean isPlayerRiptiding() {
        return isPlayerRiptiding;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
