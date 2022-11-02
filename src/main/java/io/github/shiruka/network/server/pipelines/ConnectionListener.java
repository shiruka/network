package io.github.shiruka.network.server.pipelines;

import io.github.shiruka.network.options.RakNetConfig;
import io.github.shiruka.network.packets.ConnectionRequest1;
import io.github.shiruka.network.packets.InvalidVersion;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import java.net.InetSocketAddress;

/**
 * a class that represents connection listener pipelines.
 */
public final class ConnectionListener
  extends UdpPacketHandler<ConnectionRequest1> {

  /**
   * the name.
   */
  public static final String NAME = "rn-connect-init";

  /**
   * ctor.
   */
  public ConnectionListener() {
    super(ConnectionRequest1.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void handle(
    final ChannelHandlerContext ctx,
    final InetSocketAddress sender,
    final ConnectionRequest1 request
  ) {
    final var config = RakNetConfig.cast(ctx);
    if (!config.containsProtocolVersion(request.protocolVersion())) {
      ConnectionListener.sendResponse(
        ctx,
        sender,
        new InvalidVersion(config.magic(), config.serverId())
      );
      return;
    }
    ReferenceCountUtil.retain(request);
    ctx
      .channel()
      .connect(sender)
      .addListeners(
        ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE,
        future -> {
          if (future.isSuccess()) {
            ConnectionListener.resendRequest(ctx, sender, request);
          } else {
            ReferenceCountUtil.safeRelease(request);
          }
        }
      );
  }
}
