package com.solegendary.reignofnether.units;

import com.solegendary.reignofnether.cursor.CursorClientVanillaEvents;
import com.solegendary.reignofnether.gui.TopdownGuiServerVanillaEvents;
import com.solegendary.reignofnether.gui.TopdownGuiServerboundPackets;
import com.solegendary.reignofnether.orthoview.OrthoviewClientVanillaEvents;
import com.solegendary.reignofnether.registrars.Keybinds;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class UnitServerboundPackets {

    private boolean stopCommand;
    private int unitIdToAttack;
    private int unitIdToFollow;
    private int[] unitIdsToMove;
    private int[] unitIdsToAttackMove;
    private int[] preselectedUnitIds;
    private int[] selectedUnitIds;

    // packet-handler functions
    public UnitServerboundPackets(
            boolean stopCommand,
            int unitIdToAttack,
            int unitIdToFollow,
            int[] unitIdsToMove,
            int[] unitIdsToAttackMove,
            int[] preselectedUnitIds,
            int[] selectedUnitIds
    ) {
        this.stopCommand = stopCommand;
        this.unitIdToAttack = unitIdToAttack;
        this.unitIdToFollow = unitIdToFollow;
        this.unitIdsToMove = unitIdsToMove;
        this.unitIdsToAttackMove = unitIdsToAttackMove;
        this.preselectedUnitIds = preselectedUnitIds;
        this.selectedUnitIds = selectedUnitIds;
    }

    public UnitServerboundPackets(FriendlyByteBuf buffer) {
        this.stopCommand = buffer.readBoolean();
        this.unitIdToAttack = buffer.readInt();
        this.unitIdToFollow = buffer.readInt();
        this.unitIdsToMove = buffer.readVarIntArray();
        this.unitIdsToAttackMove = buffer.readVarIntArray();
        this.preselectedUnitIds = buffer.readVarIntArray();
        this.selectedUnitIds = buffer.readVarIntArray();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.stopCommand);
        buffer.writeInt(this.unitIdToAttack);
        buffer.writeInt(this.unitIdToFollow);
        buffer.writeVarIntArray(this.unitIdsToMove);
        buffer.writeVarIntArray(this.unitIdsToAttackMove);
        buffer.writeVarIntArray(this.preselectedUnitIds);
        buffer.writeVarIntArray(this.selectedUnitIds);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            System.out.println("Got Unit packet from client!");

            UnitServerVanillaEvents.consumeUnitActionQueues(
                    this.stopCommand,
                    this.unitIdToAttack,
                    this.unitIdToFollow,
                    this.unitIdsToMove,
                    this.unitIdsToAttackMove,
                    this.preselectedUnitIds,
                    this.selectedUnitIds
            );
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
