package cn.jeyor1337.zanticheat.check.checks.combat.autoclicker;

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

public class AutoClickerD extends CombatCheck implements Listener {
    private static final long RECENT_COMBAT_WINDOW_MS = 3000L;
    private static final long READY_TIMEOUT_MS = 1500L;
    private static final long WINDOW_RESET_MS = 5000L;
    private static final double EPSILON = 0.000001;

    public AutoClickerD() {
        super(CheckName.AUTOCLICKER_D);
    }

    @EventHandler
    public void onAsyncPacket(ZACAsyncPacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.USE_ENTITY || !event.isAttack())
            return;

        Player player = event.getPlayer();
        ZACPlayer zacPlayer = event.getZacPlayer();
        if (!isCheckAllowed(player, zacPlayer, true))
            return;

        CheckSetting setting = getCheckSetting();
        Buffer buffer = getBuffer(player, true);
        long now = System.currentTimeMillis();

        ConnectionStability stability = ConnectionStabilityListener.getConnectionStability(player);
        if (setting.autoClickerDIgnoreLowConnectionStability && stability == ConnectionStability.LOW) {
            resetSamples(buffer);
            decay(buffer, now, setting, setting.autoClickerDVlDecay + 1);
            return;
        }

        if (now - buffer.getLong("recentCombatAt") > RECENT_COMBAT_WINDOW_MS) {
            decay(buffer, now, setting, 1);
            return;
        }

        if (!buffer.isExists("lastAttackAt")) {
            buffer.put("lastAttackAt", now);
            return;
        }

        long interval = now - buffer.getLong("lastAttackAt");
        buffer.put("lastAttackAt", now);

        if (interval < setting.autoClickerDMinInterval || interval > setting.autoClickerDMaxInterval) {
            if (interval > setting.autoClickerDMaxInterval)
                resetSamples(buffer);
            buffer.put("contamination", Math.min(getWindowSize(setting), buffer.getInt("contamination") + 1));
            decay(buffer, now, setting, 1);
            return;
        }

        recordSample(buffer, setting, interval);

        int sampleSize = buffer.getInt("sampleSize");
        if (sampleSize < getMinSamples(setting))
            return;

        long sequence = buffer.getLong("sampleSequence");
        if (sequence - buffer.getLong("lastAnalysisSequence") < getAnalysisStep(setting))
            return;
        buffer.put("lastAnalysisSequence", sequence);

        WindowStats stats = analyzeWindow(buffer, setting);
        if (stats == null) {
            decay(buffer, now, setting, setting.autoClickerDVlDecay);
            return;
        }

        double score = applyConfidence(score(stats, setting), buffer, setting, stability);
        if (score >= setting.autoClickerDScoreThreshold && hasEnoughEvidence(stats, setting)) {
            buffer.put("suspiciousWindows", Math.min(getRequiredWindows(setting) + 2,
                    buffer.getInt("suspiciousWindows") + 1));
            buffer.put("vl", Math.min(setting.autoClickerDFlagVl * 2,
                    buffer.getInt("vl") + setting.autoClickerDVlAdd));
            buffer.put("lastSuspiciousAt", now);
        } else {
            decay(buffer, now, setting, setting.autoClickerDVlDecay);
        }

        if (buffer.getInt("suspiciousWindows") < getRequiredWindows(setting) ||
                buffer.getInt("vl") < setting.autoClickerDFlagVl)
            return;
        if (now - buffer.getLong("lastFlagAt") < setting.autoClickerDFlagCooldown)
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

        CheckSetting setting = getCheckSetting();
        buffer.put("readyToFlag", false);
        buffer.put("readyToFlagAt", 0L);
        buffer.put("lastFlagAt", now);
        buffer.put("vl", Math.max(0, buffer.getInt("vl") - setting.autoClickerDVlDecay));
        callViolationEvent(player, zacPlayer, event.getEvent());
    }

    private static void recordSample(Buffer buffer, CheckSetting setting, long interval) {
        int windowSize = getWindowSize(setting);
        int index = buffer.getInt("sampleIndex") % windowSize;
        buffer.put("sampleInterval_" + index, interval);
        buffer.put("sampleIndex", (index + 1) % windowSize);
        buffer.put("sampleSize", Math.min(buffer.getInt("sampleSize") + 1, windowSize));
        buffer.put("sampleSequence", buffer.getLong("sampleSequence") + 1L);
    }

    private static WindowStats analyzeWindow(Buffer buffer, CheckSetting setting) {
        List<Long> intervals = getIntervals(buffer, setting);
        if (intervals.size() < getMinSamples(setting))
            return null;

        double mean = getAverage(intervals);
        if (mean <= 0.0 || mean > setting.autoClickerDMaxAverageInterval)
            return null;

        double std = getStandardDeviation(intervals, mean);
        double cv = std / mean;

        List<Double> values = new ArrayList<>(intervals.size());
        for (Long interval : intervals)
            values.add(interval.doubleValue());

        double median = getMedian(values);
        if (median <= 0.0)
            return null;

        double mad = getMedianAbsoluteDeviation(values, median);
        double madRatio = mad / median;
        List<Long> buckets = getBuckets(intervals, getBucketSize(setting));
        BucketStats bucketStats = analyzeBuckets(buckets);
        double longGapRatio = getLongGapRatio(intervals, median, mad);
        double outlierRatio = getOutlierRatio(intervals, median, mad);
        double serialRepeatRatio = getSerialRepeatRatio(buckets);
        double twoStepRepeatRatio = getTwoStepRepeatRatio(buckets);
        double stableDeltaRatio = getStableDeltaRatio(intervals, setting.autoClickerDStableDelta);
        double patternRatio = Math.max(Math.max(serialRepeatRatio, twoStepRepeatRatio), stableDeltaRatio);
        double sliceDeviation = getSliceDeviation(intervals, mean);

        return new WindowStats(mean, cv, madRatio, bucketStats.entropy, bucketStats.top3Ratio,
                longGapRatio, outlierRatio, patternRatio, sliceDeviation);
    }

    private static List<Long> getIntervals(Buffer buffer, CheckSetting setting) {
        int windowSize = getWindowSize(setting);
        int size = Math.min(buffer.getInt("sampleSize"), windowSize);
        int index = buffer.getInt("sampleIndex") % windowSize;
        int start = (index - size + windowSize) % windowSize;
        List<Long> intervals = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            int sampleIndex = (start + i) % windowSize;
            long interval = buffer.getLong("sampleInterval_" + sampleIndex);
            if (interval > 0L)
                intervals.add(interval);
        }
        return intervals;
    }

    private static double score(WindowStats stats, CheckSetting setting) {
        double score = 0.0;
        if (stats.cv <= setting.autoClickerDCvThreshold * 0.75)
            score += 0.22;
        else if (stats.cv <= setting.autoClickerDCvThreshold)
            score += 0.16;

        if (stats.madRatio <= setting.autoClickerDMadRatioThreshold * 0.75)
            score += 0.18;
        else if (stats.madRatio <= setting.autoClickerDMadRatioThreshold)
            score += 0.12;

        if (stats.entropy <= setting.autoClickerDEntropyThreshold)
            score += 0.16;
        if (stats.top3Ratio >= setting.autoClickerDTop3RatioThreshold)
            score += 0.14;
        if (stats.patternRatio >= setting.autoClickerDPatternRatioThreshold)
            score += 0.16;
        if (stats.sliceDeviation <= setting.autoClickerDStableSliceThreshold)
            score += 0.14;
        if (stats.longGapRatio <= setting.autoClickerDLongGapRatioThreshold)
            score += 0.08;
        if (stats.outlierRatio <= setting.autoClickerDLongGapRatioThreshold)
            score += 0.05;
        if (isBoundedRandom(stats, setting))
            score += 0.08;
        return Math.min(1.0, score);
    }

    private static boolean hasEnoughEvidence(WindowStats stats, CheckSetting setting) {
        boolean regularity = stats.cv <= setting.autoClickerDCvThreshold &&
                stats.madRatio <= setting.autoClickerDMadRatioThreshold;
        boolean distribution = stats.entropy <= setting.autoClickerDEntropyThreshold ||
                stats.top3Ratio >= setting.autoClickerDTop3RatioThreshold;
        boolean pattern = stats.patternRatio >= setting.autoClickerDPatternRatioThreshold;
        boolean sustained = stats.sliceDeviation <= setting.autoClickerDStableSliceThreshold &&
                stats.longGapRatio <= setting.autoClickerDLongGapRatioThreshold;
        int families = 0;
        if (regularity)
            families++;
        if (distribution)
            families++;
        if (pattern)
            families++;
        if (sustained)
            families++;

        return families >= 3 || regularity && sustained && isBoundedRandom(stats, setting);
    }

    private static boolean isBoundedRandom(WindowStats stats, CheckSetting setting) {
        return stats.cv <= setting.autoClickerDCvThreshold * 1.25 &&
                stats.madRatio <= setting.autoClickerDMadRatioThreshold * 1.35 &&
                stats.outlierRatio <= setting.autoClickerDLongGapRatioThreshold &&
                stats.sliceDeviation <= setting.autoClickerDStableSliceThreshold * 0.75;
    }

    private static double applyConfidence(double score, Buffer buffer, CheckSetting setting, ConnectionStability stability) {
        double confidence = 1.0;
        if (stability == ConnectionStability.MEDIUM)
            confidence *= 0.9;

        int contamination = Math.min(buffer.getInt("contamination"), getWindowSize(setting));
        if (contamination > 0) {
            double contaminationRatio = contamination / (double) getWindowSize(setting);
            confidence *= Math.max(0.65, 1.0 - contaminationRatio);
            buffer.put("contamination", Math.max(0, contamination - 1));
        }

        return score * confidence;
    }

    private static void decay(Buffer buffer, long now, CheckSetting setting, int amount) {
        if (now - buffer.getLong("lastSuspiciousAt") > WINDOW_RESET_MS)
            buffer.put("suspiciousWindows", 0);
        buffer.put("vl", Math.max(0, buffer.getInt("vl") - amount));
        if (amount >= setting.autoClickerDVlDecay)
            buffer.put("suspiciousWindows", Math.max(0, buffer.getInt("suspiciousWindows") - 1));
    }

    private static void resetSamples(Buffer buffer) {
        buffer.put("sampleSize", 0);
        buffer.put("sampleIndex", 0);
        buffer.put("sampleSequence", 0L);
        buffer.put("lastAnalysisSequence", 0L);
    }

    private static BucketStats analyzeBuckets(List<Long> buckets) {
        Map<Long, Integer> counts = new HashMap<>();
        for (Long bucket : buckets)
            counts.put(bucket, counts.getOrDefault(bucket, 0) + 1);

        double entropy = 0.0;
        for (Integer count : counts.values()) {
            double probability = count / (double) buckets.size();
            entropy -= probability * (Math.log(probability) / Math.log(2.0));
        }

        int[] sorted = new int[counts.size()];
        int index = 0;
        for (Integer count : counts.values())
            sorted[index++] = count;
        Arrays.sort(sorted);

        int top = 0;
        for (int i = sorted.length - 1; i >= 0 && i >= sorted.length - 3; i--)
            top += sorted[i];

        return new BucketStats(Math.max(EPSILON, entropy), top / (double) buckets.size());
    }

    private static List<Long> getBuckets(List<Long> intervals, long bucketSize) {
        List<Long> buckets = new ArrayList<>(intervals.size());
        for (Long interval : intervals)
            buckets.add(Math.round(interval / (double) bucketSize));
        return buckets;
    }

    private static double getLongGapRatio(List<Long> intervals, double median, double mad) {
        double boundary = median + Math.max(12.0, mad * 3.0);
        int count = 0;
        for (Long interval : intervals) {
            if (interval > boundary)
                count++;
        }
        return count / (double) intervals.size();
    }

    private static double getOutlierRatio(List<Long> intervals, double median, double mad) {
        double boundary = Math.max(10.0, mad * 3.0);
        int count = 0;
        for (Long interval : intervals) {
            if (Math.abs(interval - median) > boundary)
                count++;
        }
        return count / (double) intervals.size();
    }

    private static double getSerialRepeatRatio(List<Long> buckets) {
        if (buckets.size() < 2)
            return 0.0;
        int count = 0;
        for (int i = 1; i < buckets.size(); i++) {
            if (buckets.get(i).equals(buckets.get(i - 1)))
                count++;
        }
        return count / (double) (buckets.size() - 1);
    }

    private static double getTwoStepRepeatRatio(List<Long> buckets) {
        if (buckets.size() < 3)
            return 0.0;
        int count = 0;
        for (int i = 2; i < buckets.size(); i++) {
            if (buckets.get(i).equals(buckets.get(i - 2)))
                count++;
        }
        return count / (double) (buckets.size() - 2);
    }

    private static double getStableDeltaRatio(List<Long> intervals, long stableDelta) {
        if (intervals.size() < 2)
            return 0.0;
        int count = 0;
        for (int i = 1; i < intervals.size(); i++) {
            if (Math.abs(intervals.get(i) - intervals.get(i - 1)) <= stableDelta)
                count++;
        }
        return count / (double) (intervals.size() - 1);
    }

    private static double getSliceDeviation(List<Long> intervals, double globalMean) {
        int sliceCount = 4;
        int sliceSize = intervals.size() / sliceCount;
        if (sliceSize < 8)
            return 1.0;

        List<Double> means = new ArrayList<>(sliceCount);
        for (int i = 0; i < sliceCount; i++) {
            int from = i * sliceSize;
            int to = i == sliceCount - 1 ? intervals.size() : from + sliceSize;
            means.add(getAverage(intervals.subList(from, to)));
        }

        return getStandardDeviationDouble(means, getAverageDouble(means)) / Math.max(EPSILON, globalMean);
    }

    private static double getAverage(List<Long> values) {
        double total = 0.0;
        for (Long value : values)
            total += value;
        return total / values.size();
    }

    private static double getAverageDouble(List<Double> values) {
        double total = 0.0;
        for (Double value : values)
            total += value;
        return total / values.size();
    }

    private static double getStandardDeviation(List<Long> values, double average) {
        double total = 0.0;
        for (Long value : values) {
            double offset = value - average;
            total += offset * offset;
        }
        return Math.sqrt(total / values.size());
    }

    private static double getStandardDeviationDouble(List<Double> values, double average) {
        double total = 0.0;
        for (Double value : values) {
            double offset = value - average;
            total += offset * offset;
        }
        return Math.sqrt(total / values.size());
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
        for (Double value : values)
            deviations.add(Math.abs(value - median));
        return getMedian(deviations);
    }

    private static int getWindowSize(CheckSetting setting) {
        return Math.max(16, setting.autoClickerDWindowSize);
    }

    private static int getMinSamples(CheckSetting setting) {
        return Math.max(16, Math.min(setting.autoClickerDMinSamples, getWindowSize(setting)));
    }

    private static int getAnalysisStep(CheckSetting setting) {
        return Math.max(1, setting.autoClickerDAnalysisStep);
    }

    private static int getRequiredWindows(CheckSetting setting) {
        return Math.max(1, setting.autoClickerDRequiredWindows);
    }

    private static long getBucketSize(CheckSetting setting) {
        return Math.max(1L, setting.autoClickerDBucketSizeMs);
    }

    private static final class WindowStats {
        private final double mean;
        private final double cv;
        private final double madRatio;
        private final double entropy;
        private final double top3Ratio;
        private final double longGapRatio;
        private final double outlierRatio;
        private final double patternRatio;
        private final double sliceDeviation;

        private WindowStats(double mean, double cv, double madRatio, double entropy, double top3Ratio,
                            double longGapRatio, double outlierRatio, double patternRatio,
                            double sliceDeviation) {
            this.mean = mean;
            this.cv = cv;
            this.madRatio = madRatio;
            this.entropy = entropy;
            this.top3Ratio = top3Ratio;
            this.longGapRatio = longGapRatio;
            this.outlierRatio = outlierRatio;
            this.patternRatio = patternRatio;
            this.sliceDeviation = sliceDeviation;
        }
    }

    private static final class BucketStats {
        private final double entropy;
        private final double top3Ratio;

        private BucketStats(double entropy, double top3Ratio) {
            this.entropy = entropy;
            this.top3Ratio = top3Ratio;
        }
    }
}
