package io.github.shiruka.network.packets;

import com.google.common.base.Preconditions;
import io.github.shiruka.network.Packet;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * an interface to determine framed packets.
 */
public interface FramedPacket extends Packet {

  /**
   * obtains the order channel.
   *
   * @return order channel.
   */
  int orderChannel();

  /**
   * sets the order channel.
   *
   * @param orderChannel the order channel to set.
   */
  void orderChannel(int orderChannel);

  /**
   * obtains reliability.
   *
   * @return reliability.
   */
  @NotNull
  Reliability reliability();

  /**
   * sets the reliability
   *
   * @param reliability the reliability to set.
   */
  void reliability(@NotNull Reliability reliability);

  /**
   * an enum class that contains reliabilities.
   */
  @Accessors(fluent = true)
  enum Reliability {
    /**
     * the unreliable.
     */
    UNRELIABLE(false, false, false, false),
    /**
     * the unreliable sequenced.
     */
    UNRELIABLE_SEQUENCED(false, true, true, false),
    /**
     * the reliable.
     */
    RELIABLE(true, false, false, false),
    /**
     * the reliable ordered.
     */
    RELIABLE_ORDERED(true, true, false, false),
    /**
     * the reliable sequenced.
     */
    RELIABLE_SEQUENCED(true, true, true, false),
    /**
     * the unreliable ack.
     */
    UNRELIABLE_ACK(false, false, false, true),
    /**
     * the reliable ack.
     */
    RELIABLE_ACK(true, false, false, true),
    /**
     * the reliable ordered ack.
     */
    RELIABLE_ORDERED_ACK(true, true, false, true);

    /**
     * the cache.
     */
    private static final Reliability[] CACHE = Reliability.values();

    /**
     * the is ackd
     */
    @Getter
    private final boolean isAckd;

    /**
     * the is ordered
     */
    @Getter
    private final boolean isOrdered;

    /**
     * the is reliable
     */
    @Getter
    private final boolean isReliable;

    /**
     * the is sequenced
     */
    @Getter
    private final boolean isSequenced;

    /**
     * ctor.
     *
     * @param isReliable the is reliable.
     * @param isOrdered the is ordered.
     * @param isSequenced the is sequenced.
     * @param isAckd the is ackd.
     */
    Reliability(final boolean isReliable, final boolean isOrdered, final boolean isSequenced, final boolean isAckd) {
      this.isReliable = isReliable;
      this.isOrdered = isOrdered;
      this.isSequenced = isSequenced;
      this.isAckd = isAckd;
    }

    /**
     * gets reliability from code.
     *
     * @param code the code to get.
     *
     * @return reliability.
     */
    @NotNull
    public static Reliability get(final int code) {
      Preconditions.checkArgument(code >= 0 && code < Reliability.CACHE.length, "Invalid code!");
      return Reliability.CACHE[code];
    }

    /**
     * obtains the code.
     *
     * @return code.
     */
    public int code() {
      return this.ordinal();
    }

    /**
     * makes {@code this} reliable.
     *
     * @return {@code this} as reliable.
     */
    @NotNull
    public Reliability makeReliable() {
      if (this.isReliable) {
        return this;
      }
      return switch (this) {
        case UNRELIABLE -> Reliability.RELIABLE;
        case UNRELIABLE_SEQUENCED -> Reliability.RELIABLE_SEQUENCED;
        case UNRELIABLE_ACK -> Reliability.RELIABLE_ACK;
        default -> throw new IllegalArgumentException("No reliable form of " + this);
      };
    }
  }

  /**
   * an abstract implementation of {@link FramedPacket}.
   */
  @Getter
  @Setter
  @Accessors(fluent = true)
  abstract class Base implements FramedPacket {

    /**
     * the order channel.
     */
    private int orderChannel = 0;

    /**
     * the reliability.
     */
    @NotNull
    private FramedPacket.Reliability reliability;

    /**
     * ctor.
     */
    protected Base() {
      this(FramedPacket.Reliability.RELIABLE_ORDERED);
    }

    /**
     * ctor.
     *
     * @param reliability the reliability.
     */
    protected Base(@NotNull final Reliability reliability) {
      this.reliability = reliability;
    }

    @Override
    public final void orderChannel(final int orderChannel) {
      this.orderChannel = orderChannel;
    }

    @Override
    public final void reliability(@NotNull final Reliability reliability) {
      this.reliability = reliability;
    }
  }
}
