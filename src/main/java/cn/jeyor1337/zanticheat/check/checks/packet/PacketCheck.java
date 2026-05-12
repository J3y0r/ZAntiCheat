package cn.jeyor1337.zanticheat.check.checks.packet;

import cn.jeyor1337.zanticheat.check.Check;
import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.player.ZACPlayerListener;
import cn.jeyor1337.zanticheat.util.hook.server.folia.FoliaUtil;
import cn.jeyor1337.zanticheat.util.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class PacketCheck extends Check {
    public PacketCheck(CheckName name) {
        super(name);
    }

    public boolean limitPackets(char prefix, Buffer buffer, long interval, int limit, int limitRepeatsNeeded) {
        buffer.put(prefix + "methodPackets", buffer.getInt(prefix + "methodPackets") + 1);

        long currentTime = System.currentTimeMillis();
        if (currentTime - buffer.getLong(prefix + "methodStartTime") <= interval)
            return false;
        buffer.put(prefix + "methodStartTime", currentTime);

        int packets = buffer.getInt(prefix + "methodPackets");
        buffer.put(prefix + "methodPackets", 0);

        if (packets <= limit) {
            buffer.put(prefix + "methodFlags", 0);
            return false;
        }
        buffer.put(prefix + "methodFlags", buffer.getInt(prefix + "methodFlags") + 1);

        if (buffer.getInt(prefix + "methodFlags") < limitRepeatsNeeded)
            return false;
        buffer.put(prefix + "methodFlags", 0);
        return true;
    }

    @Nullable
    public Player getPlayer(Object object) {
        if (!(object instanceof Player))
            return null;
        Player player = (Player) object;
        if (!player.isOnline())
            return null;
        return player;
    }

    @Nullable
    public ZACPlayer getZacPlayer(UUID uuid) {
        ZACPlayer zacPlayer = ZACPlayerListener.getAsyncPlayers().getOrDefault(uuid, null);
        if (zacPlayer == null || zacPlayer.leaveTime != 0L)
            return null;
        return zacPlayer;
    }

    public void flag(Player player, ZACPlayer zacPlayer) {
        if (zacPlayer.leaveTime != 0L || !player.isOnline())
            return;
        Scheduler.runTask(true, () -> {
            Scheduler.entityThread(player, () -> {
                if (!FoliaUtil.isStable(player))
                    return;
                if (zacPlayer.leaveTime != 0L || !player.isOnline())
                    return;
                callViolationEvent(getCheckSetting(), player, zacPlayer, null);
            });
        });
    }

}
