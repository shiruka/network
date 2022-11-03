package io.github.shiruka.network.packets;

import io.github.shiruka.network.PacketBuffer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents connected ping packets.
 */
@Getter
@Setter
@NoArgsConstructor
@Accessors(fluent = true)
public final class ConnectedPing extends FramedPacket.Base {

  /**
   * the timestamp of the sender.
   */
  private long timestamp;

  /**
   * ctor.
   *
   * @param timestamp the timestamp.
   */
  public ConnectedPing(final long timestamp) {
    this.timestamp = timestamp;
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
