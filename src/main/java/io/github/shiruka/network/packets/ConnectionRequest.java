package io.github.shiruka.network.packets;

import io.github.shiruka.network.PacketBuffer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents connection request packets.
 */
@Getter
@Setter
@Accessors(fluent = true)
public final class ConnectionRequest extends FramedPacket.Base {

  /**
   * the client id.
   */
  private long clientId;

  /**
   * the timestamp.
   */
  private long timestamp;

  /**
   * ctor.
   */
  public ConnectionRequest() {
    super(Reliability.RELIABLE);
  }

  /**
   * ctor.
   *
   * @param clientId the client id.
   * @param timestamp the timestamp.
   */
  public ConnectionRequest(final long clientId, final long timestamp) {
    super(Reliability.RELIABLE);
    this.clientId = clientId;
    this.timestamp = timestamp;
  }

  @Override
  public void decode(@NotNull final PacketBuffer buffer) {
    this.clientId = buffer.readLong();
    this.timestamp = buffer.readLong();
    buffer.readBoolean();
  }

  @Override
  public void encode(@NotNull final PacketBuffer buffer) {
    buffer.writeLong(this.clientId);
    buffer.writeLong(this.timestamp);
    buffer.writeBoolean(false);
  }
}
