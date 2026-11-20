package com.solegendary.reignofnether.p2p;

import javax.annotation.Nullable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * One STUN binding request (RFC 5389): asks a public STUN server which address and port the router gave this
 * UDP socket, so the other player can aim at it. Uses the socket before anything else reads from it.
 */
public final class Stun {

    private static final String[] SERVERS = { "stun.l.google.com:19302", "stun.cloudflare.com:3478", "stun1.l.google.com:19302" };
    private static final int MAGIC = 0x2112A442;

    private Stun() { }

    @Nullable
    public static InetSocketAddress publicAddress(DatagramSocket socket) {
        int timeout;
        try {
            timeout = socket.getSoTimeout();
        } catch (Exception e) {
            return null;
        }
        try {
            socket.setSoTimeout(800);
            for (String server : SERVERS) {
                String[] hp = server.split(":");
                InetAddress ip = ipv4(hp[0]);
                if (ip == null)
                    continue;
                byte[] txn = new byte[12];
                new SecureRandom().nextBytes(txn);
                byte[] req = ByteBuffer.allocate(20).putShort((short) 1).putShort((short) 0).putInt(MAGIC).put(txn).array();
                for (int attempt = 0; attempt < 2; attempt++) {
                    socket.send(new DatagramPacket(req, req.length, ip, Integer.parseInt(hp[1])));
                    byte[] buf = new byte[512];
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    try {
                        socket.receive(p);
                    } catch (java.net.SocketTimeoutException e) {
                        continue;
                    }
                    InetSocketAddress mapped = parse(buf, p.getLength(), txn);
                    if (mapped != null)
                        return mapped;
                }
            }
        } catch (Exception ignored) {
        } finally {
            try {
                socket.setSoTimeout(timeout);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @Nullable
    private static InetAddress ipv4(String host) {
        try {
            for (InetAddress a : InetAddress.getAllByName(host))
                if (a instanceof Inet4Address)
                    return a;
        } catch (Exception ignored) {
        }
        return null;
    }

    @Nullable
    static InetSocketAddress parse(byte[] buf, int len, byte[] txn) {
        ByteBuffer b = ByteBuffer.wrap(buf, 0, len);
        if (len < 20 || b.getShort() != 0x0101)
            return null;
        int bodyLen = b.getShort() & 0xFFFF;
        if (b.getInt() != MAGIC || !Arrays.equals(Arrays.copyOfRange(buf, 8, 20), txn) || 20 + bodyLen > len)
            return null;
        b.position(20);
        while (b.remaining() >= 4) {
            int type = b.getShort() & 0xFFFF, alen = b.getShort() & 0xFFFF;
            if (alen > b.remaining())
                return null;
            int start = b.position();
            if ((type == 0x0020 || type == 0x0001) && alen >= 8 && buf[start + 1] == 1) { // IPv4 only
                int port = b.getShort(start + 2) & 0xFFFF;
                byte[] ip = Arrays.copyOfRange(buf, start + 4, start + 8);
                if (type == 0x0020) { // XOR-MAPPED-ADDRESS
                    port ^= MAGIC >>> 16;
                    for (int i = 0; i < 4; i++)
                        ip[i] ^= (byte) (MAGIC >>> (24 - 8 * i));
                }
                try {
                    return new InetSocketAddress(InetAddress.getByAddress(ip), port);
                } catch (Exception e) {
                    return null;
                }
            }
            b.position(start + ((alen + 3) & ~3));
        }
        return null;
    }
}
