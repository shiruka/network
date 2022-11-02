package io.github.shiruka.network.utils;

import it.unimi.dsi.fastutil.ints.IntComparator;

/**
 * a class that contains utility methods for integers.
 */
public final class Integers {

  /**
   * ctor.
   */
  private Integers() {}

  /**
   * a class that represents b2 math operations.
   */
  public static final class B2 {

    /**
     * the max value.
     */
    private static final int MAX_VALUE = (1 << Byte.SIZE * 2) - 1;

    /**
     * ctor.
     */
    private B2() {}

    /**
     * plus.
     *
     * @param value the value to plus.
     * @param add the add to plus.
     *
     * @return plus.
     */
    public static int plus(final int value, final int add) {
      return value + add & B2.MAX_VALUE;
    }
  }

  /**
   * a class that represents b3 math operations.
   */
  public static final class B3 {

    /**
     * the comparator.
     */
    public static final IntComparator COMPARATOR = new Comparator();

    /**
     * the max value.
     */
    private static final int MAX_VALUE = (1 << Byte.SIZE * 3) - 1;

    /**
     * the half of max.
     */
    private static final int HALF_MAX = B3.MAX_VALUE / 2;

    /**
     * ctor.
     */
    private B3() {}

    /**
     * minus wrap.
     *
     * @param value the value to wrap.
     * @param minus the minus to wrap.
     *
     * @return minus wrap.
     */
    public static int minusWrap(final int value, final int minus) {
      final var dist = value - minus;
      if (dist < 0) {
        return -B3.minusWrap(minus, value);
      }
      if (dist > B3.HALF_MAX) {
        return value - (minus + B3.MAX_VALUE + 1);
      }
      return dist;
    }

    /**
     * plus.
     *
     * @param value the value to plus.
     * @param add the add to plus.
     *
     * @return plus.
     */
    public static int plus(final int value, final int add) {
      return value + add & B3.MAX_VALUE;
    }

    /**
     * a class that represents int comparators.
     */
    public static final class Comparator implements IntComparator {

      @Override
      public int compare(final int k1, final int k2) {
        final var d = B3.minusWrap(k1, k2);
        return Integer.compare(d, 0);
      }
    }
  }
}
