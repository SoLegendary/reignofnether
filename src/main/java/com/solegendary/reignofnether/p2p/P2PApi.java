package com.solegendary.reignofnether.p2p;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.config.ReignOfNetherClientConfigs;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.versions.forge.ForgeVersion;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** The website's P2P introductions (/api/p2p), plus what both sides need: identity, mod lists, addresses. */
public final class P2PApi {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final Gson GSON = new Gson();
    static final SecureRandom RANDOM = new SecureRandom();

    private P2PApi() { }

    /** A failed call; `code` is the website's error code (not_found, auth_failed...) or "offline". */
    public static final class ApiException extends IOException {
        public final String code;

        ApiException(String code, String message) {
            super(message);
            this.code = code;
        }
    }

    public static JsonObject call(String method, String path, @Nullable JsonObject body, @Nullable String token) throws ApiException {
        String base = ReignOfNetherClientConfigs.P2P_API_URL.get().replaceAll("/+$", "");
        // the account token goes in every request, so never over plain http (except a local test site)
        if (!base.startsWith("https://") && !base.matches("http://(localhost|127\\.0\\.0\\.1)(:\\d+)?"))
            throw new ApiException("offline", "p2p_api_url must use https");
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(base + "/api/p2p" + path))
                .timeout(Duration.ofSeconds(8)).header("Accept", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(GSON.toJson(body)));
        if (body != null)
            req.header("Content-Type", "application/json");
        if (token != null)
            req.header("Authorization", "Bearer " + token);
        HttpResponse<String> r;
        try {
            r = HTTP.send(req.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ApiException("offline", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("offline", "interrupted");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(r.body()).getAsJsonObject();
        } catch (Exception e) {
            throw new ApiException(r.statusCode() >= 500 ? "offline" : "bad_response", "HTTP " + r.statusCode());
        }
        if (r.statusCode() >= 400)
            throw new ApiException(json.has("error") ? json.get("error").getAsString() : "error",
                    json.has("message") ? json.get("message").getAsString() : "HTTP " + r.statusCode());
        return json;
    }

    /**
     * Proves to the website who we are without giving it our login: we tell Mojang we're joining "server" `serverId`,
     * the website asks Mojang whether we did (like any Minecraft server does). The id is a one-time challenge from
     * the website, so an id we gave some other server can't be replayed there.
     */
    public static JsonObject proof() throws ApiException {
        User user = Minecraft.getInstance().getUser();
        String serverId = call("POST", "/challenge", null, null).get("serverId").getAsString();
        try {
            Minecraft.getInstance().getMinecraftSessionService().joinServer(user.getGameProfile(), user.getAccessToken(), serverId);
        } catch (Exception e) {
            throw new ApiException("auth_failed", e.getMessage());
        }
        JsonObject o = new JsonObject();
        o.addProperty("name", user.getName());
        o.addProperty("serverId", serverId);
        return o;
    }

    public static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RANDOM.nextBytes(b);
        return b;
    }

    // ------------------------------------------------------------------ mod lists

    /** What the host runs: {mc, forge, ron, mods: {id: version}}. */
    public static JsonObject meta() {
        JsonObject meta = new JsonObject();
        meta.addProperty("mc", SharedConstants.getCurrentVersion().getName());
        meta.addProperty("forge", ForgeVersion.getVersion());
        meta.addProperty("ron", ReignOfNether.VERSION_STRING);
        JsonObject mods = new JsonObject();
        localMods().forEach(mods::addProperty);
        meta.add("mods", mods);
        return meta;
    }

    static Map<String, String> localMods() {
        Map<String, String> mods = new TreeMap<>();
        ModList.get().getMods().forEach(m -> mods.put(m.getModId(), m.getVersion().toString()));
        return mods;
    }

    /** The difference between the host's mods and ours. `blocking` means the join can't work. */
    public record Diff(List<String> blocking, List<String> warnings) {
        public boolean isEmpty() {
            return blocking.isEmpty() && warnings.isEmpty();
        }
    }

    public static Diff diff(JsonObject hostMeta) {
        return diff(hostMeta, SharedConstants.getCurrentVersion().getName(), ForgeVersion.getVersion(), ReignOfNether.VERSION_STRING, localMods());
    }

    static Diff diff(JsonObject hostMeta, String mc, String forge, String ron, Map<String, String> mine) {
        List<String> blocking = new ArrayList<>(), warnings = new ArrayList<>();
        compare(blocking, "Reign of Nether", str(hostMeta, "ron"), ron);
        compare(blocking, "Minecraft", str(hostMeta, "mc"), mc);
        compare(blocking, "Forge", str(hostMeta, "forge"), forge);
        Map<String, String> theirs = new TreeMap<>();
        JsonElement mods = hostMeta.get("mods");
        if (mods != null && mods.isJsonObject()) // an empty list can come back as []
            mods.getAsJsonObject().entrySet().forEach(e -> theirs.put(e.getKey(), e.getValue().getAsString()));
        for (Map.Entry<String, String> e : theirs.entrySet()) {
            if (List.of("minecraft", "forge", ReignOfNether.MOD_ID).contains(e.getKey()))
                continue;
            String own = mine.get(e.getKey());
            if (own == null)
                warnings.add("p2p.reignofnether.diff.missing|" + e.getKey() + "|" + e.getValue());
            else if (!own.equals(e.getValue()))
                warnings.add("p2p.reignofnether.diff.version|" + e.getKey() + "|" + e.getValue() + "|" + own);
        }
        for (Map.Entry<String, String> e : mine.entrySet())
            if (!theirs.isEmpty() && !theirs.containsKey(e.getKey()))
                warnings.add("p2p.reignofnether.diff.extra|" + e.getKey() + "|" + e.getValue());
        return new Diff(blocking, warnings);
    }

    private static void compare(List<String> into, String what, String theirs, String mine) {
        if (!theirs.equals(mine))
            into.add("p2p.reignofnether.diff.blocking|" + what + "|" + theirs + "|" + mine);
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "?";
    }

    // ------------------------------------------------------------------ addresses

    /** Where the other player can try to reach this UDP port: our LAN and IPv6 addresses, then public ones. */
    public static List<InetSocketAddress> candidates(int port, @Nullable InetSocketAddress stun, @Nullable InetAddress upnpIp) {
        List<InetSocketAddress> out = new ArrayList<>();
        if (upnpIp != null)
            out.add(new InetSocketAddress(upnpIp, port));
        if (stun != null && !out.contains(stun))
            out.add(stun);
        for (InetAddress a : localAddresses())
            out.add(new InetSocketAddress(a, port));
        return out;
    }

    static List<InetAddress> localAddresses() {
        List<InetAddress> out = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual())
                    continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses()))
                    if ((a instanceof Inet4Address && a.isSiteLocalAddress())
                            || (a instanceof Inet6Address && !a.isLinkLocalAddress() && !a.isLoopbackAddress()))
                        out.add(a);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /** A public IPv6 address of this computer, if it has one. */
    @Nullable
    static InetAddress globalIpv6() {
        for (InetAddress a : localAddresses())
            if (a instanceof Inet6Address && !HandshakeSecret.isLocal(a))
                return a;
        return null;
    }

    public static String candidatesJson(byte[] key, List<InetSocketAddress> candidates) {
        JsonObject o = new JsonObject();
        if (key != null)
            o.addProperty("k", HexFormat.of().formatHex(key));
        JsonArray c = new JsonArray();
        for (InetSocketAddress a : candidates)
            c.add(a.getAddress().getHostAddress() + "|" + a.getPort());
        o.add("c", c);
        return GSON.toJson(o);
    }

    public static List<InetSocketAddress> parseCandidates(JsonObject o) {
        List<InetSocketAddress> out = new ArrayList<>();
        if (o.has("c") && o.get("c").isJsonArray())
            for (JsonElement e : o.getAsJsonArray("c")) {
                String[] parts = e.getAsString().split("\\|");
                // literal IPs only: never let the other side make us look up a host name
                if (parts.length == 2 && (parts[0].matches("\\d{1,3}(\\.\\d{1,3}){3}") || parts[0].matches("[0-9a-fA-F:]*:[0-9a-fA-F:.]*"))
                        && out.size() < 16)
                    try {
                        out.add(new InetSocketAddress(InetAddress.getByName(parts[0]), Integer.parseInt(parts[1])));
                    } catch (Exception ignored) {
                    }
            }
        return out;
    }
}
