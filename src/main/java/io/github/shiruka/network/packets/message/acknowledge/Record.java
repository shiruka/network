package io.github.shiruka.network.packets.message.acknowledge;

import io.netty.util.collection.IntObjectHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

/**
 * a class that represents packet records.
 */
@Accessors(fluent = true)
@ToString(doNotUseGetters = true, onlyExplicitlyIncluded = true)
@EqualsAndHashCode(doNotUseGetters = true, onlyExplicitlyIncluded = true)
public final class Record {

  /**
   * the record is not ranged.
   */
  private static final int NOT_RANGED = -1;

  /**
   * the end index.
   */
  @Getter
  @ToString.Include
  @EqualsAndHashCode.Include
  private int endIndex;

  /**
   * the index.
   */
  @Getter
  @ToString.Include
  @EqualsAndHashCode.Include
  private int index;

  /**
   * the sequence ids.
   */
  @Getter
  private int[] sequenceIds;

  /**
   * ctor.
   *
   * @param index the starting index.
   * @param endIndex the ending index.
   */
  public Record(@Range(from = 0, to = Integer.MAX_VALUE) final int index, final int endIndex) {
    this.index = index;
    this.endIndex = endIndex;
    this.updateSequenceIds();
  }

  /**
   * ctor.
   *
   * @param id the sequence ID.
   */
  public Record(@Range(from = 0, to = Integer.MAX_VALUE) final int id) {
    this(id, Record.NOT_RANGED);
  }

  /**
   * condenses the specified records into a Record[] with all ranges of sequence IDs being in ranged records to save
   * memory.
   *
   * @param records records to condense.
   *
   * @return the condensed records.
   */
  @NotNull
  public static Record[] condense(@NotNull final Record... records) {
    final var sequenceIds = Record.getSequenceIds(records);
    Arrays.sort(sequenceIds);
    final var condensed = new ArrayList<Record>();
    for (var index = 0; index < sequenceIds.length; index++) {
      final var startIndex = sequenceIds[index];
      var endIndex = startIndex;
      if (index + 1 < sequenceIds.length) {
        while (endIndex + 1 == sequenceIds[index + 1]) {
          endIndex = sequenceIds[++index];
          if (index + 1 >= sequenceIds.length) {
            break;
          }
        }
      }
      condensed.add(new Record(startIndex, endIndex == startIndex ? -1 : endIndex));
    }
    return condensed.toArray(new Record[0]);
  }

  /**
   * condenses the specified records into a Record[] with all ranges of sequence IDs being in ranged records to save
   * memory.
   *
   * @param records the records to condense.
   *
   * @return the condensed records.
   */
  @NotNull
  public static Record[] condense(@NotNull final Collection<Record> records) {
    return Record.condense(records.toArray(new Record[0]));
  }

  /**
   * condenses the specified sequence IDs into a Record[] with all ranges of sequence IDs being in ranged records to
   * save memory.
   *
   * @param sequenceIds the sequence IDs to condense.
   *
   * @return the condensed records.
   */
  @NotNull
  public static Record[] condense(final int... sequenceIds) {
    return Record.condense(Arrays.stream(sequenceIds)
      .mapToObj(Record::new)
      .toArray(Record[]::new));
  }

  /**
   * returns the sequence IDs contained within the specified records.
   *
   * @param records the records to get the sequence IDs from.
   *
   * @return the sequence IDs contained within the specified records.
   */
  public static int[] getSequenceIds(@NotNull final Collection<Record> records) {
    return Record.getSequenceIds(records.toArray(new Record[0]));
  }

  /**
   * returns the sequence IDs contained within the specified records.
   *
   * @param records the records to get the sequence IDs from.
   *
   * @return the sequence IDs contained within the specified records.
   */
  public static int[] getSequenceIds(@NotNull final Record... records) {
    final var sequenceIds = Arrays.stream(records)
      .flatMapToInt(record -> Arrays.stream(record.sequenceIds()))
      .distinct()
      .boxed()
      .mapToInt(value -> value)
      .sorted()
      .toArray();
    Arrays.sort(sequenceIds);
    return sequenceIds;
  }

  /**
   * simplifies the specified sequence IDs into a Record[] with all sequence IDs having their own dedicated record to
   * make handling them easier.
   *
   * @param sequenceIds the sequence IDs to simplify.
   *
   * @return the simplified records
   */
  @NotNull
  public static Record[] simplify(final int... sequenceIds) {
    final var simplified = new IntObjectHashMap<Record>();
    Arrays.stream(sequenceIds)
      .filter(sequenceId -> !simplified.containsKey(sequenceId))
      .forEach(sequenceId -> simplified.put(sequenceId, new Record(sequenceId)));
    return simplified.values().toArray(new Record[0]);
  }

  /**
   * simplifies the specified records into a Record[] with all sequence IDs within the records having their own
   * dedicated record to make handling them easier.
   *
   * @param records the records to simplify.
   *
   * @return the simplified records
   */
  @NotNull
  public static Record[] simplify(@NotNull final Record... records) {
    return Record.simplify(Record.getSequenceIds(records));
  }

  /**
   * simplifies the specified records into a Record[] with all sequence IDs within the records having their own
   * dedicated record to make handling them easier.
   *
   * @param records the records to simplify.
   *
   * @return the simplified records
   */
  @NotNull
  public static Record[] simplify(@NotNull final Collection<Record> records) {
    return Record.simplify(records.toArray(new Record[0]));
  }

  /**
   * returns the sequence ID contained within this record.
   * This is the equivalent of calling {@link #index()}, however an error will be thrown if the record is ranged.
   *
   * @return the sequence ID contained within this record.
   *
   * @throws ArrayStoreException if the record is ranged according to the {@link #isRanged()} method.
   * @see #sequenceIds()
   */
  public int getSequenceId() throws ArrayStoreException {
    if (this.isRanged()) {
      throw new ArrayStoreException("Record is ranged, there are multiple IDs");
    }
    return this.index;
  }

  /**
   * returns whether or not the record is ranged.
   *
   * @return {@code true} if the record is ranged, {@code false} otherwise.
   */
  public boolean isRanged() {
    return this.endIndex > Record.NOT_RANGED;
  }

  /**
   * sets the ending index of the record.
   *
   * @param endIndex the ending index, a value of {@value #NOT_RANGED} or lower or to the value of the index itself
   *   indicates that the record is not ranged.
   */
  public void setEndIndex(int endIndex) {
    if (endIndex <= this.index) {
      endIndex = Record.NOT_RANGED;
    }
    this.endIndex = endIndex;
    this.updateSequenceIds();
  }

  /**
   * sets the starting index of the record.
   *
   * @param index the starting index.
   */
  public void setIndex(@Range(from = 0, to = Integer.MAX_VALUE) final int index) {
    this.index = index;
    this.updateSequenceIds();
  }

  /**
   * updates the sequence IDs within the record.
   */
  private void updateSequenceIds() {
    if (!this.isRanged()) {
      this.sequenceIds = new int[]{this.index};
    } else {
      this.sequenceIds = IntStream.range(0, this.endIndex - this.index + 1)
        .map(index -> index + this.index)
        .toArray();
    }
  }
}
