package io.github.shiruka.network.packets;

import io.github.shiruka.network.Ids;
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
public final class ConnectedPing extends Packet {

  /**
   * the timestamp of the sender.
   */
  private long timestamp;

  /**
   * ctor.
   */
  public ConnectedPing() {
    super(Ids.CONNECTED_PING);
  }

  @Override
  public void decode(@NotNull final PacketBuffer buffer) {
    this.timestamp = buffer.readLong();
  }
}
