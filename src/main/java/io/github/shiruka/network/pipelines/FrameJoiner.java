package io.github.shiruka.network.pipelines;

import io.github.shiruka.network.Constants;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.options.RakNetConfig;
import io.github.shiruka.network.packets.Frame;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.ReferenceCountUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents frame joiner pipelines.
 */
public final class FrameJoiner extends MessageToMessageDecoder<Frame> {

  /**
   * the name.
   */
  public static final String NAME = "rn-join";

  /**
   * the pending packets.
   */
  private final Int2ObjectMap<Builder> pendingPackets = new Int2ObjectOpenHashMap<>();

  @Override
  public void handlerRemoved(final ChannelHandlerContext ctx) throws Exception {
    super.handlerRemoved(ctx);
    this.pendingPackets.values().forEach(Builder::release);
    this.pendingPackets.clear();
  }

  @Override
  protected void decode(
    final ChannelHandlerContext ctx,
    final Frame frame,
    final List<Object> out
  ) {
    if (!frame.hasSplit()) {
      frame.touch("Not split");
      out.add(frame.retain());
    } else {
      final var splitId = frame.splitId();
      final var partial = this.pendingPackets.get(splitId);
      final var splitCount = frame.splitCount();
      final var totalSize = splitCount * frame.roughPacketSize();
      frame.touch("Is split");
      if (totalSize > RakNetConfig.cast(ctx).maxQueuedBytes()) {
        throw new TooLongFrameException("Fragmented frame too large");
      } else if (partial == null) {
        if (splitCount > Constants.MAX_PACKET_LOSS) {
          throw new DecoderException(
            "Too big packet loss: frame join elements"
          );
        }
        this.pendingPackets.put(splitId, Builder.create(ctx.alloc(), frame));
      } else {
        partial.add(frame);
        if (partial.isDone()) {
          this.pendingPackets.remove(splitId);
          out.add(partial.finish());
        }
      }
      if (this.pendingPackets.size() > Constants.MAX_PACKET_LOSS) {
        throw new DecoderException("Too big packet loss: pending frame joins");
      }
    }
  }

  /**
   * a class that represents pending packet builders.
   */
  private static final class Builder {

    /**
     * the queue.
     */
    private final Int2ObjectMap<PacketBuffer> queue;

    /**
     * the data.
     */
    private PacketBuffer data;

    /**
     * the sample packet.
     */
    private Frame samplePacket;

    /**
     * the split idx.
     */
    private int splitIdx;

    /**
     * ctor.
     *
     * @param size the size.
     */
    private Builder(final int size) {
      this.queue = new Int2ObjectOpenHashMap<>(size);
    }

    /**
     * creates a builder.
     *
     * @param alloc the alloc to create.
     * @param frame the frame to create.
     *
     * @return builder.
     */
    @NotNull
    private static Builder create(
      @NotNull final ByteBufAllocator alloc,
      @NotNull final Frame frame
    ) {
      final var out = new Builder(frame.splitCount());
      out.init(alloc, frame);
      return out;
    }

    /**
     * adds the packet.
     *
     * @param packet the packet to add.
     */
    private void add(@NotNull final Frame packet) {
      assert packet.reliability().equals(this.samplePacket.reliability());
      assert packet.orderChannel() == this.samplePacket.orderChannel();
      assert packet.orderIndex() == this.samplePacket.orderIndex();
      if (
        !this.queue.containsKey(packet.splitIndex()) &&
        packet.splitIndex() >= this.splitIdx
      ) {
        this.queue.put(packet.splitIndex(), packet.retainedFragmentData());
        this.update();
      }
      if (this.queue.size() > Constants.MAX_PACKET_LOSS) {
        throw new DecoderException(
          "Too big packet loss: packet de fragment queue"
        );
      }
    }

    /**
     * finishes.
     *
     * @return frame.
     */
    @NotNull
    private Frame finish() {
      assert this.isDone();
      assert this.queue.isEmpty();
      try {
        return this.samplePacket.completeFragment(this.data);
      } finally {
        this.release();
      }
    }

    /**
     * initiates.
     *
     * @param alloc the alloc to initiate.
     * @param packet the packet to initiate.
     */
    private void init(
      @NotNull final ByteBufAllocator alloc,
      @NotNull final Frame packet
    ) {
      assert this.data == null;
      this.splitIdx = 0;
      this.data =
        new PacketBuffer(alloc.compositeDirectBuffer(packet.splitCount()));
      this.samplePacket = packet.retain();
      this.add(packet);
    }

    /**
     * checks if its done.
     *
     * @return {@code true} if its done.
     */
    private boolean isDone() {
      assert this.samplePacket.splitCount() >= this.splitIdx;
      return this.samplePacket.splitCount() == this.splitIdx;
    }

    /**
     * releases.
     */
    private void release() {
      if (this.data != null) {
        this.data.release();
        this.data = null;
      }
      if (this.samplePacket != null) {
        this.samplePacket.release();
        this.samplePacket = null;
      }
      this.queue.values().forEach(ReferenceCountUtil::release);
      this.queue.clear();
    }

    /**
     * updates.
     */
    private void update() {
      PacketBuffer fragment;
      while ((fragment = this.queue.remove(this.splitIdx)) != null) {
        this.data.addComponent(true, fragment);
        this.splitIdx++;
      }
    }
  }
}
