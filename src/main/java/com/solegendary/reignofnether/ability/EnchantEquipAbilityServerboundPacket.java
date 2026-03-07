package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.BlacksmithPlacement;
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

public class EnchantEquipAbilityServerboundPacket {

    UnitAction abilityAction;
    BlockPos buildingPos;

    public static void setAutocastEnchantOrEquip(UnitAction ability, BlockPos buildingPos) {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null)
            PacketHandler.INSTANCE.sendToServer(new EnchantEquipAbilityServerboundPacket(ability, buildingPos));
    }

    // packet-handler functions
    public EnchantEquipAbilityServerboundPacket(UnitAction ability, BlockPos buildingPos) {
        this.abilityAction = ability;
        this.buildingPos = buildingPos;
    }

    public EnchantEquipAbilityServerboundPacket(FriendlyByteBuf buffer) {
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
            else if (building instanceof BlacksmithPlacement blacksmith) {
                if (!player.getName().getString().equals(building.ownerName)) {
                    ReignOfNether.LOGGER.warn("EnchantEquipAbilityServerboundPacket: Tried to process packet from " + player.getName() + " for: " + blacksmith.ownerName);
                    success.set(false);
                    return;
                }
                Ability ability = null;
                for (Ability abl : building.getAbilities())
                    if (abl.action == abilityAction)
                        ability = abl;
                if (ability instanceof EquipAbility equipAbility) {
                    if (blacksmith.autoCastEquip == equipAbility)
                        blacksmith.autoCastEquip = null;
                    else
                        blacksmith.autoCastEquip = equipAbility;
                }
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}