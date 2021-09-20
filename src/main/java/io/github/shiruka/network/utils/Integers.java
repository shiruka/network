package io.github.shiruka.network.utils;

/**
 * a class that contains utility methods for integers.
 */
public final class Integers {

  /**
   * ctor.
   */
  private Integers() {
  }

  /**
   * a class that represents b3 math operations.
   */
  public static final class B3 {

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
    private B3() {
    }

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
  }
}
