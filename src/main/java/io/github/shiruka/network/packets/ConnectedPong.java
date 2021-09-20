package io.github.shiruka.network.packets;

import io.github.shiruka.network.Ids;
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
public final class ConnectedPong extends Packet {

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
  public ConnectedPong() {
    super(Ids.CONNECTED_PONG);
  }

  @Override
  public void encode(@NotNull final PacketBuffer buffer) {
    buffer.writeLong(this.timestamp);
    buffer.writeLong(this.timestampPong);
  }
}
