package io.github.shiruka.network.server.pipelines;

import io.github.shiruka.network.options.RakNetConfig;
import io.github.shiruka.network.packets.UnconnectedPing;
import io.github.shiruka.network.packets.UnconnectedPong;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import java.net.InetSocketAddress;
import lombok.SneakyThrows;

/**
 * a class that represents ping listener pipelines.
 */
public final class PingListener extends UdpPacketHandler<UnconnectedPing> {

  /**
   * the name.
   */
  public static final String NAME = "rn-ping-init";

  /**
   * ctor.
   */
  public PingListener() {
    super(UnconnectedPing.class);
  }

  @SneakyThrows
  @Override
  protected void handle(
    final ChannelHandlerContext ctx,
    final InetSocketAddress sender,
    final UnconnectedPing packet
  ) {
    final var config = RakNetConfig.cast(ctx);
    ReferenceCountUtil.retain(packet);
    final var pong = new UnconnectedPong(
      config.serverIdentifier(),
      config.magic(),
      config.serverId(),
      packet.timestamp()
    );
    PingListener.sendResponse(ctx, sender, pong);
  }
}
