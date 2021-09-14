package io.github.shiruka.network.maps;

import org.jetbrains.annotations.NotNull;

/**
 * an interface to determine dynamic keys.
 */
public interface DynamicKey<T> {

  /**
   * renames the specified key and changes it to the specified one.
   *
   * @param oldKey the old key.
   * @param newKey the new key.
   */
  void renameKey(@NotNull T oldKey, @NotNull T newKey);
}
