package io.github.shiruka.network.packets;

import com.google.common.base.Preconditions;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.options.RakNetMagic;
import java.net.InetSocketAddress;
import java.util.Objects;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * a class that represents connection reply 2 packets.
 */
@NoArgsConstructor
public final class ConnectionReply2 extends ConnectionReply {

  /**
   * the address.
   */
  @Nullable
  @Setter
  private InetSocketAddress address;

  /**
   * ctor.
   *
   * @param magic the magic.
   * @param mtu the mtu.
   * @param serverId the server id.
   * @param address the address.
   */
  public ConnectionReply2(
    @NotNull final RakNetMagic magic,
    final int mtu,
    final long serverId,
    @NotNull final InetSocketAddress address
  ) {
    super(magic, mtu, serverId);
    this.address = address;
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
    this.magic(RakNetMagic.from(buffer));
    this.serverId(buffer.readLong());
    Preconditions.checkArgument(
      !buffer.readBoolean(),
      "No security support yet"
    );
    this.mtu(buffer.readShort());
  }

  @Override
  public void encode(@NotNull final PacketBuffer buffer) {
    this.magic().write(buffer);
    buffer.writeLong(this.serverId());
    if (this.address == null) {
      buffer.writeAddress();
    } else {
      buffer.writeAddress(this.address());
    }
    buffer.writeShort(this.mtu());
    buffer.writeBoolean(ConnectionReply.NEEDS_SECURITY);
  }
}
