package io.github.shiruka.network.raknet.packets.status;

/**
 * an interface that contains rak net packet ids.
 */
interface Ids {

  /**
   * the ID of the {@link UnconnectedPingPacket} packet.
   */
  short ID_UNCONNECTED_PING = 0x01;

  /**
   * the ID of the {@link UnconnectedPingOpenConnectionsPacket} packet.
   */
  short ID_UNCONNECTED_PING_OPEN_CONNECTIONS = 0x02;
}
