package io.github.shiruka.network.raknet.server;

import io.github.shiruka.network.raknet.ConnectionType;
import org.jetbrains.annotations.NotNull;

/**
 * an interface to determine rak net server identifiers.
 */
public interface RakNetServerIdentifier {

  /**
   * builds the server id string.
   *
   * @return server id string.
   */
  @NotNull
  String build();

  /**
   * an interface to determine rak net server identifier factory.
   */
  interface Factory {

    /**
     * creates a rak net server identifier.
     *
     * @param serverIdString the server id string to create.
     * @param connectionType the connection type to create.
     *
     * @return server identifier.
     */
    @NotNull
    RakNetServerIdentifier create(@NotNull String serverIdString, @NotNull ConnectionType connectionType);
  }
}
