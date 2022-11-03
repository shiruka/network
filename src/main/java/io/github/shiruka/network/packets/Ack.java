package io.github.shiruka.network.packets;

import it.unimi.dsi.fastutil.ints.IntSortedSet;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * a class that represents ack packets.
 */
@NoArgsConstructor
public final class Ack extends Reliability {

  /**
   * ctor.
   *
   * @param ids the ids.
   */
  public Ack(@NotNull final IntSortedSet ids) {
    super(ids);
  }
}
