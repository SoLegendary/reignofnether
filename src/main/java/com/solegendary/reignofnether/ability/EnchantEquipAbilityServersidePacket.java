package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.villagers.Blacksmith;
import com.solegendary.reignofnether.building.buildings.villagers.Library;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class EnchantEquipAbilityServersidePacket {

    UnitAction abilityAction;
    BlockPos buildingPos;

    public static void setAutocastEnchantOrEquipServerside(UnitAction ability, BlockPos buildingPos) {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null)
            PacketHandler.INSTANCE.sendToServer(new EnchantEquipAbilityServersidePacket(ability, buildingPos));
    }

    // packet-handler functions
    public EnchantEquipAbilityServersidePacket(UnitAction ability, BlockPos buildingPos) {
        this.abilityAction = ability;
        this.buildingPos = buildingPos;
    }

    public EnchantEquipAbilityServersidePacket(FriendlyByteBuf buffer) {
        this.abilityAction = buffer.readEnum(UnitAction.class);
        this.buildingPos = buffer.readBlockPos();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(abilityAction);
        buffer.writeBlockPos(buildingPos);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                ReignOfNether.LOGGER.warn("EnchantEquipAbilityServerboundPacket: Sender was null");
                success.set(false);
                return;
            }

            BuildingPlacement building = BuildingUtils.findBuilding(false, buildingPos);
            if (building.getBuilding() instanceof Library) {
                if (!player.getName().getString().equals(building.ownerName)) {
                    ReignOfNether.LOGGER.warn("EnchantEquipAbilityServerboundPacket: Tried to process packet from " + player.getName() + " for: " + building.ownerName);
                    success.set(false);
                    return;
                }
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
                if (!player.getName().getString().equals(building.ownerName)) {
                    ReignOfNether.LOGGER.warn("EnchantEquipAbilityServerboundPacket: Tried to process packet from " + player.getName() + " for: " + building.ownerName);
                    success.set(false);
                    return;
                }
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
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}