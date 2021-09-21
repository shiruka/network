package io.github.shiruka.network.pipelines;

import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.options.RakNetConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageCodec;
import java.util.List;

/**
 * a class that represents raw packet codec pipelines.
 */
@ChannelHandler.Sharable
public final class RawPacketCodec extends MessageToMessageCodec<ByteBuf, Packet> {

  /**
   * the instance.
   */
  public static final RawPacketCodec INSTANCE = new RawPacketCodec();

  /**
   * the name.
   */
  public static final String NAME = "rn-raw-codec";

  @Override
  protected void encode(final ChannelHandlerContext ctx, final Packet in, final List<Object> out) {
    out.add(RakNetConfig.cast(ctx).codec().produceEncoded(in, ctx.alloc()).buffer());
  }

  @Override
  protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) {
    if (in.readableBytes() == 0) {
      return;
    }
    try {
      out.add(RakNetConfig.cast(ctx).codec().decode(new PacketBuffer(in)));
    } catch (final CorruptedFrameException ignored) {
    }
  }
}
