package io.github.shiruka.network;

import io.github.shiruka.network.packets.Ack;
import io.github.shiruka.network.packets.AlreadyConnected;
import io.github.shiruka.network.packets.ClientDisconnect;
import io.github.shiruka.network.packets.ClientHandshake;
import io.github.shiruka.network.packets.ConnectedPing;
import io.github.shiruka.network.packets.ConnectedPong;
import io.github.shiruka.network.packets.ConnectionBanned;
import io.github.shiruka.network.packets.ConnectionFailed;
import io.github.shiruka.network.packets.ConnectionReply1;
import io.github.shiruka.network.packets.ConnectionReply2;
import io.github.shiruka.network.packets.ConnectionRequest;
import io.github.shiruka.network.packets.ConnectionRequest1;
import io.github.shiruka.network.packets.ConnectionRequest2;
import io.github.shiruka.network.packets.InvalidVersion;
import io.github.shiruka.network.packets.Nack;
import io.github.shiruka.network.packets.NoFreeConnections;
import io.github.shiruka.network.packets.ServerHandshake;
import io.github.shiruka.network.packets.UnconnectedPing;
import io.github.shiruka.network.packets.UnconnectedPong;

/**
 * an interface that contains rak net packet ids.
 */
public interface Ids {

  /**
   * the id of the {@link Ack} packet.
   */
  int ACK = 192;

  /**
   * the id of the {@link AlreadyConnected} packet.
   */
  int ALREADY_CONNECTED = 18;

  /**
   * the id of the {@link ClientDisconnect} packet.
   */
  int CLIENT_DISCONNECT = 21;

  /**
   * the id of the {@link ClientHandshake} packet.
   */
  int CLIENT_HANDSHAKE = 19;

  /**
   * the id of the {@link ConnectedPing} packet.
   */
  int CONNECTED_PING = 0;

  /**
   * the id of the {@link ConnectedPong} packet.
   */
  int CONNECTED_PONG = 3;

  /**
   * the id of the {@link ConnectionBanned} packet.
   */
  int CONNECTION_BANNED = 23;

  /**
   * the id of the {@link ConnectionFailed} packet.
   */
  int CONNECTION_FAILED = 17;

  /**
   * the id of the {@link ConnectionReply1} packet.
   */
  int CONNECTION_REPLY_1 = 6;

  /**
   * the id of the {@link ConnectionReply2} packet.
   */
  int CONNECTION_REPLY_2 = 8;

  /**
   * the id of the {@link ConnectionRequest} packet.
   */
  int CONNECTION_REQUEST = 9;

  /**
   * the id of the {@link ConnectionRequest1} packet.
   */
  int CONNECTION_REQUEST_1 = 5;

  /**
   * the id of the {@link ConnectionRequest2} packet.
   */
  int CONNECTION_REQUEST_2 = 7;

  /**
   * the frame data end.
   */
  int FRAME_DATA_END = 143;

  /**
   * the frame data start.
   */
  int FRAME_DATA_START = 128;

  /**
   * the id of the {@link InvalidVersion} packet.
   */
  int INVALID_VERSION = 25;

  /**
   * the id of the {@link Nack} packet.
   */
  int NACK = 160;

  /**
   * the id of the {@link NoFreeConnections} packet.
   */
  int NO_FREE_CONNECTIONS = 20;

  /**
   * the id of the {@link ServerHandshake} packet.
   */
  int SERVER_HANDSHAKE = 16;

  /**
   * the id of the {@link UnconnectedPing} packet.
   */
  int UNCONNECTED_PING = 1;

  /**
   * the id of the {@link UnconnectedPong} packet.
   */
  int UNCONNECTED_PONG = 28;
}
