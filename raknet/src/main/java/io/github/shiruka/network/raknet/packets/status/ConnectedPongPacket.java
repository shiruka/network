package io.github.shiruka.network.raknet.packets.status;

import io.github.shiruka.network.raknet.RakNetPacket;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * a class that represents connected pong packets.
 */
@Getter
@Setter
@Accessors(fluent = true)
public final class ConnectedPongPacket extends RakNetPacket {

  /**
   * the timestamp of the sender of the ping.
   */
  public long timestamp;

  /**
   * The timestamp of the sender of the pong.
   */
  public long timestampPong;

  /**
   * ctor.
   */
  public ConnectedPongPacket() {
    super(Ids.CONNECTED_PONG);
  }

  @Override
  public void encode() {
    this.writeLong(this.timestamp);
    this.writeLong(this.timestampPong);
  }
}
