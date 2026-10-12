package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.monsters.Graveyard;
import com.solegendary.reignofnether.building.buildings.placements.GraveyardPlacement;
import com.solegendary.reignofnether.building.buildings.villagers.Blacksmith;
import com.solegendary.reignofnether.building.buildings.villagers.Library;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class BuildingAbilityClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<BuildingAbilityClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "building_ability_clientbound"));

    @Override
    public CustomPacketPayload.Type<BuildingAbilityClientboundPacket> type() {
        return TYPE;
    }

    UnitAction abilityAction;
    BlockPos buildingPos;

    public static void doAbility(UnitAction ability, BlockPos buildingPos) {
        PacketHandler.send(PacketHandler.allPlayers(), new BuildingAbilityClientboundPacket(ability, buildingPos));
    }

    // packet-handler functions
    public BuildingAbilityClientboundPacket(UnitAction ability, BlockPos buildingPos) {
        this.abilityAction = ability;
        this.buildingPos = buildingPos;
    }

    public BuildingAbilityClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.abilityAction = buffer.readEnum(UnitAction.class);
        this.buildingPos = buffer.readBlockPos();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(abilityAction);
        buffer.writeBlockPos(buildingPos);
    }

    // client-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            BuildingPlacement building = BuildingUtils.findBuilding(true, buildingPos);
            if (building != null && building.getBuilding() instanceof Library) {
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
            else if (building != null && building.getBuilding() instanceof Blacksmith) {
                Ability ability = null;
                for (Ability abl : building.getAbilities())
                    if (abl.action == abilityAction)
                        ability = abl;
                if (ability instanceof EquipAbility equipAbility) {
                    building.getDataStorage().setData(Blacksmith.AUTO_CAST_EQUIP, equipAbility);
                }
            }
            else if (building instanceof GraveyardPlacement gy) {
                if (abilityAction == UnitAction.SET_GRAVEYARD_RELEASE_ON)
                    gy.autoRelease = true;
                else if (abilityAction == UnitAction.SET_GRAVEYARD_RELEASE_OFF)
                    gy.autoRelease = false;
            }
        });
        return;
    }
}