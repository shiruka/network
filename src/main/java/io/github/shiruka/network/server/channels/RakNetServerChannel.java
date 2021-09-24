package io.github.shiruka.network.server.channels;

import com.google.common.base.Preconditions;
import io.github.shiruka.network.BlockedAddress;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.channels.DatagramChannelProxy;
import io.github.shiruka.network.packets.NoFreeConnections;
import io.github.shiruka.network.server.RakNetServer;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.PromiseCombiner;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * a class that represents rak net server channels.
 */
public final class RakNetServerChannel extends DatagramChannelProxy implements ServerChannel {

  /**
   * the blocked addresses.
   */
  private final Map<InetSocketAddress, BlockedAddress> blockedAddresses = new HashMap<>();

  /**
   * the child channels(clients).
   */
  private final Map<SocketAddress, RakNetChildChannel> children = new HashMap<>();

  /**
   * ctor.
   */
  public RakNetServerChannel() {
    this(NioDatagramChannel.class);
  }

  /**
   * ctor.
   *
   * @param supplier the supplier.
   */
  public RakNetServerChannel(@NotNull final Supplier<? extends DatagramChannel> supplier) {
    super(supplier);
    this.addDefaultPipeline();
  }

  /**
   * ctor.
   *
   * @param cls the cls.
   */
  public RakNetServerChannel(@NotNull final Class<? extends DatagramChannel> cls) {
    super(cls);
    this.addDefaultPipeline();
  }

  /**
   * blocks the address.
   *
   * @param address the address to block.
   */
  public void blockAddress(@NotNull final BlockedAddress address) {
    this.blockedAddresses.put(address.address(), address);
  }

  /**
   * gets the blocked address.
   *
   * @param address the address to get.
   *
   * @return blocked address.
   */
  @NotNull
  public Optional<BlockedAddress> blockedAddress(@NotNull final InetSocketAddress address) {
    return Optional.ofNullable(this.blockedAddresses.get(address));
  }

  /**
   * obtains the children(a.k.a. connections).
   *
   * @return children.
   */
  @NotNull
  public Map<SocketAddress, RakNetChildChannel> children() {
    return Collections.unmodifiableMap(this.children);
  }

  /**
   * gets the child channel by address.
   *
   * @param address the address to get.
   *
   * @return child channel.
   */
  @Nullable
  public RakNetChildChannel getChildChannel(@NotNull final SocketAddress address) {
    Preconditions.checkState(this.eventLoop().inEventLoop(), "Method must be called from the server eventLoop!");
    return this.children.get(address);
  }

  @Override
  public void gracefulClose(@NotNull final ChannelPromise promise) {
    final var combined = new PromiseCombiner(this.eventLoop());
    final var childrenClosed = this.newPromise();
    this.children.values().forEach(child -> combined.add(child.close()));
    combined.finish(childrenClosed);
    childrenClosed.addListener(f -> this.parent().close(this.wrapPromise(promise)));
  }

  /**
   * creates a new child channel.
   *
   * @param address the address to create.
   *
   * @return child channel.
   */
  @NotNull
  public RakNetChildChannel newChild(@NotNull final InetSocketAddress address) {
    return new RakNetChildChannel(this, address);
  }

  /**
   * adds the default pipeline.
   */
  private void addDefaultPipeline() {
    this.pipeline()
      .addLast(new ServerHandler(this))
      .addLast(RakNetServer.DefaultDatagramInitializer.INSTANCE);
  }

  /**
   * a class that represents server handlers.
   */
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  private static final class ServerHandler extends ChannelDuplexHandler {

    /**
     * the channel.
     */
    @NotNull
    private final RakNetServerChannel channel;

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
      if (!(msg instanceof DatagramPacket datagram)) {
        ctx.fireChannelRead(msg);
        return;
      }
      final var content = datagram.content();
      final var sender = datagram.sender();
      if (this.channel.blockedAddress(sender).map(BlockedAddress::shouldUnblock).isPresent()) {
        datagram.release();
        return;
      }
      try {
        final var child = this.channel.children.get(sender);
        if (child == null && datagram.recipient() != null) {
          ctx.fireChannelRead(datagram.retain());
        } else if (child != null && child.isOpen() && child.config().isAutoRead()) {
          final var retained = content.retain();
          child.eventLoop().execute(() ->
            child.pipeline().fireChannelRead(retained).fireChannelReadComplete());
        }
      } finally {
        datagram.release();
      }
    }

    @Override
    public void channelWritabilityChanged(final ChannelHandlerContext ctx) {
      this.channel.children.values().forEach(ch -> ch.pipeline().fireChannelWritabilityChanged());
      ctx.fireChannelWritabilityChanged();
    }

    @Override
    public void connect(final ChannelHandlerContext ctx, final SocketAddress remoteAddress,
                        final SocketAddress localAddress, final ChannelPromise promise) {
      try {
        Preconditions.checkArgument(localAddress == null || this.channel.localAddress().equals(localAddress),
          "Bound localAddress does not match provided %s", localAddress);
        Preconditions.checkArgument(remoteAddress instanceof InetSocketAddress,
          "Provided remote address is not an InetSocketAddress");
        final var existingChild = this.channel.getChildChannel(remoteAddress);
        if (this.channel.children.size() > this.channel.config().maxConnections() && existingChild == null) {
          final var packet = new NoFreeConnections(
            this.channel.config().magic(), this.channel.config().serverId());
          final var buffer = ctx.alloc().ioBuffer(packet.initialSizeHint());
          try {
            this.channel.config().codec().encode(packet, new PacketBuffer(buffer));
            ctx.writeAndFlush(new DatagramPacket(buffer.retain(), (InetSocketAddress) remoteAddress));
          } finally {
            ReferenceCountUtil.safeRelease(packet);
            buffer.release();
          }
          promise.tryFailure(new IllegalStateException("Too many connections"));
          return;
        }
        if (existingChild == null) {
          final var child = this.channel.newChild((InetSocketAddress) remoteAddress);
          child.closeFuture().addListener(v ->
            this.channel.eventLoop().execute(() -> this.channel.children.remove(remoteAddress, child)));
          child.config().serverId(this.channel.config().serverId());
          this.channel.pipeline().fireChannelRead(child).fireChannelReadComplete();
          this.channel.children.put(remoteAddress, child);
        }
        promise.trySuccess();
      } catch (final Exception e) {
        promise.tryFailure(e);
        throw e;
      }
    }
  }
}
