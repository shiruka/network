package io.github.shiruka.network.packets;

import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.options.RakNetMagic;
import java.util.Objects;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents invalid version packets.
 */
@Setter
@Accessors(fluent = true)
public final class InvalidVersion implements Packet {

  /**
   * the magic.
   */
  @Nullable
  private RakNetMagic magic;

  /**
   * the server.
   */
  @Getter
  private long serverId;

  /**
   * the version.
   */
  @Getter
  private int version;

  /**
   * ctor.
   */
  public InvalidVersion() {
  }

  /**
   * ctor.
   *
   * @param magic the magic.
   * @param version the version.
   * @param serverId the server id.
   */
  public InvalidVersion(@NotNull final RakNetMagic magic, final int version, final long serverId) {
    this.magic = magic;
    this.version = version;
    this.serverId = serverId;
  }

  /**
   * ctor.
   *
   * @param magic the magic.
   * @param serverId the server id.
   */
  public InvalidVersion(@NotNull final RakNetMagic magic, final long serverId) {
    this(magic, 0, serverId);
  }

  @Override
  public void decode(@NotNull final PacketBuffer buffer) {
    this.version = buffer.readUnsignedByte();
    this.magic = RakNetMagic.from(buffer);
    this.serverId = buffer.readLong();
  }

  @Override
  public void encode(@NotNull final PacketBuffer buffer) {
    buffer.writeByte(this.version);
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
