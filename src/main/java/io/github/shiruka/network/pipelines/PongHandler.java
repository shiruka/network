package io.github.shiruka.network.pipelines;

import io.github.shiruka.network.options.RakNetConfig;
import io.github.shiruka.network.packets.ConnectedPong;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * a class that represents pong handler pipelines.
 */
@ChannelHandler.Sharable
public final class PongHandler
  extends SimpleChannelInboundHandler<ConnectedPong> {

  /**
   * the instance.
   */
  public static final PongHandler INSTANCE = new PongHandler();

  /**
   * the name.
   */
  public static final String NAME = "rn-pong";

  @Override
  protected void channelRead0(
    final ChannelHandlerContext ctx,
    final ConnectedPong msg
  ) {
    if (!msg.reliability().isReliable()) {
      final var config = RakNetConfig.cast(ctx);
      config.updateRTTNanos(msg.rtt());
    }
  }
}
