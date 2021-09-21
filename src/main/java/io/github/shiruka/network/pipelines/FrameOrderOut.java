package io.github.shiruka.network.pipelines;

import io.github.shiruka.network.options.RakNetConfig;
import io.github.shiruka.network.packets.Frame;
import io.github.shiruka.network.packets.FramedPacket;
import io.github.shiruka.network.utils.Integers;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;

/**
 * a class that represents frame order out pipelines.
 */
public final class FrameOrderOut extends MessageToMessageEncoder<FramedPacket> {

  /**
   * the name.
   */
  public static final String NAME = "rn-order-out";

  /**
   * the next order index.
   */
  private final int[] nextOrderIndex = new int[8];

  /**
   * the next sequence index.
   */
  private final int[] nextSequenceIndex = new int[8];

  @Override
  protected void encode(final ChannelHandlerContext ctx, final FramedPacket packet, final List<Object> out) {
    final var config = RakNetConfig.cast(ctx);
    final var data = config.codec().encode(packet, ctx.alloc());
    try {
      if (data.reliability().isOrdered()) {
        final var channel = data.orderChannel();
        final var sequenceIndex = data.reliability().isSequenced() ? this.getNextSequenceIndex(channel) : 0;
        out.add(Frame.createOrdered(data, this.getNextOrderIndex(channel), sequenceIndex));
      } else {
        out.add(Frame.create(data));
      }
    } finally {
      data.release();
    }
  }

  private int getNextOrderIndex(final int channel) {
    final var orderIndex = this.nextOrderIndex[channel];
    this.nextOrderIndex[channel] = Integers.B3.plus(this.nextOrderIndex[channel], 1);
    return orderIndex;
  }

  private int getNextSequenceIndex(final int channel) {
    final var sequenceIndex = this.nextSequenceIndex[channel];
    this.nextSequenceIndex[channel] = Integers.B3.plus(this.nextSequenceIndex[channel], 1);
    return sequenceIndex;
  }
}
