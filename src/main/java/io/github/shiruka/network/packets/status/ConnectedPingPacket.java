package io.github.shiruka.network.packets.status;

import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
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
public final class ConnectedPingPacket extends Packet {

  /**
   * the timestamp of the sender.
   */
  private long timestamp;

  /**
   * ctor.
   */
  public ConnectedPingPacket() {
    super(Ids.CONNECTED_PING);
  }

  @Override
  public void decode(@NotNull final PacketBuffer buffer) {
    this.timestamp = buffer.readLong();
  }

  @Override
  public void encode(@NotNull final PacketBuffer buffer) {
    buffer.writeLong(this.timestamp);
  }
}
