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
   * the listener handler name.
   */
  private static final String LISTENER_HANDLER_NAME = "rn-udp-listener-handler";

  /**
   * the config.
   */
  @Getter
  private final RakNetConfig config = new Config();

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
  protected DatagramChannelProxy(@NotNull final Supplier<? extends DatagramChannel> supplier) {
    this.parent = supplier.get();
    this.pipeline = this.newChannelPipeline();
    this.parent.pipeline()
      .addLast(DatagramChannelProxy.LISTENER_HANDLER_NAME, new ListenerInboundProxy());
    this.pipeline()
      .addLast(DatagramChannelProxy.LISTENER_HANDLER_NAME, new ListenerOutboundProxy())
      .addLast(new FlushConsolidationHandler(FlushConsolidationHandler.DEFAULT_EXPLICIT_FLUSH_AFTER_FLUSHES, true));
  }

  /**
   * ctor.
   *
   * @param cls the cls.
   */
  protected DatagramChannelProxy(@NotNull final Class<? extends DatagramChannel> cls) {
    this(() -> {
      try {
        return cls.getConstructor().newInstance();
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
  public final ChannelFuture connect(final SocketAddress remoteAddress, final SocketAddress localAddress) {
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
  public final ChannelFuture bind(final SocketAddress localAddress, final ChannelPromise promise) {
    return this.pipeline.bind(localAddress, promise);
  }

  @Override
  public final ChannelFuture connect(final SocketAddress remoteAddress, final ChannelPromise promise) {
    return this.pipeline.connect(remoteAddress, promise);
  }

  @Override
  public final ChannelFuture connect(final SocketAddress remoteAddress, final SocketAddress localAddress,
                                     final ChannelPromise promise) {
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
  public final ChannelFuture write(final Object msg, final ChannelPromise promise) {
    return this.pipeline.write(msg, promise);
  }

  @Override
  public final ChannelFuture writeAndFlush(final Object msg, final ChannelPromise promise) {
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
  public boolean isActive() {
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
  public boolean isWritable() {
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
   * creates a new channel pipeline.
   *
   * @return a newly created channel pipeline.
   */
  @NotNull
  protected final DefaultChannelPipeline newChannelPipeline() {
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
   * wraps the promise.
   *
   * @param promise the promise to wrap.
   *
   * @return wrapped promise.
   */
  protected final ChannelPromise wrapPromise(@NotNull final ChannelPromise promise) {
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
   * a class that represents datagram channel proxy configurations.
   */
  private final class Config extends RakNetConfig.Base {

    /**
     * ctor.
     */
    private Config() {
      super(DatagramChannelProxy.this);
    }

    @Override
    public <T> T getOption(final ChannelOption<T> option) {
      final var superOption = super.getOption(option);
      if (superOption == null) {
        return DatagramChannelProxy.this.parent.config().getOption(option);
      }
      return superOption;
    }

    @Override
    public <T> boolean setOption(final ChannelOption<T> option, final T value) {
      return super.setOption(option, value) ||
        DatagramChannelProxy.this.parent.config().setOption(option, value);
    }
  }

  /**
   * a class that represents listener inbound proxy.
   */
  protected final class ListenerInboundProxy implements ChannelInboundHandler {

    @Override
    public void channelRegistered(final ChannelHandlerContext ctx) {
      DatagramChannelProxy.this.pipeline.fireChannelRegistered();
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) {
      DatagramChannelProxy.this.pipeline.fireChannelUnregistered();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
      DatagramChannelProxy.this.pipeline.fireChannelInactive();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
      DatagramChannelProxy.this.pipeline.fireChannelRead(msg);
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) {
      DatagramChannelProxy.this.pipeline.fireChannelReadComplete();
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
    }

    @Override
    public void channelWritabilityChanged(final ChannelHandlerContext ctx) {
      DatagramChannelProxy.this.pipeline.fireChannelWritabilityChanged();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
      if (!(cause instanceof ClosedChannelException)) {
        DatagramChannelProxy.this.pipeline.fireExceptionCaught(cause);
      }
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
      assert DatagramChannelProxy.this.parent.eventLoop().inEventLoop();
    }

    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) {
    }
  }

  /**
   * a class that represents listener outbound proxy.
   */
  protected final class ListenerOutboundProxy implements ChannelOutboundHandler {

    @Override
    public void bind(final ChannelHandlerContext ctx, final SocketAddress localAddress, final ChannelPromise promise) {
      DatagramChannelProxy.this.parent.bind(localAddress, DatagramChannelProxy.this.wrapPromise(promise));
    }

    @Override
    public void connect(final ChannelHandlerContext ctx, final SocketAddress remoteAddress,
                        final SocketAddress localAddress, final ChannelPromise promise) {
      DatagramChannelProxy.this.parent.connect(remoteAddress, localAddress, DatagramChannelProxy.this.wrapPromise(promise));
    }

    @Override
    public void disconnect(final ChannelHandlerContext ctx, final ChannelPromise promise) {
      DatagramChannelProxy.this.parent.disconnect(DatagramChannelProxy.this.wrapPromise(promise));
    }

    @Override
    public void close(final ChannelHandlerContext ctx, final ChannelPromise promise) {
      DatagramChannelProxy.this.gracefulClose(promise);
    }

    @Override
    public void deregister(final ChannelHandlerContext ctx, final ChannelPromise promise) {
      DatagramChannelProxy.this.parent.deregister(DatagramChannelProxy.this.wrapPromise(promise));
    }

    @Override
    public void read(final ChannelHandlerContext ctx) {
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {
      DatagramChannelProxy.this.parent.write(msg, DatagramChannelProxy.this.wrapPromise(promise));
    }

    @Override
    public void flush(final ChannelHandlerContext ctx) {
      DatagramChannelProxy.this.parent.flush();
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
      assert DatagramChannelProxy.this.parent.eventLoop().inEventLoop();
    }

    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) {
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
      if (!(cause instanceof PortUnreachableException)) {
        ctx.fireExceptionCaught(cause);
      }
    }
  }
}
