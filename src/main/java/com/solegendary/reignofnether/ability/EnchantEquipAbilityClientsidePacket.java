package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.villagers.Blacksmith;
import com.solegendary.reignofnether.building.buildings.villagers.Library;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class EnchantEquipAbilityClientsidePacket {

    UnitAction abilityAction;
    BlockPos buildingPos;

    public static void setAutocastEnchantOrEquipServerside(UnitAction ability, BlockPos buildingPos) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new EnchantEquipAbilityClientsidePacket(ability, buildingPos));
    }

    // packet-handler functions
    public EnchantEquipAbilityClientsidePacket(UnitAction ability, BlockPos buildingPos) {
        this.abilityAction = ability;
        this.buildingPos = buildingPos;
    }

    public EnchantEquipAbilityClientsidePacket(FriendlyByteBuf buffer) {
        this.abilityAction = buffer.readEnum(UnitAction.class);
        this.buildingPos = buffer.readBlockPos();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(abilityAction);
        buffer.writeBlockPos(buildingPos);
    }

    // client-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            BuildingPlacement building = BuildingUtils.findBuilding(true, buildingPos);
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
            else if (building.getBuilding() instanceof Blacksmith blacksmith) {
                Ability ability = null;
                for (Ability abl : building.getAbilities())
                    if (abl.action == abilityAction)
                        ability = abl;
                if (ability instanceof EquipAbility equipAbility) {
                    building.getDataStorage().setData(Blacksmith.AUTO_CAST_EQUIP, equipAbility);
                }
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}