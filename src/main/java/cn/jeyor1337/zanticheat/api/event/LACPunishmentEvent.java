package cn.jeyor1337.zanticheat.api.event;

import cn.jeyor1337.zanticheat.check.CheckSetting;
import cn.jeyor1337.zanticheat.player.ZACPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

public class LACPunishmentEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private String checkName;
    private Player player;
    private final CheckSetting CHECK_SETTING;
    private final ZACPlayer ZAC_PLAYER;
    private final Cancellable CANCELLABLE;
    private boolean cancelled;

    public LACPunishmentEvent(CheckSetting checkSetting, Player player, ZACPlayer zacPlayer, @Nullable Cancellable cancellable) {
        this.checkName = checkSetting.name.name().toLowerCase();
        this.player = player;
        this.CHECK_SETTING = checkSetting;
        this.ZAC_PLAYER = zacPlayer;
        this.CANCELLABLE = cancellable;
    }

    public LACPunishmentEvent(LACViolationEvent event) {
        this.checkName = event.getCheckName();
        this.player = event.getPlayer();
        this.CHECK_SETTING = event.getCheckSettings();
        this.ZAC_PLAYER = event.getAcPlayer();
        this.CANCELLABLE = event.getCancellable();
    }

    public String getCheckName() {
        return checkName;
    }

    public Player getPlayer() {
        return player;
    }

    public CheckSetting getCheckSettings() {
        return CHECK_SETTING;
    }

    public ZACPlayer getAcPlayer() {
        return ZAC_PLAYER;
    }

    @Nullable
    public Cancellable getCancellable() {
        return CANCELLABLE;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
