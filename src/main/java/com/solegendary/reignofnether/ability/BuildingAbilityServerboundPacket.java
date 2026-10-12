package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.GraveyardPlacement;
import com.solegendary.reignofnether.building.buildings.villagers.Blacksmith;
import com.solegendary.reignofnether.building.buildings.villagers.Library;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class BuildingAbilityServerboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<BuildingAbilityServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "building_ability_serverbound"));

    @Override
    public CustomPacketPayload.Type<BuildingAbilityServerboundPacket> type() {
        return TYPE;
    }

    UnitAction abilityAction;
    BlockPos buildingPos;

    public static void doAbility(UnitAction ability, BlockPos buildingPos, boolean oneClickOneUse) {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null) {
            if (oneClickOneUse) {
                PacketHandler.sendToServer(new BuildingAbilityServerboundPacket(ability, buildingPos));
            } else {
                BuildingPlacement firstBpl = BuildingUtils.findBuilding(true, buildingPos);
                if (firstBpl != null)
                    for (BuildingPlacement bpl : BuildingClientEvents.getSelectedBuildings())
                        if (bpl.getBuilding().structureName.equals(firstBpl.getBuilding().structureName))
                            PacketHandler.sendToServer(new BuildingAbilityServerboundPacket(ability, bpl.originPos));
            }
        }
    }

    // packet-handler functions
    public BuildingAbilityServerboundPacket(UnitAction ability, BlockPos buildingPos) {
        this.abilityAction = ability;
        this.buildingPos = buildingPos;
    }

    public BuildingAbilityServerboundPacket(RegistryFriendlyByteBuf buffer) {
        this.abilityAction = buffer.readEnum(UnitAction.class);
        this.buildingPos = buffer.readBlockPos();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(abilityAction);
        buffer.writeBlockPos(buildingPos);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {

            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                ReignOfNether.LOGGER.warn("BuildingAbilityServerboundPacket: Sender was null");
                return;
            }
            BuildingPlacement building = BuildingUtils.findBuilding(false, buildingPos);
            if (building != null) {
                if (!player.getName().getString().equals(building.ownerName)) {
                    ReignOfNether.LOGGER.warn("BuildingAbilityServerboundPacket: Tried to process packet from " + player.getName() + " for: " + building.ownerName);
                    return;
                }
                ReignOfNether.LOGGER.info("[BuildingAbility] {} performed {} at {}", player.getName(), abilityAction, buildingPos);

                if (building.getBuilding() instanceof Library) {
                    Ability ability = null;
                    for (Ability abl : building.getAbilities())
                        if (abl.action == abilityAction)
                            ability = abl;
                    if (ability instanceof EnchantAbility enchantAbility) {
                        if (building.getDataStorage().getData(Library.AUTO_CAST_ENCHANT) == enchantAbility)
                            building.getDataStorage().setData(Library.AUTO_CAST_ENCHANT, null);
                        else
                            building.getDataStorage().setData(Library.AUTO_CAST_ENCHANT, enchantAbility);
                    }
                }
                else if (building.getBuilding() instanceof Blacksmith) {
                    Ability ability = null;
                    for (Ability abl : building.getAbilities())
                        if (abl.action == abilityAction)
                            ability = abl;
                    if (ability instanceof EquipAbility equipAbility) {
                        if (building.getDataStorage().getData(Blacksmith.AUTO_CAST_EQUIP) == equipAbility)
                            building.getDataStorage().setData(Blacksmith.AUTO_CAST_EQUIP, null);
                        else
                            building.getDataStorage().setData(Blacksmith.AUTO_CAST_EQUIP, equipAbility);
                    }
                }
                else if (building instanceof GraveyardPlacement gy) {
                    if (abilityAction == UnitAction.SET_GRAVEYARD_RELEASE_ON)
                        gy.autoRelease = true;
                    else if (abilityAction == UnitAction.SET_GRAVEYARD_RELEASE_OFF)
                        gy.autoRelease = false;
                    BuildingAbilityClientboundPacket.doAbility(abilityAction, buildingPos);
                }
            }
        });
        return;
    }
}