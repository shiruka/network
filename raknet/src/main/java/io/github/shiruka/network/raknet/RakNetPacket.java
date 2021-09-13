package io.github.shiruka.network.raknet;

import com.google.common.base.Preconditions;
import io.github.shiruka.network.PacketSerializer;
import io.github.shiruka.network.raknet.exceptions.RakNetException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * an abstract class that represents rak net packets.
 */
@Accessors(fluent = true)
public class RakNetPacket extends PacketSerializer {

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
   * the id.
   */
  @Getter
  private int id;

  /**
   * ctor.
   *
   * @param buffer the buffer.
   */
  public RakNetPacket(@NotNull final ByteBuf buffer) {
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
  public RakNetPacket(@NotNull final PacketSerializer packet) {
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
  public RakNetPacket(final int id) {
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
  public RakNetPacket(@NotNull final DatagramPacket datagram) {
    this(datagram.content());
  }

  /**
   * ctor.
   *
   * @param data the data.
   */
  public RakNetPacket(final byte @NotNull [] data) {
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
   * sets the buffer and update id.
   *
   * @param buffer the buffer to set.
   * @param updateId the update id to set.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  public final RakNetPacket buffer(@NotNull final ByteBuf buffer, final boolean updateId) {
    super.buffer(buffer);
    if (updateId) {
      this.id = this.readUnsignedByte();
    }
    return this;
  }

  @NotNull
  @Override
  public final RakNetPacket buffer(final @NotNull ByteBuf buffer) {
    return this.buffer(buffer, true);
  }

  @NotNull
  @Override
  public final RakNetPacket flip() {
    return this.flip(true);
  }

  /**
   * sets the buffer and update id.
   *
   * @param packet the packet to set.
   * @param updateId the update id to set.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  public final RakNetPacket buffer(@NotNull final DatagramPacket packet, final boolean updateId) {
    return this.buffer(packet.content(), updateId);
  }

  /**
   * sets the buffer and update id.
   *
   * @param bytes the bytes to set.
   * @param updateId the update id to set.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  public final RakNetPacket buffer(final byte @NotNull [] bytes, final boolean updateId) {
    return this.buffer(Unpooled.copiedBuffer(bytes), updateId);
  }

  /**
   * sets the buffer and update id.
   *
   * @param packet the packet to set.
   * @param updateId the update id to set.
   *
   * @return {@code this} for the builder chain.
   */
  @NotNull
  public final RakNetPacket buffer(@NotNull final PacketSerializer packet, final boolean updateId) {
    return this.buffer(packet.copy(), updateId);
  }

  /**
   * flips the packet.
   *
   * @param updateId {@code true} if ID should be updated, {@code false} otherwise.
   *
   * @return the packet.
   *
   * @throws IndexOutOfBoundsException if updateId is {@code true} and the buffer has less than 1 readable byte.
   * @see #flip()
   */
  @NotNull
  public final RakNetPacket flip(final boolean updateId) throws IndexOutOfBoundsException {
    super.flip();
    if (updateId) {
      this.id = this.readUnsignedByte();
    }
    return this;
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
   * @throws RakNetException if not enough data is present in the packet after the connection type magic or there are
   *   duplicate keys in the metadata.
   */
  @NotNull
  public final ConnectionType readConnectionType() throws RakNetException {
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
        throw new RakNetException("Duplicate metadata key \"%s\"", key);
      }
      metadata.put(key, value);
    }
    return new ConnectionType(uuid, name, language, version, metadata);
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
   * writes a {@link ConnectionType} to the packet.
   *
   * @return the packet.
   *
   * @throws RuntimeException if a RakNetException or NullPointerException is caught despite the fact that this method
   *   should never throw an error in the first place.
   */
  @SneakyThrows
  @NotNull
  public final RakNetPacket writeConnectionType() {
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
   * @throws RakNetException if there are too many values in the metadata.
   * @throws NullPointerException if connection type's unique id or language or version is null.
   */
  @NotNull
  public final RakNetPacket writeConnectionType(@NotNull final ConnectionType connectionType) throws RakNetException {
    Objects.requireNonNull(connectionType.uniqueId(), "unique id");
    Objects.requireNonNull(connectionType.language(), "language");
    Objects.requireNonNull(connectionType.version(), "version");
    this.write(ConnectionType.MAGIC);
    this.writeUUID(connectionType.uniqueId());
    this.writeString(connectionType.name());
    this.writeString(connectionType.language());
    this.writeString(connectionType.version());
    if (connectionType.metadata().size() > ConnectionType.MAX_METADATA_VALUES) {
      throw new RakNetException("Too many metadata values!");
    }
    this.writeUnsignedByte(connectionType.metadata().size());
    connectionType.metadata().forEach((key, value) -> {
      this.writeString(key);
      this.writeString(value);
    });
    return this;
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

  @Override
  public String toString() {
    return "RakNetPacket{id=" + this.id + ", size()=" + this.size() + ", remaining()=" + this.remaining() + "}";
  }
}
