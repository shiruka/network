package io.github.shiruka.network.packets;

import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.options.RakNetMagic;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * a class that represents unconnected ping packets.
 */
@Setter
@Accessors(fluent = true)
public final class UnconnectedPing implements Packet {

  /**
   * the client id.
   */
  @Getter
  private long clientId;

  /**
   * the magic.
   */
  @Nullable
  private RakNetMagic magic;

  /**
   * the timestamp of the sender.
   */
  @Getter
  private long timestamp;

  /**
   * ctor.
   */
  public UnconnectedPing() {
  }

  /**
   * ctor.
   *
   * @param magic the magic.
   * @param clientId the client id.
   * @param timestamp the timestamp.
   */
  public UnconnectedPing(@NotNull final RakNetMagic magic, final long clientId, final long timestamp) {
    this.magic = magic;
    this.clientId = clientId;
    this.timestamp = timestamp;
  }

  @Override
  public void decode(@NotNull final PacketBuffer buffer) {
    this.timestamp = buffer.readLong();
    this.magic = RakNetMagic.from(buffer);
    this.clientId = buffer.readLong();
  }

  @Override
  public void encode(@NotNull final PacketBuffer buffer) {
    buffer.writeLong(this.timestamp);
    this.magic().write(buffer);
    buffer.writeLong(this.clientId);
  }

  /**
   * obtains the magic.
   *
   * @return magic.
   */
  @NotNull
  public RakNetMagic magic() {
    return Objects.requireNonNull(this.magic, "magic");
  }
}
