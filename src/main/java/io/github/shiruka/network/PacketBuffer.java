package io.github.shiruka.network;

import com.google.common.base.Preconditions;
import io.github.shiruka.api.common.vectors.Vector2f;
import io.github.shiruka.api.common.vectors.Vector3f;
import io.github.shiruka.api.common.vectors.Vector3i;
import io.github.shiruka.api.nbt.CompoundTag;
import io.github.shiruka.api.nbt.Tag;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * a class that contains helper methods to encode and decode buffers.
 */
@RequiredArgsConstructor
@Accessors(fluent = true)
public class PacketBuffer {

  /**
   * the buffer.
   */
  @Getter
  @NotNull
  private final ByteBuf buffer;

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
  public final void addComponent(final boolean increaseWriterIndex, @NotNull final PacketBuffer buffer) {
    if (this.buffer instanceof CompositeByteBuf composite) {
      composite.addComponent(increaseWriterIndex, buffer.buffer());
    }
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
   */
  public final void clear() {
    this.buffer.clear();
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
   * decodes the var long.
   *
   * @return var long
   */
  public final long decodeVarLong() {
    var result = 0;
    for (var shift = 0; shift < 64; shift += 7) {
      final var b = this.buffer.readByte();
      result |= (b & 0x7FL) << shift;
      if ((b & 0x80) == 0) {
        return result;
      }
    }
    throw new ArithmeticException("VarInt was too large!");
  }

  /**
   * encodes the value.
   *
   * @param value the value to encode.
   */
  public final void encodeVarLong(final long value) {
    var tempValue = value;
    while (true) {
      if ((tempValue & ~0x7FL) == 0) {
        this.buffer.writeByte((int) tempValue);
        return;
      } else {
        this.buffer.writeByte((byte) ((int) tempValue & 0x7F | 0x80));
        tempValue >>>= 7;
      }
    }
  }

  /**
   * flips the packet.
   *
   * @return a new flipped packet.
   */
  @NotNull
  public final PacketBuffer flip() {
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
  public final boolean isReadable() {
    return this.buffer.isReadable();
  }

  /**
   * writes the specified amount of {@code null} (0x00) bytes to the packet.
   *
   * @param length the amount of bytes to write.
   */
  public final void pad(final int length) {
    for (var index = 0; index < length; index++) {
      this.writeByte(0x00);
    }
  }

  /**
   * reads data into the specified byte[].
   *
   * @param dest the byte[] to read the data into.
   */
  public final void read(final byte[] dest) {
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
   * @throws IllegalArgumentException if no IP address for the host could be found, the family for an IPv6 address was
   *   not {@link Constants#AF_INET6}, a scope_id was specified for a global IPv6 address, or the address version is an
   *   unknown version.
   */
  @NotNull
  public final InetSocketAddress readAddress() {
    final var version = this.readUnsignedByte();
    return switch (version) {
      case Constants.IPV4 -> this.readAddressIPV4();
      case Constants.IPV6 -> this.readAddressIPV6();
      default -> throw new IllegalArgumentException("Unknown protocol IPv%s"
        .formatted(version));
    };
  }

  /**
   * reads the string array.
   *
   * @param lengthSupplier the length supplier to read.
   * @param valueSupplier the value supplier to read.
   * @param <T> type of the array element.
   *
   * @return string list.
   */
  @NotNull
  public final <T> ObjectList<T> readArray(@NotNull final Supplier<Number> lengthSupplier,
                                           @NotNull final Supplier<T> valueSupplier) {
    final var list = new ObjectArrayList<T>();
    final var length = lengthSupplier.get().longValue();
    for (var index = 0; index < length; index++) {
      list.add(valueSupplier.get());
    }
    return list;
  }

  /**
   * reads the array shor le.
   *
   * @param valueSupplier the value supplier to read.
   * @param <T> type of the array.
   *
   * @return array list.
   */
  @NotNull
  public final <T> ObjectList<T> readArrayShortLE(@NotNull final Supplier<T> valueSupplier) {
    return this.readArray(this::readUnsignedShortLE, valueSupplier);
  }

  /**
   * reads the string array.
   *
   * @param valueSupplier the value supplier to read.
   * @param <T> type of the array.
   *
   * @return array list.
   */
  @NotNull
  public final <T> ObjectList<T> readArrayUnsignedInt(@NotNull final Supplier<T> valueSupplier) {
    return this.readArray(this::readUnsignedInt, valueSupplier);
  }

  /**
   * reads the string array.
   *
   * @return string list.
   */
  @NotNull
  public final ObjectList<String> readArrayUnsignedIntWithString() {
    return this.readArrayUnsignedInt(this::readString);
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
   * reads the byte angle.
   *
   * @return byte angle.
   */
  public final float readByteAngle() {
    return this.readByte() * (360f / 256f);
  }

  /**
   * reads the byte array.
   *
   * @return byte array.
   */
  public final byte[] readByteArray() {
    final var length = this.readUnsignedVarInt();
    Preconditions.checkArgument(this.buffer().isReadable(length),
      "Tried to read %s bytes but only has %s readable", length, this.remaining());
    final var bytes = new byte[length];
    this.readBytes(bytes);
    return bytes;
  }

  /**
   * reads the byte rotation.
   *
   * @return byte rotation.
   */
  @NotNull
  public final Vector3f readByteRotation() {
    final var pitch = this.readByteAngle();
    final var yaw = this.readByteAngle();
    final var roll = this.readByteAngle();
    return Vector3f.of(pitch, yaw, roll);
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
   * reads the compound tag.
   *
   * @return compound tag
   */
  @SneakyThrows
  @NotNull
  public final CompoundTag readCompoundTag() {
    try (final var reader = Tag.createNetworkReader(new ByteBufInputStream(this.buffer()))) {
      return reader.readCompoundTag();
    }
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
   * reads retained slice.
   *
   * @param length the length to read.
   *
   * @return retained slice buffer.
   */
  @NotNull
  public final PacketBuffer readRetainedSlice(final int length) {
    return new PacketBuffer(this.buffer.readRetainedSlice(length));
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
    return this.buffer.readUnsignedMediumLE();
  }

  /**
   * reads the unsigned var int.
   *
   * @return unsigned var int.
   */
  public final int readUnsignedVarInt() {
    return (int) this.decodeVarLong();
  }

  /**
   * reads the unsigned var long.
   *
   * @return unsigned var long.
   */
  public final long readUnsignedVarLong() {
    return this.decodeVarLong();
  }

  /**
   * reads the var int.
   *
   * @return var int.
   */
  public final int readVarInt() {
    final var decode = (int) this.decodeVarLong();
    return decode >>> 1 ^ -(decode & 1);
  }

  /**
   * reads the var long.
   *
   * @return var long.
   */
  public final long readVarLong() {
    final var decode = this.decodeVarLong();
    return decode >>> 1 ^ -(decode & 1);
  }

  /**
   * reads the vector 2f.
   *
   * @return vector 2f.
   */
  @NotNull
  public final Vector2f readVector2f() {
    final var x = this.readFloatLE();
    final var y = this.readFloatLE();
    return Vector2f.of(x, y);
  }

  /**
   * reads vector 3f.
   *
   * @return vector 3f.
   */
  @NotNull
  public final Vector3f readVector3f() {
    final var x = this.readFloatLE();
    final var y = this.readFloatLE();
    final var z = this.readFloatLE();
    return Vector3f.of(x, y, z);
  }

  /**
   * reads the vector 3i.
   *
   * @return vector 3i.
   */
  @NotNull
  public final Vector3i readVector3i() {
    final var x = this.readVarInt();
    final var y = this.readUnsignedVarInt();
    final var z = this.readVarInt();
    return Vector3i.of(x, y, z);
  }

  /**
   * gets the readers index.
   *
   * @return readers index.
   */
  public final int readerIndex() {
    return this.buffer.readerIndex();
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
   * retains the buffer.
   *
   * @return retained packet buffer.
   */
  @NotNull
  public final PacketBuffer retain() {
    return new PacketBuffer(this.buffer.retain());
  }

  /**
   * duplicates.
   *
   * @return duplicated packet buffer.
   */
  @NotNull
  public final PacketBuffer retainedDuplicate() {
    return new PacketBuffer(this.buffer.retainedDuplicate());
  }

  /**
   * sets short le.
   *
   * @param index the index to set.
   * @param value the value to set.
   */
  public final void setShortLE(final int index, final int value) {
    this.buffer.setShortLE(index, value);
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
   * touch to the buffer.
   *
   * @param hint the hint to touch.
   */
  public final void touch(@NotNull final Object hint) {
    this.buffer.touch(hint);
  }

  /**
   * gets the unsigned byte.
   *
   * @param length the length
   *
   * @return unsigned byte.
   */
  public final short unsignedByte(final int length) {
    return this.buffer.getUnsignedByte(length);
  }

  /**
   * writes the specified bytes to the packet.
   *
   * @param data the data to write.
   */
  public final void write(final byte... data) {
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
  public final void write(final int... data) {
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
  public final void writeAddress() {
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
  public final void writeAddress(@NotNull final InetSocketAddress address) {
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
  public final void writeAddress(@NotNull final InetAddress host, final int port) {
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
  public final void writeAddress(@NotNull final String host, final int port) throws IllegalArgumentException,
    UnknownHostException {
    Preconditions.checkArgument(port >= 0x0000 && port <= 0xFFFF, "Port must be in between 0-65535");
    this.writeAddress(InetAddress.getByName(host), port);
  }

  /**
   * writes an IPv4 address to the packet.
   *
   * @param address the address.
   */
  public final void writeAddressIPV4(@NotNull final InetSocketAddress address) {
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
  public final void writeAddressIPV6(@NotNull final InetSocketAddress address) {
    final var ipv6Address = (Inet6Address) address.getAddress();
    this.writeByte(Constants.IPV6);
    this.writeShortLE(Constants.AF_INET6);
    this.writeShort(address.getPort());
    this.writeInt(0);
    this.write(ipv6Address.getAddress());
    this.writeInt(ipv6Address.getScopeId());
  }

  /**
   * writes the array.
   *
   * @param array the array to write.
   * @param lengthWriter the length writer to write.
   * @param valueWriter the value writer to write.
   * @param <T> type of the array.
   */
  public final <T> void writeArray(@NotNull final Collection<T> array, @NotNull final Consumer<Number> lengthWriter,
                                   @NotNull final Consumer<T> valueWriter) {
    lengthWriter.accept(array.size());
    for (final var t : array) {
      valueWriter.accept(t);
    }
  }

  /**
   * writes the array.
   *
   * @param array the array to write.
   * @param lengthWriter the length writer to write.
   * @param valueWriter the value writer to write.
   * @param <T> type of the array.
   */
  public final <T> void writeArray(@NotNull final T[] array, @NotNull final Consumer<Number> lengthWriter,
                                   @NotNull final Consumer<T> valueWriter) {
    lengthWriter.accept(array.length);
    for (final var t : array) {
      valueWriter.accept(t);
    }
  }

  /**
   * writes the array.
   *
   * @param array the array to write.
   * @param valueWriter the value writer to write.
   * @param <T> type of the array.
   */
  public final <T> void writeArrayShortLE(@NotNull final Collection<T> array, @NotNull final Consumer<T> valueWriter) {
    this.writeArray(array, i -> this.writeShortLE(i.intValue()), valueWriter);
  }

  /**
   * writes the array.
   *
   * @param array the array to write.
   * @param valueWriter the value writer to write.
   * @param <T> type of the array.
   */
  public final <T> void writeArrayUnsignedInt(@NotNull final T[] array, @NotNull final Consumer<T> valueWriter) {
    this.writeArray(array, i -> this.writeUnsignedInt(i.longValue()), valueWriter);
  }

  /**
   * writes the array.
   *
   * @param array the array to write.
   * @param valueWriter the value writer to write.
   * @param <T> type of the array.
   */
  public final <T> void writeArrayUnsignedInt(@NotNull final Collection<T> array,
                                              @NotNull final Consumer<T> valueWriter) {
    this.writeArray(array, i -> this.writeUnsignedInt(i.longValue()), valueWriter);
  }

  /**
   * writes a boolean to the packet.
   *
   * @param data the data to write.
   */
  public final void writeBoolean(final boolean data) {
    this.buffer.writeBoolean(data);
  }

  /**
   * writes a byte to the packet.
   *
   * @param data the data to write.
   */
  public final void writeByte(final int data) {
    this.buffer.writeByte((byte) data);
  }

  /**
   * writes the byte angle.
   *
   * @param angle the angle to write.
   */
  public final void writeByteAngle(final float angle) {
    this.writeByte((byte) (angle / (360f / 256f)));
  }

  /**
   * writes byte array.
   *
   * @param bytes the bytes to write.
   */
  public final void writeByteArray(final byte[] bytes) {
    this.writeUnsignedVarInt(bytes.length);
    this.writeBytes(bytes);
  }

  /**
   * writes the byte rotation.
   *
   * @param rotation the rotation to write.
   */
  public final void writeByteRotation(@NotNull final Vector3f rotation) {
    this.writeByteAngle(rotation.x());
    this.writeByteAngle(rotation.y());
    this.writeByteAngle(rotation.z());
  }

  /**
   * writes bytes.
   *
   * @param data the data to write.
   */
  public final void writeBytes(final byte[] data) {
    this.buffer.writeBytes(data);
  }

  /**
   * writes bytes.
   *
   * @param data the data to write.
   * @param readerIndex the reader index to write.
   * @param readableBytes the readable bytes to write.
   */
  public final void writeBytes(@NotNull final PacketBuffer data, final int readerIndex, final int readableBytes) {
    this.buffer.writeBytes(data.buffer(), readerIndex, readableBytes);
  }

  /**
   * writes char sequence.
   *
   * @param sequence the sequence to write.
   * @param charset the charset to write.
   *
   * @return the written number of bytes.
   */
  public final int writeCharSequence(@NotNull final CharSequence sequence, @NotNull final Charset charset) {
    return this.buffer.writeCharSequence(sequence, charset);
  }

  /**
   * writes the compound tag.
   *
   * @param tag the tag to write.
   */
  @SneakyThrows
  public final void writeCompoundTag(@NotNull final CompoundTag tag) {
    try (final var writer = Tag.createNetworkWriter(new ByteBufOutputStream(this.buffer()))) {
      writer.writeCompoundTag(tag);
    }
  }

  /**
   * writes a double to the packet.
   *
   * @param data the data to write.
   */
  public final void writeDouble(final double data) {
    this.buffer.writeDouble(data);
  }

  /**
   * writes a double to the packet.
   *
   * @param data the data to write.
   */
  public final void writeDoubleLE(final double data) {
    this.buffer.writeDoubleLE(data);
  }

  /**
   * writes a float to the packet.
   *
   * @param data the data to write.
   */
  public final void writeFloat(final double data) {
    this.buffer.writeFloat((float) data);
  }

  /**
   * writes a little-endian float to the packet.
   *
   * @param data the data to write.
   */
  public final void writeFloatLE(final double data) {
    this.buffer.writeFloatLE((float) data);
  }

  /**
   * writes an int to the packet.
   *
   * @param data the data to write.
   */
  public final void writeInt(final int data) {
    this.buffer.writeInt(data);
  }

  /**
   * writes a little-endian int to the packet.
   *
   * @param data the data to write.
   */
  public final void writeIntLE(final int data) {
    this.buffer.writeIntLE(data);
  }

  /**
   * writes ascii string.
   *
   * @param data the data to write.
   */
  public final void writeLEAsciiString(@NotNull final AsciiString data) {
    this.writeIntLE(data.length());
    this.writeBytes(data.array());
  }

  /**
   * writes a long to the packet.
   *
   * @param data the data to write.
   */
  public final void writeLong(final long data) {
    this.buffer.writeLong(data);
  }

  /**
   * writes a little-endian long to the packet.
   *
   * @param data the data to write.
   */
  public final void writeLongLE(final long data) {
    this.buffer.writeLongLE(data);
  }

  /**
   * writes a short to the packet.
   *
   * @param data the data to write.
   */
  public final void writeShort(final int data) {
    this.buffer.writeShort(data);
  }

  /**
   * writes a little-endian short to the packet.
   *
   * @param data the data to write.
   */
  public final void writeShortLE(final int data) {
    this.buffer.writeShortLE(data);
  }

  /**
   * writes a string.
   *
   * @param data the data to write.
   */
  public final void writeString(@NotNull final String data) {
    this.writeVarInt(data.length());
    this.writeBytes(data.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * writes a triad to the packet.
   *
   * @param data the data to write.
   */
  public final void writeTriad(final int data) {
    this.buffer.writeMedium(data);
  }

  /**
   * writes a little-endian triad to the packet.
   *
   * @param data the data to write.
   */
  public final void writeTriadLE(final int data) {
    this.buffer.writeMediumLE(data);
  }

  /**
   * writes a UUID to the packet.
   *
   * @param data the data to write.
   */
  public final void writeUUID(@NotNull final UUID data) {
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
  public final void writeUnsignedByte(final int data) throws IllegalArgumentException {
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
  public final void writeUnsignedInt(final long data) throws IllegalArgumentException {
    Preconditions.checkArgument(data >= 0 && data <= 4294967295L, "Value must be in between 0-4294967295");
    this.writeInt((int) data);
  }

  /**
   * writes an unsigned little-endian int to the packet.
   *
   * @param data the data to write.
   *
   * @throws IllegalArgumentException if data is not in between 0-4294967295.
   */
  public final void writeUnsignedIntLE(final long data) throws IllegalArgumentException {
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
  public final void writeUnsignedLong(@NotNull final BigInteger data) throws IllegalArgumentException {
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
  public final void writeUnsignedLong(final long data) throws IllegalArgumentException {
    this.writeUnsignedLong(new BigInteger(Long.toString(data)));
  }

  /**
   * writes an unsigned little-endian long to the packet.
   *
   * @param data the data to write.
   *
   * @throws IllegalArgumentException if the size of the data is bigger than {@link Long#BYTES} bytes or is negative.
   */
  public final void writeUnsignedLongLE(@NotNull final BigInteger data) throws IllegalArgumentException {
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
  public final void writeUnsignedLongLE(final long data) throws IllegalArgumentException {
    this.writeUnsignedLongLE(new BigInteger(Long.toString(data)));
  }

  /**
   * writes a unsigned short to the packet.
   *
   * @param data the data to write.
   *
   * @throws IllegalArgumentException if data is not within the range of  0-65535.
   */
  public final void writeUnsignedShort(final int data) throws IllegalArgumentException {
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
  public final void writeUnsignedShortLE(final int data) throws IllegalArgumentException {
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
  public final void writeUnsignedTriad(final int data) throws IllegalArgumentException {
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
  public final void writeUnsignedTriadLE(final int data) throws IllegalArgumentException {
    Preconditions.checkArgument(data >= 0x000000 && data <= 0xFFFFFF, "Value must be in between 0-16777215");
    this.writeTriadLE(data & 0xFFFFFF);
  }

  /**
   * writes the unsigned var int.
   *
   * @param value the value to write.
   */
  public final void writeUnsignedVarInt(final int value) {
    this.encodeVarLong(value & 0xFFFFFFFFL);
  }

  /**
   * writes the var unsigned var long.
   *
   * @param value the value to write.
   */
  public final void writeUnsignedVarLong(final long value) {
    this.encodeVarLong(value);
  }

  /**
   * writes the var int.
   *
   * @param value the value to write.
   */
  public final void writeVarInt(final int value) {
    this.encodeVarLong(((long) value << 1 ^ value >> 31) & 0xFFFFFFFFL);
  }

  /**
   * writes the var long.
   *
   * @param value the value to write.
   */
  public final void writeVarLong(final long value) {
    this.encodeVarLong(value << 1 ^ value >> 63);
  }

  /**
   * writes the vector 2f.
   *
   * @param vector the vector to write.
   */
  public final void writeVector2f(@NotNull final Vector2f vector) {
    this.writeFloatLE(vector.x());
    this.writeFloatLE(vector.y());
  }

  /**
   * writes the vector 3f.
   *
   * @param vector the vector to write.
   */
  public final void writeVector3f(@NotNull final Vector3f vector) {
    this.writeFloatLE(vector.x());
    this.writeFloatLE(vector.y());
    this.writeFloatLE(vector.z());
  }

  /**
   * writes the vector 31.
   *
   * @param vector the vector to write.
   */
  public final void writeVector3i(@NotNull final Vector3i vector) {
    this.writeVarInt(vector.x());
    this.writeUnsignedVarInt(vector.y());
    this.writeVarInt(vector.z());
  }

  /**
   * writes zero bytes.
   *
   * @param length the length to write.
   */
  public final void writeZero(final int length) {
    this.buffer.writeZero(length);
  }

  /**
   * reads the char sequence.
   *
   * @param length the length to read.
   * @param charset the charset to read.
   *
   * @return the sequence.
   */
  @NotNull
  public CharSequence readCharSequence(final int length, final Charset charset) {
    return this.buffer.readCharSequence(length, charset);
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
}
