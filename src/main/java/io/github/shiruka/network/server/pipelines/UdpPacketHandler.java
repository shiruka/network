package io.github.shiruka.network.server.pipelines;

import com.google.common.base.Preconditions;
import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.options.RakNetConfig;
import io.github.shiruka.network.packets.FramedPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import java.net.InetSocketAddress;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * an abstract class that represents udp packet handler pipelines.
 *
 * @param <T> type of the packet.
 */
@Getter
@Accessors(fluent = true)
public abstract class UdpPacketHandler<T extends Packet> extends SimpleChannelInboundHandler<DatagramPacket> {

  /**
   * the type.
   */
  @NotNull
  private final Class<T> type;

  /**
   * the packet id.
   */
  @Setter
  private int packetId;

  /**
   * ctor.
   *
   * @param type the type.
   */
  protected UdpPacketHandler(@NotNull final Class<T> type) {
    Preconditions.checkArgument(!FramedPacket.class.isAssignableFrom(type),
      "Framed packet types cannot be directly handled by UdpPacketHandler");
    this.type = type;
  }

  @Override
  public final boolean acceptInboundMessage(final Object msg) {
    if (msg instanceof DatagramPacket packet) {
      final var content = packet.content();
      return content.getUnsignedByte(content.readerIndex()) == this.packetId;
    }
    return false;
  }

  @Override
  protected final void channelRead0(final ChannelHandlerContext ctx, final DatagramPacket msg) {
    final var config = RakNetConfig.cast(ctx);
    //noinspection unchecked
    final var packet = (T) config.codec().decode(new PacketBuffer(msg.content()));
    try {
      this.handle(ctx, msg.sender(), packet);
    } finally {
      ReferenceCountUtil.release(packet);
    }
  }

  @Override
  public void handlerAdded(final ChannelHandlerContext ctx) {
    final var config = RakNetConfig.cast(ctx);
    this.packetId = config.codec().packetIdFor(this.type);
    Preconditions.checkArgument(this.packetId != -1, "Unknown packet ID for class %s", this.type);
  }

  /**
   * handles the packet.
   *
   * @param ctx the ctx to handle.
   * @param sender the sender to handler.
   * @param packet the packet to handler.
   */
  protected abstract void handle(ChannelHandlerContext ctx, InetSocketAddress sender, T packet);
}
