package com.solegendary.reignofnether.p2p;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

import javax.annotation.Nullable;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;

/**
 * Joining a P2P game by code. A website code: check the host's mods, get introduced, punch a UDP path, then
 * Minecraft connects to a local port that tunnels through it. A direct code: connect straight to the address in it.
 */
public final class P2PJoin {

    public enum Step { IDLE, LOOKING_UP, CONFIRM, CONNECTING, FAILED }

    public static volatile Step step = Step.IDLE;
    /** A lang key (with "|"-separated arguments) for the status line. */
    public static volatile String message = "";
    @Nullable public static volatile P2PApi.Diff diff;
    @Nullable public static volatile String hostName;

    private static volatile String code;
    private static volatile int attempt; // a newer attempt or a cancel makes older threads stop quietly
    private static Screen parent;

    private P2PJoin() { }

    public static void join(String rawCode, Screen parentScreen) {
        int me = ++attempt;
        parent = parentScreen;
        HandshakeSecret.toSend = null; // a failed direct join must not leak its secret to the next server
        diff = null;
        hostName = null;
        code = rawCode.trim().toUpperCase(Locale.ROOT);
        if (JoinCode.isDirect(code)) {
            JoinCode d = JoinCode.decode(code);
            if (d == null)
                fail("p2p.reignofnether.error.bad_code");
            else if (d.versionHash() != JoinCode.versionHash(ReignOfNether.VERSION_STRING))
                fail("p2p.reignofnether.error.direct_version|" + ReignOfNether.VERSION_STRING);
            else {
                step = Step.CONNECTING;
                message = "p2p.reignofnether.status.connecting";
                HandshakeSecret.toSend = d.secret();
                connect(d.ip().getHostAddress(), d.port());
            }
        } else if (code.matches("[A-Z0-9]{6}")) {
            fail("p2p.reignofnether.error.network_code");
        } else if (JoinCode.isWebsite(code)) {
            step = Step.LOOKING_UP;
            message = "p2p.reignofnether.status.looking_up";
            background(me, P2PJoin::lookUp);
        } else {
            fail("p2p.reignofnether.error.bad_code");
        }
    }

    /** The player saw the mod differences and joins anyway. */
    public static void confirm() {
        if (step == Step.CONFIRM) {
            step = Step.CONNECTING;
            background(attempt, P2PJoin::tunnel);
        }
    }

    public static void cancel() {
        attempt++;
        step = Step.IDLE;
        message = "";
    }

    private static void lookUp(int me) throws Exception {
        JsonObject game;
        try {
            game = P2PApi.call("GET", "/" + code, null, null);
        } catch (P2PApi.ApiException e) {
            if (current(me))
                fail(switch (e.code) {
                    case "not_found" -> "p2p.reignofnether.error.not_found";
                    case "rate_limited" -> "p2p.reignofnether.error.rate_limited";
                    default -> "p2p.reignofnether.error.website_down";
                });
            return;
        }
        if (!current(me))
            return;
        if (!"p2p".equals(game.get("type").getAsString())) {
            fail("p2p.reignofnether.error.network_code");
            return;
        }
        hostName = game.has("hostName") ? game.get("hostName").getAsString() : null;
        P2PApi.Diff d = P2PApi.diff(game.getAsJsonObject("meta"));
        diff = d;
        if (!d.blocking().isEmpty()) {
            fail("p2p.reignofnether.error.mismatch");
        } else if (!d.warnings().isEmpty()) {
            step = Step.CONFIRM;
            message = "p2p.reignofnether.status.mods_differ";
        } else {
            step = Step.CONNECTING;
            tunnel(me);
        }
    }

    private static void tunnel(int me) throws Exception {
        message = "p2p.reignofnether.status.finding_path";
        byte[] key = P2PApi.randomBytes(8);
        DatagramSocket socket = new DatagramSocket();
        int udp = socket.getLocalPort();
        InetSocketAddress stun = Stun.publicAddress(socket);
        Upnp u = Upnp.discover();
        InetAddress upnpIp = u != null && u.map(udp, "UDP") ? u.externalIp() : null;
        Rudp link = new Rudp(socket, key);
        boolean bridged = false;
        try {
            JsonObject body = P2PApi.proof();
            body.addProperty("offer", P2PApi.candidatesJson(key, P2PApi.candidates(udp, stun, upnpIp)));
            JsonObject j = P2PApi.call("POST", "/" + code + "/join", body, null);
            String id = j.get("id").getAsString(), token = j.get("token").getAsString();

            message = "p2p.reignofnether.status.waiting_host";
            String answer = null;
            for (int i = 0; i < 20 && answer == null && current(me); i++) {
                Thread.sleep(1000);
                var r = P2PApi.call("GET", "/join/" + id, null, token).get("answer");
                answer = r == null || r.isJsonNull() ? null : r.getAsString();
            }
            if (!current(me))
                return;
            if (answer == null) {
                fail("p2p.reignofnether.error.host_silent");
                return;
            }
            message = "p2p.reignofnether.status.punching";
            if (!link.punch(P2PApi.parseCandidates(JsonParser.parseString(answer).getAsJsonObject()), 12_000)) {
                if (current(me))
                    fail("p2p.reignofnether.error.no_path");
                return;
            }
            if (!current(me))
                return;
            // Minecraft connects to this local port; what it sends goes through the link
            try (ServerSocket local = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
                local.setSoTimeout(20_000);
                message = "p2p.reignofnether.status.connecting";
                connect(InetAddress.getLoopbackAddress().getHostAddress(), local.getLocalPort());
                Socket mc = local.accept();
                link.bridge(mc);
                bridged = true;
            }
        } catch (P2PApi.ApiException e) {
            if (current(me))
                fail(switch (e.code) {
                    case "not_found" -> "p2p.reignofnether.error.not_found";
                    case "auth_failed" -> "p2p.reignofnether.error.auth";
                    case "rate_limited" -> "p2p.reignofnether.error.rate_limited";
                    default -> "p2p.reignofnether.error.website_down";
                });
        } finally {
            if (!bridged)
                link.close();
            if (upnpIp != null)
                new Thread(() -> { // the router port is only needed for the punch; the path stays open without it
                    try {
                        Thread.sleep(30_000);
                    } catch (InterruptedException ignored) {
                    }
                    u.unmap(udp, "UDP");
                }).start();
        }
    }

    private static void connect(String host, int port) {
        Minecraft mc = Minecraft.getInstance();
        String name = hostName != null ? hostName : "Reign of Nether";
        mc.execute(() -> {
            step = Step.IDLE;
            ConnectScreen.startConnecting(parent, mc, new ServerAddress(host, port), new ServerData(name, host + ":" + port, false), false);
        });
    }

    private interface Job {
        void run(int attempt) throws Exception;
    }

    private static void background(int me, Job job) {
        Thread t = new Thread(() -> {
            try {
                job.run(me);
            } catch (Exception e) {
                ReignOfNether.LOGGER.warn("P2P join failed: {}", e.toString());
                if (current(me))
                    fail("p2p.reignofnether.error.no_path");
            }
        }, "RoN P2P join");
        t.setDaemon(true);
        t.start();
    }

    private static boolean current(int me) {
        return me == attempt;
    }

    private static void fail(String key) {
        step = Step.FAILED;
        message = key;
    }
}
