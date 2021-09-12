package io.github.shiruka.network.raknet.packets.status;

import io.github.shiruka.network.Packet;

/**
 * a class that represents unconnected ping open connections packets.
 */
public final class UnconnectedPingOpenConnectionsPacket extends UnconnectedPingPacket {

  /**
   * ctor.
   */
  public UnconnectedPingOpenConnectionsPacket() {
    super(Ids.ID_UNCONNECTED_PING_OPEN_CONNECTIONS);
  }

  /**
   * ctor.
   *
   * @param packet the packet.
   */
  public UnconnectedPingOpenConnectionsPacket(final Packet packet) {
    super(packet);
  }
}
