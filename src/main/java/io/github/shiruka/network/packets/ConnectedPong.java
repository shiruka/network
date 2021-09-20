package io.github.shiruka.network.packets;

import io.github.shiruka.network.PacketBuffer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents connected pong packets.
 */
@Getter
@Setter
@Accessors(fluent = true)
public final class ConnectedPong extends FramedPacket.Base {

  /**
   * the timestamp of the sender of the ping.
   */
  public long timestamp;

  /**
   * the timestamp of the sender of the pong.
   */
  public long timestampPong;

  /**
   * ctor.
   *
   * @param timestamp the timestamp.
   * @param timestampPong the timestampPong.
   * @param reliability the reliability.
   */
  public ConnectedPong(final long timestamp, final long timestampPong, @NotNull final Reliability reliability) {
    super(reliability);
    this.timestamp = timestamp;
    this.timestampPong = timestampPong;
  }

  /**
   * ctor.
   *
   * @param timestamp the timestamp.
   * @param reliability the reliability.
   */
  public ConnectedPong(final long timestamp, @NotNull final Reliability reliability) {
    this(timestamp, System.nanoTime(), reliability);
  }

  /**
   * ctor.
   *
   * @param timestamp the timestamp.
   * @param timestampPong the timestampPong.
   */
  public ConnectedPong(final long timestamp, final long timestampPong) {
    this(timestamp, timestampPong, Reliability.UNRELIABLE);
  }

  /**
   * ctor.
   *
   * @param timestamp the timestamp.
   */
  public ConnectedPong(final long timestamp) {
    this(timestamp, Reliability.UNRELIABLE);
  }

  /**
   * ctor.
   */
  public ConnectedPong() {
    super(Reliability.UNRELIABLE);
  }

  @Override
  public void decode(@NotNull final PacketBuffer buffer) {
    this.timestamp = buffer.readLong();
    this.timestampPong = buffer.readLong();
  }

  @Override
  public void encode(@NotNull final PacketBuffer buffer) {
    this.timestamp = buffer.readLong();
    if (buffer.isReadable()) {
      this.timestampPong = buffer.readLong();
    }
  }

  /**
   * gets the rtt.
   *
   * @return rtt.
   */
  public long getRTT() {
    return System.nanoTime() - this.timestamp;
  }
}
