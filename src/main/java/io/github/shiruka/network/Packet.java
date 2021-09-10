package io.github.shiruka.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * an abstract class that represents network packets.
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Packet {

  /**
   * the buffer.
   */
  @NotNull
  private final ByteBuf buffer;

  /**
   * ctor.
   *
   * @param datagram the datagram packet.
   */
  protected Packet(@NotNull final DatagramPacket datagram) {
    this(datagram.content());
  }

  /**
   * ctor.
   *
   * @param data the data.
   */
  protected Packet(final byte @NotNull [] data) {
    this(Unpooled.copiedBuffer(data));
  }

  /**
   * ctor.
   *
   * @param packet the packet.
   */
  protected Packet(@NotNull final Packet packet) {
    this(packet.buffer);
  }

  /**
   * ctor.
   */
  protected Packet() {
    this(Unpooled.buffer());
  }

  /**
   * reads data into the specified byte[].
   *
   * @param dest the byte[] to read the data into.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet read(final byte[] dest) {
    for (var index = 0; index < dest.length; index++) {
      dest[index] = this.buffer.readByte();
    }
    return this;
  }

  /**
   * reads the specified amount of bytes.
   *
   * @param length the amount of bytes to read.
   *
   * @return the read bytes.
   */
  public final byte[] read(final int length) {
    final var bytes = new byte[length];
    for (int index = 0; index < bytes.length; index++) {
      bytes[index] = this.buffer.readByte();
    }
    return bytes;
  }

  /**
   * reads an IPv4/IPv6 address.
   *
   * @return an IPv4/IPv6 address.
   *
   * @throws UnknownHostException if no IP address for the host could be found, the family for an IPv6 address was not
   *   {@value Constants#AF_INET6}, a scope_id was specified for a global IPv6 address, or the address version is an
   *   unknown version.
   */
  @NotNull
  public final InetSocketAddress readAddress() throws UnknownHostException {
    final var version = this.readUnsignedByte();
    return switch (version) {
      case Constants.IPV4 -> this.readAddressIPV4();
      case Constants.IPV6 -> this.readAddressIPV6();
      default -> throw new UnknownHostException("Unknown protocol IPv%s"
        .formatted(version));
    };
  }

  /**
   * reads a boolean, technically a byte.
   *
   * @return a boolean.
   */
  public final boolean readBoolean() {
    return this.buffer.readBoolean();
  }

  /**
   * reads a byte.
   *
   * @return a byte.
   */
  public final byte readByte() {
    return this.buffer.readByte();
  }

  /**
   * converts the buffer into a byte array with a certain length.
   *
   * @param length the length to convert.
   *
   * @return bytes
   */
  public final byte[] readBytes(final int length) {
    final var bytes = new byte[length];
    this.buffer.readBytes(bytes);
    return bytes;
  }

  /**
   * reads a char.
   *
   * @return a char.
   */
  public final char readChar() {
    return (char) this.buffer.readShort();
  }

  /**
   * reads a little-endian char.
   *
   * @return a little-endian char.
   */
  public final char readCharLE() {
    return (char) this.buffer.readShortLE();
  }

  /**
   * reads a double.
   *
   * @return a double.
   */
  public final double readDouble() {
    return this.buffer.readDouble();
  }

  /**
   * reads a little-endian double.
   *
   * @return a little-endian double.
   */
  public final double readDoubleLE() {
    return this.buffer.readDoubleLE();
  }

  /**
   * reads a float.
   *
   * @return a float.
   */
  public final float readFloat() {
    return this.buffer.readFloat();
  }

  /**
   * reads a little-endian float.
   *
   * @return a little-endian float.
   */
  public final float readFloatLE() {
    return this.buffer.readFloatLE();
  }

  /**
   * reads an int.
   *
   * @return an int.
   */
  public final int readInt() {
    return this.buffer.readInt();
  }

  /**
   * reads a little-endian int.
   *
   * @return a little-endian int.
   */
  public final int readIntLE() {
    return this.buffer.readIntLE();
  }

  /**
   * reads a long.
   *
   * @return a long.
   */
  public final long readLong() {
    return this.buffer.readLong();
  }

  /**
   * reads a little-endian long.
   *
   * @return a little-endian long.
   */
  public final long readLongLE() {
    return this.buffer.readLongLE();
  }

  /**
   * reads a short.
   *
   * @return a short.
   */
  public final short readShort() {
    return this.buffer.readShort();
  }

  /**
   * reads a little-endian short.
   *
   * @return a little-endian short.
   */
  public final short readShortLE() {
    return this.buffer.readShortLE();
  }

  /**
   * reads a string.
   *
   * @return a string.
   */
  @NotNull
  public final String readString() {
    final var length = this.readVarInt();
    final var bytes = this.readBytes(length);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  /**
   * reads a triad.
   *
   * @return a triad.
   */
  public final int readTriad() {
    return this.buffer.readMedium();
  }

  /**
   * reads a little-endian triad.
   *
   * @return a little-endian triad.
   */
  public final int readTriadLE() {
    return this.buffer.readMediumLE();
  }

  /**
   * reads a UUID.
   *
   * @return a UUID.
   */
  @NotNull
  public final UUID readUUID() {
    final var most = this.readLong();
    final var least = this.readLong();
    return new UUID(most, least);
  }

  /**
   * reads an unsigned byte.
   *
   * @return an unsigned byte.
   */
  public final short readUnsignedByte() {
    return (short) (this.buffer.readByte() & 0xFF);
  }

  /**
   * reads an unsigned int.
   *
   * @return an unsigned int.
   */
  public final long readUnsignedInt() {
    return this.buffer.readInt() & 0xFFFFFFFFL;
  }

  /**
   * reads an unsigned little-endian int.
   *
   * @return an unsigned little-endian int.
   */
  public final long readUnsignedIntLE() {
    return this.buffer.readIntLE() & 0xFFFFFFFFL;
  }

  /**
   * reads an unsigned long.
   *
   * @return an unsigned long.
   */
  @NotNull
  public final BigInteger readUnsignedLong() {
    final var bytes = this.read(Long.BYTES);
    return new BigInteger(bytes);
  }

  /**
   * reads an unsigned little-endian long.
   *
   * @return an unsigned little-endian long.
   */
  @NotNull
  public final BigInteger readUnsignedLongLE() {
    final var reversed = this.read(Long.BYTES);
    final var bytes = new byte[reversed.length];
    for (var index = 0; index < bytes.length; index++) {
      bytes[index] = reversed[reversed.length - index - 1];
    }
    return new BigInteger(bytes);
  }

  /**
   * reads an unsigned short.
   *
   * @return an unsigned short.
   */
  public final int readUnsignedShort() {
    return this.buffer.readShort() & 0xFFFF;
  }

  /**
   * reads an unsigned little-endian short.
   *
   * @return an unsigned little-endian short.
   */
  public final int readUnsignedShortLE() {
    return this.buffer.readShortLE() & 0xFFFF;
  }

  /**
   * reads an unsigned triad.
   *
   * @return an unsigned triad.
   */
  public final int readUnsignedTriad() {
    return this.readTriad() & 0xFFFFFF;
  }

  /**
   * reads an unsigned little-endian triad.
   *
   * @return an unsigned little-endian triad.
   */
  public final int readUnsignedTriadLE() {
    return this.readTriad() & 0xFFFFFF;
  }

  /**
   * returns how many readable byte>s are left in the packet's
   * buffer.
   * <p>
   * this is to only be used for packets that are being read from. To get the
   * amount of bytes that have been written to the packet, use the
   * {@link #size()} method.
   *
   * @return how many readable byte>s are left in the packet's buffer.
   */
  public final int remaining() {
    return this.buffer.readableBytes();
  }

  /**
   * returns the size of the packet in bytes.
   * <p>
   * this is to be used only for packets that are being written to. To get the
   * amount of bytes that are still readable, use the {@link #remaining()}
   * method.
   *
   * @return the size of the packet in bytes.
   */
  public final int size() {
    return this.buffer.writerIndex();
  }

  /**
   * skips the specified amount of bytes.
   *
   * @param length the amount of bytes to skip.
   */
  public final void skip(final int length) {
    this.buffer.skipBytes(Math.min(length, this.remaining()));
  }

  /**
   * writes a string.
   *
   * @param data the data to write.
   */
  public final void writeString(@NotNull final String data) {
    this.writeVarInt(data.length());
    this.buffer.writeBytes(data.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * reads an IPv4 address.
   *
   * @return an IPv4 address.
   *
   * @throws UnknownHostException if IP address is of illegal length.
   */
  @NotNull
  private InetSocketAddress readAddressIPV4() throws UnknownHostException {
    final var address = new byte[Constants.IPV4_ADDRESS_LENGTH];
    for (var index = 0; index < address.length; index++) {
      address[index] = (byte) (~this.readByte() & 0xFF);
    }
    final var port = this.readUnsignedShort();
    return new InetSocketAddress(InetAddress.getByAddress(address), port);
  }

  /**
   * reads an IPv6 address.
   *
   * @return an IPv6 address.
   *
   * @throws UnknownHostException if IP address is of illegal length.
   */
  @NotNull
  private InetSocketAddress readAddressIPV6() throws UnknownHostException {
    this.readShortLE();
    final var port = this.readUnsignedShort();
    this.readInt();
    final var ipAddress = new byte[Constants.IPV6_ADDRESS_LENGTH];
    for (var index = 0; index < ipAddress.length; index++) {
      ipAddress[index] = this.readByte();
    }
    this.readInt();
    return new InetSocketAddress(InetAddress.getByAddress(ipAddress), port);
  }

  /**
   * reads a var int.
   *
   * @return a var int.
   */
  private int readVarInt() {
    var result = 0;
    var indent = 0;
    var b = this.buffer.readByte();
    while ((b & 0x80) == 0x80) {
      if (indent >= 21) {
        throw new IllegalArgumentException("Too many bytes for a VarInt32.");
      }
      result += (b & 0x7f) << indent;
      indent += 7;
      b = this.buffer.readByte();
    }
    result += (b & 0x7f) << indent;
    return result;
  }

  /**
   * writes a var int.
   *
   * @param data the data to write.
   */
  private void writeVarInt(final int data) {
    var temp = data;
    while ((temp & 0xFFFFFF80) != 0L) {
      this.buffer.writeByte(temp & 0x7F | 0x80);
      temp >>>= 7;
    }
    this.buffer.writeByte(temp & 0x7F);
  }
}
