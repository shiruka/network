package io.github.shiruka.network.options;

import io.github.shiruka.network.BlockedAddress;
import io.netty.channel.ChannelOption;
import io.netty.util.AttributeKey;
import java.util.Set;

/**
 * an interface that contains rak net channel options.
 */
public interface RakNetChannelOptions {

  /**
   * the blocked addresses.
   */
  ChannelOption<Set<BlockedAddress>> BLOCKED_ADDRESSES = ChannelOption.valueOf("RN_BLOCKED_ADDRESSES");

  /**
   * the magic.
   */
  ChannelOption<RakNetMagic> MAGIC = ChannelOption.valueOf("RN_MAGIC");

  /**
   * the max connections.
   */
  ChannelOption<Integer> MAX_CONNECTIONS = ChannelOption.valueOf("RN_MAX_CONNECTIONS");

  /**
   * the mtu.
   */
  ChannelOption<Integer> MTU = ChannelOption.valueOf("RN_MTU");

  /**
   * the protocol version.
   */
  ChannelOption<Integer> PROTOCOL_VERSION = ChannelOption.valueOf("RN_PROTOCOL_VERSION");

  /**
   * the retry delay nanos.
   */
  ChannelOption<Long> RETRY_DELAY_NANOS = ChannelOption.valueOf("RN_RETRY_DELAY_NANOS");

  /**
   * the rtt.
   */
  ChannelOption<Long> RTT = ChannelOption.valueOf("RN_RTT");

  /**
   * the server id.
   */
  ChannelOption<Long> SERVER_ID = ChannelOption.valueOf("RN_SERVER_ID");

  /**
   * the writable.
   */
  AttributeKey<Boolean> WRITABLE = AttributeKey.valueOf("RN_WRITABLE");
}
