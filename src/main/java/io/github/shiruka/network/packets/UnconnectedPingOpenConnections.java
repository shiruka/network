package io.github.shiruka.network.packets;

import io.github.shiruka.network.ConnectionType;
import io.github.shiruka.network.Failable;
import io.github.shiruka.network.Ids;
import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * a class that represents unconnected ping open connections packets.
 */
@Setter
@Accessors(fluent = true)
public final class UnconnectedPingOpenConnections extends Packet implements Failable {

  /**
   * the client's connection type.
   */
  @Nullable
  private ConnectionType connectionType;

  /**
   * the failed.
   */
  @Getter
  private boolean failed;

  /**
   * whether or not the magic bytes read in the packet are valid.
   */
  @Getter
  private boolean magic;

  /**
   * the client's ping ID.
   */
  @Getter
  private long pingId;

  /**
   * the timestamp of the sender.
   */
  @Getter
  private long timestamp;

  /**
   * ctor.
   */
  public UnconnectedPingOpenConnections() {
    super(Ids.UNCONNECTED_PING_OPEN_CONNECTIONS);
  }

  /**
   * obtains the connection type.
   *
   * @return connection type.
   */
  @NotNull
  public ConnectionType connectionType() {
    return Objects.requireNonNull(this.connectionType, "connection type");
  }

  @Override
  public void decode(@NotNull final PacketBuffer buffer) {
    this.unchecked(buffer, () -> {
      this.timestamp = buffer.readLong();
      this.magic = buffer.readMagic();
      this.pingId = buffer.readLong();
      this.connectionType = buffer.readConnectionType();
    });
  }

  @Override
  public void onFail(@NotNull final PacketBuffer buffer) {
    this.timestamp = 0;
    this.magic = false;
    this.pingId = 0;
    this.connectionType = null;
    buffer.clear();
    this.failed = true;
  }
}