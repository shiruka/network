package io.github.shiruka.network.server.channels;

import io.github.shiruka.network.Constants;
import io.github.shiruka.network.options.RakNetChannelOptions;
import io.github.shiruka.network.options.RakNetConfig;
import io.github.shiruka.network.server.RakNetServer;
import io.github.shiruka.network.server.pipelines.ConnectionInitializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents rak net child channels.
 */
@Accessors(fluent = true)
public final class RakNetChildChannel extends AbstractChannel {

  /**
   * the metadata.
   */
  private static final ChannelMetadata METADATA = new ChannelMetadata(false);

  /**
   * the address.
   */
  @NotNull
  private final InetSocketAddress address;

  /**
   * the config.
   */
  @NotNull
  @Getter
  private final RakNetConfig config;

  /**
   * the connect promise.
   */
  @NotNull
  private final ChannelPromise connectPromise;

  /**
   * the open.
   */
  @Getter
  private volatile boolean isOpen = true;

  /**
   * ctor.
   *
   * @param parent the parent.
   * @param address the address.
   */
  public RakNetChildChannel(@NotNull final RakNetServerChannel parent, @NotNull final InetSocketAddress address) {
    super(parent);
    this.address = address;
    this.config = RakNetConfig.simple(this);
    this.connectPromise = this.newPromise();
    this.config.serverId(parent.config().serverId());
    this.pipeline().addLast(new WriteHandler(this));
    this.addDefaultPipeline();
  }

  @Override
  public boolean isActive() {
    return this.isOpen() && this.parent().isActive() && this.connectPromise.isSuccess();
  }

  @Override
  public ChannelMetadata metadata() {
    return RakNetChildChannel.METADATA;
  }

  @Override
  public boolean isWritable() {
    final var result = this.attr(RakNetChannelOptions.WRITABLE).get();
    return (result == null || result) && this.parent().isWritable();
  }

  @Override
  public long bytesBeforeUnwritable() {
    return this.parent().bytesBeforeUnwritable();
  }

  @Override
  public long bytesBeforeWritable() {
    return this.parent().bytesBeforeWritable();
  }

  @Override
  public RakNetServerChannel parent() {
    return (RakNetServerChannel) super.parent();
  }

  @Override
  protected AbstractChannel.AbstractUnsafe newUnsafe() {
    return new AbstractChannel.AbstractUnsafe() {
      @Override
      public void connect(final SocketAddress remoteAddress, final SocketAddress localAddress,
                          final ChannelPromise promise) {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  protected boolean isCompatible(final EventLoop loop) {
    return true;
  }

  @Override
  protected SocketAddress localAddress0() {
    return this.parent().localAddress();
  }

  @Override
  protected SocketAddress remoteAddress0() {
    return this.address;
  }

  @Override
  protected void doBind(final SocketAddress localAddress) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void doDisconnect() {
    this.close();
  }

  @Override
  protected void doClose() {
    this.isOpen = false;
  }

  @Override
  protected void doBeginRead() {
  }

  @Override
  protected void doWrite(final ChannelOutboundBuffer in) {
    throw new UnsupportedOperationException();
  }

  /**
   * adds the default pipeline.
   */
  private void addDefaultPipeline() {
    this.pipeline().addLast(RakNetServer.DefaultChildInitializer.INSTANCE);
    this.connectPromise.addListener(future -> {
      if (!future.isSuccess()) {
        RakNetChildChannel.this.close();
      }
    });
    this.pipeline().addLast(new ChannelInitializer<RakNetChildChannel>() {
      @Override
      protected void initChannel(final RakNetChildChannel ch) {
        RakNetChildChannel.this.pipeline().replace(ConnectionInitializer.NAME, ConnectionInitializer.NAME,
          new ConnectionInitializer(RakNetChildChannel.this.connectPromise));
      }
    });
  }

  /**
   * a class that represents write handlers.
   */
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  private static final class WriteHandler extends ChannelOutboundHandlerAdapter {

    /**
     * the channel.
     */
    @NotNull
    private final RakNetChildChannel channel;

    /**
     * the needs flush.
     */
    private boolean needsFlush = false;

    @Override
    public void read(final ChannelHandlerContext ctx) {
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {
      if (msg instanceof ByteBuf) {
        this.needsFlush = true;
        promise.trySuccess();
        this.channel.parent().write(new DatagramPacket((ByteBuf) msg, this.channel.address))
          .addListener(Constants.INTERNAL_WRITE_LISTENER);
      } else {
        ctx.write(msg, promise);
      }
    }

    @Override
    public void flush(final ChannelHandlerContext ctx) {
      if (this.needsFlush) {
        this.needsFlush = false;
        this.channel.parent().flush();
      }
    }
  }
}
