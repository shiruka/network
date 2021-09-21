package io.github.shiruka.network.server.pipelines;

import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.options.RakNetConfig;
import io.github.shiruka.network.packets.UnconnectedPing;
import io.github.shiruka.network.packets.UnconnectedPong;
import io.github.shiruka.network.server.RakNetServer;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import java.net.InetSocketAddress;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

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
    final var buffer = new PacketBuffer(ctx.alloc().ioBuffer(packet.initialSizeHint()));
    try {
      config.codec().encode(packet, buffer);
      final var datagram = new DatagramPacket(buffer.retain().buffer(), sender);
      ctx.writeAndFlush(datagram).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    } finally {
      ReferenceCountUtil.safeRelease(packet);
      buffer.release();
    }
  }

  @SneakyThrows
  @Override
  protected void handle(final ChannelHandlerContext ctx, final InetSocketAddress sender,
                        final UnconnectedPing ping) {
    final var config = RakNetConfig.cast(ctx);
    final var connections = RakNetServer.cast(ctx).children();
    if (connections.size() >= config.maxConnections()) {
      return;
    }
    ReferenceCountUtil.retain(ping);
    final var pong = new UnconnectedPong(
      config.serverIdentifier(),
      config.magic(),
      config.serverId(),
      ping.timestamp());
    PingListener.sendResponse(ctx, sender, pong);
  }
}
