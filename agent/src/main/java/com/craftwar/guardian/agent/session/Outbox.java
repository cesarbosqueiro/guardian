package com.craftwar.guardian.agent.session;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

final class Outbox {

    private final long maxBytes;
    private final ConcurrentLinkedQueue<byte[]> queue = new ConcurrentLinkedQueue<byte[]>();
    private final AtomicLong bytes = new AtomicLong();

    Outbox(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    boolean offer(byte[] frame) {
        if (bytes.addAndGet(frame.length) > maxBytes) {
            bytes.addAndGet(-frame.length);
            return false;
        }
        queue.add(frame);
        return true;
    }

    byte[] poll() {
        byte[] f = queue.poll();
        if (f != null) bytes.addAndGet(-f.length);
        return f;
    }

    void clear() {
        while (poll() != null) {
        }
    }
}
