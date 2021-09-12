package io.github.shiruka.network.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents rak net server handlers.
 */
@RequiredArgsConstructor
public final class RakNetServerHandler extends ChannelInitializer<Channel> {

  /**
   * the server.
   */
  @NotNull
  private final RakNetServer server;

  @Override
  protected void initChannel(final Channel ch) throws Exception {
    ch.pipeline()
      .addLast(RakNetServer.RAK_NET_SERVER_PREFIX + "-timeout", new ReadTimeoutHandler(10));
  }
}
