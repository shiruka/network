package io.github.shiruka.network.raknet.packets.status;

import io.github.shiruka.network.Packet;
import io.github.shiruka.network.raknet.RakNetPacket;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents connected ping packets.
 */
@Getter
@Setter
@Accessors(fluent = true)
public final class ConnectedPingPacket extends RakNetPacket {

  /**
   * the timestamp of the sender.
   */
  private long timestamp;

  /**
   * ctor.
   *
   * @param packet the packet.
   */
  public ConnectedPingPacket(@NotNull final Packet packet) {
    super(packet);
  }

  @Override
  public void decode() {
    this.timestamp = this.readLong();
  }
}
