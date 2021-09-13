package io.github.shiruka.network.raknet.packets.status;

import io.github.shiruka.network.PacketSerializer;
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
  public UnconnectedPingOpenConnectionsPacket(@NotNull final PacketSerializer packet) {
    super(packet);
  }
}
