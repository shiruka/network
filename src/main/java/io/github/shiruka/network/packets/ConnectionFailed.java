package io.github.shiruka.network.packets;

import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.options.RakNetMagic;
import java.util.Objects;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * a class that represents connection failed packets.
 */
@Setter
@NoArgsConstructor
@Accessors(fluent = true)
public final class ConnectionFailed implements Packet {

  /**
   * the code.
   */
  public long code;

  /**
   * the magic.
   */
  @Nullable
  public RakNetMagic magic;

  /**
   * ctor.
   *
   * @param code the code.
   * @param magic the magic.
   */
  public ConnectionFailed(final long code, @NotNull final RakNetMagic magic) {
    this.code = code;
    this.magic = magic;
  }

  /**
   * ctor.
   *
   * @param magic the magic.
   */
  public ConnectionFailed(@NotNull final RakNetMagic magic) {
    this(0, magic);
  }

  @Override
  public void decode(@NotNull final PacketBuffer buffer) {
    this.magic = RakNetMagic.from(buffer);
    this.code = buffer.readLong();
  }

  @Override
  public void encode(@NotNull final PacketBuffer buffer) {
    this.magic().write(buffer);
    buffer.writeLong(this.code);
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
