package io.github.shiruka.network.server;

import io.github.shiruka.network.server.channels.RakNetServerChannel;
import io.netty.channel.Channel;
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
   * a class that represents default child initializers.
   */
  final class DefaultChildInitializer extends ChannelInitializer<Channel> {

    /**
     * the instance.
     */
    public static final ChannelInitializer<Channel> INSTANCE = new DefaultChildInitializer();

    @Override
    protected void initChannel(final Channel channel) {
      channel.pipeline()
        .addLast(FlushTickHandler.NAME, new FlushTickHandler())
        .addLast(RawPacketCodec.NAME, RawPacketCodec.INSTANCE)
        .addLast(ReliableFrameHandling.INSTANCE)
        .addLast(PacketHandling.INSTANCE)
        .addLast(ConnectionInitializer.NAME, new ChannelInboundHandlerAdapter());
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
    protected void initChannel(final Channel channel) {
      channel.pipeline().addLast(ConnectionListener.NAME, new ConnectionListener());
      channel.eventLoop().execute(() ->
        channel.pipeline().addLast(DatagramConsumer.NAME, DatagramConsumer.INSTANCE));
    }
  }
}
