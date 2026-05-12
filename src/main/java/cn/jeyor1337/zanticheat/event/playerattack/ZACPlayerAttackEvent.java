package cn.jeyor1337.zanticheat.event.playerattack;

import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.detection.CheckUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class ZACPlayerAttackEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private EntityDamageByEntityEvent event;
    private Player player;
    private ZACPlayer zacPlayer;
    private Entity entity;
    private boolean isEntityAttackCause;

    public ZACPlayerAttackEvent(EntityDamageByEntityEvent event, Player player, ZACPlayer zacPlayer, Entity entity) {
        this.event = event;
        this.player = player;
        this.zacPlayer = zacPlayer;
        this.entity = entity;
        this.isEntityAttackCause = event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK;
    }

    public EntityDamageByEntityEvent getEvent() {
        return event;
    }

    public Player getPlayer() {
        return player;
    }

    public ZACPlayer getZacPlayer() {
        return zacPlayer;
    }

    public Entity getEntity() {
        return entity;
    }

    public boolean isEntityAttackCause() {
        return isEntityAttackCause;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
