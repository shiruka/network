package io.github.shiruka.network;

import io.netty.channel.ChannelFutureListener;
import java.nio.channels.ClosedChannelException;
import java.security.SecureRandom;
import java.util.Random;

/**
 * a class that contains constant values.
 */
public final class Constants {

  /**
   * the internal write listener.
   */
  public static final ChannelFutureListener INTERNAL_WRITE_LISTENER = future -> {
    if (!future.isSuccess() && !(future.cause() instanceof ClosedChannelException)) {
      future.channel().pipeline().fireExceptionCaught(future.cause());
      future.channel().close();
    }
  };

  /**
   * the random.
   */
  public static final Random RANDOM = new SecureRandom();

  /**
   * the version.
   */
  public static final String VERSION = "@version@";

  /**
   * ctor.
   */
  private Constants() {
  }
}
