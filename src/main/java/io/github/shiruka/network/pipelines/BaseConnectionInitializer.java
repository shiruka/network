package io.github.shiruka.network.pipelines;

import io.github.shiruka.network.Packet;
import io.github.shiruka.network.options.RakNetConfig;
import io.github.shiruka.network.packets.ConnectionFailed;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * an abstract class that represents connection initializer pipelines.
 */
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseConnectionInitializer extends SimpleChannelInboundHandler<Packet> {

  /**
   * the name.
   */
  public static final String NAME = "rn-init-connect";

  /**
   * the connect promise.
   */
  @NotNull
  @Getter
  private final ChannelPromise connectPromise;

  /**
   * the state.
   */
  @NotNull
  @Getter
  @Setter
  private State state = State.CR1;

  /**
   * the connect timer.
   */
  @Nullable
  private ScheduledFuture<?> connectTimer;

  /**
   * the send timer.
   */
  @Nullable
  private ScheduledFuture<?> sendTimer;

  /**
   * starts ping.
   *
   * @param ctx the ctx to start.
   */
  protected static void startPing(@NotNull final ChannelHandlerContext ctx) {
    ctx.channel().pipeline().addAfter(BaseConnectionInitializer.NAME, PingProducer.NAME, new PingProducer());
  }

  /**
   * obtains the connect timer.
   *
   * @return connect timer.
   */
  @NotNull
  public final ScheduledFuture<?> connectTimer() {
    return Objects.requireNonNull(this.connectTimer, "connect timer");
  }

  @Override
  public final void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    ctx.writeAndFlush(new ConnectionFailed(RakNetConfig.cast(ctx).magic())).addListener(v -> this.fail(cause));
  }

  @Override
  public final void handlerAdded(final ChannelHandlerContext ctx) {
    this.sendTimer = ctx.channel().eventLoop().scheduleAtFixedRate(() -> this.sendRequest(ctx),
      0, 200, TimeUnit.MILLISECONDS);
    this.connectTimer = ctx.channel().eventLoop().schedule(this::doTimeout,
      ctx.channel().config().getConnectTimeoutMillis(), TimeUnit.MILLISECONDS);
    this.sendRequest(ctx);
  }

  @Override
  public final void handlerRemoved(final ChannelHandlerContext ctx) {
    this.sendTimer().cancel(false);
    this.connectTimer().cancel(false);
  }

  /**
   * obtains the send timer.
   *
   * @return send timer.
   */
  @NotNull
  public final ScheduledFuture<?> sendTimer() {
    return Objects.requireNonNull(this.sendTimer, "send timer");
  }

  /**
   * does timeout.
   */
  protected final void doTimeout() {
    this.fail(new ConnectTimeoutException());
  }

  /**
   * fails.
   *
   * @param cause the cause to fail.
   */
  protected final void fail(@NotNull final Throwable cause) {
    this.connectPromise.tryFailure(cause);
  }

  /**
   * finishes the connection.
   *
   * @param ctx the ctx to finish.
   */
  protected final void finish(@NotNull final ChannelHandlerContext ctx) {
    final var channel = ctx.channel();
    this.connectPromise.trySuccess();
    this.removeHandler(ctx);
    channel.pipeline().fireChannelActive();
  }

  /**
   * removes handler.
   *
   * @param ctx the ctx to remove.
   */
  protected abstract void removeHandler(@NotNull ChannelHandlerContext ctx);

  /**
   * sends request.
   *
   * @param ctx the ctx to send.
   */
  protected abstract void sendRequest(@NotNull ChannelHandlerContext ctx);

  /**
   * an enum class that contains connection states.
   */
  protected enum State {
    /**
     * the c r 1.
     */
    CR1,
    /**
     * the c r 2.
     */
    CR2,
    /**
     * the c r 3.
     */
    CR3,
  }
}
