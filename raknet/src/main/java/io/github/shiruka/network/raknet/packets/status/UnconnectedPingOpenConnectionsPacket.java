package io.github.shiruka.network.raknet.packets.status;

import io.github.shiruka.network.Packet;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents unconnected ping open connections packets.
 */
public final class UnconnectedPingOpenConnectionsPacket extends UnconnectedPingPacket {

  /**
   * ctor.
   *
   * @param packet the packet.
   */
  public UnconnectedPingOpenConnectionsPacket(@NotNull final Packet packet) {
    super(packet);
  }
}
