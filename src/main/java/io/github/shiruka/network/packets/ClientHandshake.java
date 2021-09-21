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
 * a class that represents client handshake packets.
 */
@Setter
@Accessors(fluent = true)
public final class ClientHandshake extends FramedPacket.Base {

  /**
   * the address.
   */
  @Nullable
  private InetSocketAddress address;

  /**
   * the n extra addresses.
   */
  @Getter
  private int nExtraAddresses;

  /**
   * the pong timestamp.
   */
  @Getter
  private long pongTimestamp;

  /**
   * the timestamp.
   */
  @Getter
  private long timestamp;

  /**
   * ctor.
   *
   * @param pongTimestamp the pong timestamp.
   * @param timestamp the timestamp.
   * @param address the address.
   * @param nExtraAddresses the n extra addresses.
   */
  public ClientHandshake(final long pongTimestamp, final long timestamp, @NotNull final InetSocketAddress address,
                         final int nExtraAddresses) {
    super(Reliability.RELIABLE_ORDERED);
    this.pongTimestamp = pongTimestamp;
    this.timestamp = timestamp;
    this.address = address;
    this.nExtraAddresses = nExtraAddresses;
  }

  /**
   * ctor.
   *
   * @param pongTimestamp the pong timestamp.
   * @param address the address.
   * @param nExtraAddresses the n extra addresses.
   */
  public ClientHandshake(final long pongTimestamp, @NotNull final InetSocketAddress address, final int nExtraAddresses) {
    this(pongTimestamp, System.nanoTime(), address, nExtraAddresses);
  }

  /**
   * ctor.
   */
  public ClientHandshake() {
    super(Reliability.RELIABLE_ORDERED);
  }

  /**
   * obtains the address.
   *
   * @return address.
   */
  @NotNull
  public InetSocketAddress address() {
    return Objects.requireNonNull(this.address, "address");
  }

  @Override
  public void decode(@NotNull final PacketBuffer buffer) {
    this.address = buffer.readAddress();
    for (this.nExtraAddresses = 0; buffer.remaining() > 16; this.nExtraAddresses++) {
      buffer.readAddress();
    }
    this.pongTimestamp = buffer.readLong();
    this.timestamp = buffer.readLong();
  }

  @Override
  public void encode(@NotNull final PacketBuffer buffer) {
    buffer.writeAddress(this.address());
    for (var index = 0; index < this.nExtraAddresses; index++) {
      buffer.writeAddress();
    }
    buffer.writeLong(this.pongTimestamp);
    buffer.writeLong(this.timestamp);
  }
}
