package io.github.shiruka.network.packets.status;

import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
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
public final class ConnectedPongPacket extends Packet {

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

  @Override
  public void decode(@NotNull final PacketBuffer buffer) {
    this.timestamp = buffer.readLong();
    this.timestampPong = buffer.readLong();
  }

  @Override
  public void encode(@NotNull final PacketBuffer buffer) {
    buffer.writeLong(this.timestamp);
    buffer.writeLong(this.timestampPong);
  }
}