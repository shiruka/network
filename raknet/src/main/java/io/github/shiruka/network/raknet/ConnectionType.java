package io.github.shiruka.network.raknet;

import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.UUID;
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
  @NotNull String language,
  @NotNull String version,
  @NotNull Map<String, String> metadata,
  boolean vanilla
) {

  /**
   * the magic.
   */
  private static final byte[] MAGIC = new byte[]{0x03, 0x08, 0x05, 0x0B, 0x43, 0x54, 0x49};

  /**
   * the max metadata values.
   */
  private static final int MAX_METADATA_VALUES = 0xFF;

  /**
   * the compact ctor.
   */
  public ConnectionType {
    Preconditions.checkArgument(metadata.size() <= ConnectionType.MAX_METADATA_VALUES,
      "Too many metadata values");
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
    if (connectionType.uniqueId() == null || this.uniqueId() == null) {
      return false;
    }
    return this.uniqueId().equals(connectionType.uniqueId());
  }
}
