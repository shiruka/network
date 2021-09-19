package io.github.shiruka.network;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * an abstract class that represents packets.
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Packet {

  /**
   * the id.
   */
  private final int id;

  /**
   * decodes the packet.
   *
   * @param buffer the buffer to decode.
   */
  public void decode(@NotNull final PacketBuffer buffer) {
    throw new UnsupportedOperationException();
  }

  /**
   * encodes the packet.
   *
   * @param buffer the buffer to encode.
   */
  public void encode(@NotNull final PacketBuffer buffer) {
    throw new UnsupportedOperationException();
  }
}
