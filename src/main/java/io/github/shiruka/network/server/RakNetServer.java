package io.github.shiruka.network.server;

import io.github.shiruka.network.pipelines.DatagramConsumer;
import io.github.shiruka.network.pipelines.FlushTickHandler;
import io.github.shiruka.network.pipelines.PacketHandling;
import io.github.shiruka.network.pipelines.RawPacketCodec;
import io.github.shiruka.network.pipelines.ReliableFrameHandling;
import io.github.shiruka.network.server.channels.RakNetServerChannel;
import io.github.shiruka.network.server.pipelines.ConnectionInitializer;
import io.github.shiruka.network.server.pipelines.ConnectionListener;
import io.github.shiruka.network.server.pipelines.PingListener;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import org.jetbrains.annotations.NotNull;

/**
 * an interface to determine rak net servers.
 */
public interface RakNetServer {
  /**
   * the rak net server channel.
   */
  @NotNull
  Class<RakNetServerChannel> CHANNEL = RakNetServerChannel.class;

  /**
   * casts the context's channel as rak net server channel.
   *
   * @param ctx the ctx to cast.
   *
   * @return context's channel as rak net server channel.
   */
  @NotNull
  static RakNetServerChannel cast(@NotNull final ChannelHandlerContext ctx) {
    return (RakNetServerChannel) ctx.channel();
  }

  /**
   * a class that represents default child initializers.
   */
  final class DefaultChildInitializer extends ChannelInitializer<Channel> {

    /**
     * the instance.
     */
    public static final ChannelInitializer<Channel> INSTANCE = new DefaultChildInitializer();

    @Override
    protected void initChannel(final Channel channel) {
      channel
        .pipeline()
        .addLast(FlushTickHandler.NAME, new FlushTickHandler())
        .addLast(RawPacketCodec.NAME, RawPacketCodec.INSTANCE)
        .addLast(ReliableFrameHandling.INSTANCE)
        .addLast(PacketHandling.INSTANCE)
        .addLast(
          ConnectionInitializer.NAME,
          new ChannelInboundHandlerAdapter()
        );
    }
  }

  /**
   * a class that represents default datagram initializer.
   */
  final class DefaultDatagramInitializer extends ChannelInitializer<Channel> {

    /**
     * the instance.
     */
    public static final ChannelInitializer<Channel> INSTANCE = new DefaultDatagramInitializer();

    @Override
    protected void initChannel(final Channel ch) {
      ch
        .pipeline()
        .addLast(PingListener.NAME, new PingListener())
        .addLast(ConnectionListener.NAME, new ConnectionListener());
      ch
        .eventLoop()
        .execute(() ->
          ch
            .pipeline()
            .addLast(DatagramConsumer.NAME, DatagramConsumer.INSTANCE)
        );
    }
  }
}
