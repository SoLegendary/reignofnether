package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.attackwarnings.AttackWarningServerEvents;
import com.solegendary.reignofnether.blocks.BlockServerEvents;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingServerEvents;
import com.solegendary.reignofnether.commands.CommandsServerEvents;
import com.solegendary.reignofnether.config.ConfigVanillaServerEvents;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.gamemode.GameModeServerEvents;
import com.solegendary.reignofnether.gamerules.GameruleServerEvents;
import com.solegendary.reignofnether.hero.HeroServerEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.scenario.ScenarioServerEvents;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import com.solegendary.reignofnether.survival.SurvivalServerEvents;
import com.solegendary.reignofnether.time.TimeServerEvents;
import com.solegendary.reignofnether.tps.TPSServerEvents;
import com.solegendary.reignofnether.unit.NonUnitServerEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

/*
    This class is required to make sure that we don't accidentally try to load any client-side-only classes
      on a dedicated server.
    It is a rather convoluted way of doing it, but I haven't found a simpler way to do it which is robust
 */

public class ServerEventRegistrar {
    private final IEventBus vanillaEventBus = MinecraftForge.EVENT_BUS;

    public ServerEventRegistrar() { }

    /**
     * Register server only events. This method must only be called when it is certain that the mod is
     * is executing code on the server side and not the client.
     */
    public void registerServerEvents() {
        vanillaEventBus.register(GameruleServerEvents.class);
        vanillaEventBus.register(BlockServerEvents.class);
        vanillaEventBus.register(PlayerServerEvents.class);
        vanillaEventBus.register(ConfigVanillaServerEvents.class);
        vanillaEventBus.register(UnitServerEvents.class);
        vanillaEventBus.register(BuildingServerEvents.class);
        vanillaEventBus.register(AttackWarningServerEvents.class);
        vanillaEventBus.register(ResourcesServerEvents.class);
        vanillaEventBus.register(TPSServerEvents.class);
        vanillaEventBus.register(FogOfWarServerEvents.class);
        vanillaEventBus.register(ResearchServerEvents.class);
        vanillaEventBus.register(SurvivalServerEvents.class);
        vanillaEventBus.register(GameModeServerEvents.class);
        vanillaEventBus.register(StartPosServerEvents.class);
        vanillaEventBus.register(AlliancesServerEvents.class);
        vanillaEventBus.register(HeroServerEvents.class);
        vanillaEventBus.register(NonUnitServerEvents.class);
        vanillaEventBus.register(TimeServerEvents.class);
        vanillaEventBus.register(CustomBuildingServerEvents.class);
        vanillaEventBus.register(CommandsServerEvents.class);
        vanillaEventBus.register(ScenarioServerEvents.class);
    }
}
