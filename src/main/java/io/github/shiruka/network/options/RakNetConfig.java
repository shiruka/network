package io.github.shiruka.network.options;

import io.github.shiruka.network.Constants;
import io.github.shiruka.network.Identifier;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
   * checks if the protocol version contains in the config.
   *
   * @param protocolVersion the protocol version to check.
   *
   * @return {@code true} if its contains.
   */
  boolean containsProtocolVersion(int protocolVersion);

  /**
   * sets the default pending frame sets.
   *
   * @param defaultPendingFrameSets the default pending frame sets to set.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  RakNetConfig defaultPendingFrameSets(int defaultPendingFrameSets);

  /**
   * obtains the default pending frame sets.
   *
   * @return default pending frame sets.
   */
  int defaultPendingFrameSets();

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
   * obtains the max pending frame sets.
   *
   * @return max pending frame sets.
   */
  int maxPendingFrameSets();

  /**
   * sets the max pending frame sets.
   *
   * @param maxPendingFrameSets the max pending frame sets set.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  RakNetConfig maxPendingFrameSets(int maxPendingFrameSets);

  /**
   * obtains the max queued bytes.
   *
   * @return max queued bytes.
   */
  int maxQueuedBytes();

  /**
   * sets the max queued bytes.
   *
   * @param maxQueuedBytes the max queued bytes to set.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  RakNetConfig maxQueuedBytes(int maxQueuedBytes);

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
   * obtains the retry delay nanos.
   *
   * @return retry delay nanos.
   */
  long retryDelayNanos();

  /**
   * sets the retry delay nanos.
   *
   * @param retryDelayNanos the retry delay nanos to set.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  RakNetConfig retryDelayNanos(long retryDelayNanos);

  /**
   * obtains the rtt nanos.
   *
   * @return rtt nanos.
   */
  long rttNanos();

  /**
   * sets the rtt nanos.
   *
   * @param rtt the rtt to set.
   */
  void rttNanos(long rtt);

  /**
   * obtains the rtt std dev nanos.
   *
   * @return rtt std dev nanos.
   */
  long rttStdDevNanos();

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
   * obtains the server identifier.
   *
   * @return server identifier.
   */
  @NotNull
  Identifier serverIdentifier();

  /**
   * sets the server identifier.
   *
   * @param serverIdentifier the server identifier to set.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  RakNetConfig serverIdentifier(@NotNull Identifier serverIdentifier);

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
     * the rtt stats.
     */
    private final DescriptiveStatistics rttStats = new DescriptiveStatistics(
      16
    );

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
     * the default pending frame sets.
     */
    private volatile int defaultPendingFrameSets = 32;

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
     * the max pending frame sets.
     */
    private volatile int maxPendingFrameSets = 1024;

    /**
     * the max queued bytes.
     */
    private volatile int maxQueuedBytes = 3 * 1024 * 1024;

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
    private volatile int[] protocolVersions = new int[] { 9, 10 };

    /**
     * the retry delay nanos.
     */
    private volatile long retryDelayNanos = TimeUnit.NANOSECONDS.convert(
      15,
      TimeUnit.MILLISECONDS
    );

    /**
     * the server id.
     */
    @Getter
    private volatile long serverId = Constants.RANDOM.nextLong();

    /**
     * the server identifier.
     */
    @NotNull
    @Getter
    private volatile Identifier serverIdentifier = Identifier.simple("");

    /**
     * ctor.
     *
     * @param channel the channel.
     */
    protected Base(@NotNull final Channel channel) {
      super(channel);
    }

    @Override
    public final boolean containsProtocolVersion(final int protocolVersion) {
      return Arrays
        .stream(this.protocolVersions)
        .anyMatch(version -> version == protocolVersion);
    }

    @Override
    public final long rttNanos() {
      return Math.max((long) this.rttStats.getMean(), 1);
    }

    @Override
    public final void rttNanos(final long rtt) {
      this.rttStats.clear();
      this.rttStats.addValue(rtt);
    }

    @Override
    public final long rttStdDevNanos() {
      return (long) this.rttStats.getStandardDeviation();
    }

    @Override
    public final void updateRTTNanos(final long rtt) {
      this.rttStats.addValue(rtt);
    }

    @Override
    public final Map<ChannelOption<?>, Object> getOptions() {
      return this.getOptions(
          super.getOptions(),
          RakNetChannelOptions.SERVER_ID,
          RakNetChannelOptions.MTU,
          RakNetChannelOptions.RTT,
          RakNetChannelOptions.PROTOCOL_VERSION,
          RakNetChannelOptions.MAGIC,
          RakNetChannelOptions.RETRY_DELAY_NANOS,
          RakNetChannelOptions.CLIENT_ID,
          RakNetChannelOptions.CODEC,
          RakNetChannelOptions.MAX_CONNECTIONS,
          RakNetChannelOptions.MAX_QUEUED_BYTES,
          RakNetChannelOptions.SERVER_IDENTIFIER
        );
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public <T> T getOption(final ChannelOption<T> option) {
      if (option == RakNetChannelOptions.SERVER_ID) {
        return (T) (Long) this.serverId;
      } else if (option == RakNetChannelOptions.CLIENT_ID) {
        return (T) (Long) this.clientId;
      } else if (option == RakNetChannelOptions.CODEC) {
        return (T) this.codec;
      } else if (option == RakNetChannelOptions.MTU) {
        return (T) (Integer) this.mtu;
      } else if (option == RakNetChannelOptions.RTT) {
        return (T) (Long) this.rttNanos();
      } else if (option == RakNetChannelOptions.PROTOCOL_VERSION) {
        return (T) (Integer) this.protocolVersion;
      } else if (option == RakNetChannelOptions.MAGIC) {
        return (T) this.magic;
      } else if (option == RakNetChannelOptions.RETRY_DELAY_NANOS) {
        return (T) (Long) this.retryDelayNanos;
      } else if (option == RakNetChannelOptions.MAX_CONNECTIONS) {
        return (T) (Integer) this.maxConnections;
      } else if (option == RakNetChannelOptions.MAX_QUEUED_BYTES) {
        return (T) (Integer) this.maxQueuedBytes;
      } else if (option == RakNetChannelOptions.SERVER_IDENTIFIER) {
        return (T) this.serverIdentifier;
      }
      return super.getOption(option);
    }

    @Override
    public <T> boolean setOption(final ChannelOption<T> option, final T value) {
      if (option == RakNetChannelOptions.SERVER_ID) {
        this.serverId = (Long) value;
      } else if (option == RakNetChannelOptions.CLIENT_ID) {
        this.clientId = (Long) value;
      } else if (option == RakNetChannelOptions.CODEC) {
        this.codec = (RakNetCodec) value;
      } else if (option == RakNetChannelOptions.MTU) {
        this.mtu = (Integer) value;
      } else if (option == RakNetChannelOptions.RTT) {
        this.rttNanos((Long) value);
      } else if (option == RakNetChannelOptions.PROTOCOL_VERSION) {
        this.protocolVersion = (Integer) value;
      } else if (option == RakNetChannelOptions.MAGIC) {
        this.magic = (RakNetMagic) value;
      } else if (option == RakNetChannelOptions.RETRY_DELAY_NANOS) {
        this.retryDelayNanos = (Long) value;
      } else if (option == RakNetChannelOptions.MAX_CONNECTIONS) {
        this.maxConnections = (Integer) value;
      } else if (option == RakNetChannelOptions.MAX_QUEUED_BYTES) {
        this.maxQueuedBytes = (Integer) value;
      } else if (option == RakNetChannelOptions.SERVER_IDENTIFIER) {
        this.serverIdentifier = (Identifier) value;
      } else {
        return super.setOption(option, value);
      }
      return true;
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
