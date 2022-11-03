package io.github.shiruka.network.packets;

import com.google.common.base.Preconditions;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.options.RakNetMagic;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents connection reply 1 packets.
 */
@NoArgsConstructor
public final class ConnectionReply1 extends ConnectionReply {

  /**
   * ctor.
   *
   * @param magic the magic.
   * @param mtu the mtu.
   * @param serverId the server id.
   */
  public ConnectionReply1(
    @NotNull final RakNetMagic magic,
    final int mtu,
    final long serverId
  ) {
    super(magic, mtu, serverId);
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
    buffer.writeBoolean(ConnectionReply.NEEDS_SECURITY);
    buffer.writeShort(this.mtu());
  }
}
