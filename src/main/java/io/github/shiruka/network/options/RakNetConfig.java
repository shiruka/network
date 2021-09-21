package io.github.shiruka.network.options;

import io.github.shiruka.network.BlockedAddress;
import io.github.shiruka.network.Constants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;

/**
 * an interface to determine configurations for rak net.
 */
public interface RakNetConfig extends ChannelConfig {

  /**
   * casts the context's config as rak net config.
   *
   * @param context the context to cast.
   *
   * @return channel config as rak net config.
   */
  @NotNull
  static RakNetConfig cast(@NotNull final ChannelHandlerContext context) {
    return RakNetConfig.cast(context.channel());
  }

  /**
   * casts the channel's config as rak net config.
   *
   * @param channel the context to cast.
   *
   * @return channel config as rak net config.
   */
  @NotNull
  static RakNetConfig cast(@NotNull final Channel channel) {
    return (RakNetConfig) channel.config();
  }

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
   * sets the client id.
   *
   * @param clientId the client id to set.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  RakNetConfig clientId(long clientId);

  /**
   * obtains the client id.
   *
   * @return client id.
   */
  long clientId();

  /**
   * obtains the codec.
   *
   * @return codec.
   */
  @NotNull
  RakNetCodec codec();

  /**
   * sets the codec.
   *
   * @param codec the codec to set.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  RakNetConfig codec(@NotNull RakNetCodec codec);

  /**
   * checks if the protocol version contains in the config
   *
   * @param protocolVersion the protocol version to check.
   *
   * @return {@code true} if its contains.
   */
  boolean containsProtocolVersion(int protocolVersion);

  /**
   * obtains the magic.
   *
   * @return magic.
   */
  @NotNull
  RakNetMagic magic();

  /**
   * sets the magic.
   *
   * @param magic the magic to set.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  RakNetConfig magic(@NotNull RakNetMagic magic);

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
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  RakNetConfig maxConnections(int maxConnections);

  /**
   * obtains the mtu.
   *
   * @return mtu.
   */
  int mtu();

  /**
   * sets the mtu.
   *
   * @param mtu the mtu to set.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  RakNetConfig mtu(int mtu);

  /**
   * sets the protocol version.
   *
   * @param protocolVersion the protocol version to set.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  RakNetConfig protocolVersion(int protocolVersion);

  /**
   * obtains the protocol version.
   *
   * @return protocol version.
   */
  int protocolVersion();

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
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  RakNetConfig serverId(long serverId);

  /**
   * updates rtt nanos.
   *
   * @param rtt the rtt to update.
   */
  void updateRTTNanos(long rtt);

  /**
   * an abstract implementation of {@link RakNetConfig}.
   */
  @Getter
  @Setter
  @Accessors(fluent = true)
  abstract class Base extends DefaultChannelConfig implements RakNetConfig {

    /**
     * the blocked addresses.
     */
    private final Map<InetAddress, BlockedAddress> blockedAddresses = new ConcurrentHashMap<>();

    /**
     * the rtt stats.
     */
    private final DescriptiveStatistics rttStats = new DescriptiveStatistics(16);

    /**
     * the client id.
     */
    @Getter
    private volatile long clientId = Constants.RANDOM.nextLong();

    /**
     * the codec.
     */
    @Getter
    private volatile RakNetCodec codec = RakNetCodec.simple();

    /**
     * the magic.
     */
    @Getter
    private volatile RakNetMagic magic = RakNetMagic.simple();

    /**
     * the max connections.
     */
    @Getter
    private volatile int maxConnections = 2048;

    /**
     * the mtu.
     */
    @Getter
    private volatile int mtu = Constants.DEFAULT_MTU;

    /**
     * the protocol version.
     */
    private volatile int protocolVersion;

    /**
     * the protocol versions.
     */
    private volatile int[] protocolVersions = new int[]{9, 10};

    /**
     * the server id.
     */
    @Getter
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

    @Override
    public final boolean containsProtocolVersion(final int protocolVersion) {
      return Arrays.stream(this.protocolVersions).anyMatch(version -> version == protocolVersion);
    }

    @Override
    public final void updateRTTNanos(final long rtt) {
      this.rttStats.addValue(rtt);
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
