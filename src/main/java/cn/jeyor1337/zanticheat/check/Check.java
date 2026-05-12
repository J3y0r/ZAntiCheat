package cn.jeyor1337.zanticheat.check;

import cn.jeyor1337.zanticheat.Main;
import cn.jeyor1337.zanticheat.api.event.LACViolationEvent;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import cn.jeyor1337.zanticheat.util.annotation.SecureAsync;
import cn.jeyor1337.zanticheat.util.config.ConfigManager;
import cn.jeyor1337.zanticheat.util.detection.CheckUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;

public abstract class Check extends CheckUtil {

    private static final Map<Check, CheckSetting> CHECKS = new HashMap<>();
    private static final Map<CheckName, CheckSetting> CHECK_SETTING = new HashMap<>();
    private static final Map<CheckName, Listener> CHECK_LISTENERS = new HashMap<>();

    public Check(CheckName name) {
        CheckSetting checkSetting = ConfigManager.loadCheck(new CheckSetting(name));
        CHECKS.put(this, checkSetting);
        CHECK_SETTING.put(name, checkSetting);
    }

    public static CheckSetting getCheckSetting(Check check) {
        return CHECKS.getOrDefault(check, null);
    }

    public CheckSetting getCheckSetting() {
        return CHECKS.getOrDefault(this, null);
    }

    public static CheckSetting getCheckSetting(CheckName name) {
        return CHECK_SETTING.getOrDefault(name, null);
    }

    protected Buffer getBuffer(Player player) {
        return new Buffer(this, player);
    }

    @SecureAsync
    protected Buffer getBuffer(Player player, boolean async) {
        return new Buffer(this, player, async);
    }

    public static Listener getListener(CheckName name) {
        return CHECK_LISTENERS.getOrDefault(name, null);
    }

    public static void registerListener(CheckName name, Listener listener) {
        CheckSetting checkSetting = getCheckSetting(name);
        if (checkSetting.enabled && ConfigManager.Config.enabled) {
            HandlerList.unregisterAll(listener);
            Bukkit.getServer().getPluginManager().registerEvents(listener, Main.getInstance());
        } else {
            HandlerList.unregisterAll(listener);
        }
        CHECK_LISTENERS.put(name, listener);
    }

    public boolean isCheckAllowed(Player player, ZACPlayer zacPlayer) {
        return isCheckAllowed(getCheckSetting(), player, zacPlayer);
    }

    @SecureAsync
    public boolean isCheckAllowed(Player player, ZACPlayer zacPlayer, boolean async) {
        return isCheckAllowed(getCheckSetting(), player, zacPlayer, async);
    }

    public static void callViolationEvent(CheckSetting checkSetting, Player player, ZACPlayer zacPlayer, Cancellable cancellable) {
        Bukkit.getPluginManager().callEvent(new LACViolationEvent(checkSetting, player, zacPlayer, cancellable));
    }

    public void callViolationEvent(Player player, ZACPlayer zacPlayer, Cancellable cancellable) {
        callViolationEvent(getCheckSetting(), player, zacPlayer, cancellable);
    }

    public void callViolationEventIfRepeat(Player player, ZACPlayer zacPlayer, Cancellable cancellable, Buffer buffer, long duration) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - buffer.getLong("lastMethodFlag") <= duration) {
            callViolationEvent(player, zacPlayer, cancellable);
            if (buffer.isExists("missedMethodFlag") && buffer.getBoolean("missedMethodFlag")) {
                callViolationEvent(player, zacPlayer, cancellable);
                buffer.put("missedMethodFlag", false);
            }
        } else {
            buffer.put("missedMethodFlag", true);
        }
        buffer.put("lastMethodFlag", currentTime);
    }

}
