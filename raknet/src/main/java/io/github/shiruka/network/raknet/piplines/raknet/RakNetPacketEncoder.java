package io.github.shiruka.network.raknet.piplines.raknet;

import io.github.shiruka.network.raknet.RakNetPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * a class that represents rak net packet encoders.
 */
public final class RakNetPacketEncoder extends MessageToByteEncoder<RakNetPacket> {

  /**
   * ctor.
   */
  public RakNetPacketEncoder() {
    super(RakNetPacket.class);
  }

  @Override
  protected void encode(final ChannelHandlerContext ctx, final RakNetPacket msg, final ByteBuf out) {
    out.writeByte(msg.id());
    msg.encode(out);
  }
}
