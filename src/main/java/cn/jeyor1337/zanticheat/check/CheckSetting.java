package cn.jeyor1337.zanticheat.check;

import java.util.List;

public class CheckSetting {

    public CheckSetting(CheckName name) {
        this.name = name;
    }

    public CheckName name;
    public boolean enabled;
    public boolean punishable;
    public int punishmentVio;
    public double minTps;
    public int maxPing;
    public boolean detectJava;
    public boolean detectBedrock;
    public boolean setback;
    public int setbackVio;
    public List<String> punishmentCommands;
    public long killAuraECombatWindow;
    public long killAuraEDecayTime;
    public long killAuraEFlagCooldown;
    public long killAuraEAttributeGracePeriod;
    public double killAuraELargeYawStep;
    public double killAuraEMinRepeatStep;
    public double killAuraERepeatStepEpsilon;
    public double killAuraELowError;
    public double killAuraEVeryLowError;
    public double killAuraETurnForLowError;
    public double killAuraEVerticalOffsetForPitchCheck;
    public double killAuraEStaticPitchEpsilon;
    public int killAuraEViolationLevel;
    public int killAuraEViolationCap;
    public double killAuraEConfirmedOffsetMultiplier;
    public int fakeLagAPreSamples;
    public int fakeLagAPostWindowPackets;
    public long fakeLagAPostWindowTime;
    public long fakeLagAMinSpikeDelta;
    public double fakeLagAMinSpikeRatio;
    public double fakeLagAMaxPreAvgDelta;
    public double fakeLagAMaxPreAbsDev;
    public int fakeLagAFlagsRequired;
    public long fakeLagAReadyTimeout;
    public boolean fakeLagAIgnoreLowConnectionStability;
    public int autoClickerCShortWindowSize;
    public int autoClickerCMainWindowSize;
    public int autoClickerCSessionWindowSize;
    public long autoClickerCMinInterval;
    public long autoClickerCMaxInterval;
    public int autoClickerCMinSamplesForMainWindow;
    public long autoClickerCBucketSizeMs;
    public double autoClickerCCvThreshold;
    public double autoClickerCMadRatioThreshold;
    public double autoClickerCEntropyThreshold;
    public double autoClickerCTop3RatioThreshold;
    public double autoClickerCLongGapRatioThreshold;
    public double autoClickerCAimCorrelationThreshold;
    public double autoClickerCAimMinRotationPressure;
    public double autoClickerCWindowScoreThreshold;
    public int autoClickerCVlAdd;
    public int autoClickerCVlDecay;
    public int autoClickerCFlagVl;
    public long autoClickerCFlagCooldown;
    public boolean autoClickerCIgnoreLowConnectionStability;
    public int autoClickerDWindowSize;
    public int autoClickerDMinSamples;
    public int autoClickerDAnalysisStep;
    public long autoClickerDMinInterval;
    public long autoClickerDMaxInterval;
    public long autoClickerDMaxAverageInterval;
    public long autoClickerDBucketSizeMs;
    public long autoClickerDStableDelta;
    public double autoClickerDCvThreshold;
    public double autoClickerDMadRatioThreshold;
    public double autoClickerDEntropyThreshold;
    public double autoClickerDTop3RatioThreshold;
    public double autoClickerDPatternRatioThreshold;
    public double autoClickerDStableSliceThreshold;
    public double autoClickerDLongGapRatioThreshold;
    public double autoClickerDScoreThreshold;
    public int autoClickerDRequiredWindows;
    public int autoClickerDVlAdd;
    public int autoClickerDVlDecay;
    public int autoClickerDFlagVl;
    public long autoClickerDFlagCooldown;
    public boolean autoClickerDIgnoreLowConnectionStability;

}
