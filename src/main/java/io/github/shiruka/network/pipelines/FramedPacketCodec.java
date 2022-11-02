package io.github.shiruka.network.pipelines;

import io.github.shiruka.network.options.RakNetConfig;
import io.github.shiruka.network.packets.Frame;
import io.github.shiruka.network.packets.FramedPacket;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import java.util.List;

/**
 * a class that represents framed packet codec pipelines.
 */
@ChannelHandler.Sharable
public final class FramedPacketCodec
  extends MessageToMessageCodec<Frame.Data, FramedPacket> {

  /**
   * the instance.
   */
  public static final FramedPacketCodec INSTANCE = new FramedPacketCodec();

  /**
   * the name.
   */
  public static final String NAME = "rn-framed-codec";

  @Override
  protected void encode(
    final ChannelHandlerContext ctx,
    final FramedPacket in,
    final List<Object> out
  ) {
    out.add(RakNetConfig.cast(ctx).codec().encode(in, ctx.alloc()));
  }

  @Override
  protected void decode(
    final ChannelHandlerContext ctx,
    final Frame.Data in,
    final List<Object> out
  ) {
    out.add(RakNetConfig.cast(ctx).codec().decode(in));
  }
}
