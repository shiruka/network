package io.github.shiruka.network.packets;

import io.github.shiruka.network.Identifier;
import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.options.RakNetMagic;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * a class that represents unconnected pong packets.
 */
@Setter
@Accessors(fluent = true)
public final class UnconnectedPong implements Packet {

  /**
   * the server's identifier.
   */
  @Nullable
  public Identifier identifier;

  /**
   * the magic.
   */
  @Nullable
  public RakNetMagic magic;

  /**
   * the server id.
   */
  @Getter
  public long serverId;

  /**
   * the timestamp sent in the ping packet.
   */
  @Getter
  public long timestamp;

  /**
   * ctor.
   */
  public UnconnectedPong() {}

  /**
   * ctor.
   *
   * @param identifier the identifier.
   * @param magic the magic.
   * @param serverId the server id.
   * @param timestamp the timestamp
   */
  public UnconnectedPong(
    @Nullable final Identifier identifier,
    @Nullable final RakNetMagic magic,
    final long serverId,
    final long timestamp
  ) {
    this.identifier = identifier;
    this.magic = magic;
    this.serverId = serverId;
    this.timestamp = timestamp;
  }

  @Override
  public void decode(@NotNull final PacketBuffer buffer) {
    this.timestamp = buffer.readLong();
    this.serverId = buffer.readLong();
    this.magic = RakNetMagic.from(buffer);
    final var serverInfo = buffer.readString();
    this.identifier = Identifier.findAndCreate(serverInfo);
  }

  @Override
  public void encode(@NotNull final PacketBuffer buffer) {
    buffer.writeLong(this.timestamp);
    buffer.writeLong(this.serverId);
    this.magic().write(buffer);
    final var serverInfo =
      this.identifier().build().getBytes(StandardCharsets.UTF_8);
    buffer.writeShort(serverInfo.length);
    buffer.writeBytes(serverInfo);
  }

  /**
   * obtains the identifier.
   *
   * @return identifier.
   */
  @NotNull
  public Identifier identifier() {
    return Objects.requireNonNull(this.identifier, "identifier");
  }

  /**
   * obtains the magic.
   *
   * @return magic.
   */
  @NotNull
  public RakNetMagic magic() {
    return Objects.requireNonNull(this.magic, "magic");
  }
}
