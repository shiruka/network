package io.github.shiruka.network.pipelines;

import io.github.shiruka.network.packets.ConnectedPing;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * a class that represents ping producer pipelines.
 */
public final class PingProducer implements ChannelHandler {

  /**
   * the name.
   */
  public static final String NAME = "rn-ping-producer";

  /**
   * the ping task.
   */
  @Nullable
  private ScheduledFuture<?> pingTask;

  @Override
  public void handlerAdded(final ChannelHandlerContext ctx) {
    this.pingTask = ctx.channel().eventLoop().scheduleAtFixedRate(
      () -> ctx.writeAndFlush(new ConnectedPing()),
      0, 200, TimeUnit.MILLISECONDS
    );
  }

  @Override
  public void handlerRemoved(final ChannelHandlerContext ctx) {
    if (this.pingTask != null) {
      this.pingTask.cancel(false);
      this.pingTask = null;
    }
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    ctx.fireExceptionCaught(cause);
  }

  /**
   * obtains the ping task.
   *
   * @return ping task.
   */
  @NotNull
  public ScheduledFuture<?> pingTask() {
    return Objects.requireNonNull(this.pingTask, "ping task");
  }
}
