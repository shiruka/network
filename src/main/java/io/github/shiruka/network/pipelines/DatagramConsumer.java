package io.github.shiruka.network.pipelines;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents datagram consumer pipelines.
 */
@ChannelHandler.Sharable
public final class DatagramConsumer extends ChannelInboundHandlerAdapter {

  /**
   * the instance.
   */
  public static final DatagramConsumer INSTANCE = new DatagramConsumer();

  /**
   * the name.
   */
  public static final String NAME = "rn-datagram-consumer";

  @Override
  public void channelRead(
    @NotNull final ChannelHandlerContext ctx,
    @NotNull final Object msg
  ) {
    if (msg instanceof DatagramPacket) {
      ReferenceCountUtil.safeRelease(msg);
    } else {
      ctx.fireChannelRead(msg);
    }
  }
}
