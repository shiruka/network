package io.github.shiruka.network;

import io.github.shiruka.network.packets.status.ConnectedPingPacket;
import io.github.shiruka.network.packets.status.ConnectedPongPacket;
import io.github.shiruka.network.packets.status.UnconnectedPingOpenConnectionsPacket;
import io.github.shiruka.network.packets.status.UnconnectedPingPacket;
import io.github.shiruka.network.packets.status.UnconnectedPongPacket;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

/**
 * a class that contains packets in it.
 */
public final class PacketRegistry {

  /**
   * the registry.
   */
  private static final Map<Integer, Factory> FACTORIES = Map.of(
    0, ConnectedPingPacket::new,
    1, UnconnectedPingPacket::new,
    2, UnconnectedPingOpenConnectionsPacket::new,
    3, ConnectedPongPacket::new,
    28, UnconnectedPongPacket::new
  );

  /**
   * ctor.
   */
  private PacketRegistry() {
  }

  /**
   * get the packet by id.
   *
   * @param id the id to get.
   *
   * @return packet.
   */
  @NotNull
  public static Packet get(final int id) {
    return Objects.requireNonNull(PacketRegistry.FACTORIES.get(id), "packet %s not found".formatted(id))
      .get();
  }

  /**
   * an interface to determine packet factories.
   */
  private interface Factory extends Supplier<@NotNull Packet> {

  }
}
