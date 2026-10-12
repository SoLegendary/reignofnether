package com.solegendary.reignofnether.building.custombuilding;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.function.Supplier;

public class CustomBuildingClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<CustomBuildingClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "custom_building_clientbound"));

    @Override
    public CustomPacketPayload.Type<CustomBuildingClientboundPacket> type() {
        return TYPE;
    }

    // pos is used to identify the building object serverside
    public String playerName;
    public String name;
    public BlockPos structureSize;
    public CompoundTag structureNbt;
    public CompoundTag attributesNbt;
    public CompoundTag commandsNbt;

    public static void registerCustomBuilding(CustomBuilding building) {
        registerCustomBuilding("", building);
    }

    public static void registerCustomBuilding(String playerName, CustomBuilding building) {
        CompoundTag commandsTag = new CompoundTag();
        commandsTag.put("commands", building.commandsNbt);
        PacketHandler.send(PacketHandler.allPlayers(), new CustomBuildingClientboundPacket(
                playerName, building.name, new BlockPos(building.structureSize), building.structureNbt, building.attributesNbt, commandsTag
        ));
    }

    public CustomBuildingClientboundPacket(
            String playerName,
            String name,
            BlockPos structureSize,
            CompoundTag structureNbt,
            CompoundTag attributesNbt,
            CompoundTag commandsNbt
    ) {
        this.playerName = playerName;
        this.name = name;
        this.structureSize = structureSize;
        this.structureNbt = structureNbt;
        this.attributesNbt = attributesNbt;
        this.commandsNbt = commandsNbt;
    }

    private static void writeCompressedNbt(RegistryFriendlyByteBuf buffer, CompoundTag tag) {
        try {
            if (tag == null)
                tag = new CompoundTag();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, baos);
            byte[] bytes = baos.toByteArray();
            buffer.writeInt(bytes.length);
            buffer.writeByteArray(bytes);
        } catch (Exception e) {
            buffer.writeInt(0);
        }
    }

    private static CompoundTag readCompressedNbt(RegistryFriendlyByteBuf buffer) {
        try {
            int len = buffer.readInt();
            if (len <= 0 || len > 10_000_000) {
                buffer.skipBytes(Math.max(len, 0));
                return new CompoundTag();
            }
            byte[] data = buffer.readByteArray(len);
            return NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap());
        } catch (Exception e) {
            return new CompoundTag();
        }
    }

    public CustomBuildingClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.playerName = buffer.readUtf();
        this.name = buffer.readUtf();
        this.structureSize = buffer.readBlockPos();
        this.structureNbt = readCompressedNbt(buffer);
        this.attributesNbt = buffer.readNbt();
        this.commandsNbt = buffer.readNbt();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(this.playerName);
        buffer.writeUtf(this.name);
        buffer.writeBlockPos(this.structureSize);
        writeCompressedNbt(buffer, this.structureNbt);
        buffer.writeNbt(this.attributesNbt);
        buffer.writeNbt(this.commandsNbt);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                CustomBuildingClientEvents.registerCustomBuilding(
                        playerName, name, structureSize, structureNbt, attributesNbt, commandsNbt.getList("commands", Tag.TAG_COMPOUND)
                );
            });
        });
        return;
    }
}
