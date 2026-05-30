package cn.jeyor1337.zanticheat.event.packetrecive;

import cn.jeyor1337.zanticheat.event.packetrecive.packettype.PacketType;
import cn.jeyor1337.zanticheat.event.packetrecive.packettype.PacketTypeRecognizer;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.hook.server.folia.FoliaUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ZACAsyncPacketReceiveEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final ZACPlayer zacPlayer;
    private final PacketType packetType;
    private final int entityId;
    private final boolean attack;
    private final float yaw;
    private final float pitch;

    public ZACAsyncPacketReceiveEvent(Player player, ZACPlayer zacPlayer, Object nmsPacket) {
        super(!FoliaUtil.isFolia());

        this.player = player;
        this.zacPlayer = zacPlayer;
        this.packetType = PacketTypeRecognizer.getPacketType(nmsPacket);
        this.entityId = PacketTypeRecognizer.getEntityId(nmsPacket);
        this.attack = PacketTypeRecognizer.isAttack(nmsPacket);
        if (this.packetType == PacketType.FLYING) {
            this.yaw = PacketTypeRecognizer.getYaw(nmsPacket);
            this.pitch = PacketTypeRecognizer.getPitch(nmsPacket);
        } else {
            this.yaw = Float.NaN;
            this.pitch = Float.NaN;
        }
    }

    public Player getPlayer() {
        return player;
    }

    public ZACPlayer getZacPlayer() {
        return zacPlayer;
    }

    public PacketType getPacketType() {
        return packetType;
    }

    public int getEntityId() {
        return entityId;
    }

    public boolean isAttack() {
        return attack;
    }

    public boolean hasRotation() {
        return !Float.isNaN(yaw) && !Float.isNaN(pitch);
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
