package io.github.shiruka.network;

import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * an interface to determine rak net server identifiers.
 */
public interface ServerIdentifier {

  /**
   * the factories.
   */
  Set<Factory> FACTORIES = new HashSet<>();

  /**
   * finds and creates a factory for the text.
   *
   * @param text the text to find.
   *
   * @return found server identifier.
   */
  @NotNull
  static ServerIdentifier create(@NotNull final String text) {
    return ServerIdentifier.FACTORIES.stream()
      .filter(factory -> factory.check(text))
      .map(factory -> factory.create(text))
      .findFirst()
      .orElseThrow(() ->
        new IllegalStateException("Factory for %s not found!".formatted(text)));
  }

  /**
   * registers the factory.
   *
   * @param factory the factory to register.
   */
  static void register(@NotNull final Factory factory) {
    ServerIdentifier.FACTORIES.add(factory);
  }

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
     * checks if the given text is correct to create this identifier.
     *
     * @param text the text to check.
     *
     * @return {@code true} if the text is correct to create the identifier.
     */
    boolean check(@NotNull String text);

    /**
     * creates a rak net server identifier.
     *
     * @param serverInfo the server info to create.
     *
     * @return server identifier.
     */
    @NotNull
    ServerIdentifier create(@NotNull String serverInfo);
  }
}
