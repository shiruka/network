package io.github.shiruka.network.pipelines;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

/**
 * a class that represents reliable frame handling pipelines.
 */
public final class ReliableFrameHandling extends ChannelInitializer<Channel> {

  /**
   * the instance.
   */
  public static final ReliableFrameHandling INSTANCE = new ReliableFrameHandling();

  @Override
  protected void initChannel(final Channel channel) {
    channel.pipeline()
      .addLast(ReliabilityHandler.NAME, new ReliabilityHandler())
      .addLast(FrameJoiner.NAME, new FrameJoiner())
      .addLast(FrameSplitter.NAME, new FrameSplitter())
      .addLast(FrameOrderIn.NAME, new FrameOrderIn())
      .addLast(FrameOrderOut.NAME, new FrameOrderOut())
      .addLast(FramedPacketCodec.NAME, FramedPacketCodec.INSTANCE);
  }
}
