package io.github.shiruka.network.server;

import io.github.shiruka.network.BlockedAddress;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * an interface to determine rak net servers.
 */
public interface RakNetServer {

  /**
   * adds the listeners to the server.
   *
   * @param listeners the listeners to add.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  RakNetServer addListeners(@NotNull RakNetServerListener... listeners);

  /**
   * adds the blocked address.
   *
   * @param blocked the blocked to add.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  RakNetServer blockAddress(@NotNull BlockedAddress blocked);

  /**
   * obtains the blocked addresses.
   *
   * @return blocked addresses.
   */
  @NotNull
  Map<InetAddress, BlockedAddress> blockedAddresses();

  /**
   * obtains the listeners.
   *
   * @return listeners.
   */
  @NotNull
  Collection<RakNetServerListener> listeners();

  /**
   * obtains the logger.
   *
   * @return logger.
   */
  @NotNull
  Logger logger();

  /**
   * removes the listeners to the server.
   *
   * @param listeners the listeners to remove.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  RakNetServer removeListeners(@NotNull RakNetServerListener... listeners);

  /**
   * removes the blocked address.
   *
   * @param address the address.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  RakNetServer unblockAddress(@NotNull InetAddress address);

  /**
   * removes the blocked address.
   *
   * @param blocked the blocked.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  default RakNetServer unblockAddress(@NotNull final BlockedAddress blocked) {
    return this.unblockAddress(blocked.address());
  }
}
