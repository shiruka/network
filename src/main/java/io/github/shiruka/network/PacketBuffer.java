package io.github.shiruka.network;

import com.google.common.base.Preconditions;
import io.github.shiruka.network.exceptions.PacketException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import lombok.SneakyThrows;
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
   * the magic identifier.
   */
  private static final byte[] MAGIC = new byte[]{(byte) 0x00, (byte) 0xFF, (byte) 0xFF, 0x00, (byte) 0xFE,
    (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFD, (byte) 0xFD, (byte) 0xFD, (byte) 0xFD, (byte) 0x12,
    (byte) 0x34, (byte) 0x56, (byte) 0x78};

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
      case PacketBuffer.IPV4_ADDRESS_LENGTH -> PacketBuffer.IPV4;
      case PacketBuffer.IPV6_ADDRESS_LENGTH -> PacketBuffer.IPV6;
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
   *
   * @return the packet.
   */
  @NotNull
  public PacketBuffer clear() {
    this.buffer.clear();
    return this;
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
   * @return {@code this} for the builder chain.
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
   * writes the specified amount of {@code null} (0x00) bytes to the packet.
   *
   * @param length the amount of bytes to write.
   *
   * @return the packet.
   */
  @NotNull
  public PacketBuffer pad(final int length) {
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
  public PacketBuffer read(final byte[] dest) {
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
   * @throws UnknownHostException if no IP address for the host could be found, the family for an IPv6 address was not
   *   {@value #AF_INET6}, a scope_id was specified for a global IPv6 address, or the address version is an
   *   unknown version.
   */
  @NotNull
  public InetSocketAddress readAddress() throws UnknownHostException {
    final var version = this.readUnsignedByte();
    return switch (version) {
      case PacketBuffer.IPV4 -> this.readAddressIPV4();
      case PacketBuffer.IPV6 -> this.readAddressIPV6();
      default -> throw new UnknownHostException("Unknown protocol IPv%s"
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
   * reads a {@link ConnectionType}.
   * <p>
   * this method will check to make sure if there is at least enough data to read the the connection type magic before
   * reading the data. This is due to the fact that this is meant to be used strictly at the end of packets that can be
   * used to signify the protocol implementation of the sender.
   *
   * @return a {@link ConnectionType}, {@link ConnectionType#VANILLA} if not enough data to read one is present.
   *
   * @throws PacketException if not enough data is present in the packet after the connection type magic or there are
   *   duplicate keys in the metadata.
   */
  @NotNull
  public ConnectionType readConnectionType() throws PacketException {
    if (this.remaining() < ConnectionType.MAGIC.length) {
      return ConnectionType.VANILLA;
    }
    final var magic = this.read(ConnectionType.MAGIC.length);
    if (!Arrays.equals(ConnectionType.MAGIC, magic)) {
      return ConnectionType.VANILLA;
    }
    final var uuid = this.readUUID();
    final var name = this.readString();
    final var language = this.readString();
    final var version = this.readString();
    final var metadata = new HashMap<String, String>();
    final var metadataLength = this.readUnsignedByte();
    for (var index = 0; index < metadataLength; index++) {
      final var key = this.readString();
      final var value = this.readString();
      if (metadata.containsKey(key)) {
        throw new PacketException("Duplicate metadata key \"%s\"", key);
      }
      metadata.put(key, value);
    }
    return new ConnectionType(uuid, name, language, version, metadata);
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
   * reads a magic array and returns whether or not it is valid.
   *
   * @return {@code true} if the magic array was valid, {@code false} otherwise.
   */
  public boolean readMagic() {
    final var magicCheck = this.read(PacketBuffer.MAGIC.length);
    return Arrays.equals(PacketBuffer.MAGIC, magicCheck);
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
    return this.readTriad() & 0xFFFFFF;
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
   * writes the specified bytes to the packet.
   *
   * @param data the data to write.
   *
   * @return the packet.
   */
  @NotNull
  public PacketBuffer write(final byte... data) {
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
  public PacketBuffer write(final int... data) {
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
  public PacketBuffer writeAddress(@NotNull final InetSocketAddress address) throws UnknownHostException {
    Objects.requireNonNull(address.getAddress(), "address");
    return switch (PacketBuffer.getAddressVersion(address)) {
      case PacketBuffer.IPV4 -> this.writeAddressIPV4(address);
      case PacketBuffer.IPV6 -> this.writeAddressIPV6(address);
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
  public PacketBuffer writeAddress(@NotNull final InetAddress host, final int port) throws IllegalArgumentException,
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
  public PacketBuffer writeAddress(@NotNull final String host, final int port) throws IllegalArgumentException,
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
  public PacketBuffer writeAddressIPV4(@NotNull final InetSocketAddress address) {
    final var ipAddress = address.getAddress().getAddress();
    this.writeByte(PacketBuffer.IPV4);
    PacketBuffer.flip(ipAddress);
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
  public PacketBuffer writeAddressIPV6(@NotNull final InetSocketAddress address) {
    final var ipv6Address = (Inet6Address) address.getAddress();
    this.writeByte(PacketBuffer.IPV6);
    this.writeShortLE(PacketBuffer.AF_INET6);
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
  public PacketBuffer writeBoolean(final boolean data) {
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
  public PacketBuffer writeByte(final int data) {
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
  public PacketBuffer writeBytes(final byte[] data) {
    this.buffer.writeBytes(data);
    return this;
  }

  /**
   * writes a {@link ConnectionType} to the packet.
   *
   * @return the packet.
   *
   * @throws RuntimeException if a RakNetException or NullPointerException is caught despite the fact that this method
   *   should never throw an error in the first place.
   */
  @SneakyThrows
  @NotNull
  public PacketBuffer writeConnectionType() {
    return this.writeConnectionType(ConnectionType.RAK_NET);
  }

  /**
   * writes a {@link ConnectionType} to the packet.
   *
   * @param connectionType the connection type, a null value will have {@link ConnectionType#RAK_NET} connection type
   *   be used instead.
   *
   * @return the packet.
   *
   * @throws PacketException if there are too many values in the metadata.
   * @throws NullPointerException if connection type's unique id or language or version is null.
   */
  @NotNull
  public PacketBuffer writeConnectionType(@NotNull final ConnectionType connectionType) throws PacketException {
    Objects.requireNonNull(connectionType.uniqueId(), "unique id");
    Objects.requireNonNull(connectionType.language(), "language");
    Objects.requireNonNull(connectionType.version(), "version");
    this.write(ConnectionType.MAGIC);
    this.writeUUID(connectionType.uniqueId());
    this.writeString(connectionType.name());
    this.writeString(connectionType.language());
    this.writeString(connectionType.version());
    if (connectionType.metadata().size() > ConnectionType.MAX_METADATA_VALUES) {
      throw new PacketException("Too many metadata values!");
    }
    this.writeUnsignedByte(connectionType.metadata().size());
    connectionType.metadata().forEach((key, value) -> {
      this.writeString(key);
      this.writeString(value);
    });
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
  public PacketBuffer writeDouble(final double data) {
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
  public PacketBuffer writeDoubleLE(final double data) {
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
  public PacketBuffer writeFloat(final double data) {
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
  public PacketBuffer writeFloatLE(final double data) {
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
  public PacketBuffer writeInt(final int data) {
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
  public PacketBuffer writeIntLE(final int data) {
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
  public PacketBuffer writeLEAsciiString(@NotNull final AsciiString data) {
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
  public PacketBuffer writeLong(final long data) {
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
  public PacketBuffer writeLongLE(final long data) {
    this.buffer.writeLongLE(data);
    return this;
  }

  /**
   * writes the magic sequence to the packet.
   *
   * @return the packet.
   */
  @NotNull
  public PacketBuffer writeMagic() {
    this.write(PacketBuffer.MAGIC);
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
  public PacketBuffer writeShort(final int data) {
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
  public PacketBuffer writeShortLE(final int data) {
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
  public PacketBuffer writeString(@NotNull final String data) {
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
  public PacketBuffer writeTriad(final int data) {
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
  public PacketBuffer writeTriadLE(final int data) {
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
  public PacketBuffer writeUUID(@NotNull final UUID data) {
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
  public PacketBuffer writeUnsignedByte(final int data) throws IllegalArgumentException {
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
  public PacketBuffer writeUnsignedInt(final long data) throws IllegalArgumentException {
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
  public PacketBuffer writeUnsignedIntLE(final long data) throws IllegalArgumentException {
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
  public PacketBuffer writeUnsignedLong(@NotNull final BigInteger data) throws IllegalArgumentException {
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
  public PacketBuffer writeUnsignedLong(final long data) throws IllegalArgumentException {
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
  public PacketBuffer writeUnsignedLongLE(@NotNull final BigInteger data) throws IllegalArgumentException {
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
  public PacketBuffer writeUnsignedLongLE(final long data) throws IllegalArgumentException {
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
  public PacketBuffer writeUnsignedShort(final int data) throws IllegalArgumentException {
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
  public PacketBuffer writeUnsignedShortLE(final int data) throws IllegalArgumentException {
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
  public PacketBuffer writeUnsignedTriad(final int data) throws IllegalArgumentException {
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
  public PacketBuffer writeUnsignedTriadLE(final int data) throws IllegalArgumentException {
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
    final var address = this.readBytes(PacketBuffer.IPV4_ADDRESS_LENGTH);
    PacketBuffer.flip(address);
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
    final var address = this.readBytes(PacketBuffer.IPV6_ADDRESS_LENGTH);
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
  private PacketBuffer writeVarInt(final int data) {
    var temp = data;
    while ((temp & 0xFFFFFF80) != 0L) {
      this.writeByte(temp & 0x7F | 0x80);
      temp >>>= 7;
    }
    return this.writeByte(temp & 0x7F);
  }
}
