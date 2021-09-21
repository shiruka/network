package io.github.shiruka.network.server.pipelines;

import io.github.shiruka.network.Constants;
import io.github.shiruka.network.Packet;
import io.github.shiruka.network.options.RakNetConfig;
import io.github.shiruka.network.packets.ClientHandshake;
import io.github.shiruka.network.packets.ConnectionFailed;
import io.github.shiruka.network.packets.ConnectionReply1;
import io.github.shiruka.network.packets.ConnectionReply2;
import io.github.shiruka.network.packets.ConnectionRequest;
import io.github.shiruka.network.packets.ConnectionRequest1;
import io.github.shiruka.network.packets.ConnectionRequest2;
import io.github.shiruka.network.packets.InvalidVersion;
import io.github.shiruka.network.packets.ServerHandshake;
import io.github.shiruka.network.pipelines.BaseConnectionInitializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import java.net.InetSocketAddress;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents connection initializer pipelines.
 */
@Accessors(fluent = true)
public final class ConnectionInitializer extends BaseConnectionInitializer {

  /**
   * the client id set.
   */
  private boolean clientIdSet;

  /**
   * the mtu fixed.
   */
  @Setter
  private boolean mtuFixed;

  /**
   * the seen first.
   */
  private boolean seenFirst;

  /**
   * ctor.
   *
   * @param connectPromise the connect promise.
   */
  public ConnectionInitializer(@NotNull final ChannelPromise connectPromise) {
    super(connectPromise);
  }

  /**
   * sets fixed mtu.
   *
   * @param channel the channel to set.
   * @param mtu the mtu to set.
   */
  public static void fixedMTU(@NotNull final Channel channel, final int mtu) {
    channel.eventLoop().execute(() -> {
      channel.pipeline().get(ConnectionInitializer.class).mtuFixed(true);
      RakNetConfig.cast(channel).mtu(mtu);
    });
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, final Packet msg) {
    final var config = RakNetConfig.cast(ctx);
    if (msg instanceof Packet.Client client) {
      this.processClientId(ctx, client.clientId());
    } else if (msg instanceof ConnectionFailed) {
      throw new IllegalStateException("Connection failed");
    }
    switch (this.state()) {
      case CR1:
        if (msg instanceof ConnectionRequest1 request1) {
          request1.magic().verify(config.magic());
          if (!this.mtuFixed) {
            config.mtu(request1.mtu());
          }
          this.seenFirst = true;
          if (!config.containsProtocolVersion(request1.protocolVersion())) {
            ctx.writeAndFlush(new InvalidVersion(config.magic(), config.serverId()))
              .addListener(ChannelFutureListener.CLOSE);
            return;
          }
          config.protocolVersion(request1.protocolVersion());
        } else if (msg instanceof ConnectionRequest2 request2) {
          request2.magic().verify(config.magic());
          if (!this.mtuFixed) {
            config.mtu(request2.mtu());
          }
          this.state(State.CR2);
        }
        break;
      case CR2:
        if (msg instanceof ConnectionRequest request) {
          ctx.writeAndFlush(new ServerHandshake((InetSocketAddress) ctx.channel().remoteAddress(), request.timestamp()))
            .addListener(Constants.INTERNAL_WRITE_LISTENER);
          this.state(State.CR3);
          BaseConnectionInitializer.startPing(ctx);
        }
        break;
      case CR3:
        if (msg instanceof ClientHandshake) {
          this.finish(ctx);
          return;
        }
        break;
      default:
        break;
    }
    this.sendRequest(ctx);
  }

  @Override
  protected void removeHandler(@NotNull final ChannelHandlerContext ctx) {
    ctx.channel().pipeline()
      .replace(BaseConnectionInitializer.NAME, BaseConnectionInitializer.NAME, new RestartConnectionHandler());
  }

  @Override
  public void sendRequest(@NotNull final ChannelHandlerContext ctx) {
    assert ctx.channel().eventLoop().inEventLoop();
    final var config = RakNetConfig.cast(ctx);
    switch (this.state()) {
      case CR1 -> {
        if (this.seenFirst) {
          ctx.writeAndFlush(new ConnectionReply1(config.magic(), config.mtu(), config.serverId()))
            .addListener(Constants.INTERNAL_WRITE_LISTENER);
        }
      }
      case CR2 -> {
        final var packet = new ConnectionReply2(config.magic(), config.mtu(), config.serverId(),
          (InetSocketAddress) ctx.channel().remoteAddress());
        ctx.writeAndFlush(packet).addListener(Constants.INTERNAL_WRITE_LISTENER);
      }
      default -> {
      }
    }
  }

  /**
   * processes client id.
   *
   * @param ctx the ctx to process.
   * @param clientId the client id to process.
   */
  private void processClientId(@NotNull final ChannelHandlerContext ctx, final long clientId) {
    final var config = RakNetConfig.cast(ctx);
    if (!this.clientIdSet) {
      config.clientId(clientId);
      this.clientIdSet = true;
    } else if (config.clientId() != clientId) {
      throw new IllegalStateException("Connection sequence restarted!");
    }
  }

  /**
   * a class that represents restart connection handler pipelines.
   */
  private static final class RestartConnectionHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
      if (msg instanceof Packet.Client || msg instanceof ConnectionRequest1) {
        ctx.writeAndFlush(new ConnectionFailed(RakNetConfig.cast(ctx).magic()))
          .addListener(ChannelFutureListener.CLOSE);
        ReferenceCountUtil.safeRelease(msg);
      } else if (msg instanceof ConnectionFailed) {
        ReferenceCountUtil.safeRelease(msg);
        ctx.close();
      } else {
        ctx.fireChannelRead(msg);
      }
    }
  }
}
