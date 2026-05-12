package cn.jeyor1337.zanticheat.check.checks.combat.autoclicker;

import com.kireiko.utils.math.MathUtil;
import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.CheckSetting;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.combat.CombatCheck;
import cn.jeyor1337.zanticheat.event.packetrecive.ZACAsyncPacketReceiveEvent;
import cn.jeyor1337.zanticheat.event.packetrecive.packettype.PacketType;
import cn.jeyor1337.zanticheat.event.playerattack.ZACPlayerAttackEvent;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.player.connectionstability.ConnectionStability;
import cn.jeyor1337.zanticheat.util.player.connectionstability.ConnectionStabilityListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoClickerC extends CombatCheck implements Listener {
    private static final int ROTATION_BUFFER_SIZE = 24;
    private static final long AIM_LOOKBACK_MS = 100L;
    private static final long RECENT_COMBAT_WINDOW_MS = 3000L;
    private static final long READY_TIMEOUT_MS = 1500L;
    private static final double SHORT_WINDOW_BONUS = 0.15;
    private static final double SESSION_WINDOW_BONUS = 0.15;
    private static final double ROTATION_WEIGHT = 0.6;
    private static final double MIN_ENTROPY = 0.0001;

    public AutoClickerC() {
        super(CheckName.AUTOCLICKER_C);
    }

    @EventHandler
    public void onAsyncPacket(ZACAsyncPacketReceiveEvent event) {
        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();

        if (event.getPacketType() == PacketType.FLYING) {
            if (!isCheckAllowed(player, zacPlayer, true))
                return;
            recordRotation(getBuffer(player, true), player);
            return;
        }

        if (event.getPacketType() != PacketType.USE_ENTITY || !event.isAttack())
            return;
        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        CheckSetting setting = getCheckSetting();
        Buffer buffer = getBuffer(player, true);
        long now = System.currentTimeMillis();

        if (setting.autoClickerCIgnoreLowConnectionStability &&
                ConnectionStabilityListener.getConnectionStability(player) == ConnectionStability.LOW) {
            soften(buffer, now, setting, 2);
            return;
        }

        if (now - buffer.getLong("recentCombatAt") > RECENT_COMBAT_WINDOW_MS)
            return;

        if (!buffer.isExists("lastCombatClickAt")) {
            buffer.put("lastCombatClickAt", now);
            return;
        }

        long interval = now - buffer.getLong("lastCombatClickAt");
        buffer.put("lastCombatClickAt", now);

        if (interval < setting.autoClickerCMinInterval || interval > setting.autoClickerCMaxInterval) {
            buffer.put("contamination", Math.min(setting.autoClickerCMainWindowSize, buffer.getInt("contamination") + 1));
            soften(buffer, now, setting, 1);
            return;
        }

        double aimPressure = collectAimPressure(buffer, now);
        recordSample(buffer, "short", setting.autoClickerCShortWindowSize, interval, aimPressure);
        recordSample(buffer, "main", setting.autoClickerCMainWindowSize, interval, aimPressure);

        int mainSize = buffer.getInt("mainSize");
        if (mainSize < setting.autoClickerCMinSamplesForMainWindow)
            return;

        WindowStats mainStats = analyzeWindow(buffer, "main", mainSize, setting);
        if (mainStats == null) {
            soften(buffer, now, setting, 1);
            return;
        }

        WindowStats shortStats = null;
        int shortSize = buffer.getInt("shortSize");
        if (shortSize >= Math.max(8, setting.autoClickerCShortWindowSize / 2))
            shortStats = analyzeWindow(buffer, "short", shortSize, setting);

        recordSession(buffer, mainStats, setting.autoClickerCSessionWindowSize);
        SessionStats sessionStats = analyzeSession(buffer);

        double rawScore = scoreWindow(mainStats, setting);
        if (shortStats != null && scoreWindow(shortStats, setting) >= setting.autoClickerCWindowScoreThreshold)
            rawScore += SHORT_WINDOW_BONUS;
        if (sessionStats != null && sessionStats.stable)
            rawScore += SESSION_WINDOW_BONUS;

        double confidence = computeConfidence(player, buffer, mainStats, setting);
        double score = rawScore * confidence;

        if (score >= setting.autoClickerCWindowScoreThreshold) {
            buffer.put("abnormalWindows", buffer.getInt("abnormalWindows") + 1);
            buffer.put("vl", Math.min(setting.autoClickerCFlagVl * 2, buffer.getInt("vl") + setting.autoClickerCVlAdd));
            buffer.put("lastWindowAt", now);
        } else {
            soften(buffer, now, setting, setting.autoClickerCVlDecay);
        }

        if (sessionStats != null && sessionStats.stable && buffer.getInt("abnormalWindows") >= 2)
            buffer.put("vl", Math.min(setting.autoClickerCFlagVl * 2, buffer.getInt("vl") + 1));

        if (buffer.getInt("abnormalWindows") < 2 || buffer.getInt("vl") < setting.autoClickerCFlagVl)
            return;
        if (now - buffer.getLong("lastFlagTime") < setting.autoClickerCFlagCooldown)
            return;

        buffer.put("readyToFlag", true);
        buffer.put("readyToFlagAt", now);
    }

    @EventHandler
    public void onAttack(ZACPlayerAttackEvent event) {
        if (!event.isEntityAttackCause())
            return;

        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();
        if (!isCheckAllowed(player, zacPlayer))
            return;

        Buffer buffer = getBuffer(player, true);
        long now = System.currentTimeMillis();
        buffer.put("recentCombatAt", now);

        if (!buffer.getBoolean("readyToFlag"))
            return;
        if (now - buffer.getLong("readyToFlagAt") > READY_TIMEOUT_MS) {
            buffer.put("readyToFlag", false);
            return;
        }

        buffer.put("readyToFlag", false);
        buffer.put("readyToFlagAt", 0L);
        buffer.put("lastFlagTime", now);
        buffer.put("vl", Math.max(0, buffer.getInt("vl") - getCheckSetting().autoClickerCVlDecay));
        callViolationEvent(player, zacPlayer, event.getEvent());
    }

    private static void recordRotation(Buffer buffer, Player player) {
        Location current = player.getLocation();
        if (!buffer.isExists("lastRotationLocation")) {
            buffer.put("lastRotationLocation", current.clone());
            return;
        }

        Location previous = buffer.getLocation("lastRotationLocation");
        buffer.put("lastRotationLocation", current.clone());
        if (previous == null)
            return;

        double yawDelta = Math.abs(getAngleDelta(previous.getYaw(), current.getYaw()));
        double pitchDelta = Math.abs(current.getPitch() - previous.getPitch());
        double pressure = yawDelta + pitchDelta * ROTATION_WEIGHT;
        long now = System.currentTimeMillis();

        int index = buffer.getInt("rotationIndex");
        buffer.put("rotationTime_" + index, now);
        buffer.put("rotationPressure_" + index, pressure);
        buffer.put("rotationIndex", (index + 1) % ROTATION_BUFFER_SIZE);
        buffer.put("rotationSize", Math.min(buffer.getInt("rotationSize") + 1, ROTATION_BUFFER_SIZE));
    }

    private static double collectAimPressure(Buffer buffer, long now) {
        int size = buffer.getInt("rotationSize");
        if (size <= 0)
            return 0.0;

        double sum = 0.0;
        for (int i = 0; i < size; i++) {
            long time = buffer.getLong("rotationTime_" + i);
            if (time == 0L || now - time > AIM_LOOKBACK_MS)
                continue;
            sum += buffer.getDouble("rotationPressure_" + i);
        }
        return sum;
    }

    private static void recordSample(Buffer buffer, String prefix, int limit, long interval, double aimPressure) {
        int index = buffer.getInt(prefix + "Index");
        buffer.put(prefix + "Interval_" + index, interval);
        buffer.put(prefix + "Aim_" + index, aimPressure);
        buffer.put(prefix + "Index", (index + 1) % limit);
        buffer.put(prefix + "Size", Math.min(buffer.getInt(prefix + "Size") + 1, limit));
    }

    private static WindowStats analyzeWindow(Buffer buffer, String prefix, int size, CheckSetting setting) {
        List<Long> intervals = new ArrayList<>(size);
        List<Double> aimPressures = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            long interval = buffer.getLong(prefix + "Interval_" + i);
            if (interval <= 0L)
                continue;
            intervals.add(interval);
            aimPressures.add(buffer.getDouble(prefix + "Aim_" + i));
        }

        if (intervals.size() < 8)
            return null;

        double mean = MathUtil.getAverage(intervals);
        if (mean <= 0.0)
            return null;

        double std = MathUtil.getStandardDeviation(intervals);
        List<Double> intervalDoubles = new ArrayList<>(intervals.size());
        for (Long interval : intervals)
            intervalDoubles.add(interval.doubleValue());

        double median = getMedian(intervalDoubles);
        if (median <= 0.0)
            return null;
        double mad = getMedianAbsoluteDeviation(intervalDoubles, median);
        double cv = std / mean;
        double madRatio = mad / median;
        double entropy = getEntropy(intervals, setting.autoClickerCBucketSizeMs);
        double top3Ratio = getTop3Ratio(intervals, setting.autoClickerCBucketSizeMs);
        double longGapRatio = getLongGapRatio(intervals, median + (2.0 * mad));
        double aimCorrelation = getPearsonCorrelation(intervals, aimPressures, setting.autoClickerCAimMinRotationPressure);
        int activeAimSamples = getActiveAimSamples(aimPressures, setting.autoClickerCAimMinRotationPressure);

        return new WindowStats(mean, cv, madRatio, entropy, top3Ratio, longGapRatio, aimCorrelation, activeAimSamples, intervals.size());
    }

    private static void recordSession(Buffer buffer, WindowStats stats, int limit) {
        int index = buffer.getInt("sessionIndex");
        buffer.put("sessionMean_" + index, stats.mean);
        buffer.put("sessionCv_" + index, stats.cv);
        buffer.put("sessionIndex", (index + 1) % limit);
        buffer.put("sessionSize", Math.min(buffer.getInt("sessionSize") + 1, limit));
    }

    private static SessionStats analyzeSession(Buffer buffer) {
        int size = buffer.getInt("sessionSize");
        if (size < 4)
            return null;

        List<Double> means = new ArrayList<>(size);
        List<Double> cvs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            double mean = buffer.getDouble("sessionMean_" + i);
            double cv = buffer.getDouble("sessionCv_" + i);
            if (mean <= 0.0)
                continue;
            means.add(mean);
            cvs.add(cv);
        }

        if (means.size() < 4 || cvs.size() < 4)
            return null;

        double meanStd = MathUtil.getStandardDeviation(means);
        double cvStd = MathUtil.getStandardDeviation(cvs);
        return new SessionStats(meanStd <= 4.0 && cvStd <= 0.025);
    }

    private static double scoreWindow(WindowStats stats, CheckSetting setting) {
        double score = 0.0;
        if (stats.cv <= setting.autoClickerCCvThreshold)
            score += 0.25;
        if (stats.madRatio <= setting.autoClickerCMadRatioThreshold)
            score += 0.20;
        if (stats.entropy <= setting.autoClickerCEntropyThreshold)
            score += 0.20;
        if (stats.top3Ratio >= setting.autoClickerCTop3RatioThreshold)
            score += 0.15;
        if (stats.longGapRatio <= setting.autoClickerCLongGapRatioThreshold)
            score += 0.10;
        if (!Double.isNaN(stats.aimCorrelation) && stats.activeAimSamples >= Math.max(6, stats.sampleSize / 5) &&
                Math.abs(stats.aimCorrelation) <= setting.autoClickerCAimCorrelationThreshold)
            score += 0.10;
        return score;
    }

    private static double computeConfidence(Player player, Buffer buffer, WindowStats stats, CheckSetting setting) {
        double confidence = 1.0;
        ConnectionStability stability = ConnectionStabilityListener.getConnectionStability(player);
        if (stability == ConnectionStability.MEDIUM)
            confidence *= 0.9;
        else if (stability == ConnectionStability.LOW)
            confidence *= 0.6;

        if (stats.activeAimSamples < Math.max(6, stats.sampleSize / 5))
            confidence *= 0.85;

        int contamination = Math.min(buffer.getInt("contamination"), setting.autoClickerCMainWindowSize);
        if (contamination > 0) {
            double contaminationRatio = contamination / (double) Math.max(1, setting.autoClickerCMainWindowSize);
            confidence *= Math.max(0.55, 1.0 - contaminationRatio);
            buffer.put("contamination", Math.max(0, contamination - 1));
        }

        return Math.max(0.4, confidence);
    }

    private static void soften(Buffer buffer, long now, CheckSetting setting, int decay) {
        if (now - buffer.getLong("lastWindowAt") > 4000L)
            buffer.put("abnormalWindows", 0);
        buffer.put("vl", Math.max(0, buffer.getInt("vl") - decay));
        if (decay >= setting.autoClickerCVlDecay)
            buffer.put("abnormalWindows", Math.max(0, buffer.getInt("abnormalWindows") - 1));
    }

    private static double getMedian(List<Double> values) {
        if (values.isEmpty())
            return 0.0;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 0)
            return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
        return sorted.get(middle);
    }

    private static double getMedianAbsoluteDeviation(List<Double> values, double median) {
        List<Double> deviations = new ArrayList<>(values.size());
        for (double value : values)
            deviations.add(Math.abs(value - median));
        return getMedian(deviations);
    }

    private static double getEntropy(List<Long> intervals, long bucketSize) {
        Map<Long, Integer> bins = new HashMap<>();
        for (long interval : intervals) {
            long bucket = interval / bucketSize;
            bins.put(bucket, bins.getOrDefault(bucket, 0) + 1);
        }

        double entropy = 0.0;
        for (int count : bins.values()) {
            double probability = count / (double) intervals.size();
            entropy -= probability * (Math.log(probability) / Math.log(2.0));
        }
        return Math.max(MIN_ENTROPY, entropy);
    }

    private static double getTop3Ratio(List<Long> intervals, long bucketSize) {
        Map<Long, Integer> bins = new HashMap<>();
        for (long interval : intervals) {
            long bucket = interval / bucketSize;
            bins.put(bucket, bins.getOrDefault(bucket, 0) + 1);
        }

        int[] counts = new int[bins.size()];
        int index = 0;
        for (int count : bins.values())
            counts[index++] = count;
        Arrays.sort(counts);

        int top = 0;
        for (int i = counts.length - 1; i >= 0 && i >= counts.length - 3; i--)
            top += counts[i];
        return top / (double) intervals.size();
    }

    private static double getLongGapRatio(List<Long> intervals, double boundary) {
        if (boundary <= 0.0)
            return 0.0;
        int count = 0;
        for (long interval : intervals) {
            if (interval > boundary)
                count++;
        }
        return count / (double) intervals.size();
    }

    private static double getPearsonCorrelation(List<Long> intervals, List<Double> aimPressures, double minPressure) {
        int size = Math.min(intervals.size(), aimPressures.size());
        if (size < 6)
            return Double.NaN;

        double meanX = 0.0;
        double meanY = 0.0;
        int count = 0;
        for (int i = 0; i < size; i++) {
            double aim = aimPressures.get(i);
            if (aim < minPressure)
                continue;
            meanX += intervals.get(i);
            meanY += aim;
            count++;
        }

        if (count < 6)
            return Double.NaN;

        meanX /= count;
        meanY /= count;

        double numerator = 0.0;
        double varianceX = 0.0;
        double varianceY = 0.0;
        for (int i = 0; i < size; i++) {
            double aim = aimPressures.get(i);
            if (aim < minPressure)
                continue;
            double x = intervals.get(i) - meanX;
            double y = aim - meanY;
            numerator += x * y;
            varianceX += x * x;
            varianceY += y * y;
        }

        if (varianceX <= 0.0 || varianceY <= 0.0)
            return Double.NaN;
        return numerator / Math.sqrt(varianceX * varianceY);
    }

    private static int getActiveAimSamples(List<Double> aimPressures, double minPressure) {
        int count = 0;
        for (double aimPressure : aimPressures) {
            if (aimPressure >= minPressure)
                count++;
        }
        return count;
    }

    private static float getAngleDelta(float from, float to) {
        return wrapAngle(to - from);
    }

    private static float wrapAngle(float angle) {
        angle %= 360.0F;
        if (angle >= 180.0F)
            angle -= 360.0F;
        if (angle < -180.0F)
            angle += 360.0F;
        return angle;
    }

    private static final class WindowStats {
        private final double mean;
        private final double cv;
        private final double madRatio;
        private final double entropy;
        private final double top3Ratio;
        private final double longGapRatio;
        private final double aimCorrelation;
        private final int activeAimSamples;
        private final int sampleSize;

        private WindowStats(double mean, double cv, double madRatio, double entropy, double top3Ratio,
                            double longGapRatio, double aimCorrelation, int activeAimSamples, int sampleSize) {
            this.mean = mean;
            this.cv = cv;
            this.madRatio = madRatio;
            this.entropy = entropy;
            this.top3Ratio = top3Ratio;
            this.longGapRatio = longGapRatio;
            this.aimCorrelation = aimCorrelation;
            this.activeAimSamples = activeAimSamples;
            this.sampleSize = sampleSize;
        }
    }

    private static final class SessionStats {
        private final boolean stable;

        private SessionStats(boolean stable) {
            this.stable = stable;
        }
    }
}
