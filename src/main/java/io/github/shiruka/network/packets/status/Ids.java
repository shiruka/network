package io.github.shiruka.network.packets.status;

/**
 * an interface that contains rak net packet ids.
 */
interface Ids {

  /**
   * the ID of the {@link ConnectedPingPacket} packet.
   */
  short CONNECTED_PING = 0;

  /**
   * the ID of the {@link ConnectedPongPacket} packet.
   */
  short CONNECTED_PONG = 3;

  /**
   * the ID of the {@link UnconnectedPingPacket} packet.
   */
  short UNCONNECTED_PING = 1;

  /**
   * the ID of the {@link UnconnectedPingOpenConnectionsPacket} packet.
   */
  short UNCONNECTED_PING_OPEN_CONNECTIONS = 2;

  /**
   * the ID of the {@link UnconnectedPongPacket} packet.
   */
  short UNCONNECTED_PONG = 28;
}
