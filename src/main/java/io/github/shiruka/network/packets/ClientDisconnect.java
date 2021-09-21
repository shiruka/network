package io.github.shiruka.network.packets;

import io.github.shiruka.network.PacketBuffer;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents client disconnect packets.
 */
public final class ClientDisconnect extends FramedPacket.Base {

  /**
   * ctor.
   */
  public ClientDisconnect() {
    super(Reliability.RELIABLE);
  }

  @Override
  public void decode(@NotNull final PacketBuffer buffer) {
  }

  @Override
  public void encode(@NotNull final PacketBuffer buffer) {
  }
}
