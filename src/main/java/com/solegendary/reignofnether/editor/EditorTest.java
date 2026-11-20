package com.solegendary.reignofnether.editor;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.util.WorldState;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Test mode, the top bar's ▶ Test, like Unity's Play button: plays a real match on the map as it is now, and
 * ■ Stop puts the world back exactly as it was before. While isTesting() the editor panes give way to the RTS HUD.
 *
 * How: Test saves the world, copies its folder to <game dir>/ron-test/<world> and starts the match in place
 * (EditorTestServer). Stop closes the world, copies that back over it and opens it again. So everything reverts:
 * blocks (chopped trees, mined ore, capitols, battle damage), entities, every save file and ron/.
 * Chosen over recording the units, buildings and resources and respawning them on Stop: that can't undo block
 * changes, and workers change blocks within seconds of the start.
 *
 * Limits:
 * - only on a world this client hosts (singleplayer, or a LAN/P2P host): a dedicated server can't reopen its world,
 *   so Test is refused there. A host's guests are disconnected by Stop's reload.
 * - Stop takes a world reload (a few seconds).
 * - only what's saved is tested: the editor saves map.json and its other files before calling start().
 * - quitting or crashing mid-test leaves the copy behind: the world still counts as testing when reopened, and Stop
 *   puts it back then.
 */
public final class EditorTest {

    private static final Minecraft MC = Minecraft.getInstance();

    private EditorTest() { }

    private static Boolean testing = null; // null: not looked up yet for this world
    static {
        WorldState.onClientReset(() -> testing = null);
    }
    static boolean restored = false; // Stop's reload is done: EditorTestBar says so once the world is open

    /** Save first: the test plays the world as it is on disk. Faction: the editor's, unless the seat fixes one. */
    public static void start() {
        if (!isTesting())
            EditorTestServerboundPacket.start(SandboxClientEvents.getFaction());
    }

    public static boolean isTesting() {
        if (testing == null) {
            IntegratedServer server = MC.getSingleplayerServer();
            if (server == null)
                return false; // only the host can test
            testing = Files.isDirectory(EditorTestServer.snapshot(EditorTestServer.world(server)));
        }
        return testing;
    }

    static void setTesting() {
        testing = true;
    }

    /**
     * Closes the world, puts it back as it was before Test and reopens it. Runs on the host's client alone: nothing
     * the server holds survives the reload, and only the host's client has the world (getSingleplayerServer).
     */
    public static void stop() {
        MC.tell(EditorTest::reload); // not inside the click that asked: the HUD is still using the world
    }

    private static void reload() {
        IntegratedServer server = MC.getSingleplayerServer();
        if (server == null || MC.level == null || !isTesting())
            return;
        Path world = EditorTestServer.world(server);
        Path snapshot = EditorTestServer.snapshot(world);
        String id = world.getFileName().toString();
        MC.level.disconnect();
        MC.clearLevel(new GenericDirtMessageScreen(Component.translatable("editor.reignofnether.test.restoring")));
        CompletableFuture.runAsync(() -> {
            try {
                EditorTestServer.restore(world, snapshot);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, Util.backgroundExecutor()).whenCompleteAsync((v, ex) -> {
            if (ex != null) {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                ReignOfNether.LOGGER.error("[EditorTest] could not put {} back from {}", world, snapshot, cause);
                MC.setScreen(new AlertScreen(() -> MC.setScreen(new TitleScreen()),
                        Component.translatable("editor.reignofnether.test.restore_failed"),
                        Component.translatable("editor.reignofnether.test.restore_failed_detail", cause.getMessage(), snapshot.toString())));
                return;
            }
            restored = true;
            MC.createWorldOpenFlows().loadLevel(new TitleScreen(), id);
        }, MC);
    }
}
