package io.github.shiruka.network.piplines;

import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * a class that represents packet encoders.
 */
public final class PacketEncoder extends MessageToByteEncoder<Packet> {

  @Override
  protected void encode(final ChannelHandlerContext ctx, final Packet msg, final ByteBuf out) {
    final var buffer = new PacketBuffer(out);
    buffer.writeUnsignedByte(msg.id());
    msg.encode(buffer);
  }
}
