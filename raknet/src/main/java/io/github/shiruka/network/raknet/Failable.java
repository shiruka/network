package io.github.shiruka.network.raknet;

import org.jetbrains.annotations.NotNull;

/**
 * an interface to determine failable packets.
 */
public interface Failable {

  /**
   * returns whether or not the packet failed to encode/decode.
   *
   * @return {@code true} if the packet failed to encode/decode, {@code false} otherwise.
   */
  default boolean failed() {
    return false;
  }

  /**
   * runs when reading/writing fails.
   */
  default void onFail() {
  }

  /**
   * runs the given runnable.
   *
   * @param runnable the runnable to run.
   */
  default void unchecked(@NotNull final Run runnable) {
    try {
      runnable.run();
    } catch (final Exception e) {
      this.onFail();
    }
  }

  /**
   * an interface to determine runnable objects.
   */
  interface Run {

    /**
     * runs the job.
     *
     * @throws Exception when something goes wrong.
     */
    void run() throws Exception;
  }
}
