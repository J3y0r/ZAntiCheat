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
    private static final long WINDOW_RESET_MS = 5000L;
    private static final double SHORT_WINDOW_BONUS = 0.15;
    private static final double SESSION_WINDOW_BONUS = 0.15;
    private static final double ROTATION_WEIGHT = 0.6;
    private static final double MIN_ENTROPY = 0.0001;
    private static final int MIN_WINDOW_SIZE = 8;
    private static final int MIN_ANALYSIS_STEP = 6;
    private static final int MIN_SESSION_STEP = 12;

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
            recordRotation(getBuffer(player, true), event);
            return;
        }

        if (event.getPacketType() != PacketType.USE_ENTITY || !event.isAttack())
            return;
        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        CheckSetting setting = getCheckSetting();
        Buffer buffer = getBuffer(player, true);
        long now = System.currentTimeMillis();
        int mainWindowSize = getMainWindowSize(setting);
        int shortWindowSize = getShortWindowSize(setting);
        int minMainSamples = getMinMainSamples(setting);
        int sessionWindowSize = getSessionWindowSize(setting);

        if (setting.autoClickerCIgnoreLowConnectionStability &&
                ConnectionStabilityListener.getConnectionStability(player) == ConnectionStability.LOW) {
            resetSamples(buffer);
            soften(buffer, now, setting, setting.autoClickerCVlDecay + 1);
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

        long minInterval = getMinInterval(setting);
        long maxInterval = getMaxInterval(setting);
        if (interval < minInterval || interval > maxInterval) {
            if (interval > maxInterval)
                resetSamples(buffer);
            buffer.put("contamination", Math.min(mainWindowSize, buffer.getInt("contamination") + 1));
            soften(buffer, now, setting, 1);
            return;
        }

        double aimPressure = collectAimPressure(buffer, now);
        recordSample(buffer, "short", shortWindowSize, interval, aimPressure);
        recordSample(buffer, "main", mainWindowSize, interval, aimPressure);
        buffer.put("sampleSequence", buffer.getLong("sampleSequence") + 1L);

        int mainSize = buffer.getInt("mainSize");
        if (mainSize < minMainSamples)
            return;

        long sequence = buffer.getLong("sampleSequence");
        if (sequence - buffer.getLong("lastAnalysisSequence") < getAnalysisStep(setting))
            return;
        buffer.put("lastAnalysisSequence", sequence);

        WindowStats mainStats = analyzeWindow(buffer, "main", mainSize, mainWindowSize, setting);
        if (mainStats == null) {
            soften(buffer, now, setting, 1);
            return;
        }

        WindowStats shortStats = null;
        int shortSize = buffer.getInt("shortSize");
        if (shortSize >= Math.max(MIN_WINDOW_SIZE, shortWindowSize / 2))
            shortStats = analyzeWindow(buffer, "short", shortSize, shortWindowSize, setting);

        if (sequence - buffer.getLong("lastSessionSequence") >= getSessionStep(setting)) {
            recordSession(buffer, mainStats, sessionWindowSize);
            buffer.put("lastSessionSequence", sequence);
        }
        SessionStats sessionStats = analyzeSession(buffer);

        double rawScore = scoreWindow(mainStats, setting);
        if (shortStats != null && scoreWindow(shortStats, setting) >= setting.autoClickerCWindowScoreThreshold)
            rawScore += SHORT_WINDOW_BONUS;
        if (sessionStats != null && sessionStats.stable)
            rawScore += SESSION_WINDOW_BONUS;

        double confidence = computeConfidence(player, buffer, mainStats, setting);
        double score = rawScore * confidence;

        if (score >= setting.autoClickerCWindowScoreThreshold && hasEnoughEvidence(mainStats, setting)) {
            buffer.put("abnormalWindows", Math.min(4, buffer.getInt("abnormalWindows") + 1));
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

    private static void recordRotation(Buffer buffer, ZACAsyncPacketReceiveEvent event) {
        if (!event.hasRotation())
            return;

        float yaw = event.getYaw();
        float pitch = event.getPitch();
        if (!buffer.isExists("lastRotationYaw") || !buffer.isExists("lastRotationPitch")) {
            buffer.put("lastRotationYaw", yaw);
            buffer.put("lastRotationPitch", pitch);
            return;
        }

        float previousYaw = buffer.getFloat("lastRotationYaw");
        float previousPitch = buffer.getFloat("lastRotationPitch");
        buffer.put("lastRotationYaw", yaw);
        buffer.put("lastRotationPitch", pitch);
        double yawDelta = Math.abs(getAngleDelta(previousYaw, yaw));
        double pitchDelta = Math.abs(pitch - previousPitch);
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
        int index = buffer.getInt(prefix + "Index") % limit;
        buffer.put(prefix + "Interval_" + index, interval);
        buffer.put(prefix + "Aim_" + index, aimPressure);
        buffer.put(prefix + "Index", (index + 1) % limit);
        buffer.put(prefix + "Size", Math.min(buffer.getInt(prefix + "Size") + 1, limit));
    }

    private static WindowStats analyzeWindow(Buffer buffer, String prefix, int size, int limit, CheckSetting setting) {
        List<Long> intervals = new ArrayList<>(size);
        List<Double> aimPressures = new ArrayList<>(size);
        int index = buffer.getInt(prefix + "Index") % limit;
        int start = (index - size + limit) % limit;
        for (int i = 0; i < size; i++) {
            int sampleIndex = (start + i) % limit;
            long interval = buffer.getLong(prefix + "Interval_" + sampleIndex);
            if (interval <= 0L)
                continue;
            intervals.add(interval);
            aimPressures.add(buffer.getDouble(prefix + "Aim_" + sampleIndex));
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
        long bucketSize = getBucketSize(setting);
        double entropy = getEntropy(intervals, bucketSize);
        double top3Ratio = getTop3Ratio(intervals, bucketSize);
        double longGapRatio = getLongGapRatio(intervals, median + Math.max(12.0, 3.0 * mad));
        double aimCorrelation = getPearsonCorrelation(intervals, aimPressures, setting.autoClickerCAimMinRotationPressure);
        int activeAimSamples = getActiveAimSamples(aimPressures, setting.autoClickerCAimMinRotationPressure);

        return new WindowStats(mean, cv, madRatio, entropy, top3Ratio, longGapRatio, aimCorrelation, activeAimSamples, intervals.size());
    }

    private static void recordSession(Buffer buffer, WindowStats stats, int limit) {
        int index = buffer.getInt("sessionIndex") % limit;
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

    private static boolean hasEnoughEvidence(WindowStats stats, CheckSetting setting) {
        boolean regularity = stats.cv <= setting.autoClickerCCvThreshold &&
                stats.madRatio <= setting.autoClickerCMadRatioThreshold;
        boolean distribution = stats.entropy <= setting.autoClickerCEntropyThreshold ||
                stats.top3Ratio >= setting.autoClickerCTop3RatioThreshold;
        boolean sustained = stats.longGapRatio <= setting.autoClickerCLongGapRatioThreshold;
        boolean aim = !Double.isNaN(stats.aimCorrelation) &&
                stats.activeAimSamples >= Math.max(6, stats.sampleSize / 5) &&
                Math.abs(stats.aimCorrelation) <= setting.autoClickerCAimCorrelationThreshold;

        int families = 0;
        if (regularity)
            families++;
        if (distribution)
            families++;
        if (sustained)
            families++;
        if (aim)
            families++;

        return families >= 3;
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

        int contamination = Math.min(buffer.getInt("contamination"), getMainWindowSize(setting));
        if (contamination > 0) {
            double contaminationRatio = contamination / (double) getMainWindowSize(setting);
            confidence *= Math.max(0.55, 1.0 - contaminationRatio);
            buffer.put("contamination", Math.max(0, contamination - 1));
        }

        return Math.max(0.4, confidence);
    }

    private static void soften(Buffer buffer, long now, CheckSetting setting, int decay) {
        if (now - buffer.getLong("lastWindowAt") > WINDOW_RESET_MS)
            buffer.put("abnormalWindows", 0);
        buffer.put("vl", Math.max(0, buffer.getInt("vl") - decay));
        if (decay >= setting.autoClickerCVlDecay)
            buffer.put("abnormalWindows", Math.max(0, buffer.getInt("abnormalWindows") - 1));
    }

    private static void resetSamples(Buffer buffer) {
        buffer.put("shortSize", 0);
        buffer.put("shortIndex", 0);
        buffer.put("mainSize", 0);
        buffer.put("mainIndex", 0);
        buffer.put("sessionSize", 0);
        buffer.put("sessionIndex", 0);
        buffer.put("sampleSequence", 0L);
        buffer.put("lastAnalysisSequence", 0L);
        buffer.put("lastSessionSequence", 0L);
    }

    private static int getShortWindowSize(CheckSetting setting) {
        return Math.max(MIN_WINDOW_SIZE, setting.autoClickerCShortWindowSize);
    }

    private static int getMainWindowSize(CheckSetting setting) {
        return Math.max(MIN_WINDOW_SIZE, setting.autoClickerCMainWindowSize);
    }

    private static int getSessionWindowSize(CheckSetting setting) {
        return Math.max(4, setting.autoClickerCSessionWindowSize);
    }

    private static int getMinMainSamples(CheckSetting setting) {
        return Math.max(MIN_WINDOW_SIZE, Math.min(setting.autoClickerCMinSamplesForMainWindow, getMainWindowSize(setting)));
    }

    private static int getAnalysisStep(CheckSetting setting) {
        return Math.max(MIN_ANALYSIS_STEP, getMainWindowSize(setting) / 3);
    }

    private static int getSessionStep(CheckSetting setting) {
        return Math.max(MIN_SESSION_STEP, getMainWindowSize(setting) / 2);
    }

    private static long getBucketSize(CheckSetting setting) {
        return Math.max(1L, setting.autoClickerCBucketSizeMs);
    }

    private static long getMinInterval(CheckSetting setting) {
        return Math.max(1L, setting.autoClickerCMinInterval);
    }

    private static long getMaxInterval(CheckSetting setting) {
        return Math.max(getMinInterval(setting), setting.autoClickerCMaxInterval);
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
