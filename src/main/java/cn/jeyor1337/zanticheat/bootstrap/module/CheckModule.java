package cn.jeyor1337.zanticheat.bootstrap.module;

import cn.jeyor1337.zanticheat.bootstrap.PluginContext;
import cn.jeyor1337.zanticheat.check.Check;
import cn.jeyor1337.zanticheat.check.checks.combat.autoclicker.AutoClickerA;
import cn.jeyor1337.zanticheat.check.checks.combat.autoclicker.AutoClickerB;
import cn.jeyor1337.zanticheat.check.checks.combat.autoclicker.AutoClickerC;
import cn.jeyor1337.zanticheat.check.checks.combat.autoclicker.AutoClickerD;
import cn.jeyor1337.zanticheat.check.checks.combat.criticals.CriticalsA;
import cn.jeyor1337.zanticheat.check.checks.combat.criticals.CriticalsB;
import cn.jeyor1337.zanticheat.check.checks.combat.fakelag.FakeLagA;
import cn.jeyor1337.zanticheat.check.checks.combat.killaura.KillAuraA;
import cn.jeyor1337.zanticheat.check.checks.combat.killaura.KillAuraB;
import cn.jeyor1337.zanticheat.check.checks.combat.killaura.KillAuraC;
import cn.jeyor1337.zanticheat.check.checks.combat.killaura.KillAuraD;
import cn.jeyor1337.zanticheat.check.checks.combat.killaura.KillAuraE;
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
import cn.jeyor1337.zanticheat.check.checks.movement.flight.FlightA;
import cn.jeyor1337.zanticheat.check.checks.movement.flight.FlightB;
import cn.jeyor1337.zanticheat.check.checks.movement.flight.FlightC;
import cn.jeyor1337.zanticheat.check.checks.movement.jump.JumpA;
import cn.jeyor1337.zanticheat.check.checks.movement.jump.JumpB;
import cn.jeyor1337.zanticheat.check.checks.movement.liquidwalk.LiquidWalkA;
import cn.jeyor1337.zanticheat.check.checks.movement.liquidwalk.LiquidWalkB;
import cn.jeyor1337.zanticheat.check.checks.movement.nofall.NoFallA;
import cn.jeyor1337.zanticheat.check.checks.movement.nofall.NoFallB;
import cn.jeyor1337.zanticheat.check.checks.movement.noslow.NoSlowA;
import cn.jeyor1337.zanticheat.check.checks.movement.speed.SpeedA;
import cn.jeyor1337.zanticheat.check.checks.movement.speed.SpeedB;
import cn.jeyor1337.zanticheat.check.checks.movement.speed.SpeedC;
import cn.jeyor1337.zanticheat.check.checks.movement.speed.SpeedD;
import cn.jeyor1337.zanticheat.check.checks.movement.speed.SpeedE;
import cn.jeyor1337.zanticheat.check.checks.movement.speed.SpeedF;
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

public final class CheckModule implements PluginModule {

    @Override
    public void enable(PluginContext context) {
        registerMovementChecks(context);
        registerCombatChecks(context);
        registerInteractionChecks(context);
        registerInventoryChecks(context);
        registerPacketChecks(context);
        registerPlayerChecks(context);
    }

    private void registerMovementChecks(PluginContext context) {
        registerChecks(context,
                new FlightA(),
                new FlightB(),
                new FlightC(),
                new AirJumpA(),
                new LiquidWalkA(),
                new LiquidWalkB(),
                new JumpA(),
                new JumpB(),
                new ElytraA(),
                new ElytraB(),
                new ElytraC(),
                new FastClimbA(),
                new NoFallA(),
                new NoFallB(),
                new NoSlowA(),
                new SpeedA(),
                new SpeedB(),
                new SpeedD(),
                new SpeedE(),
                new SpeedF(),
                new SpeedC(),
                new StepA(),
                new TridentA(),
                new BoatA(),
                new VehicleA()
        );
    }

    private void registerCombatChecks(PluginContext context) {
        registerChecks(context,
                new KillAuraA(),
                new KillAuraB(),
                new KillAuraC(),
                new KillAuraD(),
                new KillAuraE(),
                new FakeLagA(),
                new ReachA(),
                new ReachB(),
                new CriticalsA(),
                new CriticalsB(),
                new AutoClickerA(),
                new AutoClickerB(),
                new AutoClickerC(),
                new AutoClickerD(),
                new VelocityA()
        );
    }

    private void registerInteractionChecks(PluginContext context) {
        registerChecks(context,
                new AirPlaceA(),
                new FastPlaceA(),
                new BlockPlaceA(),
                new BlockPlaceB(),
                new GhostBreakA(),
                new FastBreakA(),
                new BlockBreakA(),
                new BlockBreakB(),
                new ScaffoldA(),
                new ScaffoldB()
        );
    }

    private void registerInventoryChecks(PluginContext context) {
        registerChecks(context,
                new SortingA(),
                new ItemSwapA()
        );
    }

    private void registerPacketChecks(PluginContext context) {
        registerChecks(context,
                new MorePacketsA(),
                new MorePacketsB(),
                new TimerA(),
                new TimerB(),
                new BadPacketsA(),
                new BadPacketsB(),
                new BadPacketsC(),
                new BadPacketsD(),
                new BadPacketsE(),
                new BadPacketsF()
        );
    }

    private void registerPlayerChecks(PluginContext context) {
        registerChecks(context,
                new AutoBotA(),
                new SkinBlinkerA()
        );
    }

    private void registerChecks(PluginContext context, Check... checks) {
        for (Check check : checks) {
            context.registerCheck(check);
        }
    }
}
