package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.ability.AbilityServerboundPacket;
import com.solegendary.reignofnether.ability.BuildingAbilityClientboundPacket;
import com.solegendary.reignofnether.ability.BuildingAbilityServerboundPacket;
import com.solegendary.reignofnether.alliance.*;
import com.solegendary.reignofnether.attackwarnings.AttackWarningClientboundPacket;
import com.solegendary.reignofnether.building.BuildingClientboundPacket;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientboundPacket;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingServerboundPacket;
import com.solegendary.reignofnether.config.ClientboundSyncResourceCostPacket;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientboundPacket;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerboundPacket;
import com.solegendary.reignofnether.fogofwar.FrozenChunkClientboundPacket;
import com.solegendary.reignofnether.fogofwar.FrozenChunkServerboundPacket;
import com.solegendary.reignofnether.gamemode.GameModeClientboundPacket;
import com.solegendary.reignofnether.gamemode.GameModeServerboundPacket;
import com.solegendary.reignofnether.gamerules.GameruleClientboundPacket;
import com.solegendary.reignofnether.gamerules.GameruleServerboundPacket;
import com.solegendary.reignofnether.guiscreen.TopdownGuiServerboundPacket;
import com.solegendary.reignofnether.hero.FallenHeroClientboundPacket;
import com.solegendary.reignofnether.hero.HeroClientboundPacket;
import com.solegendary.reignofnether.hero.HeroServerboundPacket;
import com.solegendary.reignofnether.minimap.MapMarkerClientboundPacket;
import com.solegendary.reignofnether.minimap.MapMarkerServerboundPacket;
import com.solegendary.reignofnether.player.PlayerClientboundPacket;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.research.ResearchClientboundPacket;
import com.solegendary.reignofnether.research.ResearchServerboundPacket;
import com.solegendary.reignofnether.resources.ResourcesClientboundPacket;
import com.solegendary.reignofnether.resources.ResourcesServerboundPacket;
import com.solegendary.reignofnether.sandbox.SandboxServerboundPacket;
import com.solegendary.reignofnether.scenario.ScenarioClientboundPacket;
import com.solegendary.reignofnether.scenario.ScenarioServerboundPacket;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.startpos.StartPosClientboundPacket;
import com.solegendary.reignofnether.startpos.StartPosServerboundPacket;
import com.solegendary.reignofnether.survival.SurvivalClientboundPacket;
import com.solegendary.reignofnether.survival.SurvivalServerboundPacket;
import com.solegendary.reignofnether.tps.TPSClientBoundPacket;
import com.solegendary.reignofnether.tutorial.TutorialClientboundPacket;
import com.solegendary.reignofnether.tutorial.TutorialServerboundPacket;
import com.solegendary.reignofnether.unit.packets.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

// Initialises all of the client-server packet-sending classes

public final class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);


    private PacketHandler() { }

    public static void init() {
        int index = 0;

        INSTANCE.messageBuilder(TopdownGuiServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TopdownGuiServerboundPacket::encode).decoder(TopdownGuiServerboundPacket::new)
                .consumerMainThread(TopdownGuiServerboundPacket::handle).add();

        INSTANCE.messageBuilder(UnitActionServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UnitActionServerboundPacket::encode).decoder(UnitActionServerboundPacket::new)
                .consumerMainThread(UnitActionServerboundPacket::handle).add();

        INSTANCE.messageBuilder(BeaconSyncClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BeaconSyncClientboundPacket::encode).decoder(BeaconSyncClientboundPacket::new)
                .consumerMainThread(BeaconSyncClientboundPacket::handle).add();

        INSTANCE.messageBuilder(UnitConvertClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(UnitConvertClientboundPacket::encode).decoder(UnitConvertClientboundPacket::new)
                .consumerMainThread(UnitConvertClientboundPacket::handle).add();

        INSTANCE.messageBuilder(UnitSyncClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(UnitSyncClientboundPacket::encode).decoder(UnitSyncClientboundPacket::new)
                .consumerMainThread(UnitSyncClientboundPacket::handle).add();

        INSTANCE.messageBuilder(UnitSyncWorkerClientBoundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(UnitSyncWorkerClientBoundPacket::encode).decoder(UnitSyncWorkerClientBoundPacket::new)
                .consumerMainThread(UnitSyncWorkerClientBoundPacket::handle).add();

        INSTANCE.messageBuilder(UnitSyncAbilityClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(UnitSyncAbilityClientboundPacket::encode).decoder(UnitSyncAbilityClientboundPacket::new)
                .consumerMainThread(UnitSyncAbilityClientboundPacket::handle).add();

        INSTANCE.messageBuilder(UnitSyncServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UnitSyncServerboundPacket::encode).decoder(UnitSyncServerboundPacket::new)
                .consumerMainThread(UnitSyncServerboundPacket::handle).add();

        INSTANCE.messageBuilder(UnitAnimationClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(UnitAnimationClientboundPacket::encode).decoder(UnitAnimationClientboundPacket::new)
                .consumerMainThread(UnitAnimationClientboundPacket::handle).add();

        INSTANCE.messageBuilder(UnitIdleWorkerClientBoundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(UnitIdleWorkerClientBoundPacket::encode).decoder(UnitIdleWorkerClientBoundPacket::new)
                .consumerMainThread(UnitIdleWorkerClientBoundPacket::handle).add();

        INSTANCE.messageBuilder(ResearchClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ResearchClientboundPacket::encode).decoder(ResearchClientboundPacket::new)
                .consumerMainThread(ResearchClientboundPacket::handle).add();

        INSTANCE.messageBuilder(ResearchServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ResearchServerboundPacket::encode).decoder(ResearchServerboundPacket::new)
                .consumerMainThread(ResearchServerboundPacket::handle).add();

        INSTANCE.messageBuilder(PlayerServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PlayerServerboundPacket::encode).decoder(PlayerServerboundPacket::new)
                .consumerMainThread(PlayerServerboundPacket::handle).add();

        INSTANCE.messageBuilder(PlayerClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PlayerClientboundPacket::encode).decoder(PlayerClientboundPacket::new)
                .consumerMainThread(PlayerClientboundPacket::handle).add();

        INSTANCE.messageBuilder(FogOfWarClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FogOfWarClientboundPacket::encode).decoder(FogOfWarClientboundPacket::new)
                .consumerMainThread(FogOfWarClientboundPacket::handle).add();

        INSTANCE.messageBuilder(FogOfWarServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FogOfWarServerboundPacket::encode).decoder(FogOfWarServerboundPacket::new)
                .consumerMainThread(FogOfWarServerboundPacket::handle).add();

        INSTANCE.messageBuilder(FrozenChunkServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FrozenChunkServerboundPacket::encode).decoder(FrozenChunkServerboundPacket::new)
                .consumerMainThread(FrozenChunkServerboundPacket::handle).add();

        INSTANCE.messageBuilder(FrozenChunkClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FrozenChunkClientboundPacket::encode).decoder(FrozenChunkClientboundPacket::new)
                .consumerMainThread(FrozenChunkClientboundPacket::handle).add();

        INSTANCE.messageBuilder(BuildingServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BuildingServerboundPacket::encode).decoder(BuildingServerboundPacket::new)
                .consumerMainThread(BuildingServerboundPacket::handle).add();

        INSTANCE.messageBuilder(BuildingClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BuildingClientboundPacket::encode).decoder(BuildingClientboundPacket::new)
                .consumerMainThread(BuildingClientboundPacket::handle).add();

        INSTANCE.messageBuilder(ResourcesClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ResourcesClientboundPacket::encode).decoder(ResourcesClientboundPacket::new)
                .consumerMainThread(ResourcesClientboundPacket::handle).add();

        INSTANCE.messageBuilder(ResourcesServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ResourcesServerboundPacket::encode)
                .decoder(ResourcesServerboundPacket::new)
                .consumerMainThread(ResourcesServerboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(AbilityClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AbilityClientboundPacket::encode).decoder(AbilityClientboundPacket::new)
                .consumerMainThread(AbilityClientboundPacket::handle).add();

        INSTANCE.messageBuilder(AbilityServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AbilityServerboundPacket::encode).decoder(AbilityServerboundPacket::new)
                .consumerMainThread(AbilityServerboundPacket::handle).add();

        INSTANCE.messageBuilder(BuildingAbilityServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BuildingAbilityServerboundPacket::encode).decoder(BuildingAbilityServerboundPacket::new)
                .consumerMainThread(BuildingAbilityServerboundPacket::handle).add();

        INSTANCE.messageBuilder(BuildingAbilityClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BuildingAbilityClientboundPacket::encode).decoder(BuildingAbilityClientboundPacket::new)
                .consumerMainThread(BuildingAbilityClientboundPacket::handle).add();

        INSTANCE.messageBuilder(TPSClientBoundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TPSClientBoundPacket::encode).decoder(TPSClientBoundPacket::new)
                .consumerMainThread(TPSClientBoundPacket::handle).add();

        INSTANCE.messageBuilder(AttackWarningClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AttackWarningClientboundPacket::encode).decoder(AttackWarningClientboundPacket::new)
                .consumerMainThread(AttackWarningClientboundPacket::handle).add();

        INSTANCE.messageBuilder(SoundClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SoundClientboundPacket::encode).decoder(SoundClientboundPacket::new)
                .consumerMainThread(SoundClientboundPacket::handle).add();

        INSTANCE.messageBuilder(TutorialClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TutorialClientboundPacket::encode).decoder(TutorialClientboundPacket::new)
                .consumerMainThread(TutorialClientboundPacket::handle).add();

        INSTANCE.messageBuilder(TutorialServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TutorialServerboundPacket::encode).decoder(TutorialServerboundPacket::new)
                .consumerMainThread(TutorialServerboundPacket::handle).add();

        INSTANCE.messageBuilder(AllianceClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AllianceClientboundPacket::encode)
                .decoder(AllianceClientboundPacket::new)
                .consumerMainThread(AllianceClientboundPacket::handle)
                .add();


        INSTANCE.messageBuilder(AllianceServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AllianceServerboundPacket::encode)
                .decoder(AllianceServerboundPacket::new)
                .consumerMainThread(AllianceServerboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(GameModeServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GameModeServerboundPacket::encode)
                .decoder(GameModeServerboundPacket::new)
                .consumerMainThread(GameModeServerboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(GameModeClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GameModeClientboundPacket::encode)
                .decoder(GameModeClientboundPacket::new)
                .consumerMainThread(GameModeClientboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(SurvivalServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SurvivalServerboundPacket::encode)
                .decoder(SurvivalServerboundPacket::new)
                .consumerMainThread(SurvivalServerboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(SurvivalClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SurvivalClientboundPacket::encode)
                .decoder(SurvivalClientboundPacket::new)
                .consumerMainThread(SurvivalClientboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(ClientboundSyncResourceCostPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundSyncResourceCostPacket::encode)
                .decoder(ClientboundSyncResourceCostPacket::decode)
                .consumerMainThread(ClientboundSyncResourceCostPacket::handle)
                .add();

        INSTANCE.messageBuilder(SandboxServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SandboxServerboundPacket::encode)
                .decoder(SandboxServerboundPacket::new)
                .consumerMainThread(SandboxServerboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(GameruleServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GameruleServerboundPacket::encode)
                .decoder(GameruleServerboundPacket::new)
                .consumerMainThread(GameruleServerboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(GameruleClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GameruleClientboundPacket::encode)
                .decoder(GameruleClientboundPacket::new)
                .consumerMainThread(GameruleClientboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(StartPosServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(StartPosServerboundPacket::encode)
                .decoder(StartPosServerboundPacket::new)
                .consumerMainThread(StartPosServerboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(StartPosClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StartPosClientboundPacket::encode)
                .decoder(StartPosClientboundPacket::new)
                .consumerMainThread(StartPosClientboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(HeroClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HeroClientboundPacket::encode)
                .decoder(HeroClientboundPacket::new)
                .consumerMainThread(HeroClientboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(HeroServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(HeroServerboundPacket::encode)
                .decoder(HeroServerboundPacket::new)
                .consumerMainThread(HeroServerboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(FallenHeroClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FallenHeroClientboundPacket::encode)
                .decoder(FallenHeroClientboundPacket::new)
                .consumerMainThread(FallenHeroClientboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(CustomBuildingClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CustomBuildingClientboundPacket::encode)
                .decoder(CustomBuildingClientboundPacket::new)
                .consumerMainThread(CustomBuildingClientboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(CustomBuildingServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CustomBuildingServerboundPacket::encode)
                .decoder(CustomBuildingServerboundPacket::new)
                .consumerMainThread(CustomBuildingServerboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(UnitSyncMobEffectsClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(UnitSyncMobEffectsClientboundPacket::encode)
                .decoder(UnitSyncMobEffectsClientboundPacket::new)
                .consumerMainThread(UnitSyncMobEffectsClientboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(MapMarkerServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MapMarkerServerboundPacket::encode)
                .decoder(MapMarkerServerboundPacket::new)
                .consumerMainThread(MapMarkerServerboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(MapMarkerClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MapMarkerClientboundPacket::encode)
                .decoder(MapMarkerClientboundPacket::new)
                .consumerMainThread(MapMarkerClientboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(ScenarioServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ScenarioServerboundPacket::encode)
                .decoder(ScenarioServerboundPacket::new)
                .consumerMainThread(ScenarioServerboundPacket::handle)
                .add();

        INSTANCE.messageBuilder(ScenarioClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ScenarioClientboundPacket::encode)
                .decoder(ScenarioClientboundPacket::new)
                .consumerMainThread(ScenarioClientboundPacket::handle)
                .add();
    }
}
