package cn.jeyor1337.zanticheat;

import cn.jeyor1337.zanticheat.check.Check;
import cn.jeyor1337.zanticheat.check.buffer.Buffer;
import cn.jeyor1337.zanticheat.check.checks.combat.autoclicker.AutoClickerA;
import cn.jeyor1337.zanticheat.check.checks.combat.autoclicker.AutoClickerB;
import cn.jeyor1337.zanticheat.check.checks.combat.autoclicker.AutoClickerC;
import cn.jeyor1337.zanticheat.check.checks.combat.autoclicker.AutoClickerD;
import cn.jeyor1337.zanticheat.check.checks.combat.criticals.CriticalsA;
import cn.jeyor1337.zanticheat.check.checks.combat.criticals.CriticalsB;
import cn.jeyor1337.zanticheat.check.checks.combat.fakelag.FakeLagA;
import cn.jeyor1337.zanticheat.check.checks.combat.killaura.*;
import cn.jeyor1337.zanticheat.check.checks.combat.reach.ReachA;
import cn.jeyor1337.zanticheat.check.checks.combat.reach.ReachB;
import cn.jeyor1337.zanticheat.check.checks.combat.velocity.VelocityA;
import cn.jeyor1337.zanticheat.check.checks.interaction.airplace.AirPlaceA;
import cn.jeyor1337.zanticheat.check.checks.interaction.blockbreak.BlockBreakA;
import cn.jeyor1337.zanticheat.check.checks.interaction.blockbreak.BlockBreakB;
import cn.jeyor1337.zanticheat.check.checks.interaction.blockplace.BlockPlaceA;
import cn.jeyor1337.zanticheat.check.checks.interaction.blockplace.BlockPlaceB;
import cn.jeyor1337.zanticheat.check.checks.interaction.fastbreak.FastBreakA;
import cn.jeyor1337.zanticheat.check.checks.interaction.fastplace.FastPlaceA;
import cn.jeyor1337.zanticheat.check.checks.interaction.ghostbreak.GhostBreakA;
import cn.jeyor1337.zanticheat.check.checks.interaction.scaffold.ScaffoldA;
import cn.jeyor1337.zanticheat.check.checks.interaction.scaffold.ScaffoldB;
import cn.jeyor1337.zanticheat.check.checks.inventory.sorting.SortingA;
import cn.jeyor1337.zanticheat.check.checks.inventory.swapping.ItemSwapA;
import cn.jeyor1337.zanticheat.check.checks.movement.airjump.AirJumpA;
import cn.jeyor1337.zanticheat.check.checks.movement.boat.BoatA;
import cn.jeyor1337.zanticheat.check.checks.movement.elytra.ElytraA;
import cn.jeyor1337.zanticheat.check.checks.movement.elytra.ElytraB;
import cn.jeyor1337.zanticheat.check.checks.movement.elytra.ElytraC;
import cn.jeyor1337.zanticheat.check.checks.movement.fastclimb.FastClimbA;
import cn.jeyor1337.zanticheat.check.checks.movement.flight.*;
import cn.jeyor1337.zanticheat.check.checks.movement.jump.JumpA;
import cn.jeyor1337.zanticheat.check.checks.movement.jump.JumpB;
import cn.jeyor1337.zanticheat.check.checks.movement.liquidwalk.LiquidWalkA;
import cn.jeyor1337.zanticheat.check.checks.movement.liquidwalk.LiquidWalkB;
import cn.jeyor1337.zanticheat.check.checks.movement.nofall.NoFallA;
import cn.jeyor1337.zanticheat.check.checks.movement.nofall.NoFallB;
import cn.jeyor1337.zanticheat.check.checks.movement.noslow.NoSlowA;
import cn.jeyor1337.zanticheat.check.checks.movement.speed.*;
import cn.jeyor1337.zanticheat.check.checks.movement.step.StepA;
import cn.jeyor1337.zanticheat.check.checks.movement.trident.TridentA;
import cn.jeyor1337.zanticheat.check.checks.movement.vehicle.VehicleA;
import cn.jeyor1337.zanticheat.check.checks.packet.badpackets.BadPacketsA;
import cn.jeyor1337.zanticheat.check.checks.packet.badpackets.BadPacketsB;
import cn.jeyor1337.zanticheat.check.checks.packet.badpackets.BadPacketsC;
import cn.jeyor1337.zanticheat.check.checks.packet.badpackets.BadPacketsD;
import cn.jeyor1337.zanticheat.check.checks.packet.badpackets.BadPacketsE;
import cn.jeyor1337.zanticheat.check.checks.packet.badpackets.BadPacketsF;
import cn.jeyor1337.zanticheat.check.checks.packet.morepackets.MorePacketsA;
import cn.jeyor1337.zanticheat.check.checks.packet.morepackets.MorePacketsB;
import cn.jeyor1337.zanticheat.check.checks.packet.timer.TimerA;
import cn.jeyor1337.zanticheat.check.checks.packet.timer.TimerB;
import cn.jeyor1337.zanticheat.check.checks.player.autobot.AutoBotA;
import cn.jeyor1337.zanticheat.check.checks.player.skinblinker.SkinBlinkerA;
import cn.jeyor1337.zanticheat.command.ZACCommand;
import cn.jeyor1337.zanticheat.event.ZACEventCaller;
import cn.jeyor1337.zanticheat.listener.invalidping.InvalidPingListener;
import cn.jeyor1337.zanticheat.listener.punishment.BanRecordPersistenceListener;
import cn.jeyor1337.zanticheat.listener.unloadedchunk.UnloadedChunkListener;
import cn.jeyor1337.zanticheat.player.ZACPlayerListener;
import cn.jeyor1337.zanticheat.storage.mysql.MySqlBanRecordService;
import cn.jeyor1337.zanticheat.util.api.ApiUtil;
import cn.jeyor1337.zanticheat.util.config.ConfigManager;
import cn.jeyor1337.zanticheat.util.hook.server.folia.FoliaUtil;
import cn.jeyor1337.zanticheat.util.logger.Logger;
import cn.jeyor1337.zanticheat.util.npc.ExternalNPCUtil;
import cn.jeyor1337.zanticheat.util.player.connectionstability.ConnectionStabilityListener;
import cn.jeyor1337.zanticheat.util.player.cps.CPSListener;
import cn.jeyor1337.zanticheat.util.tps.TPSCalculator;
import cn.jeyor1337.zanticheat.util.updater.Updater;
import cn.jeyor1337.zanticheat.util.velocity.VelocitySupportService;
import cn.jeyor1337.zanticheat.util.violation.ViolationHandler;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private static ZACEventCaller eventCaller;
    private static final long BUFFER_DURATION_MILS = 20 * 1000L;
    private static final int PLUGIN_ID = 112053;
    private static final int STATS_ID = 12841;

    private MySqlBanRecordService mySqlBanRecordService;
    private BanRecordPersistenceListener banRecordPersistenceListener;
    private VelocitySupportService velocitySupportService;

    @Override
    public void onEnable() {
        instance = this;
        FoliaUtil.loadFoliaUtil();
        ConfigManager.loadConfig();

        Buffer.loadBufferCleaner(BUFFER_DURATION_MILS);
        TPSCalculator.loadTPSCalculator();
        Logger.logFile("");
        ApiUtil.setApiInstance();

        ZACPlayerListener.loadZACPlayerListener();
        registerListener(new ZACPlayerListener());

        ExternalNPCUtil.loadExternalNPCUtil();

        registerListener(new ViolationHandler());
        initializeDatabaseSubsystem();
        initializeVelocitySupportSubsystem();

        eventCaller = new ZACEventCaller();
        registerListener(eventCaller);

        UnloadedChunkListener.handleUnloadedChunks();
        InvalidPingListener.limitMaxPing();

        CPSListener.loadCpsCalculatorOnReload();
        CPSListener.loadCpsCalculator();
        registerListener(new CPSListener());
        ConnectionStabilityListener.loadConnectionCalculatorOnReload();
        ConnectionStabilityListener.loadConnectionCalculator();
        registerListener(new ConnectionStabilityListener());

        PluginCommand antiCheatCommand = getCommand("zanticheat");
        if (antiCheatCommand != null) {
            antiCheatCommand.setExecutor(new ZACCommand());
            antiCheatCommand.setTabCompleter(new ZACCommand());
        }
        PluginCommand reportCommand = getCommand("report");
        if (reportCommand != null) {
            reportCommand.setExecutor(new ZACCommand());
            reportCommand.setTabCompleter(new ZACCommand());
        }

        Updater.loadUpdateChecker();
        registerListener(new Updater());

        registerCheckListener(new FlightA());
        registerCheckListener(new FlightB());
        registerCheckListener(new FlightC());
        registerCheckListener(new AirJumpA());
        registerCheckListener(new LiquidWalkA());
        registerCheckListener(new LiquidWalkB());
        registerCheckListener(new JumpA());
        registerCheckListener(new JumpB());
        registerCheckListener(new ElytraA());
        registerCheckListener(new ElytraB());
        registerCheckListener(new ElytraC());
        registerCheckListener(new FastClimbA());
        registerCheckListener(new NoFallA());
        registerCheckListener(new NoFallB());
        registerCheckListener(new NoSlowA());
        registerCheckListener(new SpeedA());
        registerCheckListener(new SpeedB());
        registerCheckListener(new SpeedD());
        registerCheckListener(new SpeedE());
        registerCheckListener(new SpeedF());
        registerCheckListener(new SpeedC());
        registerCheckListener(new StepA());
        registerCheckListener(new TridentA());
        registerCheckListener(new BoatA());
        registerCheckListener(new VehicleA());
        registerCheckListener(new KillAuraA());
        registerCheckListener(new KillAuraB());
        registerCheckListener(new KillAuraC());
        registerCheckListener(new KillAuraD());
        registerCheckListener(new KillAuraE());
        registerCheckListener(new FakeLagA());
        registerCheckListener(new ReachA());
        registerCheckListener(new ReachB());
        registerCheckListener(new CriticalsA());
        registerCheckListener(new CriticalsB());
        registerCheckListener(new AutoClickerA());
        registerCheckListener(new AutoClickerB());
        registerCheckListener(new AutoClickerC());
        registerCheckListener(new AutoClickerD());
        registerCheckListener(new VelocityA());
        registerCheckListener(new AirPlaceA());
        registerCheckListener(new FastPlaceA());
        registerCheckListener(new BlockPlaceA());
        registerCheckListener(new BlockPlaceB());
        registerCheckListener(new GhostBreakA());
        registerCheckListener(new FastBreakA());
        registerCheckListener(new BlockBreakA());
        registerCheckListener(new BlockBreakB());
        registerCheckListener(new ScaffoldA());
        registerCheckListener(new ScaffoldB());
        registerCheckListener(new SortingA());
        registerCheckListener(new ItemSwapA());
        registerCheckListener(new MorePacketsA());
        registerCheckListener(new MorePacketsB());
        registerCheckListener(new TimerA());
        registerCheckListener(new TimerB());
        registerCheckListener(new BadPacketsA());
        registerCheckListener(new BadPacketsB());
        registerCheckListener(new BadPacketsC());
        registerCheckListener(new BadPacketsD());
        registerCheckListener(new BadPacketsE());
        registerCheckListener(new BadPacketsF());
        registerCheckListener(new AutoBotA());
        registerCheckListener(new SkinBlinkerA());
    }

    @Override
    public void onDisable() {
        if (eventCaller != null)
            eventCaller.close();
        shutdownVelocitySupportSubsystem();
        shutdownDatabaseSubsystem();
    }

    public static Main getInstance() {
        return instance;
    }

    public static long getBufferDurationMils() {
        return BUFFER_DURATION_MILS;
    }

    public static int getPluginId() {
        return PLUGIN_ID;
    }

    public static int getStatsId() {
        return STATS_ID;
    }

    public void reloadDatabaseSubsystem() {
        shutdownDatabaseSubsystem();
        initializeDatabaseSubsystem();
    }

    public void reloadVelocitySupportSubsystem() {
        shutdownVelocitySupportSubsystem();
        initializeVelocitySupportSubsystem();
    }

    public VelocitySupportService getVelocitySupportService() {
        return velocitySupportService;
    }

    private void initializeDatabaseSubsystem() {
        if (!ConfigManager.Config.Database.enabled)
            return;
        mySqlBanRecordService = new MySqlBanRecordService();
        if (!mySqlBanRecordService.initialize()) {
            mySqlBanRecordService = null;
            return;
        }
        banRecordPersistenceListener = new BanRecordPersistenceListener(mySqlBanRecordService);
        registerListener(banRecordPersistenceListener);
    }

    private void initializeVelocitySupportSubsystem() {
        if (!ConfigManager.Config.VelocitySupport.enabled)
            return;
        velocitySupportService = new VelocitySupportService();
        if (!velocitySupportService.initialize()) {
            velocitySupportService = null;
        }
    }

    private void shutdownVelocitySupportSubsystem() {
        if (velocitySupportService != null) {
            velocitySupportService.shutdown();
            velocitySupportService = null;
        }
    }

    private void shutdownDatabaseSubsystem() {
        if (banRecordPersistenceListener != null) {
            HandlerList.unregisterAll(banRecordPersistenceListener);
            banRecordPersistenceListener = null;
        }
        if (mySqlBanRecordService != null) {
            mySqlBanRecordService.close();
            mySqlBanRecordService = null;
        }
    }

    private void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    private void registerCheckListener(Object object) {
        Check.registerListener(((Check) object).getCheckSetting().name, (Listener) object);
    }

}
