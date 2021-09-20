package io.github.shiruka.network.packets;

import com.google.common.base.Preconditions;
import io.github.shiruka.network.utils.Integers;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelPromise;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetectorFactory;
import io.netty.util.ResourceLeakTracker;
import io.netty.util.internal.ObjectPool;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * a class that represents frames.
 */
@Accessors(fluent = true)
public final class Frame extends AbstractReferenceCounted {

  /**
   * the comparator.
   */
  public static final Comparator COMPARATOR = new Comparator();

  /**
   * the header size.
   */
  public static final int HEADER_SIZE = 24;

  /**
   * the split flag.
   */
  public static final int SPLIT_FLAG = 0x10;

  /**
   * the leak detector.
   */
  private static final ResourceLeakDetector<Frame> LEAK_DETECTOR =
    ResourceLeakDetectorFactory.instance().newResourceLeakDetector(Frame.class);

  /**
   * the recycler.
   */
  private static final ObjectPool<Frame> RECYCLER = ObjectPool.newPool(Frame::new);

  /**
   * the handle.
   */
  @NotNull
  @Getter
  private final ObjectPool.Handle<Frame> handle;

  /**
   * the frame data.
   */
  @Nullable
  @Setter
  private FrameData frameData;

  /**
   * the has split.
   */
  @Getter
  @Setter
  private boolean hasSplit;

  /**
   * the order index.
   */
  @Getter
  @Setter
  private int orderIndex;

  /**
   * the promise.
   */
  @Nullable
  @Getter
  @Setter
  private ChannelPromise promise;

  /**
   * the reliable index.
   */
  @Getter
  @Setter
  private int reliableIndex;

  /**
   * the sequence index.
   */
  @Getter
  @Setter
  private int sequenceIndex;

  /**
   * the split count.
   */
  @Getter
  @Setter
  private int splitCount;

  /**
   * the split id.
   */
  @Getter
  @Setter
  private int splitId;

  /**
   * the split index.
   */
  @Getter
  @Setter
  private int splitIndex;

  /**
   * the tracker.
   */
  @Nullable
  @Getter
  @Setter
  private ResourceLeakTracker<Frame> tracker;

  /**
   * ctor.
   *
   * @param handle the handle.
   */
  private Frame(@NotNull final ObjectPool.Handle<Frame> handle) {
    this.handle = handle;
    this.setRefCnt(0);
  }

  /**
   * creates a frame from data.
   *
   * @param packet the packet to create.
   *
   * @return frame.
   */
  @NotNull
  public static Frame create(@NotNull final FrameData packet) {
    Preconditions.checkArgument(!packet.reliability().isOrdered(), "Must provided indices for ordered data.");
    return Frame.createRaw()
      .frameData(packet.retain());
  }

  /**
   * creates ordered frame.
   *
   * @param packet the packet to create.
   * @param orderIndex the order index to create.
   * @param sequenceIndex the sequence index to crete.
   *
   * @return ordered frame.
   */
  @NotNull
  public static Frame createOrdered(final FrameData packet, final int orderIndex, final int sequenceIndex) {
    Preconditions.checkArgument(packet.reliability().isOrdered(), "No indices needed for non-ordered data.");
    return Frame.createRaw()
      .frameData(packet.retain())
      .orderIndex(orderIndex)
      .sequenceIndex(sequenceIndex);
  }

  /**
   * reads the buffer and creates a frame.
   *
   * @param buffer the buffer to create.
   *
   * @return frame.
   */
  @NotNull
  public static Frame read(@NotNull final ByteBuf buffer) {
    final var out = Frame.createRaw();
    try {
      final var flags = buffer.readUnsignedByte();
      final var bitLength = buffer.readUnsignedShort();
      final var length = (bitLength + Byte.SIZE - 1) / Byte.SIZE;
      final var hasSplit = (flags & Frame.SPLIT_FLAG) != 0;
      final var reliability = FramedPacket.Reliability.get(flags >> 5);
      int orderChannel = 0;
      if (reliability.isReliable()) {
        out.reliableIndex(buffer.readUnsignedMediumLE());
      }
      if (reliability.isSequenced()) {
        out.sequenceIndex(buffer.readUnsignedMediumLE());
      }
      if (reliability.isOrdered()) {
        out.orderIndex(buffer.readUnsignedMediumLE());
        orderChannel = buffer.readUnsignedByte();
      }
      if (hasSplit) {
        out.splitCount(buffer.readInt())
          .splitId(buffer.readUnsignedShort())
          .splitIndex(buffer.readInt())
          .hasSplit(true);
      }
      out.frameData(FrameData.read(buffer, length, hasSplit));
      out.frameData().reliability(reliability);
      out.frameData().orderChannel(orderChannel);
      return out.retain();
    } finally {
      out.release();
    }
  }

  /**
   * creates a raw frame.
   *
   * @return raw frame.
   */
  @NotNull
  private static Frame createRaw() {
    final var out = Frame.RECYCLER.get();
    assert out.refCnt() == 0;
    assert out.tracker == null;
    assert out.frameData == null;
    assert out.promise == null;
    out.hasSplit(false)
      .reliableIndex(0)
      .sequenceIndex(0)
      .orderIndex(0)
      .splitCount(0)
      .splitId(0)
      .splitIndex(0)
      .setRefCnt(1);
    out.tracker(Frame.LEAK_DETECTOR.track(out));
    return out;
  }

  /**
   * completes the fragment.
   *
   * @param fullData the full data.
   *
   * @return completed frame.
   */
  @NotNull
  public Frame completeFragment(@NotNull final ByteBuf fullData) {
    assert this.frameData().fragment();
    final var out = Frame.createRaw();
    out.reliableIndex(this.reliableIndex).
      sequenceIndex(this.sequenceIndex)
      .orderIndex(this.orderIndex)
      .frameData(FrameData.read(fullData, fullData.readableBytes(), false));
    out.frameData().orderChannel(this.orderChannel());
    out.frameData().reliability(this.reliability());
    return out;
  }

  /**
   * obtains the data size.
   *
   * @return data size.
   */
  public int dataSize() {
    return this.frameData().dataSize();
  }

  /**
   *
   */
  public int fragment(final int splitID, final int splitSize, int reliableIndex, final List<Object> outList) {
    final var data = this.frameData().createData();
    try {
      final var dataSplitSize = splitSize - Frame.HEADER_SIZE;
      final var splitCountTotal =
        (data.readableBytes() + dataSplitSize - 1) / dataSplitSize;
      for (var splitIndexIterator = 0; splitIndexIterator < splitCountTotal;
           splitIndexIterator++) {
        final var length = Math.min(dataSplitSize, data.readableBytes());
        final var out = Frame.createRaw();
        out.reliableIndex(reliableIndex)
          .sequenceIndex(this.sequenceIndex)
          .orderIndex(this.orderIndex)
          .splitCount(splitCountTotal)
          .splitId(splitID)
          .splitIndex(splitIndexIterator)
          .hasSplit(true)
          .frameData(FrameData.read(data, length, true));
        out.frameData().orderChannel(this.orderChannel());
        out.frameData().reliability(this.reliability().makeReliable());
        assert out.frameData().fragment();
        Preconditions.checkState(out.roughPacketSize() <= splitSize, "mtu fragment mismatch");
        reliableIndex = Integers.B3.plus(reliableIndex, 1);
        outList.add(out);
      }
      assert !data.isReadable();
      return splitCountTotal;
    } finally {
      data.release();
    }
  }

  /**
   * obtains the frame data.
   *
   * @return frame data.
   */
  @NotNull
  public FrameData frameData() {
    return Objects.requireNonNull(this.frameData, "frame data");
  }

  /**
   * obtains the order channel.
   *
   * @return order channel.
   */
  public int orderChannel() {
    return this.frameData().orderChannel();
  }

  /**
   * produces to the out.
   *
   * @param alloc the alloc to produce.
   * @param out the out to produce.
   */
  public void produce(@NotNull final ByteBufAllocator alloc, @NotNull final CompositeByteBuf out) {
    final var header = alloc.ioBuffer(Frame.HEADER_SIZE, Frame.HEADER_SIZE);
    try {
      this.writeHeader(header);
      out.addComponent(true, header.retain());
      out.addComponent(true, this.frameData().createData());
    } finally {
      header.release();
    }
  }

  /**
   * obtains the frame data's reliability.
   *
   * @return frame data's reliability.
   */
  @NotNull
  public FramedPacket.Reliability reliability() {
    return this.frameData().reliability();
  }

  @Override
  public Frame retain() {
    return (Frame) super.retain();
  }

  @Override
  protected void deallocate() {
    if (this.frameData != null) {
      this.frameData.release();
      this.frameData = null;
    }
    if (this.tracker != null) {
      this.tracker.close(this);
      this.tracker = null;
    }
    this.promise = null;
    this.handle.recycle(this);
  }

  /**
   * obtains the retained fragment data.
   *
   * @return retained fragment data.
   */
  @NotNull
  public ByteBuf retainedFragmentData() {
    assert this.frameData().fragment();
    return this.frameData().createData();
  }

  /**
   * obtains the retained frame data.
   *
   * @return retained frame data.
   */
  @NotNull
  public FrameData retainedFrameData() {
    return this.frameData().retain();
  }

  /**
   * obtains the rough packet size.
   *
   * @return rough packet size.
   */
  public int roughPacketSize() {
    return this.dataSize() + Frame.HEADER_SIZE;
  }

  @Override
  public ReferenceCounted touch(final Object hint) {
    if (this.tracker != null) {
      this.tracker.record(hint);
    }
    this.frameData().touch(hint);
    return this;
  }

  /**
   * writes into the buffer.
   *
   * @param buffer the buffer to write.
   */
  public void write(@NotNull final ByteBuf buffer) {
    this.writeHeader(buffer);
    this.frameData().write(buffer);
  }

  /**
   * writes header.
   *
   * @param buffer the buffer to write.
   */
  public void writeHeader(@NotNull final ByteBuf buffer) {
    buffer.writeByte(this.reliability().code() << 5 | (this.hasSplit ? Frame.SPLIT_FLAG : 0));
    buffer.writeShort(this.dataSize() * Byte.SIZE);
    assert !(this.hasSplit && !this.reliability().isReliable());
    if (this.reliability().isReliable()) {
      buffer.writeMediumLE(this.reliableIndex);
    }
    if (this.reliability().isSequenced()) {
      buffer.writeMediumLE(this.sequenceIndex);
    }
    if (this.reliability().isOrdered()) {
      buffer.writeMediumLE(this.orderIndex);
      buffer.writeByte(this.orderChannel());
    }
    if (this.hasSplit) {
      buffer.writeInt(this.splitCount);
      buffer.writeShort(this.splitId);
      buffer.writeInt(this.splitIndex);
    }
  }

  /**
   * a class that represents frame comparator.
   */
  private static final class Comparator implements java.util.Comparator<@NotNull Frame> {

    @Override
    public int compare(@NotNull final Frame a, @NotNull final Frame b) {
      if (a == b) {
        return 0;
      } else if (!a.reliability().isReliable()) {
        return -1;
      } else if (!b.reliability().isReliable()) {
        return 1;
      }
      return Integers.B3.minusWrap(a.reliableIndex, b.reliableIndex) < 0 ? -1 : 1;
    }
  }
}
