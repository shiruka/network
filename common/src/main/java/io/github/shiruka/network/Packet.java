package io.github.shiruka.network;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.AsciiString;
import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
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
   * the Inet 6 address family.
   */
  private static final int AF_INET6 = 23;

  /**
   * the IPv4 version.
   */
  private static final int IPV4 = 4;

  /**
   * the length of IPv4 addresses.
   */
  private static final int IPV4_ADDRESS_LENGTH = 4;

  /**
   * the IPv6 version.
   */
  private static final int IPV6 = 6;

  /**
   * the length of IPv6 addresses.
   */
  private static final int IPV6_ADDRESS_LENGTH = 16;

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
   * flips the bytes.
   *
   * @param bytes the bytes to flip.
   */
  private static void flip(final byte[] bytes) {
    for (var index = 0; index < bytes.length; index++) {
      bytes[index] = (byte) (~bytes[index] & 0xFF);
    }
  }

  /**
   * returns the version of the specified IP address.
   *
   * @param address the IP address.
   *
   * @return the version of the IP address, -1 if the version is unknown.
   */
  private static int getAddressVersion(@NotNull final InetAddress address) {
    return switch (address.getAddress().length) {
      case Packet.IPV4_ADDRESS_LENGTH -> Packet.IPV4;
      case Packet.IPV6_ADDRESS_LENGTH -> Packet.IPV6;
      default -> -1;
    };
  }

  /**
   * returns the version of the IP address of the specified address.
   *
   * @param address the address.
   *
   * @return the version of the IP address, -1 if the version is unknown.
   */
  private static int getAddressVersion(@NotNull final InetSocketAddress address) {
    return Packet.getAddressVersion(address.getAddress());
  }

  /**
   * returns the packet as a byte[].
   *
   * @return the packet as a byte[].
   */
  public final byte[] array() {
    Preconditions.checkState(!this.buffer.isDirect(),
      "The buffer is a direct buffer!");
    return Arrays.copyOfRange(this.buffer.array(), 0, this.size());
  }

  /**
   * clears the packet's buffer.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet clear() {
    this.buffer.clear();
    return this;
  }

  /**
   * returns a copy of the packet buffer.
   *
   * @return a copy of the packet buffer.
   */
  @NotNull
  public final ByteBuf copy() {
    return this.buffer.copy();
  }

  /**
   * writes the specified amount of {@code null} (0x00) bytes to the packet.
   *
   * @param length the amount of bytes to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet pad(final int length) {
    for (var index = 0; index < length; index++) {
      this.writeByte(0x00);
    }
    return this;
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
      dest[index] = this.readByte();
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
    this.read(bytes);
    return bytes;
  }

  /**
   * reads an IPv4/IPv6 address.
   *
   * @return an IPv4/IPv6 address.
   *
   * @throws UnknownHostException if no IP address for the host could be found, the family for an IPv6 address was not
   *   {@value #AF_INET6}, a scope_id was specified for a global IPv6 address, or the address version is an
   *   unknown version.
   */
  @NotNull
  public final InetSocketAddress readAddress() throws UnknownHostException {
    final var version = this.readUnsignedByte();
    return switch (version) {
      case Packet.IPV4 -> this.readAddressIPV4();
      case Packet.IPV6 -> this.readAddressIPV6();
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
   * reads bytes.
   *
   * @param bytes the bytes to read.
   *
   * @return bytes.
   */
  @NotNull
  public final ByteBuf readBytes(final byte[] bytes) {
    return this.buffer.readBytes(bytes);
  }

  /**
   * reads bytes.
   *
   * @param length the length to convert.
   *
   * @return bytes.
   */
  public final byte[] readBytes(final int length) {
    final var bytes = new byte[length];
    this.readBytes(bytes);
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
   * reads ascii string.
   *
   * @return ascii string.
   */
  @NotNull
  public final AsciiString readLEAsciiString() {
    final var length = this.readIntLE();
    final var bytes = this.readBytes(length);
    return new AsciiString(bytes);
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
   * reads slice.
   *
   * @return slice.
   */
  @NotNull
  public final ByteBuf readSlice() {
    return this.buffer.readSlice(this.readUnsignedVarInt());
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
    return (short) (this.readByte() & 0xFF);
  }

  /**
   * reads an unsigned int.
   *
   * @return an unsigned int.
   */
  public final long readUnsignedInt() {
    return this.readInt() & 0xFFFFFFFFL;
  }

  /**
   * reads an unsigned little-endian int.
   *
   * @return an unsigned little-endian int.
   */
  public final long readUnsignedIntLE() {
    return this.readIntLE() & 0xFFFFFFFFL;
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
    return this.readShort() & 0xFFFF;
  }

  /**
   * reads an unsigned little-endian short.
   *
   * @return an unsigned little-endian short.
   */
  public final int readUnsignedShortLE() {
    return this.readShortLE() & 0xFFFF;
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
   * releases the packet's buffer.
   *
   * @return {@code true} if and only if the reference count became 0 and this object has been deallocated, {@code
   *   false} otherwise.
   */
  public final boolean release() {
    return this.buffer.release();
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
   * writes the specified bytes to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet write(final byte... data) {
    for (final var datum : data) {
      this.writeByte(datum);
    }
    return this;
  }

  /**
   * writes the specified bytes to the packet.
   * <p>
   * this method is simply a shorthand for the {@link #write(byte...)} method, with all the values being automatically
   * casted back to a byte before being sent to the original {@link #write(byte...)} method.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet write(final int... data) {
    final var bytes = new byte[data.length];
    for (var index = 0; index < data.length; index++) {
      bytes[index] = (byte) data[index];
    }
    return this.write(bytes);
  }

  /**
   * writes an IPv4/IPv6 address to the packet.
   *
   * @param address the address.
   *
   * @return the packet.
   *
   * @throws UnknownHostException if no IP address for the host could be found, if a scope_id was specified for a
   *   global IPv6 address, or the length of the address is not either {@value #IPV4_ADDRESS_LENGTH} or {@value
   *   #IPV6_ADDRESS_LENGTH} bytes.
   */
  @NotNull
  public final Packet writeAddress(@NotNull final InetSocketAddress address) throws UnknownHostException {
    Objects.requireNonNull(address.getAddress(), "address");
    return switch (Packet.getAddressVersion(address)) {
      case Packet.IPV4 -> this.writeAddressIPV4(address);
      case Packet.IPV6 -> this.writeAddressIPV6(address);
      default -> throw new UnknownHostException("Unknown protocol for address with length of %d bytes"
        .formatted(address.getAddress().getAddress().length));
    };
  }

  /**
   * writes an IPv4 address to the packet.
   *
   * @param host the IP address.
   * @param port the port.
   *
   * @return the packet.
   *
   * @throws IllegalArgumentException if the port is not in between 0-65535.
   * @throws UnknownHostException if no IP address for the host could not be found, or if a scope_id was specified for
   *   a global IPv6 address.
   */
  @NotNull
  public final Packet writeAddress(@NotNull final InetAddress host, final int port) throws IllegalArgumentException,
    UnknownHostException {
    Preconditions.checkArgument(port >= 0x0000 && port <= 0xFFFF, "Port must be in between 0-65535");
    return this.writeAddress(new InetSocketAddress(host, port));
  }

  /**
   * writes an IPv4 address to the packet (IPv6 is not yet supported).
   *
   * @param host the IP address.
   * @param port the port.
   *
   * @return the packet.
   *
   * @throws IllegalArgumentException if the port is not in between 0-65535.
   * @throws UnknownHostException if no IP address for the host could not be found, or if a scope_id was specified for
   *   a global IPv6 address.
   */
  @NotNull
  public final Packet writeAddress(@NotNull final String host, final int port) throws IllegalArgumentException,
    UnknownHostException {
    Preconditions.checkArgument(port >= 0x0000 && port <= 0xFFFF, "Port must be in between 0-65535");
    return this.writeAddress(InetAddress.getByName(host), port);
  }

  /**
   * writes an IPv4 address to the packet.
   *
   * @param address the address.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeAddressIPV4(@NotNull final InetSocketAddress address) {
    final var ipAddress = address.getAddress().getAddress();
    this.writeByte(Packet.IPV4);
    Packet.flip(ipAddress);
    this.writeBytes(ipAddress);
    this.writeShort(address.getPort());
    return this;
  }

  /**
   * writes an IPv6 address to the packet.
   *
   * @param address the address.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeAddressIPV6(@NotNull final InetSocketAddress address) {
    final var ipv6Address = (Inet6Address) address.getAddress();
    this.writeByte(Packet.IPV6);
    this.writeShortLE(Packet.AF_INET6);
    this.writeShort(address.getPort());
    this.writeInt(0);
    this.write(ipv6Address.getAddress());
    this.writeInt(ipv6Address.getScopeId());
    return this;
  }

  /**
   * writes a boolean to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeBoolean(final boolean data) {
    this.buffer.writeBoolean(data);
    return this;
  }

  /**
   * writes a byte to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeByte(final int data) {
    this.buffer.writeByte((byte) data);
    return this;
  }

  /**
   * writes bytes.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeBytes(final byte[] data) {
    this.buffer.writeBytes(data);
    return this;
  }

  /**
   * writes a double to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeDouble(final double data) {
    this.buffer.writeDouble(data);
    return this;
  }

  /**
   * writes a double to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeDoubleLE(final double data) {
    this.buffer.writeDoubleLE(data);
    return this;
  }

  /**
   * writes a float to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeFloat(final double data) {
    this.buffer.writeFloat((float) data);
    return this;
  }

  /**
   * writes a little-endian float to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeFloatLE(final double data) {
    this.buffer.writeFloatLE((float) data);
    return this;
  }

  /**
   * writes an int to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeInt(final int data) {
    this.buffer.writeInt(data);
    return this;
  }

  /**
   * writes a little-endian int to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeIntLE(final int data) {
    this.buffer.writeIntLE(data);
    return this;
  }

  /**
   * writes ascii string.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeLEAsciiString(@NotNull final AsciiString data) {
    return this.writeIntLE(data.length())
      .writeBytes(data.array());
  }

  /**
   * writes a long to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeLong(final long data) {
    this.buffer.writeLong(data);
    return this;
  }

  /**
   * writes a little-endian long to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeLongLE(final long data) {
    this.buffer.writeLongLE(data);
    return this;
  }

  /**
   * writes a short to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeShort(final int data) {
    this.buffer.writeShort(data);
    return this;
  }

  /**
   * writes a little-endian short to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeShortLE(final int data) {
    this.buffer.writeShortLE(data);
    return this;
  }

  /**
   * writes a string.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeString(@NotNull final String data) {
    return this.writeVarInt(data.length())
      .writeBytes(data.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * writes a triad to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeTriad(final int data) {
    this.buffer.writeMedium(data);
    return this;
  }

  /**
   * writes a little-endian triad to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeTriadLE(final int data) {
    this.buffer.writeMediumLE(data);
    return this;
  }

  /**
   * writes a UUID to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public final Packet writeUUID(@NotNull final UUID data) {
    return this.writeLong(data.getMostSignificantBits())
      .writeLong(data.getLeastSignificantBits());
  }

  /**
   * writes an unsigned byte to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   *
   * @throws IllegalArgumentException if data is not within the range of 0-255.
   */
  @NotNull
  public final Packet writeUnsignedByte(final int data) throws IllegalArgumentException {
    Preconditions.checkArgument(data >= 0x00 && data <= 0xFF, "Value must be in between 0-255");
    this.writeByte((byte) data & 0xFF);
    return this;
  }

  /**
   * writes an unsigned int to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   *
   * @throws IllegalArgumentException if data is not in between 0-4294967295
   */
  @NotNull
  public final Packet writeUnsignedInt(final long data) throws IllegalArgumentException {
    Preconditions.checkArgument(data >= 0x00000000 && data <= 0xFFFFFFFFL, "Value must be in between 0-4294967295");
    return this.writeInt((int) data);
  }

  /**
   * writes an unsigned little-endian int to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   *
   * @throws IllegalArgumentException if data is not in between 0-4294967295.
   */
  @NotNull
  public final Packet writeUnsignedIntLE(final long data) throws IllegalArgumentException {
    Preconditions.checkArgument(data >= 0x00000000 && data <= 0xFFFFFFFFL, "Value must be in between 0-4294967295");
    this.buffer.writeIntLE((int) data);
    return this;
  }

  /**
   * writes an unsigned long to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   *
   * @throws IllegalArgumentException if data is bigger than {@value Long#BYTES} bytes  or is negative.
   */
  @NotNull
  public final Packet writeUnsignedLong(@NotNull final BigInteger data) throws IllegalArgumentException {
    final var bytes = data.toByteArray();
    Preconditions.checkArgument(bytes.length <= Long.BYTES, "Value is too big to fit into a long");
    Preconditions.checkArgument(data.longValue() >= 0, "Value cannot be negative");
    for (var index = 0; index < Long.BYTES; index++) {
      this.writeByte(index < bytes.length ? bytes[index] : 0x00);
    }
    return this;
  }

  /**
   * writes an unsigned long to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   *
   * @throws IllegalArgumentException if data is less than 0.
   */
  @NotNull
  public final Packet writeUnsignedLong(final long data) throws IllegalArgumentException {
    return this.writeUnsignedLong(new BigInteger(Long.toString(data)));
  }

  /**
   * writes an unsigned little-endian long to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   *
   * @throws IllegalArgumentException if the size of the data is bigger than {@value Long#BYTES} bytes or is negative.
   */
  @NotNull
  public final Packet writeUnsignedLongLE(@NotNull final BigInteger data) throws IllegalArgumentException {
    final var bytes = data.toByteArray();
    Preconditions.checkArgument(bytes.length <= Long.BYTES, "Value is too big to fit into a long");
    Preconditions.checkArgument(data.longValue() >= 0, "Value cannot be negative");
    for (var index = Long.BYTES - 1; index >= 0; index--) {
      this.writeByte(index < bytes.length ? bytes[index] : 0x00);
    }
    return this;
  }

  /**
   * writes an unsigned little-endian long to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   *
   * @throws IllegalArgumentException if data is less than 0.
   */
  @NotNull
  public final Packet writeUnsignedLongLE(final long data) throws IllegalArgumentException {
    return this.writeUnsignedLongLE(new BigInteger(Long.toString(data)));
  }

  /**
   * writes a unsigned short to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   *
   * @throws IllegalArgumentException if data is not within the range of  0-65535.
   */
  @NotNull
  public final Packet writeUnsignedShort(final int data) throws IllegalArgumentException {
    Preconditions.checkArgument(data >= 0x0000 && data <= 0xFFFF, "Value must be in between 0-65535");
    return this.writeShort((short) data & 0xFFFF);
  }

  /**
   * writes an unsigned little-endian short to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   *
   * @throws IllegalArgumentException if data is not in between 0-65535.
   */
  @NotNull
  public final Packet writeUnsignedShortLE(final int data) throws IllegalArgumentException {
    Preconditions.checkArgument(data >= 0x0000 && data <= 0xFFFF, "Value must be in between 0-65535");
    return this.writeShortLE((short) data & 0xFFFF);
  }

  /**
   * writes an unsigned triad to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   *
   * @throws IllegalArgumentException if data is not in between 0-16777215.
   */
  @NotNull
  public final Packet writeUnsignedTriad(final int data) throws IllegalArgumentException {
    Preconditions.checkArgument(data >= 0x000000 && data <= 0xFFFFFF, "Value must be in between 0-16777215");
    return this.writeTriad(data & 0xFFFFFF);
  }

  /**
   * writes an unsigned little-endian triad to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   *
   * @throws IllegalArgumentException if data is not in between 0-16777215.
   */
  @NotNull
  public final Packet writeUnsignedTriadLE(final int data) throws IllegalArgumentException {
    Preconditions.checkArgument(data >= 0x000000 && data <= 0xFFFFFF, "Value must be in between 0-16777215");
    return this.writeTriadLE(data & 0xFFFFFF);
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
    final var address = this.readBytes(Packet.IPV4_ADDRESS_LENGTH);
    Packet.flip(address);
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
    final var address = this.readBytes(Packet.IPV6_ADDRESS_LENGTH);
    final var scopeId = this.readInt();
    return new InetSocketAddress(Inet6Address.getByAddress(null, address, scopeId), port);
  }

  /**
   * reads a unsigned var int.
   *
   * @return a unsigned var int.
   */
  private int readUnsignedVarInt() {
    var value = 0;
    var i = 0;
    int data;
    while (((data = this.readByte()) & 0x80) != 0) {
      value |= (data & 0x7F) << i;
      i += 7;
      Preconditions.checkArgument(i <= 35, "VarInt too big!");
    }
    return value | data << i;
  }

  /**
   * reads a var int.
   *
   * @return a var int.
   */
  private int readVarInt() {
    var result = 0;
    var indent = 0;
    int data;
    while (((data = this.readByte()) & 0x80) == 0x80) {
      Preconditions.checkArgument(indent < 21, "Too many bytes for a VarInt32.");
      result += (data & 0x7f) << indent;
      indent += 7;
    }
    result += (data & 0x7f) << indent;
    return result;
  }

  /**
   * writes a var int.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  private Packet writeVarInt(final int data) {
    var temp = data;
    while ((temp & 0xFFFFFF80) != 0L) {
      this.writeByte(temp & 0x7F | 0x80);
      temp >>>= 7;
    }
    return this.writeByte(temp & 0x7F);
  }
}
