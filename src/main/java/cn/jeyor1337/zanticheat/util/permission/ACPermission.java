package cn.jeyor1337.zanticheat.util.permission;

import org.bukkit.command.CommandSender;

public class ACPermission {

    public static final String CHECKS = "zanticheat.checks";
    public static final String RELOAD = "zanticheat.reload";
    public static final String ALERTS = "zanticheat.alerts";
    public static final String ALERTS_NOTIFY = "zanticheat.alerts.notify";
    public static final String ALERTS_TOGGLE = "zanticheat.alerts.toggle";
    public static final String ALERTS_TELEPORT = "zanticheat.alerts.teleport";
    public static final String CLIENT = "zanticheat.client";
    public static final String TPS = "zanticheat.tps";
    public static final String PING = "zanticheat.ping";
    public static final String CPS = "zanticheat.cps";
    public static final String REPORT = "zanticheat.report";
    public static final String BYPASS = "zanticheat.bypass";
    public static final String ALL = "zanticheat.*";

    public static boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }
}
