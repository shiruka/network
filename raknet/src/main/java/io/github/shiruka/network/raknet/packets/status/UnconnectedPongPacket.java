package io.github.shiruka.network.raknet.packets.status;

import io.github.shiruka.network.raknet.ConnectionType;
import io.github.shiruka.network.raknet.Failable;
import io.github.shiruka.network.raknet.RakNetPacket;
import io.github.shiruka.network.raknet.server.RakNetServerIdentifier;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

/**
 * a class that represents unconnected pong packets.
 */
@Getter
@Setter
@Accessors(fluent = true)
public final class UnconnectedPongPacket extends RakNetPacket implements Failable {

  /**
   * the server's connection type.
   */
  @Nullable
  public ConnectionType connectionType;

  /**
   * the server's identifier.
   */
  @Nullable
  public RakNetServerIdentifier identifier;

  /**
   * whether or not the magic bytes read in the packet are valid.
   */
  public boolean magic;

  /**
   * the server's pong ID.
   */
  public long pongId;

  /**
   * the timestamp sent in the ping packet.
   */
  public long timestamp;

  /**
   * whether or not the packet failed to encode/decode.
   */
  private boolean failed;

  /**
   * ctor.
   */
  public UnconnectedPongPacket() {
    super(Ids.UNCONNECTED_PONG);
  }

  @Override
  public void encode() throws UnsupportedOperationException {
    this.unchecked(() -> {
      this.writeLong(this.timestamp);
      this.writeLong(this.pongId);
      this.writeMagic();
      this.writeString(Objects.requireNonNull(this.identifier, "identifier").build());
      this.writeConnectionType(this.connectionType == null
        ? ConnectionType.RAK_NET
        : this.connectionType);
    });
  }

  @Override
  public boolean failed() {
    return this.failed;
  }

  @Override
  public void onFail() {
    this.timestamp = 0;
    this.pongId = 0;
    this.magic = false;
    this.identifier = null;
    this.connectionType = null;
    this.clear();
    this.failed = true;
  }
}
