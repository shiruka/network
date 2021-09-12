package io.github.shiruka.network.raknet.exceptions;

import org.jetbrains.annotations.NotNull;

/**
 * an exception class that represents rak net exceptions.
 */
public final class RakNetException extends Exception {

  /**
   * ctor.
   *
   * @param message the message.
   */
  public RakNetException(@NotNull final String message, @NotNull final Object... args) {
    super(message.formatted(args));
  }

  /**
   * ctor.
   *
   * @param cause the cause.
   */
  public RakNetException(@NotNull final Throwable cause) {
    this(cause.getMessage());
  }
}
