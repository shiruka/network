package io.github.shiruka.network.raknet.server;

import io.github.shiruka.network.piplines.PacketEncoder;
import io.github.shiruka.network.raknet.piplines.raknet.RakNetPacketEncoder;
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
   * the rak net.
   */
  private static final String RAK_NET = "rn";

  /**
   * the rak net server.
   */
  private static final String RAK_NET_SERVER = RakNetServerHandler.RAK_NET + "s";

  /**
   * the rak net timeout.
   */
  private static final String RAK_NET_TIMEOUT = RakNetServerHandler.RAK_NET_SERVER + "-timeout";

  /**
   * the rak net encoder.
   */
  private static final String RAK_NET_ENCODER = RakNetServerHandler.RAK_NET_SERVER + "-" + RakNetServerHandler.RAK_NET + "encoder";

  /**
   * the server.
   */
  @NotNull
  private final RakNetServer server;

  @Override
  protected void initChannel(final Channel ch) {
    ch.pipeline()
      .addLast(RakNetServerHandler.RAK_NET_TIMEOUT, new ReadTimeoutHandler(10))
      .addLast(RakNetServerHandler.RAK_NET_ENCODER, new PacketEncoder());
  }
}
