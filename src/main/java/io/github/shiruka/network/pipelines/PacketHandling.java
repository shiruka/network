package io.github.shiruka.network.pipelines;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

/**
 * a class that represents packet handling pipelines.
 */
public final class PacketHandling extends ChannelInitializer<Channel> {

  /**
   * the instance.
   */
  public static final PacketHandling INSTANCE = new PacketHandling();

  @Override
  protected void initChannel(final Channel ch) {
    ch
      .pipeline()
      .addLast(DisconnectHandler.NAME, DisconnectHandler.INSTANCE)
      .addLast(PingHandler.NAME, PingHandler.INSTANCE)
      .addLast(PongHandler.NAME, PongHandler.INSTANCE);
  }
}
