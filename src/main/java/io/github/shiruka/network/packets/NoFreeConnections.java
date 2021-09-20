package io.github.shiruka.network.packets;

import io.github.shiruka.network.Ids;
import io.github.shiruka.network.Packet;
import io.github.shiruka.network.options.RakNetMagic;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents no free connection packets.
 */
public final class NoFreeConnections extends Packet {

  /**
   * the magic.
   */
  @NotNull
  private final RakNetMagic magic;

  /**
   * the server id.
   */
  private final long serverId;

  /**
   * ctor.
   *
   * @param magic the magic.
   * @param serverId the server id.
   */
  public NoFreeConnections(@NotNull final RakNetMagic magic, final long serverId) {
    super(Ids.NO_FREE_CONNECTIONS);
    this.magic = magic;
    this.serverId = serverId;
  }
}
