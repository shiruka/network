package io.github.shiruka.network.packets;

import io.github.shiruka.network.Ids;
import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetectorFactory;
import io.netty.util.ResourceLeakTracker;
import io.netty.util.internal.ObjectPool;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents frame set packets.
 */
@Accessors(fluent = true)
public final class FrameSet extends AbstractReferenceCounted implements Packet {

  /**
   * the header size.
   */
  public static final int HEADER_SIZE = 4;

  /**
   * the leak detector.
   */
  private static final ResourceLeakDetector<FrameSet> LEAK_DETECTOR =
    ResourceLeakDetectorFactory.instance().newResourceLeakDetector(FrameSet.class);

  /**
   * the recycler.
   */
  private static final ObjectPool<FrameSet> RECYCLER = ObjectPool.newPool(FrameSet::new);

  /**
   * the frames.
   */
  @Getter
  private final List<Frame> frames = new ArrayList<>(8);

  /**
   * the handle.
   */
  @NotNull
  private final ObjectPool.Handle<FrameSet> handle;

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
  @Getter
  @Setter
  private ResourceLeakTracker<FrameSet> tracker;

  /**
   * ctor.
   *
   * @param handle the handle.
   */
  private FrameSet(@NotNull final ObjectPool.Handle<FrameSet> handle) {
    this.handle = handle;
    this.setRefCnt(0);
  }

  /**
   * create a frame set packet
   *
   * @return frame set.
   */
  @NotNull
  public static FrameSet create() {
    final var out = FrameSet.RECYCLER.get();
    assert out.refCnt() == 0;
    assert out.tracker() == null;
    out.sentTime(System.nanoTime());
    out.sequenceId(0);
    out.tracker(FrameSet.LEAK_DETECTOR.track(out));
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
  public static FrameSet read(@NotNull final ByteBuf buffer) {
    final var out = FrameSet.create();
    try {
      buffer.skipBytes(1);
      out.sequenceId(buffer.readUnsignedMediumLE());
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
  public ByteBuf produce(@NotNull final ByteBufAllocator alloc) {
    final var header = alloc.ioBuffer(FrameSet.HEADER_SIZE, FrameSet.HEADER_SIZE);
    final var out = alloc.compositeDirectBuffer(1 + this.frames.size() * 2);
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
  public FrameSet retain() {
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
    var out = FrameSet.HEADER_SIZE;
    out += this.frames.stream().mapToInt(packet -> packet.roughPacketSize()).sum();
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
    return String.format("FramedData(frames: %s, seq: %s)", this.frames.size(), this.sequenceId);
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
  public void write(@NotNull final ByteBuf buffer) {
    this.writeHeader(buffer);
    this.frames.forEach(frame -> frame.write(buffer));
  }

  /**
   * writes the header.
   *
   * @param buffer the buffer to write.
   */
  public void writeHeader(@NotNull final ByteBuf buffer) {
    buffer.writeByte(Ids.FRAME_DATA_START);
    buffer.writeMediumLE(this.sequenceId);
  }
}
