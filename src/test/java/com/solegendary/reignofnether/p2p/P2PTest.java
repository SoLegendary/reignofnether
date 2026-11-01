package com.solegendary.reignofnether.p2p;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class P2PTest {

    @Test
    void directCodesRoundTrip() throws Exception {
        byte[] secret = { 1, 2, 3, (byte) 250, 5 };
        for (String ip : new String[]{ "203.0.113.7", "2001:db8::1234" }) {
            JoinCode c = new JoinCode(InetAddress.getByName(ip), 25565, secret, JoinCode.versionHash("1.5.0b"));
            String enc = c.encode();
            JoinCode d = JoinCode.decode(enc.toLowerCase(Locale.ROOT));
            assertNotNull(d, enc);
            assertEquals(c.ip(), d.ip());
            assertEquals(25565, d.port());
            assertArrayEquals(secret, d.secret());
            assertEquals(c.versionHash(), d.versionHash());
            assertTrue(JoinCode.isDirect(enc));
            assertFalse(JoinCode.isWebsite(enc));
        }
        assertNull(JoinCode.decode("D-IIII"));
        assertNull(JoinCode.decode("K7QM4XPA"));
        assertTrue(JoinCode.isWebsite("K7QM4XPA"));
    }

    /** 2 MB each way through a relay that drops 10% of packets and reorders some: arrives whole and in order. */
    @Test
    void streamSurvivesLossAndReordering() throws Exception {
        byte[] key = "ronkey!!".getBytes();
        DatagramSocket a = new DatagramSocket(0, InetAddress.getLoopbackAddress());
        DatagramSocket b = new DatagramSocket(0, InetAddress.getLoopbackAddress());
        DatagramSocket toA = new DatagramSocket(0, InetAddress.getLoopbackAddress()); // B talks to this
        DatagramSocket toB = new DatagramSocket(0, InetAddress.getLoopbackAddress()); // A talks to this
        lossy(toB, toA, b.getLocalSocketAddress());
        lossy(toA, toB, a.getLocalSocketAddress());

        Rudp ra = new Rudp(a, key), rb = new Rudp(b, key);
        ByteArrayOutputStream gotA = new ByteArrayOutputStream(), gotB = new ByteArrayOutputStream();
        // starts as soon as it punches, like P2PHost: only a started link answers the other side's late HELLOs
        CompletableFuture<Boolean> pa = CompletableFuture.supplyAsync(() -> {
            boolean punched = ra.punch(List.of((InetSocketAddress) toB.getLocalSocketAddress()), 5000);
            if (punched)
                ra.start(gotA, () -> { });
            return punched;
        });
        assertTrue(rb.punch(List.of((InetSocketAddress) toA.getLocalSocketAddress()), 5000));
        rb.start(gotB, () -> { });
        assertTrue(pa.get(6, TimeUnit.SECONDS));

        byte[] data = new byte[2 << 20];
        new Random(1).nextBytes(data);
        CountDownLatch done = new CountDownLatch(2);
        for (Rudp r : new Rudp[]{ ra, rb })
            new Thread(() -> {
                try {
                    for (int off = 0; off < data.length; off += 5000)
                        r.write(data, off, Math.min(5000, data.length - off));
                } catch (Exception e) {
                    fail(e);
                }
                done.countDown();
            }).start();
        assertTrue(done.await(60, TimeUnit.SECONDS));
        long end = System.currentTimeMillis() + 30_000;
        while ((gotA.size() < data.length || gotB.size() < data.length) && System.currentTimeMillis() < end)
            Thread.sleep(50);
        assertTrue(Arrays.equals(data, gotA.toByteArray()));
        assertTrue(Arrays.equals(data, gotB.toByteArray()));
        ra.close();
        rb.close();
    }

    @Test
    void wrongKeyNeverConnects() throws Exception {
        DatagramSocket a = new DatagramSocket(0, InetAddress.getLoopbackAddress());
        DatagramSocket b = new DatagramSocket(0, InetAddress.getLoopbackAddress());
        Rudp ra = new Rudp(a, "keyone!!".getBytes()), rb = new Rudp(b, "keytwo!!".getBytes());
        CompletableFuture<Boolean> pa = CompletableFuture.supplyAsync(() -> ra.punch(List.of((InetSocketAddress) b.getLocalSocketAddress()), 1500));
        assertFalse(rb.punch(List.of((InetSocketAddress) a.getLocalSocketAddress()), 1500));
        assertFalse(pa.get(3, TimeUnit.SECONDS));
    }

    /** Forwards what arrives on `in` from `out`'s socket to `dest`, dropping 10% and holding some back to reorder. */
    private static void lossy(DatagramSocket in, DatagramSocket out, java.net.SocketAddress dest) {
        Thread t = new Thread(() -> {
            Random rnd = new Random(in.getLocalPort());
            byte[] buf = new byte[2048];
            DatagramPacket held = null;
            while (true) {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                try {
                    in.receive(p);
                    if (rnd.nextDouble() < 0.10)
                        continue;
                    DatagramPacket fwd = new DatagramPacket(Arrays.copyOf(buf, p.getLength()), p.getLength(), dest);
                    if (held == null && rnd.nextDouble() < 0.05) {
                        held = fwd;
                        continue;
                    }
                    out.send(fwd);
                    if (held != null) {
                        out.send(held);
                        held = null;
                    }
                } catch (Exception e) {
                    return;
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }
}
