package io.github.shiruka.network.maps;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * a class that represents concurrent int maps.
 */
public final class ConcurrentIntMap<T> extends ConcurrentHashMap<Integer, T> implements Map<Integer, T>,
  DynamicKey<Integer> {

  /**
   * returns {@code true} if this map contains a mapping for the specified key.
   *
   * @param key the key whose presence in this map is to be tested.
   *
   * @return {@code true} if this map contains a mapping for the specified key.
   */
  public boolean containsKey(final int key) {
    return super.containsKey(key);
  }

  /**
   * returns the value to which the specified key is mapped, or null if this map contains no mapping for the key.
   *
   * @param key they key the value is mapped to.
   *
   * @return the value to which the specified key is mapped.
   */
  @Nullable
  public T get(final int key) {
    return super.get(key);
  }

  /**
   * associates the specified value with the specified key in this map.
   * If the map previously contained a mapping for the key, the old value isreplaced.
   *
   * @param key key with which the specified value is to be associated
   * @param value value to be associated with the specified key
   *
   * @return the previous value associated with key, or null if there was no mapping for key.
   */
  @Nullable
  public T put(final int key, final T value) {
    return super.put(key, value);
  }

  /**
   * removes the mapping for the specified key from this map if present.
   *
   * @param key key whose mapping is to be removed from the map
   *
   * @return the previous value associated with key, or null if there was no mapping for key.
   */
  @Nullable
  public T remove(final int key) {
    return super.remove(key);
  }

  @Override
  public void renameKey(@NotNull final Integer oldKey, @NotNull final Integer newKey) throws NullPointerException {
    final var storedObject = Objects.requireNonNull(this.remove(oldKey.intValue()),
      "No value associated with old key");
    this.put(newKey.intValue(), storedObject);
  }
}
