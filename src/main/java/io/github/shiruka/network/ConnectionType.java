package io.github.shiruka.network;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * a record class that represents connection types.
 *
 * @param uniqueId the unique id.
 * @param name the name.
 * @param language the language.
 * @param version the version
 * @param metadata the metadata.
 * @param vanilla the vanilla.
 */
public record ConnectionType(
  @Nullable UUID uniqueId,
  @NotNull String name,
  @Nullable String language,
  @Nullable String version,
  @NotNull Map<String, String> metadata,
  boolean vanilla
) {

  /**
   * the magic.
   */
  public static final byte[] MAGIC = new byte[]{0x03, 0x08, 0x05, 0x0B, 0x43, 0x54, 0x49};

  /**
   * the max metadata values.
   */
  public static final int MAX_METADATA_VALUES = 255;

  /**
   * the rak net connection type.
   */
  public static final ConnectionType RAK_NET = new ConnectionType(
    UUID.fromString("504da9b2-a31c-4db6-bcc3-18e5fe2fb178"), "RakNet", "Java", "@version@");

  /**
   * the vanilla connection type.
   */
  public static final ConnectionType VANILLA = new ConnectionType(null, "Vanilla", null, null, new HashMap<>(), true);

  /**
   * the compact ctor.
   */
  public ConnectionType {
    Preconditions.checkArgument(metadata.size() <= ConnectionType.MAX_METADATA_VALUES,
      "Too many metadata values!");
  }

  /**
   * ctor.
   *
   * @param uniqueId the unique id.
   * @param name the name.
   * @param language the language.
   * @param version the version
   * @param metadata the metadata.
   */
  public ConnectionType(@Nullable final UUID uniqueId, @NotNull final String name, @Nullable final String language,
                        @Nullable final String version, @NotNull final Map<String, String> metadata) {
    this(uniqueId, name, language, version, metadata, false);
  }

  /**
   * ctor.
   *
   * @param uniqueId the unique id.
   * @param name the name.
   * @param language the language.
   * @param version the version
   */
  public ConnectionType(@Nullable final UUID uniqueId, @NotNull final String name, @Nullable final String language,
                        @Nullable final String version) {
    this(uniqueId, name, language, version, new HashMap<>(), false);
  }

  /**
   * converts the metadata keys and values to a {@link HashMap}.
   *
   * @param metadata the metadata keys and values.
   *
   * @return the metadata as a {@link HashMap}.
   *
   * @throws IllegalArgumentException if there is a key without a value or if there are more than
   *   {@value #MAX_METADATA_VALUES} metadata values.
   */
  @NotNull
  public static Map<String, String> metaData(@NotNull final String... metadata) {
    Preconditions.checkArgument(metadata.length % 2 == 0, "There must be a value for every key");
    Preconditions.checkArgument(metadata.length / 2 <= ConnectionType.MAX_METADATA_VALUES, "Too many metadata values");
    return IntStream.iterate(0, index -> index < metadata.length, index -> index + 2)
      .boxed()
      .collect(Collectors.toMap(index -> metadata[index], index -> metadata[index + 1], (a, b) -> b, HashMap::new));
  }

  /**
   * Returns whether or not this implementation and the specified
   * implementation are the same implementation based on the UUID.
   * <p>
   * If the UUID of both implementations are {@code null} then
   * {@code false} will be returned since we have no logical way of
   * telling if the two implementations are actually the same as there are no
   * UUIDs to compare.
   *
   * @param connectionType the connection type.
   *
   * @return {@code true} if both implementations are the same, {@code false} otherwise.
   */
  public boolean is(@NotNull final ConnectionType connectionType) {
    return connectionType.uniqueId() != null &&
      this.uniqueId() != null &&
      this.uniqueId().equals(connectionType.uniqueId());
  }
}
