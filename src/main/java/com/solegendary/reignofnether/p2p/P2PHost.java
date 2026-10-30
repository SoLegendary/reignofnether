package com.solegendary.reignofnether.p2p;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.util.HttpUtil;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hosting the open singleplayer world for players elsewhere. Opens it to LAN, then:
 * - the website code: the website introduces each joiner, we punch a UDP path to them and tunnel it to our port;
 * - the direct code: our public address (UPnP-forwarded port, or IPv6) and a secret, for when the website is down.
 * Nothing goes through a relay.
 */
public final class P2PHost {

    public enum State { OFF, STARTING, HOSTING }

    public static volatile State state = State.OFF;
    @Nullable public static volatile String websiteCode, directCode;
    /** Why the website code is missing: the website's error code, or null. */
    @Nullable public static volatile String websiteError;
    public static volatile boolean isPublic;

    private static volatile IntegratedServer server;
    private static volatile String token;
    private static volatile int port;
    private static volatile byte[] secret;
    @Nullable private static volatile Upnp upnp;
    @Nullable private static volatile InetAddress upnpIp;
    private static final Set<String> handled = ConcurrentHashMap.newKeySet();
    private static final List<Rudp> links = new CopyOnWriteArrayList<>();
    private static final List<Integer> udpMapped = new CopyOnWriteArrayList<>();
    private static volatile boolean tcpMapped;
    private static boolean hookAdded;

    private P2PHost() { }

    /** On the client thread, while in our own singleplayer world. */
    public static void start(boolean listPublicly) {
        IntegratedServer s = Minecraft.getInstance().getSingleplayerServer();
        if (state != State.OFF || s == null)
            return;
        if (!s.isPublished() && !s.publishServer(s.getDefaultGameType(), false, HttpUtil.getAvailablePort()))
            return;
        s.setUsesAuthentication(true); // Mojang-checked accounts, encrypted connection
        server = s;
        port = s.getPort();
        secret = P2PApi.randomBytes(JoinCode.SECRET_BYTES);
        HandshakeSecret.required = secret;
        isPublic = listPublicly;
        websiteCode = directCode = websiteError = null;
        state = State.STARTING;
        if (!hookAdded) {
            hookAdded = true;
            Runtime.getRuntime().addShutdownHook(new Thread(P2PHost::stop)); // don't leave router ports open
        }
        Thread t = new Thread(P2PHost::run, "RoN P2P host");
        t.setDaemon(true);
        t.start();
    }

    private static void run() {
        Upnp u = Upnp.discover();
        upnp = u;
        if (u != null) {
            tcpMapped = u.map(port, "TCP");
            upnpIp = u.externalIp();
        }
        InetAddress direct = tcpMapped && upnpIp != null && !HandshakeSecret.isLocal(upnpIp) ? upnpIp : P2PApi.globalIpv6();
        if (direct != null)
            directCode = new JoinCode(direct, port, secret, JoinCode.versionHash(ReignOfNether.VERSION_STRING)).encode();
        register();
        if (state == State.STARTING)
            state = State.HOSTING;
        while (state == State.HOSTING && server.isRunning()) {
            if (token != null)
                heartbeat();
            else if (websiteError != null && !"auth_failed".equals(websiteError))
                register(); // the website was down: keep trying, the direct code works meanwhile
            sleep(token != null ? 1000 : 15000);
        }
    }

    private static void register() {
        try {
            JsonObject body = P2PApi.proof();
            body.addProperty("public", isPublic);
            body.add("meta", P2PApi.meta());
            JsonObject r = P2PApi.call("POST", "/host", body, null);
            token = r.get("token").getAsString();
            websiteCode = r.get("code").getAsString();
            websiteError = null;
        } catch (P2PApi.ApiException e) {
            websiteError = e.code;
            ReignOfNether.LOGGER.warn("P2P: no website code ({}: {})", e.code, e.getMessage());
        }
    }

    private static void heartbeat() {
        JsonObject body = new JsonObject();
        body.addProperty("players", Math.max(0, server.getPlayerCount() - 1));
        try {
            JsonObject r = P2PApi.call("POST", "/" + websiteCode + "/heartbeat", body, token);
            for (JsonElement e : r.getAsJsonArray("joins")) {
                JsonObject join = e.getAsJsonObject();
                String id = join.get("id").getAsString();
                if (handled.add(id)) {
                    Thread t = new Thread(() -> answer(id, join.get("offer").getAsString()), "RoN P2P answer");
                    t.setDaemon(true);
                    t.start();
                }
            }
        } catch (P2PApi.ApiException e) {
            if ("not_found".equals(e.code)) { // we went quiet too long and the code expired: get a new one
                token = null;
                register();
            }
        } catch (Exception ignored) {
        }
    }

    /** One joiner: tell the website where they can reach us, punch towards them, then tunnel to our LAN port. */
    private static void answer(String id, String offerJson) {
        try {
            JsonObject offer = JsonParser.parseString(offerJson).getAsJsonObject();
            byte[] key = HexFormat.of().parseHex(offer.get("k").getAsString());
            if (key.length != 8)
                return;
            DatagramSocket socket = new DatagramSocket();
            int udp = socket.getLocalPort();
            var stun = Stun.publicAddress(socket);
            Upnp u = upnp;
            boolean mapped = u != null && u.map(udp, "UDP");
            if (mapped)
                udpMapped.add(udp);
            JsonObject body = new JsonObject();
            body.addProperty("answer", P2PApi.candidatesJson(null, P2PApi.candidates(udp, stun, mapped ? upnpIp : null)));
            P2PApi.call("POST", "/join/" + id + "/answer", body, token);

            Rudp link = new Rudp(socket, key);
            if (state == State.HOSTING && link.punch(P2PApi.parseCandidates(offer), 12_000)) {
                link.bridge(new Socket(InetAddress.getLoopbackAddress(), port));
                links.add(link);
            } else {
                link.close();
            }
            if (mapped && link.isClosed()) {
                u.unmap(udp, "UDP");
                udpMapped.remove((Integer) udp);
            }
        } catch (Exception e) {
            ReignOfNether.LOGGER.warn("P2P: a join failed: {}", e.toString());
        }
    }

    public static void stop() {
        if (state == State.OFF)
            return;
        state = State.OFF;
        HandshakeSecret.required = null;
        String t = token, code = websiteCode;
        token = null;
        websiteCode = directCode = websiteError = null;
        if (t != null && code != null)
            try {
                P2PApi.call("DELETE", "/" + code, null, t);
            } catch (Exception ignored) {
            }
        links.forEach(Rudp::close);
        links.clear();
        handled.clear();
        Upnp u = upnp;
        if (u != null) {
            if (tcpMapped)
                u.unmap(port, "TCP");
            udpMapped.forEach(p -> u.unmap(p, "UDP"));
        }
        udpMapped.clear();
        tcpMapped = false;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut evt) {
        if (state != State.OFF) {
            Thread t = new Thread(P2PHost::stop, "RoN P2P stop"); // network calls, off the client thread
            t.setDaemon(true);
            t.start();
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
