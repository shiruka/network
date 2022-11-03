package io.github.shiruka.network.channels;

import io.github.shiruka.network.options.RakNetConfig;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.DatagramChannel;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * an abstract class that represents proxy datagram channels.
 */
@Accessors(fluent = true)
public abstract class DatagramChannelProxy implements Channel {

  /**
   * the name.
   */
  private static final String NAME = "rn-udp-listener-handler";

  /**
   * the config.
   */
  @Getter
  private final RakNetConfig config;

  /**
   * the parent.
   */
  @NotNull
  @Getter
  private final DatagramChannel parent;

  /**
   * the pipeline.
   */
  @NotNull
  @Getter
  private final DefaultChannelPipeline pipeline;

  /**
   * ctor.
   *
   * @param supplier the supplier.
   */
  protected DatagramChannelProxy(
    @NotNull final Supplier<? extends DatagramChannel> supplier
  ) {
    this.parent = supplier.get();
    this.pipeline = this.newChannelPipeline();
    this.parent.pipeline()
      .addLast(DatagramChannelProxy.NAME, new ListenerInboundProxy(this));
    this.pipeline()
      .addLast(DatagramChannelProxy.NAME, new ListenerOutboundProxy(this))
      .addLast(
        new FlushConsolidationHandler(
          FlushConsolidationHandler.DEFAULT_EXPLICIT_FLUSH_AFTER_FLUSHES,
          true
        )
      );
    this.config = new Config(this);
  }

  /**
   * ctor.
   *
   * @param cls the cls.
   */
  protected DatagramChannelProxy(
    @NotNull final Class<? extends DatagramChannel> cls
  ) {
    this(() -> {
      try {
        return cls.getDeclaredConstructor().newInstance();
      } catch (final Exception e) {
        throw new IllegalArgumentException("Failed to create instance", e);
      }
    });
  }

  @Override
  public final <T> Attribute<T> attr(final AttributeKey<T> key) {
    return this.parent.attr(key);
  }

  @Override
  public final <T> boolean hasAttr(final AttributeKey<T> key) {
    return this.parent.hasAttr(key);
  }

  @Override
  public final ChannelFuture bind(final SocketAddress localAddress) {
    return this.pipeline.bind(localAddress);
  }

  @Override
  public final ChannelFuture connect(final SocketAddress remoteAddress) {
    return this.pipeline.connect(remoteAddress);
  }

  @Override
  public final ChannelFuture connect(
    final SocketAddress remoteAddress,
    final SocketAddress localAddress
  ) {
    return this.pipeline.connect(remoteAddress, localAddress);
  }

  @Override
  public final ChannelFuture disconnect() {
    return this.pipeline.disconnect();
  }

  @Override
  public final ChannelFuture close() {
    return this.pipeline.close();
  }

  @Override
  public final ChannelFuture deregister() {
    return this.pipeline.deregister();
  }

  @Override
  public final ChannelFuture bind(
    final SocketAddress localAddress,
    final ChannelPromise promise
  ) {
    return this.pipeline.bind(localAddress, promise);
  }

  @Override
  public final ChannelFuture connect(
    final SocketAddress remoteAddress,
    final ChannelPromise promise
  ) {
    return this.pipeline.connect(remoteAddress, promise);
  }

  @Override
  public final ChannelFuture connect(
    final SocketAddress remoteAddress,
    final SocketAddress localAddress,
    final ChannelPromise promise
  ) {
    return this.pipeline.connect(remoteAddress, localAddress, promise);
  }

  @Override
  public final ChannelFuture disconnect(final ChannelPromise promise) {
    return this.pipeline.disconnect(promise);
  }

  @Override
  public final ChannelFuture close(final ChannelPromise promise) {
    return this.pipeline.close(promise);
  }

  @Override
  public final ChannelFuture deregister(final ChannelPromise promise) {
    return this.pipeline.deregister(promise);
  }

  @Override
  public final ChannelFuture write(final Object msg) {
    return this.pipeline.write(msg);
  }

  @Override
  public final ChannelFuture write(
    final Object msg,
    final ChannelPromise promise
  ) {
    return this.pipeline.write(msg, promise);
  }

  @Override
  public final ChannelFuture writeAndFlush(
    final Object msg,
    final ChannelPromise promise
  ) {
    return this.pipeline.writeAndFlush(msg, promise);
  }

  @Override
  public final ChannelFuture writeAndFlush(final Object msg) {
    return this.pipeline.writeAndFlush(msg);
  }

  @Override
  public final ChannelPromise newPromise() {
    return this.pipeline.newPromise();
  }

  @Override
  public final ChannelProgressivePromise newProgressivePromise() {
    return this.pipeline.newProgressivePromise();
  }

  @Override
  public final ChannelFuture newSucceededFuture() {
    return this.pipeline.newSucceededFuture();
  }

  @Override
  public final ChannelFuture newFailedFuture(final Throwable cause) {
    return this.pipeline.newFailedFuture(cause);
  }

  @Override
  public final ChannelPromise voidPromise() {
    return this.pipeline.voidPromise();
  }

  @Override
  public final int compareTo(@NotNull final Channel o) {
    return this.parent.compareTo(o);
  }

  @Override
  public final ChannelId id() {
    return this.parent.id();
  }

  @Override
  public final EventLoop eventLoop() {
    return this.parent.eventLoop();
  }

  @Override
  public final boolean isOpen() {
    return this.parent.isOpen();
  }

  @Override
  public final boolean isRegistered() {
    return this.parent.isRegistered();
  }

  @Override
  public final boolean isActive() {
    return this.parent.isActive();
  }

  @Override
  public final ChannelMetadata metadata() {
    return this.parent.metadata();
  }

  @Override
  public final SocketAddress localAddress() {
    return this.parent.localAddress();
  }

  @Override
  public final SocketAddress remoteAddress() {
    return this.parent.remoteAddress();
  }

  @Override
  public final ChannelFuture closeFuture() {
    return this.parent.closeFuture();
  }

  @Override
  public final boolean isWritable() {
    return this.parent.isWritable();
  }

  @Override
  public final long bytesBeforeUnwritable() {
    return this.parent.bytesBeforeUnwritable();
  }

  @Override
  public final long bytesBeforeWritable() {
    return this.parent.bytesBeforeWritable();
  }

  @Override
  public final Unsafe unsafe() {
    return this.parent.unsafe();
  }

  @Override
  public final ByteBufAllocator alloc() {
    return this.config().getAllocator();
  }

  @Override
  public final Channel read() {
    return this;
  }

  @Override
  public final Channel flush() {
    this.pipeline.flush();
    return this;
  }

  /**
   * wraps the promise.
   *
   * @param promise the promise to wrap.
   *
   * @return wrapped promise.
   */
  protected final ChannelPromise wrapPromise(
    @NotNull final ChannelPromise promise
  ) {
    final var out = this.parent.newPromise();
    out.addListener(res -> {
      if (res.isSuccess()) {
        promise.trySuccess();
      } else {
        promise.tryFailure(res.cause());
      }
    });
    return out;
  }

  /**
   * closes the channel gracefully.
   *
   * @param promise the promise to close.
   */
  protected void gracefulClose(@NotNull final ChannelPromise promise) {
    this.parent.close(this.wrapPromise(promise));
  }

  /**
   * creates a new channel pipeline.
   *
   * @return a newly created channel pipeline.
   */
  @NotNull
  private DefaultChannelPipeline newChannelPipeline() {
    return new DefaultChannelPipeline(this) {
      @Override
      protected void onUnhandledInboundException(final Throwable cause) {
        if (cause instanceof ClosedChannelException) {
          ReferenceCountUtil.safeRelease(cause);
          return;
        }
        super.onUnhandledInboundException(cause);
      }
    };
  }

  /**
   * a class that represents datagram channel proxy configurations.
   */
  private static final class Config extends RakNetConfig.Base {

    /**
     * ctor.
     *
     * @param channel the channel.
     */
    private Config(@NotNull final DatagramChannelProxy channel) {
      super(channel);
    }

    @Override
    public <T> T getOption(final ChannelOption<T> option) {
      final var superOption = super.getOption(option);
      if (superOption == null) {
        return this.channel().parent().config().getOption(option);
      }
      return superOption;
    }

    @Override
    public <T> boolean setOption(final ChannelOption<T> option, final T value) {
      return (
        super.setOption(option, value) ||
        this.channel().parent().config().setOption(option, value)
      );
    }

    /**
     * obtains the channel.
     *
     * @return channel.
     */
    @NotNull
    private DatagramChannelProxy channel() {
      return (DatagramChannelProxy) this.channel;
    }
  }

  /**
   * a class that represents listener inbound proxy.
   *
   * @param channel the channel.
   */
  private record ListenerInboundProxy(@NotNull DatagramChannelProxy channel)
    implements ChannelInboundHandler {
    @Override
    public void channelRegistered(final ChannelHandlerContext ctx) {
      this.channel.pipeline().fireChannelRegistered();
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) {
      this.channel.pipeline().fireChannelUnregistered();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {}

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
      this.channel.pipeline().fireChannelInactive();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
      this.channel.pipeline().fireChannelRead(msg);
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) {
      this.channel.pipeline().fireChannelReadComplete();
    }

    @Override
    public void userEventTriggered(
      final ChannelHandlerContext ctx,
      final Object evt
    ) {}

    @Override
    public void channelWritabilityChanged(final ChannelHandlerContext ctx) {
      this.channel.pipeline().fireChannelWritabilityChanged();
    }

    @Override
    public void exceptionCaught(
      final ChannelHandlerContext ctx,
      final Throwable cause
    ) {
      if (!(cause instanceof ClosedChannelException)) {
        this.channel.pipeline().fireExceptionCaught(cause);
      }
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
      assert this.channel.parent().eventLoop().inEventLoop();
    }

    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) {}
  }

  /**
   * a class that represents listener outbound proxy.
   *
   * @param channel the channel.
   */
  private record ListenerOutboundProxy(@NotNull DatagramChannelProxy channel)
    implements ChannelOutboundHandler {
    @Override
    public void bind(
      final ChannelHandlerContext ctx,
      final SocketAddress localAddress,
      final ChannelPromise promise
    ) {
      this.channel.parent()
        .bind(localAddress, this.channel.wrapPromise(promise));
    }

    @Override
    public void connect(
      final ChannelHandlerContext ctx,
      final SocketAddress remoteAddress,
      final SocketAddress localAddress,
      final ChannelPromise promise
    ) {
      this.channel.parent()
        .connect(
          remoteAddress,
          localAddress,
          this.channel.wrapPromise(promise)
        );
    }

    @Override
    public void disconnect(
      final ChannelHandlerContext ctx,
      final ChannelPromise promise
    ) {
      this.channel.parent().disconnect(this.channel.wrapPromise(promise));
    }

    @Override
    public void close(
      final ChannelHandlerContext ctx,
      final ChannelPromise promise
    ) {
      this.channel.gracefulClose(promise);
    }

    @Override
    public void deregister(
      final ChannelHandlerContext ctx,
      final ChannelPromise promise
    ) {
      this.channel.parent().deregister(this.channel.wrapPromise(promise));
    }

    @Override
    public void read(final ChannelHandlerContext ctx) {}

    @Override
    public void write(
      final ChannelHandlerContext ctx,
      final Object msg,
      final ChannelPromise promise
    ) {
      this.channel.parent().write(msg, this.channel.wrapPromise(promise));
    }

    @Override
    public void flush(final ChannelHandlerContext ctx) {
      this.channel.parent().flush();
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
      assert this.channel.parent().eventLoop().inEventLoop();
    }

    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) {}

    @Override
    public void exceptionCaught(
      final ChannelHandlerContext ctx,
      final Throwable cause
    ) {
      if (!(cause instanceof PortUnreachableException)) {
        ctx.fireExceptionCaught(cause);
      }
    }
  }
}
