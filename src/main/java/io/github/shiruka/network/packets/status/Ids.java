package io.github.shiruka.network.packets.status;

/**
 * an interface that contains rak net packet ids.
 */
interface Ids {

  /**
   * the ID of the {@link ConnectedPingPacket} packet.
   */
  short CONNECTED_PING = 0x00;

  /**
   * the ID of the {@link ConnectedPongPacket} packet.
   */
  short CONNECTED_PONG = 0x03;

  /**
   * the ID of the {@link UnconnectedPingPacket} packet.
   */
  short UNCONNECTED_PING = 0x01;

  /**
   * the ID of the {@link UnconnectedPingOpenConnectionsPacket} packet.
   */
  short UNCONNECTED_PING_OPEN_CONNECTIONS = 0x02;

  /**
   * the ID of the {@link UnconnectedPongPacket} packet.
   */
  short UNCONNECTED_PONG = 0x1C;
}
