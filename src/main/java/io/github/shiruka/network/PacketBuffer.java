package io.github.shiruka.network;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
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
import org.jetbrains.annotations.NotNull;

/**
 * a record class that contains helper methods to encode and decode buffers.
 *
 * @param buffer the buffer.
 */
public record PacketBuffer(
  @NotNull ByteBuf buffer
) {

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
      case Constants.IPV4_ADDRESS_LENGTH -> Constants.IPV4;
      case Constants.IPV6_ADDRESS_LENGTH -> Constants.IPV6;
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
    return PacketBuffer.getAddressVersion(address.getAddress());
  }

  /**
   * adds component.
   *
   * @param increaseWriterIndex the increase writer index to add.
   * @param buffer the buffer to add.
   */
  public void addComponent(final boolean increaseWriterIndex, @NotNull final PacketBuffer buffer) {
    if (this.buffer instanceof CompositeByteBuf composite) {
      composite.addComponent(increaseWriterIndex, buffer.buffer());
    }
  }

  /**
   * returns the packet as a byte[].
   *
   * @return the packet as a byte[].
   */
  public byte[] array() {
    Preconditions.checkState(!this.buffer.isDirect(),
      "The buffer is a direct buffer!");
    return Arrays.copyOfRange(this.buffer.array(), 0, this.size());
  }

  /**
   * clears the packet's buffer.
   */
  public void clear() {
    this.buffer.clear();
  }

  /**
   * returns a copy of the packet buffer.
   *
   * @return a copy of the packet buffer.
   */
  @NotNull
  public ByteBuf copy() {
    return this.buffer.copy();
  }

  /**
   * flips the packet.
   *
   * @return a new flipped packet.
   */
  @NotNull
  public PacketBuffer flip() {
    final var data = this.buffer.array();
    final var increment = this.buffer.refCnt();
    this.buffer.release(increment);
    final var newBuffer = Unpooled.copiedBuffer(data);
    newBuffer.retain(increment);
    return new PacketBuffer(newBuffer);
  }

  /**
   * checks if the buffer is readable.
   *
   * @return {@code true} if the buffer is readable.
   */
  public boolean isReadable() {
    return this.buffer.isReadable();
  }

  /**
   * writes the specified amount of {@code null} (0x00) bytes to the packet.
   *
   * @param length the amount of bytes to write.
   */
  public void pad(final int length) {
    for (var index = 0; index < length; index++) {
      this.writeByte(0x00);
    }
  }

  /**
   * reads data into the specified byte[].
   *
   * @param dest the byte[] to read the data into.
   */
  public void read(final byte[] dest) {
    for (var index = 0; index < dest.length; index++) {
      dest[index] = this.readByte();
    }
  }

  /**
   * reads the specified amount of bytes.
   *
   * @param length the amount of bytes to read.
   *
   * @return the read bytes.
   */
  public byte[] read(final int length) {
    final var bytes = new byte[length];
    this.read(bytes);
    return bytes;
  }

  /**
   * reads an IPv4/IPv6 address.
   *
   * @return an IPv4/IPv6 address.
   *
   * @throws IllegalArgumentException if no IP address for the host could be found, the family for an IPv6 address was
   *   not {@link Constants#AF_INET6}, a scope_id was specified for a global IPv6 address, or the address version is an
   *   unknown version.
   */
  @NotNull
  public InetSocketAddress readAddress() {
    final var version = this.readUnsignedByte();
    return switch (version) {
      case Constants.IPV4 -> this.readAddressIPV4();
      case Constants.IPV6 -> this.readAddressIPV6();
      default -> throw new IllegalArgumentException("Unknown protocol IPv%s"
        .formatted(version));
    };
  }

  /**
   * reads a boolean, technically a byte.
   *
   * @return a boolean.
   */
  public boolean readBoolean() {
    return this.buffer.readBoolean();
  }

  /**
   * reads a byte.
   *
   * @return a byte.
   */
  public byte readByte() {
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
  public ByteBuf readBytes(final byte[] bytes) {
    return this.buffer.readBytes(bytes);
  }

  /**
   * reads bytes.
   *
   * @param length the length to convert.
   *
   * @return bytes.
   */
  public byte[] readBytes(final int length) {
    final var bytes = new byte[length];
    this.readBytes(bytes);
    return bytes;
  }

  /**
   * reads a char.
   *
   * @return a char.
   */
  public char readChar() {
    return (char) this.buffer.readShort();
  }

  /**
   * reads a little-endian char.
   *
   * @return a little-endian char.
   */
  public char readCharLE() {
    return (char) this.buffer.readShortLE();
  }

  /**
   * reads a double.
   *
   * @return a double.
   */
  public double readDouble() {
    return this.buffer.readDouble();
  }

  /**
   * reads a little-endian double.
   *
   * @return a little-endian double.
   */
  public double readDoubleLE() {
    return this.buffer.readDoubleLE();
  }

  /**
   * reads a float.
   *
   * @return a float.
   */
  public float readFloat() {
    return this.buffer.readFloat();
  }

  /**
   * reads a little-endian float.
   *
   * @return a little-endian float.
   */
  public float readFloatLE() {
    return this.buffer.readFloatLE();
  }

  /**
   * reads an int.
   *
   * @return an int.
   */
  public int readInt() {
    return this.buffer.readInt();
  }

  /**
   * reads a little-endian int.
   *
   * @return a little-endian int.
   */
  public int readIntLE() {
    return this.buffer.readIntLE();
  }

  /**
   * reads ascii string.
   *
   * @return ascii string.
   */
  @NotNull
  public AsciiString readLEAsciiString() {
    final var length = this.readIntLE();
    final var bytes = this.readBytes(length);
    return new AsciiString(bytes);
  }

  /**
   * reads a long.
   *
   * @return a long.
   */
  public long readLong() {
    return this.buffer.readLong();
  }

  /**
   * reads a little-endian long.
   *
   * @return a little-endian long.
   */
  public long readLongLE() {
    return this.buffer.readLongLE();
  }

  /**
   * reads retained slice.
   *
   * @param length the length to read.
   *
   * @return retained slice buffer.
   */
  @NotNull
  public PacketBuffer readRetainedSlice(final int length) {
    return new PacketBuffer(this.buffer.readRetainedSlice(length));
  }

  /**
   * reads a short.
   *
   * @return a short.
   */
  public short readShort() {
    return this.buffer.readShort();
  }

  /**
   * reads a little-endian short.
   *
   * @return a little-endian short.
   */
  public short readShortLE() {
    return this.buffer.readShortLE();
  }

  /**
   * reads slice.
   *
   * @return slice.
   */
  @NotNull
  public ByteBuf readSlice() {
    return this.buffer.readSlice(this.readUnsignedVarInt());
  }

  /**
   * reads a string.
   *
   * @return a string.
   */
  @NotNull
  public String readString() {
    final var length = this.readVarInt();
    final var bytes = this.readBytes(length);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  /**
   * reads a triad.
   *
   * @return a triad.
   */
  public int readTriad() {
    return this.buffer.readMedium();
  }

  /**
   * reads a little-endian triad.
   *
   * @return a little-endian triad.
   */
  public int readTriadLE() {
    return this.buffer.readMediumLE();
  }

  /**
   * reads a UUID.
   *
   * @return a UUID.
   */
  @NotNull
  public UUID readUUID() {
    final var most = this.readLong();
    final var least = this.readLong();
    return new UUID(most, least);
  }

  /**
   * reads an unsigned byte.
   *
   * @return an unsigned byte.
   */
  public short readUnsignedByte() {
    return (short) (this.readByte() & 0xFF);
  }

  /**
   * reads an unsigned int.
   *
   * @return an unsigned int.
   */
  public long readUnsignedInt() {
    return this.readInt() & 0xFFFFFFFFL;
  }

  /**
   * reads an unsigned little-endian int.
   *
   * @return an unsigned little-endian int.
   */
  public long readUnsignedIntLE() {
    return this.readIntLE() & 0xFFFFFFFFL;
  }

  /**
   * reads an unsigned long.
   *
   * @return an unsigned long.
   */
  @NotNull
  public BigInteger readUnsignedLong() {
    final var bytes = this.read(Long.BYTES);
    return new BigInteger(bytes);
  }

  /**
   * reads an unsigned little-endian long.
   *
   * @return an unsigned little-endian long.
   */
  @NotNull
  public BigInteger readUnsignedLongLE() {
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
  public int readUnsignedShort() {
    return this.readShort() & 0xFFFF;
  }

  /**
   * reads an unsigned little-endian short.
   *
   * @return an unsigned little-endian short.
   */
  public int readUnsignedShortLE() {
    return this.readShortLE() & 0xFFFF;
  }

  /**
   * reads an unsigned triad.
   *
   * @return an unsigned triad.
   */
  public int readUnsignedTriad() {
    return this.readTriad() & 0xFFFFFF;
  }

  /**
   * reads an unsigned little-endian triad.
   *
   * @return an unsigned little-endian triad.
   */
  public int readUnsignedTriadLE() {
    return this.buffer.readUnsignedMediumLE();
  }

  /**
   * reads a var int.
   *
   * @return a var int.
   */
  public int readVarInt() {
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
   * gets the readers index.
   *
   * @return readers index.
   */
  public int readerIndex() {
    return this.buffer.readerIndex();
  }

  /**
   * releases the packet's buffer.
   *
   * @return {@code true} if and only if the reference count became 0 and this object has been deallocated, {@code
   *   false} otherwise.
   */
  public boolean release() {
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
  public int remaining() {
    return this.buffer.readableBytes();
  }

  /**
   * retains the buffer.
   *
   * @return retained packet buffer.
   */
  @NotNull
  public PacketBuffer retain() {
    return new PacketBuffer(this.buffer.retain());
  }

  /**
   * duplicates.
   *
   * @return duplicated packet buffer.
   */
  @NotNull
  public PacketBuffer retainedDuplicate() {
    return new PacketBuffer(this.buffer.retainedDuplicate());
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
  public int size() {
    return this.buffer.writerIndex();
  }

  /**
   * skips the specified amount of bytes.
   *
   * @param length the amount of bytes to skip.
   */
  public void skip(final int length) {
    this.buffer.skipBytes(Math.min(length, this.remaining()));
  }

  /**
   * touch to the buffer.
   *
   * @param hint the hint to touch.
   */
  public void touch(@NotNull final Object hint) {
    this.buffer.touch(hint);
  }

  /**
   * gets the unsigned byte.
   *
   * @param length the length
   *
   * @return unsigned byte.
   */
  public short unsignedByte(final int length) {
    return this.buffer.getUnsignedByte(length);
  }

  /**
   * writes the specified bytes to the packet.
   *
   * @param data the data to write.
   */
  public void write(final byte... data) {
    for (final var datum : data) {
      this.writeByte(datum);
    }
  }

  /**
   * writes the specified bytes to the packet.
   * <p>
   * this method is simply a shorthand for the {@link #write(byte...)} method, with all the values being automatically
   * casted back to a byte before being sent to the original {@link #write(byte...)} method.
   *
   * @param data the data to write.
   */
  public void write(final int... data) {
    final var bytes = new byte[data.length];
    for (var index = 0; index < data.length; index++) {
      bytes[index] = (byte) data[index];
    }
    this.write(bytes);
  }

  /**
   * writes an IPv4/IPv6 address to the packet.
   *
   * @throws IllegalArgumentException if no IP address for the host could be found, if a scope_id was specified for a
   *   global IPv6 address, or the length of the address is not either {@link Constants#IPV4_ADDRESS_LENGTH} or
   *   {@link Constants#IPV6_ADDRESS_LENGTH} bytes.
   */
  public void writeAddress() {
    this.writeAddress(Constants.NULL_ADDRESS);
  }

  /**
   * writes an IPv4/IPv6 address to the packet.
   *
   * @param address the address.
   *
   * @throws IllegalArgumentException if no IP address for the host could be found, if a scope_id was specified for a
   *   global IPv6 address, or the length of the address is not either {@link Constants#IPV4_ADDRESS_LENGTH} or
   *   {@link Constants#IPV6_ADDRESS_LENGTH} bytes.
   */
  public void writeAddress(@NotNull final InetSocketAddress address) {
    Objects.requireNonNull(address.getAddress(), "address");
    switch (PacketBuffer.getAddressVersion(address)) {
      case Constants.IPV4 -> this.writeAddressIPV4(address);
      case Constants.IPV6 -> this.writeAddressIPV6(address);
      default -> throw new IllegalArgumentException("Unknown protocol for address with length of %d bytes"
        .formatted(address.getAddress().getAddress().length));
    }
  }

  /**
   * writes an IPv4 address to the packet.
   *
   * @param host the IP address.
   * @param port the port.
   *
   * @throws IllegalArgumentException if the port is not in between 0-65535 or
   *   if no IP address for the host could not be found, or if a scope_id was specified for a global IPv6 address.
   */
  public void writeAddress(@NotNull final InetAddress host, final int port) {
    Preconditions.checkArgument(port >= 0x0000 && port <= 0xFFFF, "Port must be in between 0-65535");
    this.writeAddress(new InetSocketAddress(host, port));
  }

  /**
   * writes an IPv4 address to the packet (IPv6 is not yet supported).
   *
   * @param host the IP address.
   * @param port the port.
   *
   * @throws IllegalArgumentException if the port is not in between 0-65535.
   * @throws UnknownHostException if no IP address for the host could not be found, or if a scope_id was specified for
   *   a global IPv6 address.
   */
  public void writeAddress(@NotNull final String host, final int port) throws IllegalArgumentException,
    UnknownHostException {
    Preconditions.checkArgument(port >= 0x0000 && port <= 0xFFFF, "Port must be in between 0-65535");
    this.writeAddress(InetAddress.getByName(host), port);
  }

  /**
   * writes an IPv4 address to the packet.
   *
   * @param address the address.
   */
  public void writeAddressIPV4(@NotNull final InetSocketAddress address) {
    final var ipAddress = address.getAddress().getAddress();
    this.writeByte(Constants.IPV4);
    PacketBuffer.flip(ipAddress);
    this.writeBytes(ipAddress);
    this.writeShort(address.getPort());
  }

  /**
   * writes an IPv6 address to the packet.
   *
   * @param address the address.
   */
  public void writeAddressIPV6(@NotNull final InetSocketAddress address) {
    final var ipv6Address = (Inet6Address) address.getAddress();
    this.writeByte(Constants.IPV6);
    this.writeShortLE(Constants.AF_INET6);
    this.writeShort(address.getPort());
    this.writeInt(0);
    this.write(ipv6Address.getAddress());
    this.writeInt(ipv6Address.getScopeId());
  }

  /**
   * writes a boolean to the packet.
   *
   * @param data the data to write.
   */
  public void writeBoolean(final boolean data) {
    this.buffer.writeBoolean(data);
  }

  /**
   * writes a byte to the packet.
   *
   * @param data the data to write.
   */
  public void writeByte(final int data) {
    this.buffer.writeByte((byte) data);
  }

  /**
   * writes bytes.
   *
   * @param data the data to write.
   */
  public void writeBytes(final byte[] data) {
    this.buffer.writeBytes(data);
  }

  /**
   * writes bytes.
   *
   * @param data the data to write.
   * @param readerIndex the reader index to write.
   * @param readableBytes the readable bytes to write.
   */
  public void writeBytes(@NotNull final PacketBuffer data, final int readerIndex, final int readableBytes) {
    this.buffer.writeBytes(data.buffer(), readerIndex, readableBytes);
  }

  /**
   * writes a double to the packet.
   *
   * @param data the data to write.
   */
  public void writeDouble(final double data) {
    this.buffer.writeDouble(data);
  }

  /**
   * writes a double to the packet.
   *
   * @param data the data to write.
   */
  public void writeDoubleLE(final double data) {
    this.buffer.writeDoubleLE(data);
  }

  /**
   * writes a float to the packet.
   *
   * @param data the data to write.
   */
  public void writeFloat(final double data) {
    this.buffer.writeFloat((float) data);
  }

  /**
   * writes a little-endian float to the packet.
   *
   * @param data the data to write.
   */
  public void writeFloatLE(final double data) {
    this.buffer.writeFloatLE((float) data);
  }

  /**
   * writes an int to the packet.
   *
   * @param data the data to write.
   */
  public void writeInt(final int data) {
    this.buffer.writeInt(data);
  }

  /**
   * writes a little-endian int to the packet.
   *
   * @param data the data to write.
   */
  public void writeIntLE(final int data) {
    this.buffer.writeIntLE(data);
  }

  /**
   * writes ascii string.
   *
   * @param data the data to write.
   */
  public void writeLEAsciiString(@NotNull final AsciiString data) {
    this.writeIntLE(data.length());
    this.writeBytes(data.array());
  }

  /**
   * writes a long to the packet.
   *
   * @param data the data to write.
   */
  public void writeLong(final long data) {
    this.buffer.writeLong(data);
  }

  /**
   * writes a little-endian long to the packet.
   *
   * @param data the data to write.
   */
  public void writeLongLE(final long data) {
    this.buffer.writeLongLE(data);
  }

  /**
   * writes a short to the packet.
   *
   * @param data the data to write.
   */
  public void writeShort(final int data) {
    this.buffer.writeShort(data);
  }

  /**
   * writes a little-endian short to the packet.
   *
   * @param data the data to write.
   */
  public void writeShortLE(final int data) {
    this.buffer.writeShortLE(data);
  }

  /**
   * writes a string.
   *
   * @param data the data to write.
   */
  public void writeString(@NotNull final String data) {
    this.writeVarInt(data.length());
    this.writeBytes(data.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * writes a triad to the packet.
   *
   * @param data the data to write.
   */
  public void writeTriad(final int data) {
    this.buffer.writeMedium(data);
  }

  /**
   * writes a little-endian triad to the packet.
   *
   * @param data the data to write.
   */
  public void writeTriadLE(final int data) {
    this.buffer.writeMediumLE(data);
  }

  /**
   * writes a UUID to the packet.
   *
   * @param data the data to write.
   */
  public void writeUUID(@NotNull final UUID data) {
    this.writeLong(data.getMostSignificantBits());
    this.writeLong(data.getLeastSignificantBits());
  }

  /**
   * writes an unsigned byte to the packet.
   *
   * @param data the data to write.
   *
   * @throws IllegalArgumentException if data is not within the range of 0-255.
   */
  public void writeUnsignedByte(final int data) throws IllegalArgumentException {
    Preconditions.checkArgument(data >= 0x00 && data <= 0xFF, "Value must be in between 0-255");
    this.writeByte((byte) data & 0xFF);
  }

  /**
   * writes an unsigned int to the packet.
   *
   * @param data the data to write.
   *
   * @throws IllegalArgumentException if data is not in between 0-4294967295
   */
  public void writeUnsignedInt(final long data) throws IllegalArgumentException {
    Preconditions.checkArgument(data >= 0x00000000 && data <= 0xFFFFFFFFL, "Value must be in between 0-4294967295");
    this.writeInt((int) data);
  }

  /**
   * writes an unsigned little-endian int to the packet.
   *
   * @param data the data to write.
   *
   * @throws IllegalArgumentException if data is not in between 0-4294967295.
   */
  public void writeUnsignedIntLE(final long data) throws IllegalArgumentException {
    Preconditions.checkArgument(data >= 0x00000000 && data <= 0xFFFFFFFFL, "Value must be in between 0-4294967295");
    this.buffer.writeIntLE((int) data);
  }

  /**
   * writes an unsigned long to the packet.
   *
   * @param data the data to write.
   *
   * @throws IllegalArgumentException if data is bigger than {@link Long#BYTES} bytes  or is negative.
   */
  public void writeUnsignedLong(@NotNull final BigInteger data) throws IllegalArgumentException {
    final var bytes = data.toByteArray();
    Preconditions.checkArgument(bytes.length <= Long.BYTES, "Value is too big to fit into a long");
    Preconditions.checkArgument(data.longValue() >= 0, "Value cannot be negative");
    for (var index = 0; index < Long.BYTES; index++) {
      this.writeByte(index < bytes.length ? bytes[index] : 0x00);
    }
  }

  /**
   * writes an unsigned long to the packet.
   *
   * @param data the data to write.
   *
   * @throws IllegalArgumentException if data is less than 0.
   */
  public void writeUnsignedLong(final long data) throws IllegalArgumentException {
    this.writeUnsignedLong(new BigInteger(Long.toString(data)));
  }

  /**
   * writes an unsigned little-endian long to the packet.
   *
   * @param data the data to write.
   *
   * @throws IllegalArgumentException if the size of the data is bigger than {@link Long#BYTES} bytes or is negative.
   */
  public void writeUnsignedLongLE(@NotNull final BigInteger data) throws IllegalArgumentException {
    final var bytes = data.toByteArray();
    Preconditions.checkArgument(bytes.length <= Long.BYTES, "Value is too big to fit into a long");
    Preconditions.checkArgument(data.longValue() >= 0, "Value cannot be negative");
    for (var index = Long.BYTES - 1; index >= 0; index--) {
      this.writeByte(index < bytes.length ? bytes[index] : 0x00);
    }
  }

  /**
   * writes an unsigned little-endian long to the packet.
   *
   * @param data the data to write.
   *
   * @throws IllegalArgumentException if data is less than 0.
   */
  public void writeUnsignedLongLE(final long data) throws IllegalArgumentException {
    this.writeUnsignedLongLE(new BigInteger(Long.toString(data)));
  }

  /**
   * writes a unsigned short to the packet.
   *
   * @param data the data to write.
   *
   * @throws IllegalArgumentException if data is not within the range of  0-65535.
   */
  public void writeUnsignedShort(final int data) throws IllegalArgumentException {
    Preconditions.checkArgument(data >= 0x0000 && data <= 0xFFFF, "Value must be in between 0-65535");
    this.writeShort((short) data & 0xFFFF);
  }

  /**
   * writes an unsigned little-endian short to the packet.
   *
   * @param data the data to write.
   *
   * @throws IllegalArgumentException if data is not in between 0-65535.
   */
  public void writeUnsignedShortLE(final int data) throws IllegalArgumentException {
    Preconditions.checkArgument(data >= 0x0000 && data <= 0xFFFF, "Value must be in between 0-65535");
    this.writeShortLE((short) data & 0xFFFF);
  }

  /**
   * writes an unsigned triad to the packet.
   *
   * @param data the data to write.
   *
   * @throws IllegalArgumentException if data is not in between 0-16777215.
   */
  public void writeUnsignedTriad(final int data) throws IllegalArgumentException {
    Preconditions.checkArgument(data >= 0x000000 && data <= 0xFFFFFF, "Value must be in between 0-16777215");
    this.writeTriad(data & 0xFFFFFF);
  }

  /**
   * writes an unsigned little-endian triad to the packet.
   *
   * @param data the data to write.
   *
   * @throws IllegalArgumentException if data is not in between 0-16777215.
   */
  public void writeUnsignedTriadLE(final int data) throws IllegalArgumentException {
    Preconditions.checkArgument(data >= 0x000000 && data <= 0xFFFFFF, "Value must be in between 0-16777215");
    this.writeTriadLE(data & 0xFFFFFF);
  }

  /**
   * writes a var int.
   *
   * @param data the data to write.
   */
  public void writeVarInt(final int data) {
    var temp = data;
    while ((temp & 0xFFFFFF80) != 0L) {
      this.writeByte(temp & 0x7F | 0x80);
      temp >>>= 7;
    }
    this.writeByte(temp & 0x7F);
  }

  /**
   * writes zero bytes.
   *
   * @param length the length to write.
   */
  public void writeZero(final int length) {
    this.buffer.writeZero(length);
  }

  /**
   * reads an IPv4 address.
   *
   * @return an IPv4 address.
   *
   * @throws IllegalArgumentException if IP address is of illegal length.
   */
  @NotNull
  private InetSocketAddress readAddressIPV4() {
    final var address = this.readBytes(Constants.IPV4_ADDRESS_LENGTH);
    PacketBuffer.flip(address);
    final var port = this.readUnsignedShort();
    try {
      return new InetSocketAddress(InetAddress.getByAddress(address), port);
    } catch (final Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * reads an IPv6 address.
   *
   * @return an IPv6 address.
   *
   * @throws IllegalArgumentException if IP address is of illegal length.
   */
  @NotNull
  private InetSocketAddress readAddressIPV6() {
    this.readShortLE();
    final var port = this.readUnsignedShort();
    this.readInt();
    final var address = this.readBytes(Constants.IPV6_ADDRESS_LENGTH);
    final var scopeId = this.readInt();
    try {
      return new InetSocketAddress(Inet6Address.getByAddress(null, address, scopeId), port);
    } catch (final Exception e) {
      throw new IllegalArgumentException(e);
    }
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
}
