package io.github.shiruka.network.piplines;

import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.Packets;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;

/**
 * a class that represents packet decoders.
 */
public final class PacketDecoder extends MessageToMessageDecoder<ByteBuf> {

  @Override
  protected void decode(final ChannelHandlerContext ctx, final ByteBuf msg, final List<Object> out) {
    if (!msg.isReadable()) {
      return;
    }
    final var buffer = new PacketBuffer(msg);
    final var packetId = buffer.readUnsignedByte();
    final var packet = Packets.get(packetId);
    packet.decode(buffer);
    if (buffer.remaining() > 0) {
      throw new DecoderException("%d bytes left after decoding packet %s"
        .formatted(buffer.remaining(), packet.getClass()));
    }
    out.add(packet);
  }
}
