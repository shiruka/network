package io.github.shiruka.network.packets;

import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.utils.Integers;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.ArrayList;
import java.util.Objects;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * an abstract class that represents reliability packets.
 */
public abstract class Reliability implements Packet {

  /**
   * the entries.
   */
  private Entry@Nullable[] entries;

  /**
   * ctor.
   */
  protected Reliability() {}

  /**
   * ctor.
   *
   * @param ids the ids.
   */
  protected Reliability(@NotNull final IntSortedSet ids) {
    this.entries = new Entry[0];
    if (ids.isEmpty()) {
      return;
    }
    final var result = new ArrayList<Entry>();
    var startId = -1;
    var endId = -1;
    for (final var id : ids) {
      if (startId == -1) {
        startId = id;
        endId = id;
      } else if (id == Integers.B3.plus(endId, 1)) {
        endId = id;
      } else {
        result.add(new Entry(startId, endId));
        startId = id;
        endId = id;
      }
    }
    result.add(new Entry(startId, endId));
    this.entries = result.toArray(this.entries);
  }

  @Override
  public final void decode(@NotNull final PacketBuffer buffer) {
    this.entries = new Entry[buffer.readUnsignedShort()];
    for (var index = 0; index < this.entries.length; index++) {
      final var single = buffer.readBoolean();
      if (single) {
        final var id = buffer.readUnsignedTriadLE();
        this.entries[index] = new Entry(id);
      } else {
        final var idStart = buffer.readUnsignedTriadLE();
        final var idFinish = buffer.readUnsignedTriadLE();
        this.entries[index] = new Entry(idStart, idFinish);
      }
    }
  }

  @Override
  public final void encode(@NotNull final PacketBuffer buffer) {
    buffer.writeShort(this.entries().length);
    for (final var entry : this.entries()) {
      if (entry.idStart() == entry.idFinish()) {
        buffer.writeBoolean(true);
        buffer.writeTriadLE(entry.idStart);
      } else {
        buffer.writeBoolean(false);
        buffer.writeTriadLE(entry.idStart);
        buffer.writeTriadLE(entry.idFinish);
      }
    }
  }

  /**
   * obtains the entries.
   *
   * @return entries.
   */
  @NotNull
  public final Entry@NotNull[] entries() {
    return Objects.requireNonNull(this.entries, "entries");
  }

  /**
   * a class that represents reliability entries.
   */
  @Getter
  @Accessors(fluent = true)
  public static final class Entry {

    /**
     * the id finish.
     */
    private final int idFinish;

    /**
     * the id start.
     */
    private final int idStart;

    /**
     * ctor.
     *
     * @param idStart the id start.
     * @param idFinish the id finish.
     */
    private Entry(final int idStart, final int idFinish) {
      this.idFinish = idFinish;
      this.idStart = idStart;
    }

    /**
     * ctor.
     *
     * @param id the id.
     */
    private Entry(final int id) {
      this(id, id);
    }
  }
}
