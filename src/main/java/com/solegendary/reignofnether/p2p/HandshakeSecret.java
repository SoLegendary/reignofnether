package com.solegendary.reignofnether.p2p;

import javax.annotation.Nullable;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * The secret from a direct join code rides in the handshake's host name ("host#ron<hex>"), so a P2P host only
 * lets in internet players who have the code. Port scanners that find the open port get turned away.
 * Players on the same network (and our own website-path tunnel, which arrives from localhost) need no secret.
 */
public final class HandshakeSecret {

    public static final String MARKER = "#ron";

    /** Set by a hosting player: what joiners from the internet must send. */
    @Nullable public static volatile byte[] required;
    /** Set by a joining player right before connecting; used once. */
    @Nullable public static volatile byte[] toSend;

    private HandshakeSecret() { }

    public static String addTo(String hostName) {
        byte[] s = toSend;
        toSend = null;
        return s == null ? hostName : hostName + MARKER + HexFormat.of().formatHex(s);
    }

    public static boolean rejects(SocketAddress from, String hostName) {
        byte[] need = required;
        if (need == null || !(from instanceof InetSocketAddress isa) || isLocal(isa.getAddress()))
            return false;
        int i = hostName.lastIndexOf(MARKER);
        if (i < 0)
            return true;
        try {
            return !MessageDigest.isEqual(need, HexFormat.of().parseHex(hostName.substring(i + MARKER.length())));
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    static boolean isLocal(@Nullable InetAddress a) {
        return a == null || a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress()
                || (a instanceof Inet6Address && (a.getAddress()[0] & 0xFE) == 0xFC); // IPv6 unique local
    }
}
