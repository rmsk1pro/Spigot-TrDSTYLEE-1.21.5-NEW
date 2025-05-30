package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;

public class PacketPlayOutAnimation implements Packet<PacketListenerPlayOut> {

    public static final StreamCodec<PacketDataSerializer, PacketPlayOutAnimation> STREAM_CODEC = Packet.<PacketDataSerializer, PacketPlayOutAnimation>codec(PacketPlayOutAnimation::write, PacketPlayOutAnimation::new);
    public static final int SWING_MAIN_HAND = 0;
    public static final int WAKE_UP = 2;
    public static final int SWING_OFF_HAND = 3;
    public static final int CRITICAL_HIT = 4;
    public static final int MAGIC_CRITICAL_HIT = 5;
    private final int id;
    private final int action;

    public PacketPlayOutAnimation(Entity entity, int i) {
        this.id = entity.getId();
        this.action = i;
    }

    private PacketPlayOutAnimation(PacketDataSerializer packetdataserializer) {
        this.id = packetdataserializer.readVarInt();
        this.action = packetdataserializer.readUnsignedByte();
    }

    private void write(PacketDataSerializer packetdataserializer) {
        packetdataserializer.writeVarInt(this.id);
        packetdataserializer.writeByte(this.action);
    }

    @Override
    public PacketType<PacketPlayOutAnimation> type() {
        return GamePacketTypes.CLIENTBOUND_ANIMATE;
    }

    public void handle(PacketListenerPlayOut packetlistenerplayout) {
        packetlistenerplayout.handleAnimate(this);
    }

    public int getId() {
        return this.id;
    }

    public int getAction() {
        return this.action;
    }
}
