package io.github.shiruka.network.packets;

import com.google.common.base.Preconditions;
import io.github.shiruka.network.Ids;
import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.utils.Integers;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetectorFactory;
import io.netty.util.ResourceLeakTracker;
import io.netty.util.internal.ObjectPool;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
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
  private Data frameData;

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
  public static Frame create(@NotNull final Data packet) {
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
  public static Frame createOrdered(final Data packet, final int orderIndex, final int sequenceIndex) {
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
  public static Frame read(@NotNull final PacketBuffer buffer) {
    final var out = Frame.createRaw();
    try {
      final var flags = buffer.readUnsignedByte();
      final var bitLength = buffer.readUnsignedShort();
      final var length = (bitLength + Byte.SIZE - 1) / Byte.SIZE;
      final var hasSplit = (flags & Frame.SPLIT_FLAG) != 0;
      final var reliability = FramedPacket.Reliability.get(flags >> 5);
      int orderChannel = 0;
      if (reliability.isReliable()) {
        out.reliableIndex(buffer.readUnsignedTriadLE());
      }
      if (reliability.isSequenced()) {
        out.sequenceIndex(buffer.readUnsignedTriadLE());
      }
      if (reliability.isOrdered()) {
        out.orderIndex(buffer.readUnsignedTriadLE());
        orderChannel = buffer.readUnsignedByte();
      }
      if (hasSplit) {
        out.splitCount(buffer.readInt())
          .splitId(buffer.readUnsignedShort())
          .splitIndex(buffer.readInt())
          .hasSplit(true);
      }
      out.frameData(Data.read(buffer, length, hasSplit));
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
  public Frame completeFragment(@NotNull final PacketBuffer fullData) {
    assert this.frameData().fragment();
    final var out = Frame.createRaw();
    out.reliableIndex(this.reliableIndex).
      sequenceIndex(this.sequenceIndex)
      .orderIndex(this.orderIndex)
      .frameData(Data.read(fullData, fullData.remaining(), false));
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
        (data.remaining() + dataSplitSize - 1) / dataSplitSize;
      for (var splitIndexIterator = 0; splitIndexIterator < splitCountTotal;
           splitIndexIterator++) {
        final var length = Math.min(dataSplitSize, data.remaining());
        final var out = Frame.createRaw();
        out.reliableIndex(reliableIndex)
          .sequenceIndex(this.sequenceIndex)
          .orderIndex(this.orderIndex)
          .splitCount(splitCountTotal)
          .splitId(splitID)
          .splitIndex(splitIndexIterator)
          .hasSplit(true)
          .frameData(Data.read(data, length, true));
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
  public Data frameData() {
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
  public void produce(@NotNull final ByteBufAllocator alloc, @NotNull final PacketBuffer out) {
    final var header = new PacketBuffer(alloc.ioBuffer(Frame.HEADER_SIZE, Frame.HEADER_SIZE));
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
  public PacketBuffer retainedFragmentData() {
    assert this.frameData().fragment();
    return this.frameData().createData();
  }

  /**
   * obtains the retained frame data.
   *
   * @return retained frame data.
   */
  @NotNull
  public Data retainedFrameData() {
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
  public void write(@NotNull final PacketBuffer buffer) {
    this.writeHeader(buffer);
    this.frameData().write(buffer);
  }

  /**
   * writes header.
   *
   * @param buffer the buffer to write.
   */
  public void writeHeader(@NotNull final PacketBuffer buffer) {
    buffer.writeByte(this.reliability().code() << 5 | (this.hasSplit ? Frame.SPLIT_FLAG : 0));
    buffer.writeShort(this.dataSize() * Byte.SIZE);
    assert !(this.hasSplit && !this.reliability().isReliable());
    if (this.reliability().isReliable()) {
      buffer.writeTriadLE(this.reliableIndex);
    }
    if (this.reliability().isSequenced()) {
      buffer.writeTriadLE(this.sequenceIndex);
    }
    if (this.reliability().isOrdered()) {
      buffer.writeTriadLE(this.orderIndex);
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

  /**
   * a class that represents frame data packets.
   */
  @Accessors(fluent = true)
  public static final class Data extends AbstractReferenceCounted implements FramedPacket {

    /**
     * the leak detector.
     */
    private static final ResourceLeakDetector<Data> LEAK_DETECTOR =
      ResourceLeakDetectorFactory.instance().newResourceLeakDetector(Data.class);

    /**
     * the recycler.
     */
    private static final ObjectPool<Data> RECYCLER = ObjectPool.newPool(Data::new);

    /**
     * the handle.
     */
    @NotNull
    private final ObjectPool.Handle<Data> handle;

    /**
     * the data.
     */
    @Nullable
    private PacketBuffer data;

    /**
     * the fragment.
     */
    @Setter
    @Getter
    private boolean fragment;

    /**
     * the order id.
     */
    @Setter
    @Getter
    private int orderChannel;

    /**
     * teh reliability.
     */
    @Nullable
    @Setter
    private Reliability reliability;

    /**
     * the tracker.
     */
    @Nullable
    @Setter
    @Getter
    private ResourceLeakTracker<Data> tracker;

    /**
     * ctor.
     *
     * @param handle the handle.
     */
    private Data(@NotNull final ObjectPool.Handle<Data> handle) {
      this.handle = handle;
      this.setRefCnt(0);
    }

    /**
     * creates a frame data.
     *
     * @param alloc the alloc to create.
     * @param packetId the packet id to create.
     * @param buffer the buffer to create.
     *
     * @return frame data.
     */
    @NotNull
    public static Data create(@NotNull final ByteBufAllocator alloc, final int packetId,
                              @NotNull final PacketBuffer buffer) {
      final var out = new PacketBuffer(alloc.compositeDirectBuffer(2));
      try {
        out.addComponent(true, new PacketBuffer(alloc.ioBuffer(1, 1).writeByte(packetId)));
        out.addComponent(true, buffer.retain());
        return Data.read(out, out.remaining(), false);
      } finally {
        out.release();
      }
    }

    /**
     * reads buffer and creates a frame data from it.
     *
     * @param buffer the buffer to read.
     * @param length the length to read.
     * @param fragment the fragment to read.
     *
     * @return frame data.
     */
    @NotNull
    public static Data read(@NotNull final PacketBuffer buffer, final int length, final boolean fragment) {
      assert length > 0;
      final var packet = Data.createRaw();
      try {
        packet.data(buffer.readRetainedSlice(length))
          .fragment(fragment);
        assert packet.dataSize() == length;
        return packet.retain();
      } finally {
        packet.release();
      }
    }

    /**
     * creates a raw frame data.
     *
     * @return raw frame data.
     */
    @NotNull
    private static Data createRaw() {
      final var out = Data.RECYCLER.get();
      assert out.refCnt() == 0 && out.tracker() == null : "bad reuse";
      out.orderChannel(0)
        .fragment(false)
        .data(null)
        .reliability(FramedPacket.Reliability.RELIABLE_ORDERED)
        .setRefCnt(1);
      out.tracker(Data.LEAK_DETECTOR.track(out));
      return out;
    }

    /**
     * creates a data.
     *
     * @return data.
     */
    @NotNull
    public PacketBuffer createData() {
      return this.data().retainedDuplicate();
    }

    /**
     * obtains the data.
     *
     * @return data.
     */
    @NotNull
    public PacketBuffer data() {
      return Objects.requireNonNull(this.data, "data");
    }

    /**
     * sets the data.
     *
     * @param data the data to set.
     *
     * @return {@code this} for builder chain.
     */
    @NotNull
    public Data data(@Nullable final PacketBuffer data) {
      this.data = data;
      return this;
    }

    /**
     * obtains the data size.
     *
     * @return data size.
     */
    public int dataSize() {
      return this.data().remaining();
    }

    @Override
    public void decode(@NotNull final PacketBuffer buffer) {
    }

    @Override
    public void encode(@NotNull final PacketBuffer buffer) {
    }

    /**
     * obtains the packet id.
     *
     * @return packet id.
     */
    public int packetId() {
      assert !this.fragment;
      return this.data().unsignedByte(this.data().readerIndex());
    }

    @NotNull
    @Override
    public Reliability reliability() {
      return Objects.requireNonNull(this.reliability, "reliability");
    }

    @Override
    public Data retain() {
      return (Data) super.retain();
    }

    @Override
    protected void deallocate() {
      if (this.data != null) {
        this.data().release();
        this.data = null;
      }
      if (this.tracker != null) {
        this.tracker.close(this);
        this.tracker = null;
      }
      this.handle.recycle(this);
    }

    @Override
    public String toString() {
      return String.format("Frame.Data(%s, length: %s, framed: %s, packetId: %s)",
        this.reliability, this.dataSize(), this.fragment,
        this.fragment ? "n/a" : String.format("%02x", this.packetId()));
    }

    @Override
    public ReferenceCounted touch(final Object hint) {
      if (this.tracker != null) {
        this.tracker.record(hint);
      }
      this.data().touch(hint);
      return this;
    }

    /**
     * writes the packet into the buffer.
     *
     * @param buffer the buffer to write.
     */
    public void write(@NotNull final PacketBuffer buffer) {
      buffer.writeBytes(this.data(), this.data().readerIndex(), this.data().remaining());
    }
  }

  /**
   * a class that represents frame set packets.
   */
  @Accessors(fluent = true)
  public static final class Set extends AbstractReferenceCounted implements Packet {

    /**
     * the header size.
     */
    public static final int HEADER_SIZE = 4;

    /**
     * the leak detector.
     */
    private static final ResourceLeakDetector<Set> LEAK_DETECTOR =
      ResourceLeakDetectorFactory.instance().newResourceLeakDetector(Set.class);

    /**
     * the recycler.
     */
    private static final ObjectPool<Set> RECYCLER = ObjectPool.newPool(Set::new);

    /**
     * the frames.
     */
    @Getter
    private final List<Frame> frames = new ArrayList<>(8);

    /**
     * the handle.
     */
    @NotNull
    private final ObjectPool.Handle<Set> handle;

    /**
     * the sent time.
     */
    @Getter
    @Setter
    private long sentTime;

    /**
     * the sequence id.
     */
    @Getter
    @Setter
    private int sequenceId;

    /**
     * the tracker.
     */
    @Nullable
    @Getter
    @Setter
    private ResourceLeakTracker<Set> tracker;

    /**
     * ctor.
     *
     * @param handle the handle.
     */
    private Set(@NotNull final ObjectPool.Handle<Set> handle) {
      this.handle = handle;
      this.setRefCnt(0);
    }

    /**
     * create a frame set packet
     *
     * @return frame set.
     */
    @NotNull
    public static Frame.Set create() {
      final var out = Set.RECYCLER.get();
      assert out.refCnt() == 0;
      assert out.tracker() == null;
      out.sentTime(System.nanoTime());
      out.sequenceId(0);
      out.tracker(Set.LEAK_DETECTOR.track(out));
      out.setRefCnt(1);
      return out;
    }

    /**
     * reads the buffer and creates a frame set.
     *
     * @param buffer the buffer to read.
     *
     * @return frame set.
     */
    @NotNull
    public static Frame.Set read(@NotNull final PacketBuffer buffer) {
      final var out = Set.create();
      try {
        buffer.skip(1);
        out.sequenceId(buffer.readUnsignedTriadLE());
        while (buffer.isReadable()) {
          out.frames().add(Frame.read(buffer));
        }
        return out.retain();
      } catch (final IndexOutOfBoundsException e) {
        throw new CorruptedFrameException("Failed to parse Frame", e);
      } finally {
        out.release();
      }
    }

    /**
     * adds the packet.
     *
     * @param packet the packet to add.
     */
    public void addPacket(@NotNull final Frame packet) {
      this.frames.add(packet);
    }

    /**
     * creates frames.
     *
     * @param consumer the consumer to craete.
     */
    public void createFrames(@NotNull final Consumer<Frame> consumer) {
      this.frames.forEach(frame -> consumer.accept(frame.retain()));
    }

    @Override
    public void decode(@NotNull final PacketBuffer buffer) {
    }

    @Override
    public void encode(@NotNull final PacketBuffer buffer) {
    }

    @Override
    public int initialSizeHint() {
      return this.roughSize();
    }

    /**
     * does fail with the throwable.
     *
     * @param throwable the throwable to fail.
     */
    public void fail(@NotNull final Throwable throwable) {
      this.frames.forEach(frame -> {
        final var promise = frame.promise();
        if (promise != null) {
          promise.tryFailure(throwable);
          frame.promise(null);
        }
      });
    }

    /**
     * checks if the frames is empty.
     *
     * @return frames is empty.
     */
    public boolean isEmpty() {
      return this.frames.isEmpty();
    }

    /**
     * produces a byte buffer.
     *
     * @param alloc the alloc to produce.
     *
     * @return byte buffer.
     */
    @NotNull
    public PacketBuffer produce(@NotNull final ByteBufAllocator alloc) {
      final var header = new PacketBuffer(alloc.ioBuffer(Set.HEADER_SIZE, Set.HEADER_SIZE));
      final var out = new PacketBuffer(alloc.compositeDirectBuffer(1 + this.frames.size() * 2));
      try {
        this.writeHeader(header);
        out.addComponent(true, header.retain());
        this.frames.forEach(frame -> frame.produce(alloc, out));
        return out.retain();
      } finally {
        header.release();
        out.release();
      }
    }

    @Override
    public Set retain() {
      super.retain();
      return this;
    }

    /**
     * deallocates the frame set.
     */
    @Override
    public void deallocate() {
      this.frames.forEach(Frame::release);
      this.frames.clear();
      if (this.tracker != null) {
        this.tracker.close(this);
        this.tracker = null;
      }
      this.handle.recycle(this);
    }

    /**
     * obtains rough size.
     *
     * @return rough size.
     */
    public int roughSize() {
      var out = Set.HEADER_SIZE;
      out += this.frames.stream().mapToInt(Frame::roughPacketSize).sum();
      return out;
    }

    /**
     * success all frames.
     */
    public void succeed() {
      this.frames.forEach(frame -> {
        final var promise = frame.promise();
        if (promise != null) {
          promise.trySuccess();
          frame.promise(null);
        }
      });
    }

    @NotNull
    @Override
    public String toString() {
      return String.format("Frame.Set(frames: %s, seq: %s)", this.frames.size(), this.sequenceId);
    }

    @Override
    @NotNull
    public ReferenceCounted touch(@NotNull final Object hint) {
      if (this.tracker != null) {
        this.tracker.record(hint);
      }
      this.frames.forEach(packet -> packet.touch(hint));
      return this;
    }

    /**
     * writes the buffer.
     *
     * @param buffer the buffer to write.
     */
    public void write(@NotNull final PacketBuffer buffer) {
      this.writeHeader(buffer);
      this.frames.forEach(frame -> frame.write(buffer));
    }

    /**
     * writes the header.
     *
     * @param buffer the buffer to write.
     */
    public void writeHeader(@NotNull final PacketBuffer buffer) {
      buffer.writeByte(Ids.FRAME_DATA_START);
      buffer.writeTriadLE(this.sequenceId);
    }
  }
}
