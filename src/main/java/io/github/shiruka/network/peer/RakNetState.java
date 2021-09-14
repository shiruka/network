package io.github.shiruka.network.peer;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * an enum class that contains rak net states.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum RakNetState {

  /**
   * the peer is connected.
   */
  CONNECTED(1, 1),
  /**
   * the peer is handshaking.
   */
  HANDSHAKING(2, 3),
  /**
   * the peer is logged in.
   */
  LOGGED_IN(5, 5),
  /**
   * the peer is disconnected.
   */
  DISCONNECTED(0, 0);

  /**
   * the defining.
   */
  private final int defining;

  /**
   * the flags.
   */
  private final int flags;

  /**
   * returns whether or not this state is a derivative of the specified state.
   *
   * @param state the state.
   *
   * @return {@code true} if the state is a derivative of this state, {@code false} otherwise.
   *
   * @throws IllegalArgumentException if the defining flags of the state are null.
   */
  public boolean isDerivative(@NotNull final RakNetState state) throws UnsupportedOperationException {
    Preconditions.checkArgument(state.defining != 0, "Defining flags are null (0)");
    return (this.flags & state.defining) > 0;
  }
}
