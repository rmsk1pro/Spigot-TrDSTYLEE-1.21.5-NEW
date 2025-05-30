package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundStartConfigurationPacket implements Packet<PacketListenerPlayOut> {

    public static final ClientboundStartConfigurationPacket INSTANCE = new ClientboundStartConfigurationPacket();
    public static final StreamCodec<ByteBuf, ClientboundStartConfigurationPacket> STREAM_CODEC = StreamCodec.<ByteBuf, ClientboundStartConfigurationPacket>unit(ClientboundStartConfigurationPacket.INSTANCE);

    private ClientboundStartConfigurationPacket() {}

    @Override
    public PacketType<ClientboundStartConfigurationPacket> type() {
        return GamePacketTypes.CLIENTBOUND_START_CONFIGURATION;
    }

    public void handle(PacketListenerPlayOut packetlistenerplayout) {
        packetlistenerplayout.handleConfigurationStart(this);
    }

    @Override
    public boolean isTerminal() {
        return true;
    }
}
