package io.github.shiruka.network.raknet;

import com.google.common.base.Preconditions;
import io.github.shiruka.network.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import java.util.Arrays;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * an abstract class that represents rak net packets.
 */
@Accessors(fluent = true)
public abstract class RakNetPacket extends Packet {

  /**
   * the name of the decode() method.
   */
  private static final String DECODE_METHOD_NAME = "decode";

  /**
   * the name of the encode() method.
   */
  private static final String ENCODE_METHOD_NAME = "encode";

  /**
   * the magic identifier.
   */
  private static final byte[] MAGIC = new byte[]{(byte) 0x00, (byte) 0xFF, (byte) 0xFF, 0x00, (byte) 0xFE,
    (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFD, (byte) 0xFD, (byte) 0xFD, (byte) 0xFD, (byte) 0x12,
    (byte) 0x34, (byte) 0x56, (byte) 0x78};

  /**
   * the id.
   */
  @Getter
  private final int id;

  /**
   * the supports decoding.
   */
  @Getter
  private final boolean supportsDecoding;

  /**
   * the supports encoding.
   */
  @Getter
  private final boolean supportsEncoding;

  /**
   * ctor.
   *
   * @param buffer the buffer.
   */
  protected RakNetPacket(@NotNull final ByteBuf buffer) {
    super(buffer);
    Preconditions.checkArgument(this.remaining() >= 1,
      "Buffer must have at least one readable byte for the ID");
    this.id = this.readUnsignedByte();
    this.supportsEncoding = RakNetPacket.isEncodeMethodOverridden();
    this.supportsDecoding = RakNetPacket.isDecodeMethodOverridden();
  }

  /**
   * ctor.
   *
   * @param packet the packet.
   */
  protected RakNetPacket(@NotNull final Packet packet) {
    super(packet);
    if (packet instanceof RakNetPacket rakNetPacket) {
      this.id = rakNetPacket.id();
    } else {
      Preconditions.checkArgument(this.remaining() >= 1,
        "The packet must have at least one byte to read the ID");
      this.id = this.readUnsignedByte();
    }
    this.supportsEncoding = RakNetPacket.isEncodeMethodOverridden();
    this.supportsDecoding = RakNetPacket.isDecodeMethodOverridden();
  }

  /**
   * ctor.
   *
   * @param id the id.
   */
  protected RakNetPacket(final int id) {
    super();
    Preconditions.checkArgument(id >= 0x00 && id <= 0xFF,
      "ID must be in between 0-255");
    this.id = id;
    this.writeUnsignedByte(id);
    this.supportsEncoding = RakNetPacket.isEncodeMethodOverridden();
    this.supportsDecoding = RakNetPacket.isDecodeMethodOverridden();
  }

  /**
   * ctor.
   *
   * @param datagram the datagram.
   */
  protected RakNetPacket(@NotNull final DatagramPacket datagram) {
    this(datagram.content());
  }

  /**
   * ctor.
   *
   * @param data the data.
   */
  protected RakNetPacket(final byte @NotNull [] data) {
    this(Unpooled.copiedBuffer(data));
  }

  /**
   * checks if the given class override the decode method.
   *
   * @return {@code true} if the decode method has been overridden, {@code false} otherwise.
   */
  private static boolean isDecodeMethodOverridden() {
    return RakNetPacket.isMethodOverridden(RakNetPacket.DECODE_METHOD_NAME);
  }

  /**
   * checks if the given class override the encode method.
   *
   * @return {@code true} if the encode method has been overridden, {@code false} otherwise.
   */
  private static boolean isEncodeMethodOverridden() {
    return RakNetPacket.isMethodOverridden(RakNetPacket.ENCODE_METHOD_NAME);
  }

  /**
   * checks if the class override the method name.
   *
   * @param methodName the method name to check.
   *
   * @return {@code true} if the method has been overridden, {@code false} otherwise.
   */
  private static boolean isMethodOverridden(@NotNull final String methodName) {
    try {
      return !RakNetPacket.class.getMethod(methodName).getDeclaringClass().equals(RakNetPacket.class);
    } catch (final NoSuchMethodException | SecurityException e) {
      return false;
    }
  }

  /**
   * reads a magic array and returns whether or not it is valid.
   *
   * @return {@code true} if the magic array was valid, {@code false} otherwise.
   */
  public final boolean readMagic() {
    final var magicCheck = this.read(RakNetPacket.MAGIC.length);
    return Arrays.equals(RakNetPacket.MAGIC, magicCheck);
  }

  /**
   * writes the magic sequence to the packet.
   *
   * @return the packet.
   */
  @NotNull
  public final RakNetPacket writeMagic() {
    this.write(RakNetPacket.MAGIC);
    return this;
  }

  /**
   * decodes the packet.
   *
   * @throws UnsupportedOperationException if decoding the packet is not supported.
   */
  public void decode() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Decoding not supported!");
  }

  /**
   * encodes the packet.
   *
   * @throws UnsupportedOperationException if encoding the packet is not supported.
   */
  public void encode() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Encoding not supported!");
  }
}
