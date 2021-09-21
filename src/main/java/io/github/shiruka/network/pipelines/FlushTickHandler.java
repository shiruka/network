package io.github.shiruka.network.pipelines;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents flush tick handler pipelines.
 */
public final class FlushTickHandler extends ChannelDuplexHandler {

  /**
   * the name.
   */
  public static final String NAME = "rn-flush-tick";

  /**
   * the tick resolution.
   */
  public static final long TICK_RESOLUTION = TimeUnit.NANOSECONDS.convert(5, TimeUnit.MILLISECONDS);

  /**
   * fired near the end of a pipeline to trigger a flush check.
   */
  private static final Object FLUSH_CHECK_SIGNAL = new Object();

  /**
   * the flush task.
   */
  private ScheduledFuture<?> flushTask;

  /**
   * the last tick accum.
   */
  private long lastTickAccum = System.nanoTime();

  /**
   * the tick accum.
   */
  private long tickAccum = 0;

  /**
   * checks flush tick.
   *
   * @param channel the channel to check.
   */
  public static void checkFlushTick(@NotNull final Channel channel) {
    channel.pipeline().fireUserEventTriggered(FlushTickHandler.FLUSH_CHECK_SIGNAL);
  }

  @Override
  public void channelReadComplete(final ChannelHandlerContext ctx) {
    ctx.fireChannelReadComplete();
    this.maybeFlush(ctx.channel());
  }

  @Override
  public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
    if (evt == FlushTickHandler.FLUSH_CHECK_SIGNAL) {
      this.maybeFlush(ctx.channel());
    } else {
      ctx.fireUserEventTriggered(evt);
    }
  }

  @Override
  public void channelWritabilityChanged(final ChannelHandlerContext ctx) {
    this.maybeFlush(ctx.channel());
    ctx.fireChannelWritabilityChanged();
  }

  @Override
  public void flush(final ChannelHandlerContext ctx) {
    if (this.tickAccum >= FlushTickHandler.TICK_RESOLUTION) {
      this.tickAccum -= FlushTickHandler.TICK_RESOLUTION;
    } else {
      this.tickAccum = 0;
    }
    ctx.flush();
  }

  @Override
  public void handlerAdded(final ChannelHandlerContext ctx) {
    assert this.flushTask == null;
    this.flushTask = ctx.channel().eventLoop().scheduleAtFixedRate(
      () -> FlushTickHandler.checkFlushTick(ctx.channel()),
      0, 50, TimeUnit.MILLISECONDS
    );
  }

  @Override
  public void handlerRemoved(final ChannelHandlerContext ctx) {
    this.flushTask.cancel(false);
    this.flushTask = null;
  }

  /**
   * flushes the channel.
   *
   * @param channel the channel to flush.
   */
  private void maybeFlush(final Channel channel) {
    final var curTime = System.nanoTime();
    this.tickAccum += curTime - this.lastTickAccum;
    this.lastTickAccum = curTime;
    if (this.tickAccum >= FlushTickHandler.TICK_RESOLUTION) {
      channel.flush();
      final var nFlushes = (int) (this.tickAccum / FlushTickHandler.TICK_RESOLUTION);
      if (nFlushes > 0) {
        this.tickAccum -= nFlushes * FlushTickHandler.TICK_RESOLUTION;
        channel.pipeline().fireUserEventTriggered(new MissedFlushes(nFlushes));
      }
    }
  }

  /**
   * a record class that represents missed flushes.
   *
   * @param flushes the flushes.
   */
  public record MissedFlushes(
    int flushes
  ) {

  }
}
