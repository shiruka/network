package io.github.shiruka.network.packets;

import it.unimi.dsi.fastutil.ints.IntSortedSet;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents nack packets.
 */
@NoArgsConstructor
public final class Nack extends Reliability {

  /**
   * ctor.
   *
   * @param ids the ids.
   */
  public Nack(@NotNull final IntSortedSet ids) {
    super(ids);
  }
}
