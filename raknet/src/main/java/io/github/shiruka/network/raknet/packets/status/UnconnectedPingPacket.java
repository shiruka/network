package io.github.shiruka.network.raknet.packets.status;

import io.github.shiruka.network.Packet;
import io.github.shiruka.network.raknet.ConnectionType;
import io.github.shiruka.network.raknet.Failable;
import io.github.shiruka.network.raknet.RakNetPacket;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * a class that represents unconnected ping packets.
 */
@Getter
@Setter
@Accessors(fluent = true)
public class UnconnectedPingPacket extends RakNetPacket implements Failable {

  /**
   * the client's connection type.
   */
  @Nullable
  private ConnectionType connectionType;

  /**
   * the failed.
   */
  private boolean failed;

  /**
   * whether or not the magic bytes read in the packet are valid.
   */
  private boolean magic;

  /**
   * the client's ping ID.
   */
  private long pingId;

  /**
   * the timestamp of the sender.
   */
  private long timestamp;

  /**
   * ctor.
   *
   * @param id the id.
   */
  protected UnconnectedPingPacket(final int id) {
    super(id);
  }

  /**
   * ctor.
   */
  public UnconnectedPingPacket() {
    this(Ids.ID_UNCONNECTED_PING);
  }

  /**
   * ctor.
   *
   * @param packet the packet.
   */
  public UnconnectedPingPacket(@NotNull final Packet packet) {
    super(packet);
  }

  @Override
  public final void decode() throws UnsupportedOperationException {
    this.unchecked(() -> {
      this.timestamp = this.readLong();
      this.magic = this.readMagic();
      this.pingId = this.readLong();
      this.connectionType = this.readConnectionType();
    });
  }

  @Override
  public final void encode() throws UnsupportedOperationException {
    this.unchecked(() -> {
      this.writeLong(this.timestamp);
      this.writeMagic();
      this.writeLong(this.pingId);
      this.writeConnectionType(this.connectionType == null
        ? ConnectionType.RAK_NET
        : this.connectionType);
    });
  }

  @Override
  public final boolean failed() {
    return this.failed;
  }

  @Override
  public final void onFail() {
    this.timestamp = 0;
    this.magic = false;
    this.pingId = 0;
    this.connectionType = null;
    this.clear();
    this.failed = true;
  }
}
