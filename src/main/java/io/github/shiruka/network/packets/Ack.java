package io.github.shiruka.network.packets;

import it.unimi.dsi.fastutil.ints.IntSortedSet;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents ack packets.
 */
public final class Ack extends Reliability {

  /**
   * ctor.
   */
  public Ack() {
  }

  /**
   * ctor.
   *
   * @param ids the ids.
   */
  public Ack(@NotNull final IntSortedSet ids) {
    super(ids);
  }
}
