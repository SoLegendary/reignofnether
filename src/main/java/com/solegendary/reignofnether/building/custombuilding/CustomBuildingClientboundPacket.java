package com.solegendary.reignofnether.building.custombuilding;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class CustomBuildingClientboundPacket {

    // pos is used to identify the building object serverside
    public String playerName;
    public String name;
    public BlockPos structureSize;
    public CompoundTag structureNbt;
    public CompoundTag attributesNbt;

    public static void registerCustomBuilding(CustomBuilding building) {
        registerCustomBuilding("", building);
    }

    public static void registerCustomBuilding(String playerName, CustomBuilding building) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new CustomBuildingClientboundPacket(
                playerName, building.name, new BlockPos(building.structureSize), building.structureNbt, building.attributesNbt
        ));
    }

    public CustomBuildingClientboundPacket(
            String playerName,
            String name,
            BlockPos structureSize,
            CompoundTag structureNbt,
            CompoundTag attributesNbt
    ) {
        this.playerName = playerName;
        this.name = name;
        this.structureSize = structureSize;
        this.structureNbt = structureNbt;
        this.attributesNbt = attributesNbt;
    }

    private static void writeCompressedNbt(FriendlyByteBuf buffer, CompoundTag tag) {
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

    private static CompoundTag readCompressedNbt(FriendlyByteBuf buffer) {
        try {
            int len = buffer.readInt();
            if (len <= 0 || len > 10_000_000) {
                buffer.skipBytes(Math.max(len, 0));
                return new CompoundTag();
            }
            byte[] data = buffer.readByteArray(len);
            return NbtIo.readCompressed(new ByteArrayInputStream(data));
        } catch (Exception e) {
            return new CompoundTag();
        }
    }

    public CustomBuildingClientboundPacket(FriendlyByteBuf buffer) {
        this.playerName = buffer.readUtf();
        this.name = buffer.readUtf();
        this.structureSize = buffer.readBlockPos();
        this.structureNbt = readCompressedNbt(buffer);
        this.attributesNbt = buffer.readNbt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.playerName);
        buffer.writeUtf(this.name);
        buffer.writeBlockPos(this.structureSize);
        writeCompressedNbt(buffer, this.structureNbt);
        buffer.writeNbt(this.attributesNbt);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                CustomBuildingClientEvents.registerCustomBuilding(
                        playerName, name, structureSize, structureNbt, attributesNbt
                );
                success.set(true);
            });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
