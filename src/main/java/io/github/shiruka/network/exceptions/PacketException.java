package io.github.shiruka.network.exceptions;

import org.jetbrains.annotations.NotNull;

/**
 * an exception class that represents packet exceptions.
 */
public final class PacketException extends Exception {

  /**
   * ctor.
   *
   * @param message the message.
   * @param args the args.
   */
  public PacketException(@NotNull final String message, @NotNull final Object... args) {
    super(message.formatted(args));
  }

  /**
   * ctor.
   *
   * @param cause the cause.
   */
  public PacketException(@NotNull final Throwable cause) {
    this(cause.getMessage());
  }
}
