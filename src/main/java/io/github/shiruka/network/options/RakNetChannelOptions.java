package io.github.shiruka.network.options;

import io.github.shiruka.network.Identifier;
import io.netty.channel.ChannelOption;
import io.netty.util.AttributeKey;

/**
 * an interface that contains rak net channel options.
 */
public interface RakNetChannelOptions {

  /**
   * the client id.
   */
  ChannelOption<Long> CLIENT_ID = ChannelOption.valueOf("RN_CLIENT_ID");

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
   * the server identifier.
   */
  ChannelOption<Identifier> SERVER_IDENTIFIER = ChannelOption.valueOf("RN_SERVER_IDENTIFIER");

  /**
   * the writable.
   */
  AttributeKey<Boolean> WRITABLE = AttributeKey.valueOf("RN_WRITABLE");
}
