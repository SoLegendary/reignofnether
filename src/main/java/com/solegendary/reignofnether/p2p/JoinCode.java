package com.solegendary.reignofnether.p2p;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;

/**
 * A direct join code, for when the website is down: "D-" + base32 of
 * [4 or 6][ip][port 2][secret 5][RoN version hash 2]. It holds the host's public address, so it shows their IP.
 * Website codes are the 8 letters the website hands out.
 */
public record JoinCode(InetAddress ip, int port, byte[] secret, int versionHash) {

    public static final int SECRET_BYTES = 5;
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTVWXYZ0123456789"; // no I, L, O, U
    private static final String PREFIX = "D-";

    public static boolean isDirect(String code) {
        return code.trim().toUpperCase(Locale.ROOT).startsWith(PREFIX);
    }

    /** A website code: 8 letters or digits. */
    public static boolean isWebsite(String code) {
        return code.trim().matches("[A-Za-z0-9]{8}");
    }

    public static int versionHash(String version) {
        return version.hashCode() & 0xFFFF;
    }

    public String encode() {
        byte[] addr = ip.getAddress();
        ByteBuffer b = ByteBuffer.allocate(1 + addr.length + 2 + SECRET_BYTES + 2);
        b.put((byte) (addr.length == 4 ? 4 : 6)).put(addr).putShort((short) port).put(secret).putShort((short) versionHash);
        return PREFIX + base32(b.array());
    }

    /** Null when the code is not a valid direct code. */
    public static JoinCode decode(String code) {
        String s = code.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        if (!s.startsWith(PREFIX))
            return null;
        byte[] bytes = unbase32(s.substring(PREFIX.length()));
        if (bytes == null || bytes.length < 1)
            return null;
        int ipLen = bytes[0] == 4 ? 4 : bytes[0] == 6 ? 16 : -1;
        if (ipLen < 0 || bytes.length != 1 + ipLen + 2 + SECRET_BYTES + 2)
            return null;
        try {
            ByteBuffer b = ByteBuffer.wrap(bytes, 1, bytes.length - 1);
            byte[] addr = new byte[ipLen];
            b.get(addr);
            int port = b.getShort() & 0xFFFF;
            byte[] secret = new byte[SECRET_BYTES];
            b.get(secret);
            return new JoinCode(InetAddress.getByAddress(addr), port, secret, b.getShort() & 0xFFFF);
        } catch (Exception e) {
            return null;
        }
    }

    static String base32(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte d : data) {
            buffer = (buffer << 8) | (d & 0xFF);
            bits += 8;
            while (bits >= 5) {
                sb.append(ALPHABET.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0)
            sb.append(ALPHABET.charAt((buffer << (5 - bits)) & 31));
        return sb.toString();
    }

    static byte[] unbase32(String s) {
        ByteBuffer out = ByteBuffer.allocate(s.length() * 5 / 8);
        int buffer = 0, bits = 0;
        for (char c : s.toCharArray()) {
            int v = ALPHABET.indexOf(c);
            if (v < 0)
                return null;
            buffer = (buffer << 5) | v;
            bits += 5;
            if (bits >= 8) {
                out.put((byte) (buffer >> (bits - 8)));
                bits -= 8;
            }
        }
        return Arrays.copyOf(out.array(), out.position());
    }
}
