package io.github.shiruka.network.pipelines;

import io.github.shiruka.network.Constants;
import io.github.shiruka.network.packets.Frame;
import io.github.shiruka.network.packets.FramedPacket;
import io.github.shiruka.network.utils.Integers;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.ReferenceCountUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.List;

/**
 * a class that represents frame order in pipelines.
 */
public final class FrameOrderIn extends MessageToMessageDecoder<Frame> {

  /**
   * the name.
   */
  public static final String NAME = "rn-order-in";

  /**
   * the channels.
   */
  private final OrderedChannelPacketQueue[] channels = new OrderedChannelPacketQueue[8];

  {
    Arrays.fill(this.channels, new OrderedChannelPacketQueue());
  }

  @Override
  public void handlerRemoved(final ChannelHandlerContext ctx) throws Exception {
    super.handlerRemoved(ctx);
    Arrays.stream(this.channels).forEach(OrderedChannelPacketQueue::clear);
  }

  @Override
  protected void decode(final ChannelHandlerContext ctx, final Frame frame, final List<Object> out) {
    if (frame.reliability().isSequenced()) {
      frame.touch("Sequenced");
      this.channels[frame.orderChannel()].decodeSequenced(frame, out);
    } else if (frame.reliability().isOrdered()) {
      frame.touch("Ordered");
      this.channels[frame.orderChannel()].decodeOrdered(frame, out);
    } else {
      frame.touch("No order");
      out.add(frame.retainedFrameData());
    }
  }

  /**
   * a class that represents ordered channel packet queue.
   */
  private static final class OrderedChannelPacketQueue {

    /**
     * the queue.
     */
    private final Int2ObjectMap<FramedPacket> queue = new Int2ObjectOpenHashMap<>();

    /**
     * the last order index.
     */
    private int lastOrderIndex = -1;

    /**
     * the last sequence index.
     */
    private int lastSequenceIndex = -1;

    /**
     * clears.
     */
    private void clear() {
      this.queue.values().forEach(ReferenceCountUtil::release);
      this.queue.clear();
    }

    private void decodeOrdered(final Frame frame, final List<Object> list) {
      final var indexDiff = Integers.B3.minusWrap(frame.orderIndex(), this.lastOrderIndex);
      if (indexDiff > Constants.MAX_PACKET_LOSS) {
        throw new DecoderException("Too big packet loss: ordered difference");
      }
      if (indexDiff == 1) {
        FramedPacket data = frame.retainedFrameData();
        do {
          list.add(data);
          this.lastOrderIndex = Integers.B3.plus(this.lastOrderIndex, 1);
          data = this.queue.remove(Integers.B3.plus(this.lastOrderIndex, 1));
        } while (data != null);
      } else if (indexDiff > 1 && !this.queue.containsKey(frame.orderIndex())) {
        this.queue.put(frame.orderIndex(), frame.retainedFrameData());
      }
      if (this.queue.size() > Constants.MAX_PACKET_LOSS) {
        throw new DecoderException("Too big packet loss: missed ordered packets");
      }
    }

    private void decodeSequenced(final Frame frame, final List<Object> list) {
      if (Integers.B3.minusWrap(frame.sequenceIndex(), this.lastSequenceIndex) > 0) {
        this.lastSequenceIndex = frame.sequenceIndex();
        while (Integers.B3.minusWrap(frame.orderIndex(), this.lastOrderIndex) > 1) {
          ReferenceCountUtil.release(this.queue.remove(this.lastOrderIndex));
          this.lastOrderIndex = Integers.B3.plus(this.lastOrderIndex, 1);
        }
      }
      this.decodeOrdered(frame, list);
    }
  }
}
