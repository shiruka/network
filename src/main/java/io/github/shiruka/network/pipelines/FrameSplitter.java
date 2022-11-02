package io.github.shiruka.network.pipelines;

import io.github.shiruka.network.options.RakNetConfig;
import io.github.shiruka.network.packets.Frame;
import io.github.shiruka.network.utils.Integers;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;

/**
 * a class that represents frame splitter pipelines.
 */
public final class FrameSplitter extends MessageToMessageEncoder<Frame> {

  /**
   * the name.
   */
  public static final String NAME = "rn-split";

  /**
   * the next reliable id.
   */
  private int nextReliableId = 0;

  /**
   * the next split id.
   */
  private int nextSplitId = 0;

  @Override
  protected void encode(
    final ChannelHandlerContext ctx,
    final Frame packet,
    final List<Object> out
  ) {
    final var config = RakNetConfig.cast(ctx);
    final var maxSize =
      config.mtu() - 2 * (Frame.Set.HEADER_SIZE + Frame.HEADER_SIZE);
    if (packet.roughPacketSize() > maxSize) {
      final var splits = packet.fragment(
        this.nextSplitID(),
        maxSize,
        this.nextReliableId,
        out
      );
      this.nextReliableId = Integers.B3.plus(this.nextReliableId, splits);
    } else {
      if (packet.reliability().isReliable()) {
        packet.reliableIndex(this.nextReliableId());
      }
      out.add(packet.retain());
    }
  }

  private int nextReliableId() {
    final var reliableIndex = this.nextReliableId;
    this.nextReliableId = Integers.B3.plus(this.nextReliableId, 1);
    return reliableIndex;
  }

  private int nextSplitID() {
    final var splitId = this.nextSplitId;
    this.nextSplitId = Integers.B2.plus(this.nextSplitId, 1);
    return splitId;
  }
}
