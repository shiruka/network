package io.github.shiruka.network.packets.status;

import io.github.shiruka.network.ConnectionType;
import io.github.shiruka.network.Failable;
import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.ServerIdentifier;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * a class that represents unconnected pong packets.
 */
@Setter
@Accessors(fluent = true)
public final class UnconnectedPongPacket extends Packet implements Failable {

  /**
   * the server's connection type.
   */
  @Nullable
  public ConnectionType connectionType;

  /**
   * the server's identifier.
   */
  @Nullable
  public ServerIdentifier identifier;

  /**
   * whether or not the magic bytes read in the packet are valid.
   */
  @Getter
  public boolean magic;

  /**
   * the server's pong ID.
   */
  @Getter
  public long pongId;

  /**
   * the timestamp sent in the ping packet.
   */
  @Getter
  public long timestamp;

  /**
   * whether or not the packet failed to encode/decode.
   */
  @Getter
  private boolean failed;

  /**
   * ctor.
   */
  public UnconnectedPongPacket() {
    super(Ids.UNCONNECTED_PONG);
  }

  /**
   * obtains the connection type.
   *
   * @return connection type.
   */
  @NotNull
  public ConnectionType connectionType() {
    return this.connectionType == null
      ? ConnectionType.RAK_NET
      : this.connectionType;
  }

  @Override
  public void encode(@NotNull final PacketBuffer buffer) {
    this.unchecked(buffer, () -> {
      buffer.writeLong(this.timestamp);
      buffer.writeLong(this.pongId);
      buffer.writeMagic();
      buffer.writeString(this.identifier().build());
      buffer.writeConnectionType(this.connectionType());
    });
  }

  /**
   * obtains the identifier.
   *
   * @return identifier.
   */
  @NotNull
  public ServerIdentifier identifier() {
    return Objects.requireNonNull(this.identifier, "identifier");
  }

  @Override
  public void onFail(@NotNull final PacketBuffer buffer) {
    this.timestamp = 0;
    this.pongId = 0;
    this.magic = false;
    this.identifier = null;
    this.connectionType = null;
    buffer.clear();
    this.failed = true;
  }
}
