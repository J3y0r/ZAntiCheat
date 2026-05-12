package cn.jeyor1337.zanticheat.check.checks.combat.fakelag;

import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.CheckSetting;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.combat.CombatCheck;
import cn.jeyor1337.zanticheat.event.packetrecive.ZACAsyncPacketReceiveEvent;
import cn.jeyor1337.zanticheat.event.packetrecive.packettype.PacketType;
import cn.jeyor1337.zanticheat.event.playerattack.ZACPlayerAttackEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.hook.plugin.FloodgateHook;
import cn.jeyor1337.zanticheat.util.player.connectionstability.ConnectionStability;
import cn.jeyor1337.zanticheat.util.player.connectionstability.ConnectionStabilityListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class FakeLagA extends CombatCheck implements Listener {

    public FakeLagA() {
        super(CheckName.FAKELAG_A);
    }

    @EventHandler
    public void onAsyncPacketReceive(ZACAsyncPacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.FLYING)
            return;

        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();
        Buffer buffer = getBuffer(player, true);
        long currentTime = System.currentTimeMillis();
        long lastFlyingAt = buffer.getLong("lastFlyingAt");
        buffer.put("lastFlyingAt", currentTime);

        if (lastFlyingAt == 0L)
            return;

        long delta = currentTime - lastFlyingAt;
        if (delta < 5L || delta > 250L) {
            if (buffer.getBoolean("awaitingPostWindow") && currentTime - buffer.getLong("postWindowStartAt") > getCheckSetting().fakeLagAPostWindowTime)
                clearPostWindow(buffer);
            return;
        }

        if (!isCheckAllowed(player, zacPlayer, true)) {
            resetState(buffer);
            return;
        }

        if (FloodgateHook.isProbablyPocketEditionPlayer(player, true)) {
            resetState(buffer);
            return;
        }

        ConnectionStability connectionStability = ConnectionStabilityListener.getConnectionStability(player);
        if (getCheckSetting().fakeLagAIgnoreLowConnectionStability && connectionStability == ConnectionStability.LOW) {
            resetState(buffer);
            return;
        }

        if (!buffer.getBoolean("awaitingPostWindow")) {
            updateBaseline(buffer, delta, getCheckSetting());
            return;
        }

        if (currentTime - buffer.getLong("postWindowStartAt") > getCheckSetting().fakeLagAPostWindowTime) {
            clearPostWindow(buffer);
            updateBaseline(buffer, delta, getCheckSetting());
            return;
        }

        buffer.put("postPacketCount", buffer.getInt("postPacketCount") + 1);
        updateAverage(buffer, "postAvgDelta", "postPacketCount", delta);
        buffer.put("postMaxDelta", Math.max(buffer.getLong("postMaxDelta"), delta));

        double preAvgDelta = buffer.getDouble("preAvgDelta");
        double preAbsDev = buffer.getDouble("preAvgAbsDev");
        double spikeRatio = preAvgDelta <= 0.0 ? 0.0 : delta / preAvgDelta;
        if (delta - preAvgDelta >= getCheckSetting().fakeLagAMinSpikeDelta ||
                spikeRatio >= getCheckSetting().fakeLagAMinSpikeRatio ||
                delta > preAvgDelta + Math.max(40.0, preAbsDev * 3.0)) {
            buffer.put("postSpikeCount", buffer.getInt("postSpikeCount") + 1);
        }

        if (buffer.getInt("postSpikeCount") >= 2 ||
                buffer.getLong("postMaxDelta") - Math.round(preAvgDelta) >= getCheckSetting().fakeLagAMinSpikeDelta + 50L) {
            buffer.put("readyToFlag", true);
            buffer.put("postConfirmedAt", currentTime);
            clearPostWindow(buffer);
            return;
        }

        if (buffer.getInt("postPacketCount") >= getCheckSetting().fakeLagAPostWindowPackets)
            clearPostWindow(buffer);
    }

    @EventHandler
    public void onHit(ZACPlayerAttackEvent event) {
        if (!event.isEntityAttackCause())
            return;

        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();
        if (!isCheckAllowed(player, zacPlayer))
            return;

        if (FloodgateHook.isProbablyPocketEditionPlayer(player))
            return;

        ConnectionStability connectionStability = ConnectionStabilityListener.getConnectionStability(player);
        if (getCheckSetting().fakeLagAIgnoreLowConnectionStability && connectionStability == ConnectionStability.LOW)
            return;

        Buffer buffer = getBuffer(player, true);
        long currentTime = System.currentTimeMillis();

        if (buffer.getBoolean("readyToFlag") && currentTime - buffer.getLong("postConfirmedAt") <= getCheckSetting().fakeLagAReadyTimeout) {
            int fakeLagFlags = currentTime - buffer.getLong("lastFlagWindowAt") > getCheckSetting().fakeLagAReadyTimeout * 2L
                    ? 1
                    : buffer.getInt("fakeLagFlags") + 1;
            buffer.put("fakeLagFlags", fakeLagFlags);
            buffer.put("lastFlagWindowAt", currentTime);
            buffer.put("readyToFlag", false);
            if (fakeLagFlags >= getCheckSetting().fakeLagAFlagsRequired) {
                buffer.put("fakeLagFlags", 0);
                callViolationEvent(player, zacPlayer, event.getEvent());
            }
        } else if (buffer.getBoolean("readyToFlag")) {
            buffer.put("readyToFlag", false);
            buffer.put("fakeLagFlags", 0);
        }

        if (!hasStableBaseline(buffer, getCheckSetting())) {
            clearPostWindow(buffer);
            return;
        }

        buffer.put("lastAttackAt", currentTime);
        buffer.put("awaitingPostWindow", true);
        buffer.put("postWindowStartAt", currentTime);
        buffer.put("postPacketCount", 0);
        buffer.put("postSpikeCount", 0);
        buffer.put("postMaxDelta", 0L);
        buffer.put("postAvgDelta", 0.0);
    }

    private static void updateBaseline(Buffer buffer, long delta, CheckSetting setting) {
        int previousCount = Math.min(buffer.getInt("preCount"), setting.fakeLagAPreSamples);
        int nextCount = Math.min(previousCount + 1, setting.fakeLagAPreSamples);
        double currentAverage = buffer.getDouble("preAvgDelta");
        double newAverage;
        if (previousCount <= 0) {
            newAverage = delta;
        } else if (previousCount < setting.fakeLagAPreSamples) {
            newAverage = ((currentAverage * previousCount) + delta) / nextCount;
        } else {
            newAverage = currentAverage + (delta - currentAverage) / setting.fakeLagAPreSamples;
        }
        buffer.put("preCount", nextCount);
        buffer.put("preAvgDelta", newAverage);

        double currentAbsDev = buffer.getDouble("preAvgAbsDev");
        double absDiff = Math.abs(delta - newAverage);
        double newAbsDev;
        if (previousCount <= 0) {
            newAbsDev = 0.0;
        } else if (previousCount < setting.fakeLagAPreSamples) {
            newAbsDev = ((currentAbsDev * previousCount) + absDiff) / nextCount;
        } else {
            newAbsDev = currentAbsDev + (absDiff - currentAbsDev) / setting.fakeLagAPreSamples;
        }
        buffer.put("preAvgAbsDev", newAbsDev);
    }

    private static void updateAverage(Buffer buffer, String averageKey, String countKey, long delta) {
        int count = buffer.getInt(countKey);
        double average = buffer.getDouble(averageKey);
        if (count <= 1) {
            buffer.put(averageKey, (double) delta);
            return;
        }
        buffer.put(averageKey, average + (delta - average) / count);
    }

    private static boolean hasStableBaseline(Buffer buffer, CheckSetting setting) {
        return buffer.getInt("preCount") >= setting.fakeLagAPreSamples &&
                buffer.getDouble("preAvgDelta") > 0.0 &&
                buffer.getDouble("preAvgDelta") <= setting.fakeLagAMaxPreAvgDelta &&
                buffer.getDouble("preAvgAbsDev") <= setting.fakeLagAMaxPreAbsDev;
    }

    private static void clearPostWindow(Buffer buffer) {
        buffer.put("awaitingPostWindow", false);
        buffer.put("postWindowStartAt", 0L);
        buffer.put("postPacketCount", 0);
        buffer.put("postSpikeCount", 0);
        buffer.put("postMaxDelta", 0L);
        buffer.put("postAvgDelta", 0.0);
    }

    private static void resetState(Buffer buffer) {
        clearPostWindow(buffer);
        buffer.put("readyToFlag", false);
        buffer.put("postConfirmedAt", 0L);
        buffer.put("fakeLagFlags", 0);
    }

}
