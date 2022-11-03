package io.github.shiruka.network.packets;

import com.google.common.base.Preconditions;
import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.options.RakNetMagic;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * a class that represents connection request 1 packets.
 */
@Setter
@NoArgsConstructor
@Accessors(fluent = true)
public final class ConnectionRequest1 implements Packet {

  /**
   * the magic.
   */
  @Nullable
  private RakNetMagic magic;

  /**
   * the mtu.
   */
  @Getter
  private int mtu;

  /**
   * the protocol version.
   */
  @Getter
  private int protocolVersion;

  /**
   * ctor.
   *
   * @param magic the magic.
   * @param protocolVersion the protocol version.
   * @param mtu the mtu.
   */
  public ConnectionRequest1(
    @NotNull final RakNetMagic magic,
    final int protocolVersion,
    final int mtu
  ) {
    this.magic = magic;
    this.protocolVersion = protocolVersion;
    this.mtu = mtu;
  }

  @Override
  public void decode(@NotNull final PacketBuffer buffer) {
    this.mtu = buffer.remaining();
    this.magic = RakNetMagic.from(buffer);
    this.protocolVersion = buffer.readByte();
    buffer.skip(buffer.remaining());
    Preconditions.checkArgument(
      this.mtu >= 128,
      "ConnectionRequest1 MTU is too small!"
    );
    if (this.mtu > 8192) {
      this.mtu = 8192;
    }
  }

  @Override
  public void encode(@NotNull final PacketBuffer buffer) {
    this.magic().write(buffer);
    buffer.writeByte(this.protocolVersion);
    buffer.writeZero(this.mtu - buffer.remaining());
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
