package io.github.shiruka.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * a class that contains packets in it.
 */
public final class Packets {

  /**
   * the registry.
   */
  private static final Map<Integer, Packet> REGISTRY = new HashMap<>();

  /**
   * ctor.
   */
  private Packets() {
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
    return Objects.requireNonNull(REGISTRY.get(id), "packet %s not found".formatted(id));
  }

  /**
   * puts the packet to the registry map.
   *
   * @param id the id to put.
   * @param packet the packet to put.
   */
  public static void put(final int id, @NotNull final Packet packet) {
    Packets.REGISTRY.put(id, packet);
  }
}
