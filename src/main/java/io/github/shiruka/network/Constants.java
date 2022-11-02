package io.github.shiruka.network;

import io.netty.channel.ChannelFutureListener;
import io.netty.util.internal.SystemPropertyUtil;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.security.SecureRandom;
import java.util.Random;

/**
 * a class that contains constant values.
 */
public final class Constants {

  /**
   * the Inet 6 address family.
   */
  public static final int AF_INET6 = 23;

  /**
   * the default mtu.
   */
  public static final int DEFAULT_MTU = 8192;

  /**
   * the internal write listener.
   */
  public static final ChannelFutureListener INTERNAL_WRITE_LISTENER = future -> {
    if (
      !future.isSuccess() && !(future.cause() instanceof ClosedChannelException)
    ) {
      future.channel().pipeline().fireExceptionCaught(future.cause());
      future.channel().close();
    }
  };

  /**
   * the IPv4 version.
   */
  public static final int IPV4 = 4;

  /**
   * the length of IPv4 addresses.
   */
  public static final int IPV4_ADDRESS_LENGTH = 4;

  /**
   * the IPv6 version.
   */
  public static final int IPV6 = 6;

  /**
   * the length of IPv6 addresses.
   */
  public static final int IPV6_ADDRESS_LENGTH = 16;

  /**
   * the max packet loss.
   */
  public static final int MAX_PACKET_LOSS = SystemPropertyUtil.getInt(
    "raknetserver.maxPacketLoss",
    8192
  );

  /**
   * the null address.
   */
  public static final InetSocketAddress NULL_ADDRESS = new InetSocketAddress(0);

  /**
   * the random.
   */
  public static final Random RANDOM = new SecureRandom();

  /**
   * ctor.
   */
  private Constants() {}
}
