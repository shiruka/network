package io.github.shiruka.network.packets;

import io.github.shiruka.network.PacketBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetectorFactory;
import io.netty.util.ResourceLeakTracker;
import io.netty.util.internal.ObjectPool;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents frame data packets.
 */
@Accessors(fluent = true)
public final class FrameData extends AbstractReferenceCounted implements FramedPacket {

  /**
   * the leak detector.
   */
  private static final ResourceLeakDetector<FrameData> LEAK_DETECTOR =
    ResourceLeakDetectorFactory.instance().newResourceLeakDetector(FrameData.class);

  /**
   * the recycler.
   */
  private static final ObjectPool<FrameData> recycler = ObjectPool.newPool(FrameData::new);

  /**
   * the handle.
   */
  @NotNull
  private final ObjectPool.Handle<FrameData> handle;

  /**
   * the data.
   */
  @Setter
  @Getter
  private ByteBuf data;

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
  @Setter
  @Getter
  private Reliability reliability;

  /**
   * the tracker.
   */
  @Setter
  @Getter
  private ResourceLeakTracker<FrameData> tracker;

  /**
   * ctor.
   *
   * @param handle the handle.
   */
  private FrameData(@NotNull final ObjectPool.Handle<FrameData> handle) {
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
  public static FrameData create(@NotNull final ByteBufAllocator alloc, final int packetId,
                                 @NotNull final ByteBuf buffer) {
    final var out = alloc.compositeDirectBuffer(2);
    try {
      out.addComponent(true, alloc.ioBuffer(1, 1).writeByte(packetId));
      out.addComponent(true, buffer.retain());
      return FrameData.read(out, out.readableBytes(), false);
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
  public static FrameData read(@NotNull final ByteBuf buffer, final int length, final boolean fragment) {
    assert length > 0;
    final FrameData packet = FrameData.createRaw();
    try {
      packet.data(buffer.readRetainedSlice(length));
      packet.fragment(fragment);
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
  private static FrameData createRaw() {
    final var out = FrameData.recycler.get();
    assert out.refCnt() == 0 && out.tracker() == null : "bad reuse";
    out.orderChannel(0);
    out.fragment(false);
    out.data(null);
    out.reliability(FramedPacket.Reliability.RELIABLE_ORDERED);
    out.setRefCnt(1);
    out.tracker(FrameData.LEAK_DETECTOR.track(out));
    return out;
  }

  /**
   * creates a data.
   *
   * @return data.
   */
  @NotNull
  public ByteBuf createData() {
    return this.data.retainedDuplicate();
  }

  /**
   * obtains the data size.
   *
   * @return data size.
   */
  public int dataSize() {
    return this.data.readableBytes();
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
    return this.data.getUnsignedByte(this.data.readerIndex());
  }

  @Override
  public FrameData retain() {
    return (FrameData) super.retain();
  }

  @Override
  protected void deallocate() {
    if (this.data != null) {
      this.data.release();
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
    return String.format("PacketData(%s, length: %s, framed: %s, packetId: %s)",
      this.reliability, this.dataSize(), this.fragment,
      this.fragment ? "n/a" : String.format("%02x", this.packetId()));
  }

  @Override
  public ReferenceCounted touch(final Object hint) {
    if (this.tracker != null) {
      this.tracker.record(hint);
    }
    this.data.touch(hint);
    return this;
  }

  /**
   * writes the packet into the buffer.
   *
   * @param buffer the buffer to write.
   */
  public void write(final ByteBuf buffer) {
    buffer.writeBytes(this.data, this.data.readerIndex(), this.data.readableBytes());
  }
}
