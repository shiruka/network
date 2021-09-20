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
     * ctor.
     */
    private B3() {
    }

    public static int plus(final int value, final int add) {
      return value + add & B3.MAX_VALUE;
    }
  }
}
