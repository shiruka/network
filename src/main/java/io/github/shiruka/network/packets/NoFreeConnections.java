package io.github.shiruka.network.packets;

import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.options.RakNetMagic;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents no free connection packets.
 */
@Getter
@Setter
@Accessors(fluent = true)
public final class NoFreeConnections implements Packet {

  /**
   * the magic.
   */
  @NotNull
  private RakNetMagic magic;

  /**
   * the server id.
   */
  private long serverId;

  /**
   * ctor.
   */
  public NoFreeConnections() {}

  /**
   * ctor.
   *
   * @param magic the magic.
   * @param serverId the server id.
   */
  public NoFreeConnections(
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
    this.magic.write(buffer);
    buffer.writeLong(this.serverId);
  }
}
