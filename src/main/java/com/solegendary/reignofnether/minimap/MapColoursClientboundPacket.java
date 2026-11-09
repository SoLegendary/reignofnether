package com.solegendary.reignofnether.minimap;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/** A strip of the map the server drew at load: deflated RGB, three bytes a column. */
public class MapColoursClientboundPacket {
    private final int minX;
    private final int minZ;
    private final int size;
    private final int row;
    private final byte[] data;

    public MapColoursClientboundPacket(int minX, int minZ, int size, int row, byte[] data) {
        this.minX = minX;
        this.minZ = minZ;
        this.size = size;
        this.row = row;
        this.data = data;
    }

    public MapColoursClientboundPacket(FriendlyByteBuf buffer) {
        this.minX = buffer.readInt();
        this.minZ = buffer.readInt();
        this.size = buffer.readVarInt();
        this.row = buffer.readVarInt();
        this.data = buffer.readByteArray();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.minX);
        buffer.writeInt(this.minZ);
        buffer.writeVarInt(this.size);
        buffer.writeVarInt(this.row);
        buffer.writeByteArray(this.data);
    }

    /** The strip's colours, or null if it doesn't fit the map it claims to be part of. */
    private int[] decode() {
        if (size <= 0 || size > 4096 || row < 0 || row >= size)
            return null;
        int rows = Math.min(MapColourServerEvents.STRIP, size - row);
        byte[] raw = new byte[rows * size * 3];
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(data);
            int n = 0;
            while (n < raw.length && !inflater.finished()) {
                int got = inflater.inflate(raw, n, raw.length - n);
                if (got == 0 && (inflater.needsInput() || inflater.needsDictionary()))
                    break;
                n += got;
            }
            if (n != raw.length)
                return null;
        } catch (DataFormatException e) {
            return null;
        } finally {
            inflater.end();
        }
        int[] rgb = new int[rows * size];
        for (int i = 0; i < rgb.length; i++)
            rgb[i] = (raw[i * 3] & 0xFF) << 16 | (raw[i * 3 + 1] & 0xFF) << 8 | raw[i * 3 + 2] & 0xFF;
        return rgb;
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            int[] rgb = decode();
            if (rgb != null)
                MinimapClientEvents.receiveMapStrip(minX, minZ, size, row, rgb);
        }));
        ctx.get().setPacketHandled(true);
    }
}
