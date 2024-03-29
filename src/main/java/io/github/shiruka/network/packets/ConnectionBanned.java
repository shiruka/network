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
 * a class that represents connection banned packets.
 */
@Setter
@Accessors(fluent = true)
public final class ConnectionBanned implements Packet {

  /**
   * the magic.
   */
  @Nullable
  private RakNetMagic magic;

  /**
   * the server id.
   */
  @Getter
  private long serverId;

  /**
   * ctor.
   */
  public ConnectionBanned() {}

  /**
   * ctor.
   *
   * @param magic the magic.
   * @param serverId the server id.
   */
  public ConnectionBanned(
    @NotNull final RakNetMagic magic,
    final long serverId
  ) {
    this.magic = magic;
    this.serverId = serverId;
  }

  @Override
  public void decode(@NotNull final PacketBuffer buffer) {
    this.magic = RakNetMagic.from(buffer);
    this.serverId = buffer.readLong();
  }

  @Override
  public void encode(@NotNull final PacketBuffer buffer) {
    this.magic().write(buffer);
    buffer.writeLong(this.serverId);
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
