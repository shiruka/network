package io.github.shiruka.network.pipelines;

import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.packets.Frame;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import java.util.List;

/**
 * a class that represents user data codec pipelines.
 */
@ChannelHandler.Sharable
public final class UserDataCodec extends MessageToMessageCodec<Frame.Data, PacketBuffer> {

  /**
   * the name.
   */
  public static final String NAME = "rn-user-data-codec";

  /**
   * the packet id.
   */
  private final int packetId;

  /**
   * ctor.
   *
   * @param packetId the packet id.
   */
  public UserDataCodec(final int packetId) {
    this.packetId = packetId;
  }

  @Override
  protected void encode(final ChannelHandlerContext ctx, final PacketBuffer buf, final List<Object> out) {
    if (buf.isReadable()) {
      out.add(Frame.Data.create(ctx.alloc(), this.packetId, buf));
    }
  }

  @Override
  protected void decode(final ChannelHandlerContext ctx, final Frame.Data packet, final List<Object> out) {
    assert !packet.fragment();
    if (packet.dataSize() <= 0) {
      return;
    }
    if (this.packetId == packet.packetId()) {
      final var buffer = packet.createData();
      buffer.skip(1);
      out.add(buffer);
    } else {
      out.add(packet.retain());
    }
  }
}
