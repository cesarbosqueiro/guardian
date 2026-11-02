package com.craftwar.guardian.agent.session;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.craftwar.guardian.agent.AgentConfig;
import com.craftwar.guardian.agent.Environment;
import com.craftwar.guardian.agent.GameState;
import com.craftwar.guardian.agent.Health;
import com.craftwar.guardian.agent.Log;
import com.craftwar.guardian.agent.integrity.ClassFilter;
import com.craftwar.guardian.agent.integrity.IntegrityMonitor;
import com.craftwar.guardian.agent.patch.Patch;
import com.craftwar.guardian.agent.record.ClickCounter;
import com.craftwar.guardian.agent.record.Recorder;
import com.craftwar.guardian.protocol.ClientHandshake;
import com.craftwar.guardian.protocol.FindingKind;
import com.craftwar.guardian.protocol.Frame;
import com.craftwar.guardian.protocol.MessageType;
import com.craftwar.guardian.protocol.ProtocolException;
import com.craftwar.guardian.protocol.SecureChannel;
import com.craftwar.guardian.protocol.message.Finding;
import com.craftwar.guardian.protocol.message.Heartbeat;
import com.craftwar.guardian.protocol.message.Policy;
import com.craftwar.guardian.protocol.message.Request;

public final class Session implements Runnable, Sender {

    private static final Object CONNECTED = new Object();
    private static final Object DISCONNECTED = new Object();
    private static final long SECOND = 1_000_000_000L;
    private static final long CONTEXT_WAIT = 5 * SECOND;
    private static final long FIRST_INVENTORY_DELAY = 3 * SECOND;
    private static final Policy DEFAULT_POLICY = new Policy(5, 60, java.util.Collections.<String>emptyList());

    private final AgentConfig config;
    private final IntegrityMonitor integrity;
    private final List<Patch> patches;
    private final BlockingQueue<Object> events = new ArrayBlockingQueue<Object>(256);
    private final Outbox outbox = new Outbox(4L << 20);
    private final ClickCounter clicks = new ClickCounter(Recorder.INSTANCE);
    private final RequestHandler requests = new RequestHandler();
    private ClientHandshake handshake;
    private SecureChannel channel;
    private Policy policy = DEFAULT_POLICY;
    private long establishedAt;
    private long nextHeartbeat;
    private long nextInventory;
    private boolean contextSent;

    public Session(AgentConfig config, IntegrityMonitor integrity, List<Patch> patches) {
        this.config = config;
        this.integrity = integrity;
        this.patches = patches;
    }

    public void start() {
        Thread t = new Thread(this, "Guardian");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    public void connected() {
        events.offer(CONNECTED);
    }

    public void disconnected() {
        events.offer(DISCONNECTED);
    }

    public void received(byte[] frame) {
        events.offer(frame);
    }

    public byte[] nextOutgoing() {
        return outbox.poll();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Object event = events.poll(200, TimeUnit.MILLISECONDS);
                if (event != null) handle(event);
                if (channel != null) periodic(System.nanoTime());
            } catch (InterruptedException e) {
                return;
            } catch (ProtocolException e) {
                Log.warn("protocol: " + e.getMessage());
            } catch (Throwable t) {
                Health.degrade("session: " + t);
                reset(null);
            }
        }
    }

    @Override
    public void send(int type, byte[] payload) {
        SecureChannel c = channel;
        if (c != null && !outbox.offer(c.seal(type, payload))) Log.warn("outbox full, dropped type " + type);
    }

    private void handle(Object event) {
        if (event == CONNECTED) reset(new ClientHandshake(config.serverKey()));
        else if (event == DISCONNECTED) reset(null);
        else frame((byte[]) event);
    }

    private void reset(ClientHandshake next) {
        handshake = next;
        channel = null;
        contextSent = false;
        outbox.clear();
    }

    private void frame(byte[] frame) {
        if (Frame.typeOf(frame) == MessageType.HELLO) {
            if (handshake == null || channel != null) return;
            outbox.offer(handshake.reply(frame));
            channel = handshake.channel();
            long now = System.nanoTime();
            establishedAt = now;
            nextHeartbeat = now;
            nextInventory = now + FIRST_INVENTORY_DELAY;
            return;
        }
        if (channel == null) return;
        Frame f = channel.open(frame);
        if (f.type == MessageType.POLICY) applyPolicy(Policy.decode(f.payload));
        else if (f.type == MessageType.REQUEST) requests.handle(Request.decode(f.payload), this);
    }

    private void applyPolicy(Policy p) {
        policy = p;
        integrity.filter(ClassFilter.withExtra(p.ignoredClassPrefixes));
    }

    private void periodic(long now) {
        if (!contextSent && (Environment.current() != null || now - establishedAt > CONTEXT_WAIT)) sendContext();
        if (now - nextHeartbeat >= 0) {
            nextHeartbeat = now + Math.max(1, policy.heartbeatSeconds) * SECOND;
            heartbeat();
        }
        if (now - nextInventory >= 0) {
            nextInventory = now + Math.max(10, policy.inventorySeconds) * SECOND;
            sendAll(integrity.scan());
        }
        sendAll(integrity.drain());
        for (String problem; (problem = Health.nextProblem()) != null; ) {
            send(MessageType.FINDING, new Finding(FindingKind.AGENT_DEGRADED, "health", problem).encode());
        }
    }

    private void sendContext() {
        contextSent = true;
        for (Patch p : patches) {
            if (!p.applied()) Health.degrade("patch not applied: " + p.name());
        }
        List<String> args = ContextFactory.jvmArguments();
        send(MessageType.CONTEXT, ContextFactory.build(config.mapping(), args).encode());
        sendAll(ContextFactory.suspiciousArguments(args));
    }

    private void heartbeat() {
        clicks.advance();
        send(MessageType.HEARTBEAT, new Heartbeat(GameState.fps(), clicks.left(), clicks.right(), GameState.flags(),
            Health.state(), integrity.loadedClasses()).encode());
    }

    private void sendAll(List<Finding> findings) {
        for (Finding f : findings) send(MessageType.FINDING, f.encode());
    }
}
