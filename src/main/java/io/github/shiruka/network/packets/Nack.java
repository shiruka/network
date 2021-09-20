package io.github.shiruka.network.packets;

import it.unimi.dsi.fastutil.ints.IntSortedSet;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents nack packets.
 */
public final class Nack extends Reliability {

  /**
   * ctor.
   */
  public Nack() {
  }

  /**
   * ctor.
   *
   * @param ids the ids.
   */
  public Nack(@NotNull final IntSortedSet ids) {
    super(ids);
  }
}
