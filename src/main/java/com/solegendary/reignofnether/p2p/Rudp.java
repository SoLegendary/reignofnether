package com.solegendary.reignofnether.p2p;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A reliable byte stream over one UDP socket, so a Minecraft TCP connection can ride a hole-punched UDP path.
 * Every packet carries an 8-byte key from the join offer; anything without it is dropped.
 * Selective repeat: numbered segments, cumulative ack + 64-bit SACK, per-segment retransmit.
 */
public final class Rudp implements Closeable {

    private static final byte HELLO = 0, HELLO_ACK = 1, DATA = 2, ACK = 3, PING = 4, FIN = 5;
    private static final int KEY = 8, HEADER = 1 + KEY, MAX_PAYLOAD = 1150;
    // ponytail: fixed window, no congestion control. ~300 KB in flight is plenty for one Minecraft connection.
    private static final int WINDOW = 256;
    private static final long DEAD_MS = 15_000;

    private final DatagramSocket socket;
    private final byte[] key;
    private volatile InetSocketAddress peer;
    private volatile boolean closed;
    private volatile long lastHeard = System.currentTimeMillis(), lastSent;
    private Runnable onClose = () -> { };

    // sender
    private int nextSeq;
    private final TreeMap<Integer, Segment> unacked = new TreeMap<>();
    private double srtt = 200;
    // receiver
    private int expected;
    private final TreeMap<Integer, byte[]> early = new TreeMap<>();
    private OutputStream sink;

    private static final class Segment {
        final byte[] packet;
        long sentAt;
        final long firstSent;
        boolean resent;

        Segment(byte[] packet, long now) {
            this.packet = packet;
            this.sentAt = now;
            this.firstSent = now;
        }
    }

    public Rudp(DatagramSocket socket, byte[] key) {
        this.socket = socket;
        this.key = key;
        try {
            socket.setReceiveBufferSize(1 << 20);
            socket.setSendBufferSize(1 << 20);
        } catch (IOException ignored) {
        }
    }

    /**
     * Both sides call this at about the same time with the other's candidate addresses. Each sends HELLOs to every
     * candidate; the first keyed packet back fixes the peer. False after `timeoutMs` without one.
     */
    public boolean punch(List<InetSocketAddress> candidates, long timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        byte[] buf = new byte[2048];
        try {
            socket.setSoTimeout(100);
            while (System.currentTimeMillis() < end && !closed) {
                for (InetSocketAddress c : candidates)
                    send(c, packet(HELLO, 0));
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(p);
                } catch (SocketTimeoutException e) {
                    continue;
                }
                if (!keyed(buf, p.getLength()))
                    continue;
                peer = (InetSocketAddress) p.getSocketAddress();
                send(peer, packet(HELLO_ACK, 0));
                lastHeard = System.currentTimeMillis();
                return true;
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    /** Starts the receive and retransmit threads; data is written to `sink`. Call after a successful punch. */
    public void start(OutputStream sink, Runnable onClose) {
        this.sink = sink;
        this.onClose = onClose;
        Thread rx = new Thread(this::receiveLoop, "RoN P2P receive");
        Thread tx = new Thread(this::tickLoop, "RoN P2P resend");
        rx.setDaemon(true);
        tx.setDaemon(true);
        rx.start();
        tx.start();
    }

    /** Pipes a TCP socket both ways through this stream until either side closes. */
    public void bridge(Socket tcp) throws IOException {
        tcp.setTcpNoDelay(true);
        InputStream in = tcp.getInputStream();
        start(tcp.getOutputStream(), () -> closeQuietly(tcp));
        Thread up = new Thread(() -> {
            byte[] buf = new byte[16 * 1024];
            try {
                int n;
                while (!closed && (n = in.read(buf)) >= 0)
                    write(buf, 0, n);
            } catch (IOException | InterruptedException ignored) {
            }
            close();
        }, "RoN P2P send");
        up.setDaemon(true);
        up.start();
    }

    public void write(byte[] b, int off, int len) throws InterruptedException, IOException {
        while (len > 0) {
            int n = Math.min(len, MAX_PAYLOAD);
            synchronized (this) {
                while (unacked.size() >= WINDOW && !closed)
                    wait(100);
                if (closed)
                    throw new IOException("closed");
                int seq = nextSeq++;
                byte[] p = packet(DATA, 4 + n);
                ByteBuffer.wrap(p, HEADER, 4).putInt(seq);
                System.arraycopy(b, off, p, HEADER + 4, n);
                unacked.put(seq, new Segment(p, System.currentTimeMillis()));
                send(peer, p);
            }
            off += n;
            len -= n;
        }
    }

    private void receiveLoop() {
        byte[] buf = new byte[2048];
        try {
            socket.setSoTimeout(1000);
        } catch (IOException ignored) {
        }
        while (!closed) {
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(p);
            } catch (SocketTimeoutException e) {
                continue;
            } catch (IOException e) {
                break;
            }
            int len = p.getLength();
            if (!keyed(buf, len) || !p.getSocketAddress().equals(peer))
                continue;
            lastHeard = System.currentTimeMillis();
            try {
                switch (buf[0]) {
                    case HELLO -> send(peer, packet(HELLO_ACK, 0)); // their HELLO_ACK got lost
                    case DATA -> onData(ByteBuffer.wrap(buf, HEADER, 4).getInt(), Arrays.copyOfRange(buf, HEADER + 4, len));
                    case ACK -> onAck(ByteBuffer.wrap(buf, HEADER, 4).getInt(), ByteBuffer.wrap(buf, HEADER + 4, 8).getLong());
                    case FIN -> close();
                    default -> { } // HELLO_ACK, PING: just proof of life
                }
            } catch (IOException e) {
                close(); // the local TCP side went away
            }
        }
        close();
    }

    private void onData(int seq, byte[] payload) throws IOException {
        if (seq - expected >= 2 * WINDOW)
            return;
        if (seq - expected >= 0)
            early.put(seq, payload);
        byte[] next;
        while ((next = early.remove(expected)) != null) {
            sink.write(next);
            expected++;
        }
        sink.flush();
        long sack = 0;
        for (Map.Entry<Integer, byte[]> e : early.entrySet()) {
            int bit = e.getKey() - expected - 1;
            if (bit >= 0 && bit < 64)
                sack |= 1L << bit;
        }
        byte[] ack = packet(ACK, 12);
        ByteBuffer.wrap(ack, HEADER, 12).putInt(expected).putLong(sack);
        send(peer, ack);
    }

    private synchronized void onAck(int cumulative, long sack) {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<Integer, Segment>> it = unacked.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Segment> e = it.next();
            int seq = e.getKey();
            int bit = seq - cumulative - 1;
            if (seq - cumulative < 0 || (bit >= 0 && bit < 64 && (sack & (1L << bit)) != 0)) {
                if (!e.getValue().resent)
                    srtt = srtt * 0.875 + (now - e.getValue().firstSent) * 0.125;
                it.remove();
            }
        }
        notifyAll();
    }

    private void tickLoop() {
        while (!closed) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                break;
            }
            long now = System.currentTimeMillis();
            if (now - lastHeard > DEAD_MS) {
                close();
                break;
            }
            long rto = Math.max(150, Math.min(3000, (long) (srtt * 2) + 50));
            synchronized (this) {
                for (Segment s : unacked.values())
                    if (now - s.sentAt > rto) {
                        s.sentAt = now;
                        s.resent = true;
                        send(peer, s.packet);
                    }
            }
            if (now - lastSent > 1000)
                send(peer, packet(PING, 0));
        }
    }

    private byte[] packet(byte type, int bodyLen) {
        byte[] p = new byte[HEADER + bodyLen];
        p[0] = type;
        System.arraycopy(key, 0, p, 1, KEY);
        return p;
    }

    private boolean keyed(byte[] buf, int len) {
        if (len < HEADER)
            return false;
        int diff = 0; // constant time
        for (int i = 0; i < KEY; i++)
            diff |= buf[1 + i] ^ key[i];
        return diff == 0;
    }

    private void send(@Nullable InetSocketAddress to, byte[] p) {
        if (to == null)
            return;
        try {
            socket.send(new DatagramPacket(p, p.length, to));
            lastSent = System.currentTimeMillis();
        } catch (IOException ignored) {
        }
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        synchronized (this) {
            if (closed)
                return;
            closed = true;
            notifyAll();
        }
        for (int i = 0; i < 3; i++)
            send(peer, packet(FIN, 0));
        socket.close();
        onClose.run();
    }

    private static void closeQuietly(Closeable c) {
        try {
            c.close();
        } catch (IOException ignored) {
        }
    }
}
