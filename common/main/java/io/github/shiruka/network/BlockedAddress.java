package io.github.shiruka.network;

import com.google.common.base.Preconditions;
import java.net.InetAddress;
import org.jetbrains.annotations.NotNull;

/**
 * a record class that represents blocked addresses.
 *
 * @param address the address.
 * @param blockedTime the blocked time.
 * @param expireTime the expire time.
 */
public record BlockedAddress(
  @NotNull InetAddress address,
  @NotNull String reason,
  long blockedTime,
  long expireTime
) {

  /**
   * the permanent block time.
   */
  private static final long PERMANENT_BLOCK = -1L;

  /**
   * the compact ctor.
   */
  public BlockedAddress {
    Preconditions.checkArgument(expireTime > 0L || expireTime == BlockedAddress.PERMANENT_BLOCK,
      "Block time must be greater than 0 or equal to %s for a permanent block",
      BlockedAddress.PERMANENT_BLOCK);
  }

  /**
   * ctor.
   *
   * @param address the address.
   * @param reason the reason.
   * @param expireTime the expire time.
   */
  public BlockedAddress(@NotNull final InetAddress address, @NotNull final String reason, final long expireTime) {
    this(address, reason, System.currentTimeMillis(), expireTime);
  }

  /**
   * ctor.
   *
   * @param address the address.
   * @param reason the reason.
   */
  public BlockedAddress(@NotNull final InetAddress address, @NotNull final String reason) {
    this(address, reason, BlockedAddress.PERMANENT_BLOCK);
  }

  /**
   * checks if the address should unblock.
   *
   * @return {@code true} if the address should unblock.
   */
  public boolean shouldUnblock() {
    return this.expireTime > BlockedAddress.PERMANENT_BLOCK &&
      System.currentTimeMillis() - this.blockedTime >= this.expireTime;
  }
}
