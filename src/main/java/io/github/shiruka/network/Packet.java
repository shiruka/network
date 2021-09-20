package io.github.shiruka.network;

import org.jetbrains.annotations.NotNull;

/**
 * an interface to determine rak net packets.
 */
public interface Packet {

  /**
   * decodes the packet.
   *
   * @param buffer the buffer to decode.
   */
  void decode(@NotNull PacketBuffer buffer);

  /**
   * encodes the packet.
   *
   * @param buffer the buffer to encode.
   */
  void encode(@NotNull PacketBuffer buffer);

  /**
   * obtains the initial size hint.
   *
   * @return initial size hint.
   */
  default int initialSizeHint() {
    return 128;
  }
}
