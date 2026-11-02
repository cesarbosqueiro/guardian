package com.craftwar.client.guardian;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import com.craftwar.guardian.agent.Bridge;

public final class GuardianLink {

    public static final String CHANNEL = "CW|Guard";
    private static final int SENDS_PER_TICK = 2;
    private static final int STATE_INTERVAL_TICKS = 20;
    private static volatile boolean available;

    private final GameStateReporter state = new GameStateReporter();
    private volatile boolean pendingRegistration;
    private int ticks;

    private GuardianLink() {
    }

    public static GuardianLink create() {
        try {
            if (!Bridge.active()) return null;
        } catch (LinkageError e) {
            return null;
        }
        GuardianLink link = new GuardianLink();
        NetworkRegistry.INSTANCE.newEventDrivenChannel(CHANNEL).register(link);
        available = true;
        return link;
    }

    public static String notice() {
        return available ? Bridge.notice() : null;
    }

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        pendingRegistration = true;
        Bridge.connected();
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        pendingRegistration = false;
        Bridge.disconnected();
    }

    @SubscribeEvent
    public void onPacket(FMLNetworkEvent.ClientCustomPacketEvent e) {
        if (!CHANNEL.equals(e.packet.channel())) return;
        ByteBuf payload = e.packet.payload();
        byte[] frame = new byte[payload.readableBytes()];
        payload.getBytes(payload.readerIndex(), frame);
        Bridge.received(frame);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        NetHandlerPlayClient net = mc.getNetHandler();
        if (net == null || mc.thePlayer == null) return;
        if (pendingRegistration) register(mc, net);
        if (++ticks % STATE_INTERVAL_TICKS == 0) state.report(mc);
        for (int i = 0; i < SENDS_PER_TICK; i++) {
            byte[] frame = Bridge.nextOutgoing();
            if (frame == null) break;
            net.addToSendQueue(new C17PacketCustomPayload(CHANNEL, new PacketBuffer(Unpooled.wrappedBuffer(frame))));
        }
    }

    private void register(Minecraft mc, NetHandlerPlayClient net) {
        pendingRegistration = false;
        byte[] name = CHANNEL.getBytes(StandardCharsets.UTF_8);
        net.addToSendQueue(new C17PacketCustomPayload("REGISTER", new PacketBuffer(Unpooled.wrappedBuffer(name))));
        state.report(mc);
        state.publishEnvironment(mc);
    }
}
