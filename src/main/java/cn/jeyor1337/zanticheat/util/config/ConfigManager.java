package cn.jeyor1337.zanticheat.util.config;

import com.tchristofferson.configupdater.ConfigUpdater;
import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.check.Check;
import cn.jeyor1337.zanticheat.check.CheckName;
import cn.jeyor1337.zanticheat.check.CheckSetting;
import cn.jeyor1337.zanticheat.util.config.placeholder.PlaceholderConvertor;
import cn.jeyor1337.zanticheat.util.hook.plugin.FloodgateHook;
import cn.jeyor1337.zanticheat.util.logger.LogType;
import cn.jeyor1337.zanticheat.util.logger.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

public class ConfigManager extends PlaceholderConvertor {

    public static class Config {
        public static boolean enabled;
        public static boolean silentMode;

        public static class Messages {
            public static String prefix;
            public static boolean hexColorCodes;

            public static class CommandMessages {
                public static class Help {
                    public static String message;
                }

                public static class Reload {
                    public static String message;
                }

                public static class Alerts {
                    public static String toggledOnMessage;
                    public static String toggledOffMessage;
                }

                public static class Tps {
                    public static String message;
                }

                public static class Ping {
                    public static String message;

                    public static class ConnectionStability {
                        public static String high;
                        public static String medium;
                        public static String low;
                    }
                }

                public static class Client {
                    public static String message;
                }

                public static class Cps {
                    public static String message;
                }

                public static class Checks {
                    public static String message;
                }

                public static class Report {
                    public static String message;
                }
            }

            public static class ErrorMessages {
                public static String noPermission;
                public static String invalidFormat;
            }

        }

        public static class Alerts {
            public static class BroadcastViolations {
                public static boolean enabled;
                public static String message;
                public static int cooldown;
                public static String onHover;
                public static String onClick;
            }

            public static class BroadcastPunishments {
                public static boolean enabled;
                public static String message;
                public static int cooldown;
                public static String onHover;
                public static String onClick;
            }

        }

        public static class Log {
            public static boolean enabled;
            public static String file;

            public static class LogViolations {
                public static boolean enabled;
                public static String message;
                public static int cooldown;
            }

            public static class LogPunishments {
                public static boolean enabled;
                public static String message;
                public static int cooldown;
            }
        }

        public static class DiscordWebhook {
            public static boolean enabled;

            public static class SendViolations {
                public static boolean enabled;
                public static String webhookUrl;
                public static String message;
                public static int cooldown;
            }

            public static class SendPunishments {
                public static boolean enabled;
                public static String webhookUrl;
                public static String message;
                public static int cooldown;
            }
        }

        public static class Permission {
            public static boolean perCheckBypassPermission;
            public static boolean disableAllBypassPermissions;
        }

        public static class Violation {
            public static class Reset {
                public static int resetInterval;
            }

            public static class Cache {
                public static boolean enabled;
                public static int cacheDuration;
            }
        }

        public static class LagProtection {
            public static int tickThreshold;
            public static int ignoreTimeOnJoin;
            public static int ignoreTimeOnTeleport;
            public static boolean preventEnteringIntoUnloadedChucks;
            public static boolean prioritizeAccuracy;
        }

        public static class GeyserHook {
            public static boolean enabled;

            public static class Floodgate {
                public static boolean enabled;
            }

            public static class UUID {
                public static boolean enabled;
            }

            public static class Prefix {
                public static boolean enabled;
                public static String prefixString;
            }
        }

        public static class Database {
            public static boolean enabled;

            public static class Mysql {
                public static String host;
                public static int port;
                public static String database;
                public static String username;
                public static String password;

                public static class Advanced {
                    public static boolean ssl;
                    public static boolean useUnicode;
                    public static boolean verifyServerCertificate;
                    public static String characterEncoding;
                    public static boolean autoReconnect;
                    public static int maxReconnects;
                }

                public static class Init {
                    public static boolean createTables;
                }
            }
        }

        public static class VelocitySupport {
            public static boolean enabled;
            public static String serverId;
            public static boolean mainServer;
            public static String syncMode;
            public static String mainServerBanCommand;

            public static class MessageBridge {
                public static String type;
                public static String channel;
                public static String host;
                public static int port;
                public static String password;
                public static int database;
                public static int connectTimeoutMs;
                public static int publishTimeoutMs;
            }
        }

        public static class UpdateChecker {
            public static boolean enabled;

            public static class Notification {
                public static class Console {
                    public static boolean enabled;
                    public static String message;
                }

                public static class OnJoin {
                    public static boolean enabled;
                    public static String message;
                    public static boolean requirePermission;
                }
            }
        }

        public static class Bstats {
            public static boolean enabled;
        }

        public static class Api {
            public static boolean enabled;
        }
    }

    public static void loadConfig() {
        Main instance = Main.getInstance();
        instance.saveDefaultConfig();
        FloodgateHook.loadFloodgateHook();

        try {
            ConfigUpdater.update(instance, "config.yml",
                    new File(instance.getDataFolder(), "config.yml"), Collections.emptyList());
        } catch (IOException e) {
            Logger.logConsole(LogType.ERROR, "(" + instance.getName() + ") config.yml is invalid! " +
                    "Something went wrong while updating the file! ");
        }

        FileConfiguration config = instance.getConfig();
        loadConfig(Config.class, Config.class, config);
    }

    private static void loadConfig(Class<?> aClass, final Class<?> configClass, final FileConfiguration config) {
        for (Class<?> innerClass : aClass.getClasses()) {
            loadConfig(innerClass, configClass, config);
        }
        for (Field field : aClass.getDeclaredFields()) {
            for (Method method : config.getClass().getMethods()) {
                if (!method.getName().startsWith("get"))
                    continue;
                if (!method.getReturnType().getName().equals(field.getType().getName()))
                    continue;
                if (method.getParameterCount() != 1)
                    continue;
                try {
                    String path = aClass.getName().replaceAll("\\$", ".") + "." + field.getName();
                    path = path.replace(configClass.getName().replace("$", "."), "").substring(1);
                    for (int i = path.length() - 1; i >= 0; i--) {
                        char c = path.charAt(i);
                        if (!Character.isUpperCase(c))
                            continue;
                        if (i == 0 || path.charAt(i - 1) == '.')
                            path = path.substring(0, i) + Character.toLowerCase(c) + path.substring(i + 1);
                        else
                            path = path.substring(0, i) + "-" + Character.toLowerCase(c) + path.substring(i + 1);
                    }
                    field.set(aClass, method.invoke(config, path));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") config.yml is invalid! " +
                            "Something went wrong while loading the file!");
                }
                break;
            }
        }
    }

    public static void reloadConfig() {
        Main instance = Main.getInstance();
        instance.reloadConfig();
        loadConfig();
        for (CheckName checkName : CheckName.values()) {
            CheckSetting checkSetting = Check.getCheckSetting(checkName);
            if (checkSetting == null) {
                Logger.logConsole(LogType.ERROR, "(" + instance.getName() + ") config.yml is invalid! " +
                        "Something went wrong while loading " + checkName.title + " settings!");
                continue;
            }
            loadCheck(checkSetting);
            Check.registerListener(checkName, Check.getListener(checkName));
        }
        Logger.logFile("");
    }

    public static CheckSetting loadCheck(CheckSetting checkSetting) {
        CheckName checkName = checkSetting.name;
        ConfigurationSection section = Main.getInstance().getConfig()
                .getConfigurationSection("checks" +
                        "." + checkName.type.name().toLowerCase() +
                        "." + checkName.group.toLowerCase() +
                        "." + checkName.group.toLowerCase() + "_" + checkName.check.toString().toLowerCase());
        if (section == null || section.getKeys(false).size() == 0) {
            Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") config.yml is invalid! " +
                    "The config selection of " + checkName.title + " check is invalid!");
            return checkSetting;
        }
        checkSetting.enabled = section.getBoolean("enabled");
        checkSetting.punishable = section.getBoolean("punishment.punishable");
        checkSetting.punishmentVio = section.getInt("punishment.punishment-vio");
        checkSetting.minTps = section.getDouble("detection.min-tps");
        checkSetting.maxPing = section.getInt("detection.max-ping");
        checkSetting.detectJava = section.getBoolean("detection.java");
        checkSetting.detectBedrock = section.getBoolean("detection.bedrock");
        checkSetting.setback = section.getBoolean("setback.setback");
        checkSetting.setbackVio = section.getInt("setback.setback-vio");
        checkSetting.punishmentCommands = section.getStringList("punishment.commands");

        checkSetting.killAuraECombatWindow = 3500L;
        checkSetting.killAuraEDecayTime = 3000L;
        checkSetting.killAuraEFlagCooldown = 750L;
        checkSetting.killAuraEAttributeGracePeriod = 3500L;
        checkSetting.killAuraELargeYawStep = 28.0;
        checkSetting.killAuraEMinRepeatStep = 3.5;
        checkSetting.killAuraERepeatStepEpsilon = 0.10;
        checkSetting.killAuraELowError = 1.35;
        checkSetting.killAuraEVeryLowError = 0.85;
        checkSetting.killAuraETurnForLowError = 10.0;
        checkSetting.killAuraEVerticalOffsetForPitchCheck = 0.35;
        checkSetting.killAuraEStaticPitchEpsilon = 0.20;
        checkSetting.killAuraEViolationLevel = 6;
        checkSetting.killAuraEViolationCap = 12;
        checkSetting.killAuraEConfirmedOffsetMultiplier = 1.5;

        checkSetting.fakeLagAPreSamples = 8;
        checkSetting.fakeLagAPostWindowPackets = 4;
        checkSetting.fakeLagAPostWindowTime = 300L;
        checkSetting.fakeLagAMinSpikeDelta = 70L;
        checkSetting.fakeLagAMinSpikeRatio = 2.2;
        checkSetting.fakeLagAMaxPreAvgDelta = 110.0;
        checkSetting.fakeLagAMaxPreAbsDev = 30.0;
        checkSetting.fakeLagAFlagsRequired = 2;
        checkSetting.fakeLagAReadyTimeout = 1500L;
        checkSetting.fakeLagAIgnoreLowConnectionStability = true;

        checkSetting.autoClickerCShortWindowSize = 16;
        checkSetting.autoClickerCMainWindowSize = 36;
        checkSetting.autoClickerCSessionWindowSize = 6;
        checkSetting.autoClickerCMinInterval = 15L;
        checkSetting.autoClickerCMaxInterval = 300L;
        checkSetting.autoClickerCMinSamplesForMainWindow = 28;
        checkSetting.autoClickerCBucketSizeMs = 2L;
        checkSetting.autoClickerCCvThreshold = 0.12;
        checkSetting.autoClickerCMadRatioThreshold = 0.08;
        checkSetting.autoClickerCEntropyThreshold = 2.25;
        checkSetting.autoClickerCTop3RatioThreshold = 0.85;
        checkSetting.autoClickerCLongGapRatioThreshold = 0.035;
        checkSetting.autoClickerCAimCorrelationThreshold = 0.075;
        checkSetting.autoClickerCAimMinRotationPressure = 12.0;
        checkSetting.autoClickerCWindowScoreThreshold = 0.55;
        checkSetting.autoClickerCVlAdd = 6;
        checkSetting.autoClickerCVlDecay = 2;
        checkSetting.autoClickerCFlagVl = 12;
        checkSetting.autoClickerCFlagCooldown = 2500L;
        checkSetting.autoClickerCIgnoreLowConnectionStability = true;
        checkSetting.autoClickerDWindowSize = 60;
        checkSetting.autoClickerDMinSamples = 48;
        checkSetting.autoClickerDAnalysisStep = 12;
        checkSetting.autoClickerDMinInterval = 20L;
        checkSetting.autoClickerDMaxInterval = 240L;
        checkSetting.autoClickerDMaxAverageInterval = 175L;
        checkSetting.autoClickerDBucketSizeMs = 2L;
        checkSetting.autoClickerDStableDelta = 4L;
        checkSetting.autoClickerDCvThreshold = 0.16;
        checkSetting.autoClickerDMadRatioThreshold = 0.11;
        checkSetting.autoClickerDEntropyThreshold = 2.75;
        checkSetting.autoClickerDTop3RatioThreshold = 0.72;
        checkSetting.autoClickerDPatternRatioThreshold = 0.42;
        checkSetting.autoClickerDStableSliceThreshold = 0.045;
        checkSetting.autoClickerDLongGapRatioThreshold = 0.04;
        checkSetting.autoClickerDScoreThreshold = 0.68;
        checkSetting.autoClickerDRequiredWindows = 3;
        checkSetting.autoClickerDVlAdd = 5;
        checkSetting.autoClickerDVlDecay = 2;
        checkSetting.autoClickerDFlagVl = 12;
        checkSetting.autoClickerDFlagCooldown = 3500L;
        checkSetting.autoClickerDIgnoreLowConnectionStability = true;

        if (checkName == CheckName.KILLAURA_E) {
            checkSetting.killAuraECombatWindow = section.getLong("detection.combat-window", checkSetting.killAuraECombatWindow);
            checkSetting.killAuraEDecayTime = section.getLong("detection.decay-time", checkSetting.killAuraEDecayTime);
            checkSetting.killAuraEFlagCooldown = section.getLong("detection.flag-cooldown", checkSetting.killAuraEFlagCooldown);
            checkSetting.killAuraEAttributeGracePeriod = section.getLong("detection.attribute-grace-period", checkSetting.killAuraEAttributeGracePeriod);
            checkSetting.killAuraELargeYawStep = section.getDouble("detection.large-yaw-step", checkSetting.killAuraELargeYawStep);
            checkSetting.killAuraEMinRepeatStep = section.getDouble("detection.min-repeat-step", checkSetting.killAuraEMinRepeatStep);
            checkSetting.killAuraERepeatStepEpsilon = section.getDouble("detection.repeat-step-epsilon", checkSetting.killAuraERepeatStepEpsilon);
            checkSetting.killAuraELowError = section.getDouble("detection.low-error", checkSetting.killAuraELowError);
            checkSetting.killAuraEVeryLowError = section.getDouble("detection.very-low-error", checkSetting.killAuraEVeryLowError);
            checkSetting.killAuraETurnForLowError = section.getDouble("detection.turn-for-low-error", checkSetting.killAuraETurnForLowError);
            checkSetting.killAuraEVerticalOffsetForPitchCheck = section.getDouble("detection.vertical-offset-for-pitch-check", checkSetting.killAuraEVerticalOffsetForPitchCheck);
            checkSetting.killAuraEStaticPitchEpsilon = section.getDouble("detection.static-pitch-epsilon", checkSetting.killAuraEStaticPitchEpsilon);
            checkSetting.killAuraEViolationLevel = section.getInt("detection.violation-level", checkSetting.killAuraEViolationLevel);
            checkSetting.killAuraEViolationCap = section.getInt("detection.violation-cap", checkSetting.killAuraEViolationCap);
            checkSetting.killAuraEConfirmedOffsetMultiplier = section.getDouble("detection.confirmed-offset-multiplier", checkSetting.killAuraEConfirmedOffsetMultiplier);
        }

        if (checkName == CheckName.FAKELAG_A) {
            checkSetting.fakeLagAPreSamples = section.getInt("detection.pre-samples", checkSetting.fakeLagAPreSamples);
            checkSetting.fakeLagAPostWindowPackets = section.getInt("detection.post-window-packets", checkSetting.fakeLagAPostWindowPackets);
            checkSetting.fakeLagAPostWindowTime = section.getLong("detection.post-window-time", checkSetting.fakeLagAPostWindowTime);
            checkSetting.fakeLagAMinSpikeDelta = section.getLong("detection.min-spike-delta", checkSetting.fakeLagAMinSpikeDelta);
            checkSetting.fakeLagAMinSpikeRatio = section.getDouble("detection.min-spike-ratio", checkSetting.fakeLagAMinSpikeRatio);
            checkSetting.fakeLagAMaxPreAvgDelta = section.getDouble("detection.max-pre-avg-delta", checkSetting.fakeLagAMaxPreAvgDelta);
            checkSetting.fakeLagAMaxPreAbsDev = section.getDouble("detection.max-pre-abs-dev", checkSetting.fakeLagAMaxPreAbsDev);
            checkSetting.fakeLagAFlagsRequired = section.getInt("detection.flags-required", checkSetting.fakeLagAFlagsRequired);
            checkSetting.fakeLagAReadyTimeout = section.getLong("detection.ready-timeout", checkSetting.fakeLagAReadyTimeout);
            checkSetting.fakeLagAIgnoreLowConnectionStability = section.getBoolean("detection.ignore-low-connection-stability", checkSetting.fakeLagAIgnoreLowConnectionStability);
        }

        if (checkName == CheckName.AUTOCLICKER_C) {
            checkSetting.autoClickerCShortWindowSize = section.getInt("detection.short-window-size", checkSetting.autoClickerCShortWindowSize);
            checkSetting.autoClickerCMainWindowSize = section.getInt("detection.main-window-size", checkSetting.autoClickerCMainWindowSize);
            checkSetting.autoClickerCSessionWindowSize = section.getInt("detection.session-window-size", checkSetting.autoClickerCSessionWindowSize);
            checkSetting.autoClickerCMinInterval = section.getLong("detection.min-interval", checkSetting.autoClickerCMinInterval);
            checkSetting.autoClickerCMaxInterval = section.getLong("detection.max-interval", checkSetting.autoClickerCMaxInterval);
            checkSetting.autoClickerCMinSamplesForMainWindow = section.getInt("detection.min-samples-for-main-window", checkSetting.autoClickerCMinSamplesForMainWindow);
            checkSetting.autoClickerCBucketSizeMs = section.getLong("detection.bucket-size-ms", checkSetting.autoClickerCBucketSizeMs);
            checkSetting.autoClickerCCvThreshold = section.getDouble("detection.cv-threshold", checkSetting.autoClickerCCvThreshold);
            checkSetting.autoClickerCMadRatioThreshold = section.getDouble("detection.mad-ratio-threshold", checkSetting.autoClickerCMadRatioThreshold);
            checkSetting.autoClickerCEntropyThreshold = section.getDouble("detection.entropy-threshold", checkSetting.autoClickerCEntropyThreshold);
            checkSetting.autoClickerCTop3RatioThreshold = section.getDouble("detection.top3-ratio-threshold", checkSetting.autoClickerCTop3RatioThreshold);
            checkSetting.autoClickerCLongGapRatioThreshold = section.getDouble("detection.long-gap-ratio-threshold", checkSetting.autoClickerCLongGapRatioThreshold);
            checkSetting.autoClickerCAimCorrelationThreshold = section.getDouble("detection.aim-correlation-threshold", checkSetting.autoClickerCAimCorrelationThreshold);
            checkSetting.autoClickerCAimMinRotationPressure = section.getDouble("detection.aim-min-rotation-pressure", checkSetting.autoClickerCAimMinRotationPressure);
            checkSetting.autoClickerCWindowScoreThreshold = section.getDouble("detection.window-score-threshold", checkSetting.autoClickerCWindowScoreThreshold);
            checkSetting.autoClickerCVlAdd = section.getInt("detection.vl-add", checkSetting.autoClickerCVlAdd);
            checkSetting.autoClickerCVlDecay = section.getInt("detection.vl-decay", checkSetting.autoClickerCVlDecay);
            checkSetting.autoClickerCFlagVl = section.getInt("detection.flag-vl", checkSetting.autoClickerCFlagVl);
            checkSetting.autoClickerCFlagCooldown = section.getLong("detection.flag-cooldown", checkSetting.autoClickerCFlagCooldown);
            checkSetting.autoClickerCIgnoreLowConnectionStability = section.getBoolean("detection.ignore-low-connection-stability", checkSetting.autoClickerCIgnoreLowConnectionStability);
        }

        if (checkName == CheckName.AUTOCLICKER_D) {
            checkSetting.autoClickerDWindowSize = section.getInt("detection.window-size", checkSetting.autoClickerDWindowSize);
            checkSetting.autoClickerDMinSamples = section.getInt("detection.min-samples", checkSetting.autoClickerDMinSamples);
            checkSetting.autoClickerDAnalysisStep = section.getInt("detection.analysis-step", checkSetting.autoClickerDAnalysisStep);
            checkSetting.autoClickerDMinInterval = section.getLong("detection.min-interval", checkSetting.autoClickerDMinInterval);
            checkSetting.autoClickerDMaxInterval = section.getLong("detection.max-interval", checkSetting.autoClickerDMaxInterval);
            checkSetting.autoClickerDMaxAverageInterval = section.getLong("detection.max-average-interval", checkSetting.autoClickerDMaxAverageInterval);
            checkSetting.autoClickerDBucketSizeMs = section.getLong("detection.bucket-size-ms", checkSetting.autoClickerDBucketSizeMs);
            checkSetting.autoClickerDStableDelta = section.getLong("detection.stable-delta", checkSetting.autoClickerDStableDelta);
            checkSetting.autoClickerDCvThreshold = section.getDouble("detection.cv-threshold", checkSetting.autoClickerDCvThreshold);
            checkSetting.autoClickerDMadRatioThreshold = section.getDouble("detection.mad-ratio-threshold", checkSetting.autoClickerDMadRatioThreshold);
            checkSetting.autoClickerDEntropyThreshold = section.getDouble("detection.entropy-threshold", checkSetting.autoClickerDEntropyThreshold);
            checkSetting.autoClickerDTop3RatioThreshold = section.getDouble("detection.top3-ratio-threshold", checkSetting.autoClickerDTop3RatioThreshold);
            checkSetting.autoClickerDPatternRatioThreshold = section.getDouble("detection.pattern-ratio-threshold", checkSetting.autoClickerDPatternRatioThreshold);
            checkSetting.autoClickerDStableSliceThreshold = section.getDouble("detection.stable-slice-threshold", checkSetting.autoClickerDStableSliceThreshold);
            checkSetting.autoClickerDLongGapRatioThreshold = section.getDouble("detection.long-gap-ratio-threshold", checkSetting.autoClickerDLongGapRatioThreshold);
            checkSetting.autoClickerDScoreThreshold = section.getDouble("detection.score-threshold", checkSetting.autoClickerDScoreThreshold);
            checkSetting.autoClickerDRequiredWindows = section.getInt("detection.required-windows", checkSetting.autoClickerDRequiredWindows);
            checkSetting.autoClickerDVlAdd = section.getInt("detection.vl-add", checkSetting.autoClickerDVlAdd);
            checkSetting.autoClickerDVlDecay = section.getInt("detection.vl-decay", checkSetting.autoClickerDVlDecay);
            checkSetting.autoClickerDFlagVl = section.getInt("detection.flag-vl", checkSetting.autoClickerDFlagVl);
            checkSetting.autoClickerDFlagCooldown = section.getLong("detection.flag-cooldown", checkSetting.autoClickerDFlagCooldown);
            checkSetting.autoClickerDIgnoreLowConnectionStability = section.getBoolean("detection.ignore-low-connection-stability", checkSetting.autoClickerDIgnoreLowConnectionStability);
        }

        return checkSetting;
    }

}
