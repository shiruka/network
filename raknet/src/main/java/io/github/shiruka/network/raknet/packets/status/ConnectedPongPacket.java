package io.github.shiruka.network.raknet.packets.status;

import io.github.shiruka.network.Packet;
import io.github.shiruka.network.raknet.RakNetPacket;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents connected pong packets.
 */
@Getter
@Setter
@Accessors(fluent = true)
public final class ConnectedPongPacket extends RakNetPacket {

  /**
   * the timestamp of the sender of the ping.
   */
  public long timestamp;

  /**
   * The timestamp of the sender of the pong.
   */
  public long timestampPong;

  /**
   * ctor.
   */
  public ConnectedPongPacket() {
    super(Ids.CONNECTED_PONG);
  }

  /**
   * ctor.
   *
   * @param packet the packet.
   */
  public ConnectedPongPacket(@NotNull final Packet packet) {
    super(packet);
  }

  @Override
  public void decode() {
    this.timestamp = this.readLong();
    this.timestampPong = -1L;
    if (this.remaining() >= Long.BYTES) {
      this.timestampPong = this.readLong();
    }
  }

  @Override
  public void encode() {
    this.writeLong(this.timestamp);
    this.writeLong(this.timestampPong);
  }
}
