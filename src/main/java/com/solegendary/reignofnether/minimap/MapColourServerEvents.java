package com.solegendary.reignofnether.minimap;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.loading.LoadProgress;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.util.WorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;

/**
 * Draws the whole map of an RTS-optimised world once at load and sends it to each player as they join, so their
 * minimap shows the whole play area from the start, even chunks their client has never had. Their client then keeps
 * it up to date from the chunks it receives.
 */
public final class MapColourServerEvents {

    private MapColourServerEvents() { }

    public static final int STRIP = 16; // rows per packet: 1280 wide is ~60 KB raw, well under the packet limit

    private static MapGrid grid = null;
    private static List<byte[]> strips = List.of(); // compressed once, sent to every player
    static {
        WorldState.onServerReset(() -> {
            grid = null;
            strips = List.of();
        });
    }

    /** The square the match setup map shows: the border, centred, as wide as its longest side. */
    public static MapGrid gridFor(WorldBorder border) {
        int size = (int) Math.ceil(Math.max(border.getMaxX() - border.getMinX(), border.getMaxZ() - border.getMinZ()));
        int minX = (int) Math.floor((border.getMinX() + border.getMaxX()) / 2 - size / 2.0);
        int minZ = (int) Math.floor((border.getMinZ() + border.getMaxZ()) / 2 - size / 2.0);
        return new MapGrid(minX, minZ, size);
    }

    /** Loads every chunk in the border, which the navmesh and fog snapshot then reuse. On the server thread, at load. */
    public static void build(ServerLevel level) {
        MapGrid g = gridFor(level.getWorldBorder());
        int cxMin = g.minX >> 4, cxMax = (g.minX + g.size - 1) >> 4;
        int czMin = g.minZ >> 4, czMax = (g.minZ + g.size - 1) >> 4;
        LoadProgress.map = g;
        LoadProgress.begin(LoadProgress.Step.MAP, (cxMax - cxMin + 1) * (czMax - czMin + 1));
        long startMs = System.currentTimeMillis();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int cz = czMin; cz <= czMax; cz++) {
            for (int cx = cxMin; cx <= cxMax; cx++) {
                level.getChunk(cx, cz);
                LoadProgress.done++;
            }
            int zFrom = Math.max(g.minZ, cz << 4), zTo = Math.min(g.minZ + g.size, (cz + 1) << 4);
            for (int z = zFrom; z < zTo; z++)
                for (int x = g.minX; x < g.minX + g.size; x++) // the top row has nothing loaded north of it to compare
                    g.set(x, z, MapColours.column(level, x, z, z == g.minZ ? z : z - 1, pos));
            g.rows = zTo - g.minZ;
        }
        grid = g;
        strips = compress(g);
        ReignOfNether.LOGGER.info("Drew the RTS map: {}x{} in {} ms", g.size, g.size, System.currentTimeMillis() - startMs);
    }

    private static List<byte[]> compress(MapGrid g) {
        List<byte[]> out = new ArrayList<>();
        for (int row = 0; row < g.size; row += STRIP) {
            int rows = Math.min(STRIP, g.size - row);
            byte[] raw = new byte[rows * g.size * 3];
            for (int i = 0; i < rows * g.size; i++) {
                int c = g.rgb[row * g.size + i];
                raw[i * 3] = (byte) (c >> 16);
                raw[i * 3 + 1] = (byte) (c >> 8);
                raw[i * 3 + 2] = (byte) c;
            }
            Deflater deflater = new Deflater(Deflater.BEST_SPEED);
            deflater.setInput(raw);
            deflater.finish();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(raw.length / 4);
            byte[] buf = new byte[8192];
            while (!deflater.finished())
                bytes.write(buf, 0, deflater.deflate(buf));
            deflater.end();
            out.add(bytes.toByteArray());
        }
        return out;
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (grid == null || !(evt.getEntity() instanceof ServerPlayer sp))
            return;
        // the picture is the world at load: in a resumed match with fog that could show enemy bases, so it waits
        // ponytail: those players get only the chunks they receive; draw it from the fog snapshot if that matters
        if (FogOfWarServerEvents.isEnabled() && PlayerServerEvents.isGameActive())
            return;
        for (int i = 0; i < strips.size(); i++)
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp),
                    new MapColoursClientboundPacket(grid.minX, grid.minZ, grid.size, i * STRIP, strips.get(i)));
    }

    // statics outlive the world: clear them so the next world opened in this game starts clean
}
