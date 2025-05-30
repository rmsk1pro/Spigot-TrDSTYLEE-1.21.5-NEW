package net.minecraft.network.protocol.login;

import net.minecraft.network.EnumProtocol;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.ProtocolInfoBuilder;
import net.minecraft.network.protocol.SimpleUnboundProtocol;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.CookiePacketTypes;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;

public class LoginProtocols {

    public static final SimpleUnboundProtocol<PacketLoginInListener, PacketDataSerializer> SERVERBOUND_TEMPLATE = ProtocolInfoBuilder.<PacketLoginInListener, PacketDataSerializer>serverboundProtocol(EnumProtocol.LOGIN, (protocolinfobuilder) -> {
        protocolinfobuilder.addPacket(LoginPacketTypes.SERVERBOUND_HELLO, PacketLoginInStart.STREAM_CODEC).addPacket(LoginPacketTypes.SERVERBOUND_KEY, PacketLoginInEncryptionBegin.STREAM_CODEC).addPacket(LoginPacketTypes.SERVERBOUND_CUSTOM_QUERY_ANSWER, ServerboundCustomQueryAnswerPacket.STREAM_CODEC).addPacket(LoginPacketTypes.SERVERBOUND_LOGIN_ACKNOWLEDGED, ServerboundLoginAcknowledgedPacket.STREAM_CODEC).addPacket(CookiePacketTypes.SERVERBOUND_COOKIE_RESPONSE, ServerboundCookieResponsePacket.STREAM_CODEC);
    });
    public static final ProtocolInfo<PacketLoginInListener> SERVERBOUND = LoginProtocols.SERVERBOUND_TEMPLATE.bind(PacketDataSerializer::new);
    public static final SimpleUnboundProtocol<PacketLoginOutListener, PacketDataSerializer> CLIENTBOUND_TEMPLATE = ProtocolInfoBuilder.<PacketLoginOutListener, PacketDataSerializer>clientboundProtocol(EnumProtocol.LOGIN, (protocolinfobuilder) -> {
        protocolinfobuilder.addPacket(LoginPacketTypes.CLIENTBOUND_LOGIN_DISCONNECT, PacketLoginOutDisconnect.STREAM_CODEC).addPacket(LoginPacketTypes.CLIENTBOUND_HELLO, PacketLoginOutEncryptionBegin.STREAM_CODEC).addPacket(LoginPacketTypes.CLIENTBOUND_LOGIN_FINISHED, PacketLoginOutSuccess.STREAM_CODEC).addPacket(LoginPacketTypes.CLIENTBOUND_LOGIN_COMPRESSION, PacketLoginOutSetCompression.STREAM_CODEC).addPacket(LoginPacketTypes.CLIENTBOUND_CUSTOM_QUERY, PacketLoginOutCustomPayload.STREAM_CODEC).addPacket(CookiePacketTypes.CLIENTBOUND_COOKIE_REQUEST, ClientboundCookieRequestPacket.STREAM_CODEC);
    });
    public static final ProtocolInfo<PacketLoginOutListener> CLIENTBOUND = LoginProtocols.CLIENTBOUND_TEMPLATE.bind(PacketDataSerializer::new);

    public LoginProtocols() {}
}
