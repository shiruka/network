package io.github.shiruka.network;

import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * a class that contains packets in it.
 */
public final class PacketRegistry {

  /**
   * the registry.
   */
  private static final Map<Integer, Factory> FACTORIES = Map.of(
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
      .create();
  }

  /**
   * an interface to determine packet factories.
   */
  private interface Factory {

    /**
     * creates a new packet.
     *
     * @return packet.
     */
    @NotNull
    Packet create();
  }
}
