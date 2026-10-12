package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.ability.AbilityServerboundPacket;
import com.solegendary.reignofnether.ability.BuildingAbilityClientboundPacket;
import com.solegendary.reignofnether.ability.BuildingAbilityServerboundPacket;
import com.solegendary.reignofnether.alliance.*;
import com.solegendary.reignofnether.attackwarnings.AttackWarningClientboundPacket;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientboundPacket;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingServerboundPacket;
import com.solegendary.reignofnether.config.ClientboundSyncResourceCostPacket;
import com.solegendary.reignofnether.fogofwar.*;
import com.solegendary.reignofnether.gamemode.GameModeClientboundPacket;
import com.solegendary.reignofnether.gamemode.GameModeServerboundPacket;
import com.solegendary.reignofnether.gamerules.GameruleClientboundPacket;
import com.solegendary.reignofnether.gamerules.GameruleServerboundPacket;
import com.solegendary.reignofnether.guiscreen.TopdownGuiServerboundPacket;
import com.solegendary.reignofnether.hero.FallenHeroClientboundPacket;
import com.solegendary.reignofnether.hero.HeroClientboundPacket;
import com.solegendary.reignofnether.hero.HeroServerboundPacket;
import com.solegendary.reignofnether.hud.HudClientboundPacket;
import com.solegendary.reignofnether.hud.custombutton.CustomButtonActionServerboundPacket;
import com.solegendary.reignofnether.hud.custombutton.CustomButtonClientboundPacket;
import com.solegendary.reignofnether.items.ItemClientboundPacket;
import com.solegendary.reignofnether.items.ItemServerboundPacket;
import com.solegendary.reignofnether.items.ItemShopClientboundPacket;
import com.solegendary.reignofnether.minimap.MapMarkerClientboundPacket;
import com.solegendary.reignofnether.minimap.MapMarkerServerboundPacket;
import com.solegendary.reignofnether.network.PacketTarget;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import com.solegendary.reignofnether.orthoview.CameraClientboundPacket;
import com.solegendary.reignofnether.orthoview.CameraFadeClientboundPacket;
import com.solegendary.reignofnether.player.MatchStatsClientboundPacket;
import com.solegendary.reignofnether.player.PlayerClientboundPacket;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.research.ResearchClientboundPacket;
import com.solegendary.reignofnether.research.ResearchServerboundPacket;
import com.solegendary.reignofnether.resources.ResourcesClientboundPacket;
import com.solegendary.reignofnether.resources.ResourcesServerboundPacket;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoClientboundPacket;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoServerboundPacket;
import com.solegendary.reignofnether.sandbox.SandboxServerboundPacket;
import com.solegendary.reignofnether.scenario.ScenarioClientboundPacket;
import com.solegendary.reignofnether.scenario.ScenarioServerboundPacket;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.startpos.StartPosClientboundPacket;
import com.solegendary.reignofnether.startpos.StartPosServerboundPacket;
import com.solegendary.reignofnether.survival.SurvivalClientboundPacket;
import com.solegendary.reignofnether.survival.SurvivalServerboundPacket;
import com.solegendary.reignofnether.tutorial.TutorialClientboundPacket;
import com.solegendary.reignofnether.tutorial.TutorialServerboundPacket;
import com.solegendary.reignofnether.unit.packets.*;
import com.solegendary.reignofnether.debug.RtsDebugChunksClientboundPacket;
import com.solegendary.reignofnether.debug.RtsDebugStatsClientboundPacket;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registration and sending of the mod's 71 payloads.
 *
 * <p>On 1.20.1 this was a {@code SimpleChannel} with a numeric index per message and a
 * {@code PacketDistributor} chosen at each call site. NeoForge 21.1 removed SimpleChannel, and
 * with it the index: payloads are now identified by their {@link CustomPacketPayload.Type}, and
 * the recipient is a direct call on {@link PacketDistributor}.
 *
 * <p>Two pieces of the old shape are kept deliberately, because the alternative is touching 148
 * call sites for no behavioural gain:
 * <ul>
 *   <li>{@link #allPlayers()} / {@link #toPlayer(Supplier)} reproduce the two
 *       {@code PacketDistributor} targets this mod actually used ({@code ALL.noArg()} and
 *       {@code PLAYER.with(supplier)}); {@link PacketTarget} is the recipient abstraction.</li>
 *   <li>each payload keeps its {@code encode(FriendlyByteBuf)} + {@code handle} pair, so
 *       {@link #codec(Function)} can build the StreamCodec from the existing decoder.</li>
 * </ul>
 */
public final class PacketHandler {

    // Bumped by hand when a packet's field order changes; NeoForge compares it during the
    // configuration handshake and refuses the connection on a mismatch.
    private static final String PROTOCOL_VERSION = "1";

    private PacketHandler() { }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registerServer(registrar, TopdownGuiServerboundPacket.TYPE, TopdownGuiServerboundPacket::new);
        registerServer(registrar, UnitActionServerboundPacket.TYPE, UnitActionServerboundPacket::new);
        registerClient(registrar, BeaconSyncClientboundPacket.TYPE, BeaconSyncClientboundPacket::new);
        registerClient(registrar, UnitConvertClientboundPacket.TYPE, UnitConvertClientboundPacket::new);
        registerClient(registrar, UnitSyncClientboundPacket.TYPE, UnitSyncClientboundPacket::new);
        registerClient(registrar, UnitSyncWorkerClientBoundPacket.TYPE, UnitSyncWorkerClientBoundPacket::new);
        registerClient(registrar, UnitSyncAbilityClientboundPacket.TYPE, UnitSyncAbilityClientboundPacket::new);
        registerServer(registrar, UnitSyncServerboundPacket.TYPE, UnitSyncServerboundPacket::new);
        registerClient(registrar, UnitAnimationClientboundPacket.TYPE, UnitAnimationClientboundPacket::new);
        registerClient(registrar, UnitPathClientboundPacket.TYPE, UnitPathClientboundPacket::new);
        registerClient(registrar, RtsDebugStatsClientboundPacket.TYPE, RtsDebugStatsClientboundPacket::new);
        registerClient(registrar, RtsDebugChunksClientboundPacket.TYPE, RtsDebugChunksClientboundPacket::new);
        registerClient(registrar, UnitIdleWorkerClientBoundPacket.TYPE, UnitIdleWorkerClientBoundPacket::new);
        registerClient(registrar, ResearchClientboundPacket.TYPE, ResearchClientboundPacket::new);
        registerServer(registrar, ResearchServerboundPacket.TYPE, ResearchServerboundPacket::new);
        registerServer(registrar, PlayerServerboundPacket.TYPE, PlayerServerboundPacket::new);
        registerClient(registrar, PlayerClientboundPacket.TYPE, PlayerClientboundPacket::new);
        registerClient(registrar, FogOfWarClientboundPacket.TYPE, FogOfWarClientboundPacket::new);
        registerClient(registrar, FogChunksClientboundPacket.TYPE, FogChunksClientboundPacket::new);
        registerServer(registrar, FogOfWarServerboundPacket.TYPE, FogOfWarServerboundPacket::new);
        registerClient(registrar, FrozenChunkClientboundPacket.TYPE, FrozenChunkClientboundPacket::new);
        registerServer(registrar, BuildingServerboundPacket.TYPE, BuildingServerboundPacket::new);
        registerClient(registrar, BuildingClientboundPacket.TYPE, BuildingClientboundPacket::new);
        registerServer(registrar, BuildingProductionServerboundPacket.TYPE, BuildingProductionServerboundPacket::new);
        registerClient(registrar, BuildingProductionClientboundPacket.TYPE, BuildingProductionClientboundPacket::new);
        registerClient(registrar, ResourcesClientboundPacket.TYPE, ResourcesClientboundPacket::new);
        registerServer(registrar, ResourcesServerboundPacket.TYPE, ResourcesServerboundPacket::new);
        registerClient(registrar, AbilityClientboundPacket.TYPE, AbilityClientboundPacket::new);
        registerServer(registrar, AbilityServerboundPacket.TYPE, AbilityServerboundPacket::new);
        registerServer(registrar, BuildingAbilityServerboundPacket.TYPE, BuildingAbilityServerboundPacket::new);
        registerClient(registrar, BuildingAbilityClientboundPacket.TYPE, BuildingAbilityClientboundPacket::new);
        registerClient(registrar, AttackWarningClientboundPacket.TYPE, AttackWarningClientboundPacket::new);
        registerClient(registrar, SoundClientboundPacket.TYPE, SoundClientboundPacket::new);
        registerClient(registrar, TutorialClientboundPacket.TYPE, TutorialClientboundPacket::new);
        registerServer(registrar, TutorialServerboundPacket.TYPE, TutorialServerboundPacket::new);
        registerClient(registrar, AllianceClientboundPacket.TYPE, AllianceClientboundPacket::new);
        registerServer(registrar, AllianceServerboundPacket.TYPE, AllianceServerboundPacket::new);
        registerServer(registrar, GameModeServerboundPacket.TYPE, GameModeServerboundPacket::new);
        registerClient(registrar, GameModeClientboundPacket.TYPE, GameModeClientboundPacket::new);
        registerServer(registrar, SurvivalServerboundPacket.TYPE, SurvivalServerboundPacket::new);
        registerClient(registrar, SurvivalClientboundPacket.TYPE, SurvivalClientboundPacket::new);
        registerClient(registrar, ClientboundSyncResourceCostPacket.TYPE, ClientboundSyncResourceCostPacket::decode);
        registerServer(registrar, SandboxServerboundPacket.TYPE, SandboxServerboundPacket::new);
        registerServer(registrar, GameruleServerboundPacket.TYPE, GameruleServerboundPacket::new);
        registerClient(registrar, GameruleClientboundPacket.TYPE, GameruleClientboundPacket::new);
        registerServer(registrar, StartPosServerboundPacket.TYPE, StartPosServerboundPacket::new);
        registerClient(registrar, StartPosClientboundPacket.TYPE, StartPosClientboundPacket::new);
        registerClient(registrar, HeroClientboundPacket.TYPE, HeroClientboundPacket::new);
        registerServer(registrar, HeroServerboundPacket.TYPE, HeroServerboundPacket::new);
        registerClient(registrar, FallenHeroClientboundPacket.TYPE, FallenHeroClientboundPacket::new);
        registerClient(registrar, CustomBuildingClientboundPacket.TYPE, CustomBuildingClientboundPacket::new);
        registerServer(registrar, CustomBuildingServerboundPacket.TYPE, CustomBuildingServerboundPacket::new);
        registerClient(registrar, UnitSyncMobEffectsClientboundPacket.TYPE, UnitSyncMobEffectsClientboundPacket::new);
        registerServer(registrar, MapMarkerServerboundPacket.TYPE, MapMarkerServerboundPacket::new);
        registerClient(registrar, MapMarkerClientboundPacket.TYPE, MapMarkerClientboundPacket::new);
        registerServer(registrar, ScenarioServerboundPacket.TYPE, ScenarioServerboundPacket::new);
        registerClient(registrar, ScenarioClientboundPacket.TYPE, ScenarioClientboundPacket::new);
        registerClient(registrar, MatchStatsClientboundPacket.TYPE, MatchStatsClientboundPacket::new);
        registerClient(registrar, RTSMapInfoClientboundPacket.TYPE, RTSMapInfoClientboundPacket::new);
        registerServer(registrar, RTSMapInfoServerboundPacket.TYPE, RTSMapInfoServerboundPacket::new);
        registerClient(registrar, PlayerChunksClientboundPacket.TYPE, PlayerChunksClientboundPacket::new);
        registerClient(registrar, HudClientboundPacket.TYPE, HudClientboundPacket::new);
        registerClient(registrar, CameraClientboundPacket.TYPE, CameraClientboundPacket::new);
        registerClient(registrar, CameraFadeClientboundPacket.TYPE, CameraFadeClientboundPacket::new);
        registerClient(registrar, FogNeutralUnitClientboundPacket.TYPE, FogNeutralUnitClientboundPacket::new);
        registerClient(registrar, FogBuildingClientboundPacket.TYPE, FogBuildingClientboundPacket::new);
        registerServer(registrar, CustomButtonActionServerboundPacket.TYPE, CustomButtonActionServerboundPacket::decode);
        registerClient(registrar, CustomButtonClientboundPacket.TYPE, CustomButtonClientboundPacket::decode);
        registerServer(registrar, ItemServerboundPacket.TYPE, ItemServerboundPacket::new);
        registerClient(registrar, ItemClientboundPacket.TYPE, ItemClientboundPacket::new);
        registerClient(registrar, ItemShopClientboundPacket.TYPE, ItemShopClientboundPacket::new);
    }

    /** Builds the StreamCodec for a payload from the buffer-writing half it already has. */
    private static <T extends RTSSimplePayload> StreamCodec<RegistryFriendlyByteBuf, T> codec(
            Function<RegistryFriendlyByteBuf, T> decoder) {
        // The two halves are named explicitly: StreamEncoder/StreamDecoder are not supertypes of
        // the lambdas' target types, so without them javac cannot pick B.
        return StreamCodec.of(
                (StreamEncoder<RegistryFriendlyByteBuf, T>) (buf, payload) -> payload.encode(buf),
                (StreamDecoder<RegistryFriendlyByteBuf, T>) decoder::apply);
    }

    private static <T extends RTSSimplePayload> void registerServer(
            PayloadRegistrar registrar, CustomPacketPayload.Type<T> type, Function<RegistryFriendlyByteBuf, T> decoder) {
        registrar.playToServer(type, codec(decoder), (payload, ctx) -> payload.handle(ctx));
    }

    private static <T extends RTSSimplePayload> void registerClient(
            PayloadRegistrar registrar, CustomPacketPayload.Type<T> type, Function<RegistryFriendlyByteBuf, T> decoder) {
        registrar.playToClient(type, codec(decoder), (payload, ctx) -> payload.handle(ctx));
    }

    // ---------------------------------------------------------------------------------
    // Sending
    // ---------------------------------------------------------------------------------

    public static PacketTarget allPlayers() {
        return PacketTarget.ALL_PLAYERS;
    }

    public static PacketTarget toPlayer(Supplier<ServerPlayer> player) {
        return PacketTarget.toPlayer(player);
    }

    public static void send(PacketTarget target, RTSSimplePayload payload) {
        target.send(payload);
    }

    /** Sends straight to the connection this code is running on (client -> server). */
    public static void sendToServer(RTSSimplePayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
