package io.github.shiruka.network.options;

import io.github.shiruka.network.BlockedAddress;
import io.github.shiruka.network.Constants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * an interface to determine configurations for rak net.
 */
public interface RakNetConfig extends ChannelConfig {

  /**
   * creates a simple rak net config.
   *
   * @param channel the channel to create.
   *
   * @return a newly created rak net config.
   */
  @NotNull
  static RakNetConfig simple(@NotNull final Channel channel) {
    return new Impl(channel);
  }

  /**
   * gets the blocked address.
   *
   * @param address the address to block.
   *
   * @return blocked address.
   */
  @NotNull
  default Optional<BlockedAddress> blockedAddress(@NotNull final InetSocketAddress address) {
    return this.blockedAddress(address.getAddress());
  }

  /**
   * gets the blocked address.
   *
   * @param address the address to block.
   *
   * @return blocked address.
   */
  @NotNull
  Optional<BlockedAddress> blockedAddress(@NotNull InetAddress address);

  /**
   * adds the blocked address.
   *
   * @param address the address to add.
   */
  void blockedAddress(@NotNull BlockedAddress address);

  /**
   * obtains the magic.
   *
   * @return magic.
   */
  @NotNull
  RakNetMagic magic();

  /**
   * obtains the max connections.
   *
   * @return max connections.
   */
  int maxConnections();

  /**
   * sets the max connections.
   *
   * @param maxConnections the max connections tos et.
   */
  void maxConnections(int maxConnections);

  /**
   * obtains the server id.
   *
   * @return server id.
   */
  long serverId();

  /**
   * sets the server id.
   *
   * @param serverId the server id to set.
   */
  void serverId(long serverId);

  /**
   * an abstract implementation of {@link RakNetConfig}.
   */
  @Accessors(fluent = true)
  abstract class Base extends DefaultChannelConfig implements RakNetConfig {

    /**
     * the blocked addresses.
     */
    private final Map<InetAddress, BlockedAddress> blockedAddresses = new ConcurrentHashMap<>();

    /**
     * the magic.
     */
    private final RakNetMagic magic = RakNetMagic.simple();

    /**
     * the max connections.
     */
    private volatile int maxConnections = 2048;

    /**
     * the server id.
     */
    private volatile long serverId = Constants.RANDOM.nextLong();

    /**
     * ctor.
     *
     * @param channel the channel.
     */
    protected Base(@NotNull final Channel channel) {
      super(channel);
    }

    @NotNull
    @Override
    public final Optional<BlockedAddress> blockedAddress(@NotNull final InetAddress address) {
      return Optional.ofNullable(this.blockedAddresses.get(address));
    }

    @Override
    public final void blockedAddress(@NotNull final BlockedAddress address) {
      this.blockedAddresses.put(address.address(), address);
    }

    @NotNull
    @Override
    public final RakNetMagic magic() {
      return this.magic;
    }

    @Override
    public final int maxConnections() {
      return this.maxConnections;
    }

    @Override
    public final void maxConnections(final int maxConnections) {
      this.maxConnections = maxConnections;
    }

    @Override
    public final long serverId() {
      return this.serverId;
    }

    @Override
    public final void serverId(final long serverId) {
      this.serverId = serverId;
    }

    @Override
    public final Map<ChannelOption<?>, Object> getOptions() {
      return this.getOptions(
        super.getOptions(),
        RakNetChannelOptions.SERVER_ID, RakNetChannelOptions.MTU, RakNetChannelOptions.RTT,
        RakNetChannelOptions.PROTOCOL_VERSION, RakNetChannelOptions.MAGIC, RakNetChannelOptions.RETRY_DELAY_NANOS);
    }
  }

  /**
   * a simple implementation of {@link RakNetConfig}.
   */
  final class Impl extends Base {

    /**
     * ctor.
     *
     * @param channel the channel.
     */
    private Impl(@NotNull final Channel channel) {
      super(channel);
    }
  }
}
