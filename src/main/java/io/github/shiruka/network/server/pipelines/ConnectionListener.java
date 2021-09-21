package io.github.shiruka.network.server.pipelines;

import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.options.RakNetConfig;
import io.github.shiruka.network.packets.ConnectionRequest1;
import io.github.shiruka.network.packets.InvalidVersion;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import java.net.InetSocketAddress;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents connection listener pipelines.
 */
public final class ConnectionListener extends UdpPacketHandler<ConnectionRequest1> {

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

  /**
   * resends the request.
   *
   * @param ctx the ctx to resend.
   * @param sender the sender to resend.
   * @param request1 the request 1 to resend.
   */
  private static void resendRequest(@NotNull final ChannelHandlerContext ctx, @NotNull final InetSocketAddress sender,
                                    @NotNull final ConnectionRequest1 request1) {
    final var config = RakNetConfig.cast(ctx);
    final var buf = new PacketBuffer(ctx.alloc().ioBuffer(request1.initialSizeHint()));
    try {
      config.codec().encode(request1, buf);
      final var packet = new DatagramPacket(buf.retain().buffer(), null, sender);
      ctx.pipeline().fireChannelRead(packet).fireChannelReadComplete();
    } finally {
      ReferenceCountUtil.safeRelease(request1);
      buf.release();
    }
  }

  /**
   * sends response.
   *
   * @param ctx the ctx to send.
   * @param sender the sender to send.
   * @param packet the packet to send.
   */
  private static void sendResponse(@NotNull final ChannelHandlerContext ctx, @NotNull final InetSocketAddress sender,
                                   @NotNull final Packet packet) {
    final var config = RakNetConfig.cast(ctx);
    final var buf = new PacketBuffer(ctx.alloc().ioBuffer(packet.initialSizeHint()));
    try {
      config.codec().encode(packet, buf);
      final var datagram = new DatagramPacket(buf.retain().buffer(), sender);
      ctx.writeAndFlush(datagram).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    } finally {
      ReferenceCountUtil.safeRelease(packet);
      buf.release();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void handle(final ChannelHandlerContext ctx, final InetSocketAddress sender, final ConnectionRequest1 request) {
    final var config = RakNetConfig.cast(ctx);
    if (!config.containsProtocolVersion(request.protocolVersion())) {
      ConnectionListener.sendResponse(ctx, sender, new InvalidVersion(config.magic(), config.serverId()));
      return;
    }
    ReferenceCountUtil.retain(request);
    ctx.channel().connect(sender).addListeners(
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
