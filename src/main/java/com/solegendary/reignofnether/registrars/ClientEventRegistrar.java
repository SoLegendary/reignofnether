package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.attackwarnings.AttackWarningClientEvents;
import com.solegendary.reignofnether.attackwarnings.AttackWarningServerEvents;
import com.solegendary.reignofnether.blocks.BlockClientEvents;
import com.solegendary.reignofnether.blocks.BlockServerEvents;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingServerEvents;
import com.solegendary.reignofnether.commands.CommandsServerEvents;
import com.solegendary.reignofnether.config.ConfigClientEvents;
import com.solegendary.reignofnether.config.ConfigVanillaServerEvents;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.gamemode.GameModeServerEvents;
import com.solegendary.reignofnether.gamerules.GameruleServerEvents;
import com.solegendary.reignofnether.guiscreen.TopdownGuiClientEvents;
import com.solegendary.reignofnether.healthbars.HealthBarClientEvents;
import com.solegendary.reignofnether.hero.HeroClientEvents;
import com.solegendary.reignofnether.hero.HeroServerEvents;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.TextInputClientEvents;
import com.solegendary.reignofnether.hud.playerdisplay.PlayerDisplayClientEvents;
import com.solegendary.reignofnether.hud.TitleClientEvents;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.scenario.ScenarioClientEvents;
import com.solegendary.reignofnether.scenario.ScenarioServerEvents;
import com.solegendary.reignofnether.sounds.SoundClientEvents;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import com.solegendary.reignofnether.survival.SurvivalClientEvents;
import com.solegendary.reignofnether.survival.SurvivalServerEvents;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.time.TimeServerEvents;
import com.solegendary.reignofnether.tps.TPSClientEvents;
import com.solegendary.reignofnether.tps.TPSServerEvents;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialServerEvents;
import com.solegendary.reignofnether.unit.NonUnitClientEvents;
import com.solegendary.reignofnether.unit.NonUnitServerEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.worldborder.WorldBorderClientEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

public class ClientEventRegistrar {
    private final IEventBus vanillaEventBus = MinecraftForge.EVENT_BUS;

    public ClientEventRegistrar() { }

    /**
     * Register client only events. This method must only be called when it is certain that the mod is
     * is executing code on the client side and not the dedicated server.
     */

    public void registerClientEvents() {
        vanillaEventBus.register(OrthoviewClientEvents.class);
        vanillaEventBus.register(TopdownGuiClientEvents.class);
        vanillaEventBus.register(ConfigClientEvents.class);
        vanillaEventBus.register(BuildingClientEvents.class); // being first fixes a bug with drawBuildingToPlace()
        vanillaEventBus.register(UnitClientEvents.class);
        vanillaEventBus.register(HealthBarClientEvents.class);
        vanillaEventBus.register(SandboxClientEvents.class);
        vanillaEventBus.register(HudClientEvents.class); // ensure this is first so cursor is rendered above hud
        vanillaEventBus.register(AttackWarningClientEvents.class);
        vanillaEventBus.register(CursorClientEvents.class);
        vanillaEventBus.register(MinimapClientEvents.class);
        vanillaEventBus.register(TimeClientEvents.class);
        vanillaEventBus.register(BlockClientEvents.class);
        vanillaEventBus.register(FogOfWarClientEvents.class);
        vanillaEventBus.register(ResourcesClientEvents.class);
        vanillaEventBus.register(TPSClientEvents.class);
        vanillaEventBus.register(PlayerClientEvents.class);
        vanillaEventBus.register(TutorialClientEvents.class);
        vanillaEventBus.register(TitleClientEvents.class);
        vanillaEventBus.register(WorldBorderClientEvents.class);
        vanillaEventBus.register(SurvivalClientEvents.class);
        vanillaEventBus.register(StartPosClientEvents.class);
        vanillaEventBus.register(NonUnitClientEvents.class);
        vanillaEventBus.register(HeroClientEvents.class);
        vanillaEventBus.register(CustomBuildingClientEvents.class);
        vanillaEventBus.register(SoundClientEvents.class);
        vanillaEventBus.register(PlayerDisplayClientEvents.class);
        vanillaEventBus.register(ScenarioClientEvents.class);
        vanillaEventBus.register(TextInputClientEvents.class);

        // to allow singleplayer integrated server to work
        vanillaEventBus.register(GameruleServerEvents.class);
        vanillaEventBus.register(BlockServerEvents.class);
        vanillaEventBus.register(TutorialServerEvents.class);
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
