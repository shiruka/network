package io.github.shiruka.network.packets;

import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.options.RakNetMagic;
import java.net.InetSocketAddress;
import java.util.Objects;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents connection request 2 packets.
 */
@Setter
@NoArgsConstructor
@Accessors(fluent = true)
public final class ConnectionRequest2 implements Packet.Client {

  /**
   * the address.
   */
  @Nullable
  private InetSocketAddress address;

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
   * the mtu.
   */
  @Getter
  private int mtu;

  /**
   * ctor.
   *
   * @param address the address.
   * @param clientId the client id.
   * @param magic the magic.
   * @param mtu the mtu.
   */
  public ConnectionRequest2(
    @NotNull final InetSocketAddress address,
    final long clientId,
    @NotNull final RakNetMagic magic,
    final int mtu
  ) {
    this.address = address;
    this.clientId = clientId;
    this.magic = magic;
    this.mtu = mtu;
  }

  /**
   * obtains the address.
   *
   * @return address.
   */
  @NotNull
  public InetSocketAddress address() {
    return Objects.requireNonNull(this.address, "address");
  }

  @Override
  public void decode(@NotNull final PacketBuffer buffer) {
    this.magic = RakNetMagic.from(buffer);
    this.address = buffer.readAddress();
    this.mtu = buffer.readUnsignedShort();
    this.clientId = buffer.readLong();
  }

  @Override
  public void encode(@NotNull final PacketBuffer buffer) {
    this.magic().write(buffer);
    buffer.writeAddress(this.address());
    buffer.writeShort(this.mtu);
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
