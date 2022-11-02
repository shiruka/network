package io.github.shiruka.network;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents blocked addresses.
 */
@Accessors(fluent = true)
@ToString(doNotUseGetters = true, onlyExplicitlyIncluded = true)
@EqualsAndHashCode(doNotUseGetters = true, onlyExplicitlyIncluded = true)
public final class BlockedAddress {

  /**
   * the permanent block time.
   */
  private static final long PERMANENT_BLOCK = -1L;

  /**
   * the address.
   */
  @NotNull
  @Getter
  @ToString.Include
  @EqualsAndHashCode.Include
  private final InetSocketAddress address;

  /**
   * the blocked time.
   */
  @Getter
  @ToString.Include
  @EqualsAndHashCode.Include
  private final long blockedTime;

  /**
   * the expire time.
   */
  @Getter
  @ToString.Include
  @EqualsAndHashCode.Include
  private final long expireTime;

  /**
   * the reason.
   */
  @NotNull
  @Getter
  @ToString.Include
  @EqualsAndHashCode.Include
  private final String reason;

  /**
   * ctor.
   *
   * @param address the address.
   * @param reason the reason.
   * @param blockedTime the blocked time.
   * @param expireTime the expire time.
   */
  public BlockedAddress(
    @NotNull final InetSocketAddress address,
    @NotNull final String reason,
    final long blockedTime,
    final long expireTime
  ) {
    Preconditions.checkArgument(
      expireTime > 0L || expireTime == BlockedAddress.PERMANENT_BLOCK,
      "Block time must be greater than 0 or equal to %s for a permanent block",
      BlockedAddress.PERMANENT_BLOCK
    );
    this.address = address;
    this.reason = reason;
    this.blockedTime = blockedTime;
    this.expireTime = expireTime;
  }

  /**
   * ctor.
   *
   * @param address the address.
   * @param reason the reason.
   * @param expireTime the expire time.
   */
  public BlockedAddress(
    @NotNull final InetSocketAddress address,
    @NotNull final String reason,
    final long expireTime
  ) {
    this(address, reason, System.currentTimeMillis(), expireTime);
  }

  /**
   * ctor.
   *
   * @param address the address.
   * @param reason the reason.
   */
  public BlockedAddress(
    @NotNull final InetSocketAddress address,
    @NotNull final String reason
  ) {
    this(address, reason, BlockedAddress.PERMANENT_BLOCK);
  }

  /**
   * checks if the address should unblock.
   *
   * @return {@code true} if the address should unblock.
   */
  public boolean shouldUnblock() {
    return (
      this.expireTime > BlockedAddress.PERMANENT_BLOCK &&
      System.currentTimeMillis() - this.blockedTime >= this.expireTime
    );
  }
}
