package io.github.shiruka.network.pipelines;

import io.github.shiruka.network.packets.ConnectedPing;
import io.github.shiruka.network.packets.ConnectedPong;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * a class that represents ping handler pipelines.
 */
@ChannelHandler.Sharable
public final class PingHandler extends SimpleChannelInboundHandler<ConnectedPing> {

  /**
   * the instance.
   */
  public static final PingHandler INSTANCE = new PingHandler();

  /**
   * the name.
   */
  public static final String NAME = "rn-ping";

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final ConnectedPing ping) {
    ctx.write(new ConnectedPong(ping.timestamp(), ping.reliability()));
  }
}
