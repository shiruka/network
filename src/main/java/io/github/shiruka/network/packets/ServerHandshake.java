package io.github.shiruka.network.packets;

import io.github.shiruka.network.PacketBuffer;
import java.net.InetSocketAddress;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * a class that represents server handshake packets.
 */
@Setter
@Accessors(fluent = true)
public final class ServerHandshake extends FramedPacket.Base {

  /**
   * the client address.
   */
  @Nullable
  private InetSocketAddress clientAddress;

  /**
   * the n extra address.
   */
  @Getter
  private int nExtraAddresses;

  /**
   * the timestamp.
   */
  @Getter
  private long timestamp;

  /**
   * ctor.
   */
  public ServerHandshake() {
    super(Reliability.RELIABLE);
  }

  /**
   * ctor.
   *
   * @param clientAddress the client address.
   * @param timestamp the timestamp.
   * @param nExtraAddresses the n extra address.
   */
  public ServerHandshake(
    @NotNull final InetSocketAddress clientAddress,
    final long timestamp,
    final int nExtraAddresses
  ) {
    super(Reliability.RELIABLE);
    this.clientAddress = clientAddress;
    this.timestamp = timestamp;
    this.nExtraAddresses = nExtraAddresses;
  }

  /**
   * ctor.
   *
   * @param clientAddress the client address.
   * @param timestamp the timestamp.
   */
  public ServerHandshake(
    @NotNull final InetSocketAddress clientAddress,
    final long timestamp
  ) {
    this(clientAddress, timestamp, 20);
  }

  /**
   * obtains the client address.
   *
   * @return client address.
   */
  @NotNull
  public InetSocketAddress clientAddress() {
    return Objects.requireNonNull(this.clientAddress, "client address");
  }

  @Override
  public void decode(@NotNull final PacketBuffer buffer) {
    this.clientAddress = buffer.readAddress();
    buffer.readShort();
    for (
      this.nExtraAddresses = 0;
      buffer.remaining() > 16;
      this.nExtraAddresses++
    ) {
      buffer.readAddress();
    }
    this.timestamp = buffer.readLong();
    this.timestamp = buffer.readLong();
  }

  @Override
  public void encode(@NotNull final PacketBuffer buffer) {
    buffer.writeAddress(this.clientAddress());
    buffer.writeShort(0);
    for (var index = 0; index < this.nExtraAddresses; index++) {
      buffer.writeAddress();
    }
    buffer.writeLong(this.timestamp);
    buffer.writeLong(System.currentTimeMillis());
  }
}
