package io.github.shiruka.network;

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
   *
   * @param buffer the buffer to run.
   */
  default void onFail(@NotNull final PacketBuffer buffer) {
  }

  /**
   * runs the given runnable.
   *
   * @param buffer the buffer to run.
   * @param runnable the runnable to run.
   */
  default void unchecked(@NotNull final PacketBuffer buffer, @NotNull final Run runnable) {
    try {
      runnable.run();
    } catch (final Exception e) {
      this.onFail(buffer);
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
