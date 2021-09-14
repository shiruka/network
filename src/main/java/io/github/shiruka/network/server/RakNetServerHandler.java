package io.github.shiruka.network.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * a class that represents rak net server handlers.
 */
@RequiredArgsConstructor
public final class RakNetServerHandler extends ChannelInboundHandlerAdapter {

  /**
   * the server.
   */
  @NotNull
  private final RakNetServer server;

  /**
   * the cause address.
   */
  @Nullable
  private InetSocketAddress causeAddress;

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (!(msg instanceof DatagramPacket)) {
      return;
    }
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
    if (this.causeAddress != null) {
      this.server.handler().handlerException(this.causeAddress, cause);
    }
  }
}
