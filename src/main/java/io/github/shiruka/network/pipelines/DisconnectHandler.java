package io.github.shiruka.network.pipelines;

import io.github.shiruka.network.packets.ClientDisconnect;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * a class that represents disconnect handler pipelines.
 */
@ChannelHandler.Sharable
public final class DisconnectHandler extends ChannelDuplexHandler {

  /**
   * the instance.
   */
  public static final DisconnectHandler INSTANCE = new DisconnectHandler();

  /**
   * the name.
   */
  public static final String NAME = "rn-disconnect";

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof ClientDisconnect) {
      ReferenceCountUtil.release(msg);
      ctx.pipeline().remove(this);
      ctx.channel().flush().close();
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  @Override
  public void close(
    final ChannelHandlerContext ctx,
    final ChannelPromise promise
  ) {
    final var channel = ctx.channel();
    if (!channel.isActive()) {
      ctx.close(promise);
      return;
    }
    final var disconnectPromise = ctx.newPromise();
    final var timeout = channel
      .eventLoop()
      .schedule(
        (Callable<Boolean>) disconnectPromise::trySuccess,
        1,
        TimeUnit.SECONDS
      );
    channel
      .writeAndFlush(new ClientDisconnect())
      .addListener(future -> disconnectPromise.trySuccess());
    disconnectPromise.addListener(future -> {
      timeout.cancel(false);
      ctx.close(promise);
    });
  }
}
