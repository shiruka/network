package io.github.shiruka.network.pipelines;

import io.github.shiruka.network.Constants;
import io.github.shiruka.network.options.RakNetChannelOptions;
import io.github.shiruka.network.options.RakNetConfig;
import io.github.shiruka.network.packets.Ack;
import io.github.shiruka.network.packets.Frame;
import io.github.shiruka.network.packets.Nack;
import io.github.shiruka.network.utils.Integers;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.DecoderException;
import io.netty.util.ReferenceCountUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * a class that represents reliability handler pipelines.
 */
public final class ReliabilityHandler extends ChannelDuplexHandler {

  /**
   * the name.
   */
  public static final String NAME = "rn-reliability";

  /**
   * the act set.
   */
  private final IntSortedSet ackSet = new IntRBTreeSet(Integers.B3.COMPARATOR);

  /**
   * the frame queue.
   */
  private final ObjectSortedSet<Frame> frameQueue = new ObjectRBTreeSet<>(
    Frame.COMPARATOR
  );

  /**
   * the nack set.
   */
  private final IntSortedSet nackSet = new IntRBTreeSet(Integers.B3.COMPARATOR);

  /**
   * the pending frame sets.
   */
  private final Int2ObjectMap<Frame.Set> pendingFrameSets = new Int2ObjectOpenHashMap<>();

  /**
   * the burst tokens.
   */
  private int burstTokens;

  /**
   * the last received sequence id.
   */
  private int lastReceivedSequenceId;

  /**
   * the next send sequence id.
   */
  private int nextSendSequenceId;

  /**
   * the resend gauge.
   */
  private int resendGauge;

  @Override
  public void channelRead(
    @NotNull final ChannelHandlerContext ctx,
    @NotNull final Object msg
  ) {
    try {
      if (msg instanceof Ack ack) {
        this.readAck(ctx, ack);
      } else if (msg instanceof Nack nack) {
        this.readNack(ctx, nack);
      } else if (msg instanceof Frame.Set set) {
        this.readFrameSet(ctx, set);
      } else {
        ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
      }
    } finally {
      ReferenceCountUtil.release(msg);
    }
  }

  @Override
  public void userEventTriggered(
    final ChannelHandlerContext ctx,
    final Object evt
  ) {
    if (evt instanceof FlushTickHandler.MissedFlushes missedFlushes) {
      this.updateBurstTokens(ctx, missedFlushes.flushes());
    }
    ctx.fireUserEventTriggered(evt);
  }

  @Override
  public void handlerAdded(final ChannelHandlerContext ctx) {
    ctx.channel().attr(RakNetChannelOptions.WRITABLE).set(true);
  }

  @Override
  public void handlerRemoved(final ChannelHandlerContext ctx) {
    this.clearQueue(null);
  }

  @Override
  public void write(
    final ChannelHandlerContext ctx,
    final Object msg,
    final ChannelPromise promise
  ) {
    if (msg instanceof Frame frame) {
      this.queueFrame(ctx, frame);
      frame.promise(promise);
    } else {
      ctx.write(msg, promise);
    }
    if (this.pendingFrameSets.size() > Constants.MAX_PACKET_LOSS) {
      throw new DecoderException(
        "Too big packet loss: unconfirmed sent packets!"
      );
    }
    FlushTickHandler.checkFlushTick(ctx.channel());
  }

  @Override
  public void flush(final ChannelHandlerContext ctx) {
    if (!ctx.channel().isOpen()) {
      ctx.flush();
      return;
    }
    this.sendResponses(ctx);
    this.recallExpiredFrameSets(ctx);
    this.updateBurstTokens(ctx, 1);
    this.produceFrameSets(ctx);
    this.updateBackPressure(ctx);
    if (this.pendingFrameSets.size() > Constants.MAX_PACKET_LOSS) {
      throw new DecoderException("Too big packet loss: resend queue!");
    }
    ctx.flush();
  }

  /**
   * adjusts resend gauge.
   *
   * @param ctx the ctx to adjust.
   * @param n the n to adjust.
   */
  private void adjustResendGauge(
    @NotNull final ChannelHandlerContext ctx,
    final int n
  ) {
    final var config = RakNetConfig.cast(ctx);
    this.resendGauge =
      Math.max(
        -config.defaultPendingFrameSets(),
        Math.min(config.defaultPendingFrameSets(), this.resendGauge + n)
      );
  }

  /**
   * clears the queue.
   *
   * @param throwable the throwable to clear.
   */
  private void clearQueue(@Nullable final Throwable throwable) {
    if (throwable != null) {
      this.frameQueue.stream()
        .map(Frame::promise)
        .forEach(promise -> {
          if (promise != null) {
            promise.tryFailure(throwable);
          }
        });
      this.pendingFrameSets.values().forEach(set -> set.fail(throwable));
    }
    this.frameQueue.forEach(Frame::release);
    this.frameQueue.clear();
    this.pendingFrameSets.values().forEach(Frame.Set::release);
    this.pendingFrameSets.clear();
  }

  /**
   * obtains the queued bytes.
   *
   * @return queued bytes.
   */
  private int getQueuedBytes() {
    return this.frameQueue.stream().mapToInt(Frame::roughPacketSize).sum();
  }

  /**
   * produces frame set.
   *
   * @param ctx the ctx to produce.
   * @param maxSize the max size to produce.
   */
  private void produceFrameSet(
    @NotNull final ChannelHandlerContext ctx,
    final int maxSize
  ) {
    final var itr = this.frameQueue.iterator();
    final var frameSet = Frame.Set.create();
    while (itr.hasNext()) {
      final var frame = itr.next();
      assert frame.refCnt() > 0 : "Frame has lost reference!";
      if (frameSet.roughSize() + frame.roughPacketSize() > maxSize) {
        if (frameSet.isEmpty()) {
          throw new CorruptedFrameException(
            "Finished frame larger than the MTU by %d!".formatted(
                frame.roughPacketSize() - maxSize
              )
          );
        }
        break;
      }
      itr.remove();
      frameSet.addPacket(frame);
    }
    if (!frameSet.isEmpty()) {
      frameSet.sequenceId(this.nextSendSequenceId);
      this.nextSendSequenceId = Integers.B3.plus(this.nextSendSequenceId, 1);
      this.pendingFrameSets.put(frameSet.sequenceId(), frameSet);
      frameSet.touch("Added to pending FrameSet list");
      ctx
        .write(frameSet.retain())
        .addListener(Constants.INTERNAL_WRITE_LISTENER);
      assert frameSet.refCnt() > 0;
    } else {
      frameSet.release();
    }
  }

  /**
   * produces frame sets.
   *
   * @param ctx the ctx to produce.
   */
  private void produceFrameSets(@NotNull final ChannelHandlerContext ctx) {
    final var config = RakNetConfig.cast(ctx);
    final var maxSize =
      config.mtu() - Frame.Set.HEADER_SIZE - Frame.HEADER_SIZE;
    final var maxPendingFrameSets =
      config.defaultPendingFrameSets() + this.burstTokens;
    while (
      this.pendingFrameSets.size() < maxPendingFrameSets &&
      !this.frameQueue.isEmpty()
    ) {
      this.produceFrameSet(ctx, maxSize);
    }
  }

  /**
   * queues the frame.
   *
   * @param ctx the ctx to queue.
   * @param frame the frame to queue.
   */
  private void queueFrame(
    @NotNull final ChannelHandlerContext ctx,
    @NotNull final Frame frame
  ) {
    final var config = RakNetConfig.cast(ctx);
    if (frame.roughPacketSize() > config.mtu()) {
      throw new CorruptedFrameException(
        "Finished frame larger than the MTU by %d!".formatted(
            frame.roughPacketSize() - config.mtu()
          )
      );
    }
    this.frameQueue.add(frame);
  }

  /**
   * reads ack.
   *
   * @param ctx the ctx to read.
   * @param ack the ack to read.
   */
  private void readAck(
    @NotNull final ChannelHandlerContext ctx,
    @NotNull final Ack ack
  ) {
    //    var ackdBytes = 0;
    var nIterations = 0;
    for (final var entry : ack.entries()) {
      final var max = Integers.B3.plus(entry.idFinish(), 1);
      for (var id = entry.idStart(); id != max; id = Integers.B3.plus(id, 1)) {
        final var frameSet = this.pendingFrameSets.remove(id);
        if (frameSet != null) {
          //          ackdBytes += frameSet.roughSize();
          this.adjustResendGauge(ctx, 1);
          frameSet.succeed();
          frameSet.release();
        }
        if (nIterations++ > Constants.MAX_PACKET_LOSS) {
          throw new DecoderException("Too big packet loss: ack confirm range!");
        }
      }
    }
  }

  /**
   * reads frame set.
   *
   * @param ctx the ctx to read.
   * @param frameSet the frame set to read.
   */
  private void readFrameSet(
    @NotNull final ChannelHandlerContext ctx,
    @NotNull final Frame.Set frameSet
  ) {
    final var packetSeqId = frameSet.sequenceId();
    this.ackSet.add(packetSeqId);
    this.nackSet.remove(packetSeqId);
    if (Integers.B3.minusWrap(packetSeqId, this.lastReceivedSequenceId) > 0) {
      this.lastReceivedSequenceId =
        Integers.B3.plus(this.lastReceivedSequenceId, 1);
      while (this.lastReceivedSequenceId != packetSeqId) {
        this.nackSet.add(this.lastReceivedSequenceId);
        this.lastReceivedSequenceId =
          Integers.B3.plus(this.lastReceivedSequenceId, 1);
      }
    }
    frameSet.createFrames(ctx::fireChannelRead);
    ctx.fireChannelReadComplete();
  }

  /**
   * reads nack.
   *
   * @param ctx the ctx to read.
   * @param nack the nack to read.
   */
  private void readNack(
    @NotNull final ChannelHandlerContext ctx,
    @NotNull final Nack nack
  ) {
    //    var bytesNACKd = 0;
    var nIterations = 0;
    for (final var entry : nack.entries()) {
      final var max = Integers.B3.plus(entry.idFinish(), 1);
      for (var id = entry.idStart(); id != max; id = Integers.B3.plus(id, 1)) {
        final var frameSet = this.pendingFrameSets.remove(id);
        if (frameSet != null) {
          //          bytesNACKd += frameSet.roughSize();
          this.recallFrameSet(ctx, frameSet);
        }
        if (nIterations++ > Constants.MAX_PACKET_LOSS) {
          throw new DecoderException(
            "Too big packet loss: nack confirm range!"
          );
        }
      }
    }
  }

  /**
   * recalls expired frame sets.
   *
   * @param ctx the ctx to recall.
   */
  private void recallExpiredFrameSets(
    @NotNull final ChannelHandlerContext ctx
  ) {
    final var config = RakNetConfig.cast(ctx);
    final var packetItr = this.pendingFrameSets.values().iterator();
    final var deadline =
      System.nanoTime() -
      (
        config.rttNanos() +
        2 *
        config.rttStdDevNanos() +
        config.retryDelayNanos()
      );
    while (packetItr.hasNext()) {
      final var frameSet = packetItr.next();
      if (frameSet.sentTime() < deadline) {
        packetItr.remove();
        this.recallFrameSet(ctx, frameSet);
      }
    }
  }

  /**
   * recalls frame set.
   *
   * @param ctx the ctx to recall.
   * @param frameSet the frame set to recall.
   */
  private void recallFrameSet(
    @NotNull final ChannelHandlerContext ctx,
    @NotNull final Frame.Set frameSet
  ) {
    try {
      this.adjustResendGauge(ctx, -1);
      frameSet.touch("Recalled");
      frameSet.createFrames(frame -> {
        if (frame.reliability().isReliable()) {
          this.queueFrame(ctx, frame);
        } else {
          final var promise = frame.promise();
          if (promise != null) {
            promise.trySuccess();
          }
          frame.release();
        }
      });
    } finally {
      frameSet.release();
    }
  }

  /**
   * sends responses.
   *
   * @param ctx the ctx to send.
   */
  private void sendResponses(@NotNull final ChannelHandlerContext ctx) {
    final var config = RakNetConfig.cast(ctx);
    if (!this.ackSet.isEmpty()) {
      ctx
        .write(new Ack(this.ackSet))
        .addListener(Constants.INTERNAL_WRITE_LISTENER);
      this.ackSet.clear();
    }
    if (!this.nackSet.isEmpty() && config.isAutoRead()) {
      ctx
        .write(new Nack(this.nackSet))
        .addListener(Constants.INTERNAL_WRITE_LISTENER);
      this.nackSet.clear();
    }
  }

  /**
   * updates the back pressure.
   *
   * @param ctx the ctx to update.
   */
  private void updateBackPressure(@NotNull final ChannelHandlerContext ctx) {
    final var config = RakNetConfig.cast(ctx);
    final var queuedBytes = this.getQueuedBytes();
    final var oldWritable = ctx
      .channel()
      .attr(RakNetChannelOptions.WRITABLE)
      .get();
    var newWritable = oldWritable;
    if (queuedBytes > config.maxQueuedBytes()) {
      final var exception = new CodecException("Frame queue is too large!");
      this.clearQueue(exception);
      ctx.close();
      throw exception;
    } else if (queuedBytes > config.getWriteBufferHighWaterMark()) {
      newWritable = false;
    } else if (queuedBytes < config.getWriteBufferLowWaterMark()) {
      newWritable = true;
    }
    if (newWritable != oldWritable) {
      ctx
        .channel()
        .attr(RakNetChannelOptions.WRITABLE)
        .set(newWritable ? Boolean.TRUE : Boolean.FALSE);
      ctx.fireChannelWritabilityChanged();
    }
  }

  /**
   * updates the burst tokens.
   *
   * @param ctx the ctx to update.
   * @param nTicks the n ticks to update.
   */
  private void updateBurstTokens(
    @NotNull final ChannelHandlerContext ctx,
    final int nTicks
  ) {
    final var config = RakNetConfig.cast(ctx);
    final var burstUnused = this.pendingFrameSets.size() < this.burstTokens / 2;
    if (this.resendGauge > 1 && !burstUnused) {
      this.burstTokens += nTicks;
    } else if (this.resendGauge < -1 || burstUnused) {
      this.burstTokens -= 3 * nTicks;
    }
    this.burstTokens =
      Math.max(Math.min(this.burstTokens, config.maxPendingFrameSets()), 0);
  }
}
