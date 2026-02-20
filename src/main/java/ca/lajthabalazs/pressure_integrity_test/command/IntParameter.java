package ca.lajthabalazs.pressure_integrity_test.command;

/**
 * A {@link Parameter} that accepts a string parseable as an integer. Optionally enforces min/max.
 */
public class IntParameter implements Parameter {

  private final int min;
  private final int max;

  /** No range restriction. */
  public IntParameter() {
    this.min = Integer.MIN_VALUE;
    this.max = Integer.MAX_VALUE;
  }

  /** Restricts value to [min, max] (inclusive). */
  public IntParameter(int min, int max) {
    this.min = min;
    this.max = max;
  }

  @Override
  public boolean checkValue(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    try {
      int n = Integer.parseInt(value.trim());
      return n >= min && n <= max;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  @Override
  public Object normalize(String value) {
    return Integer.parseInt(value.trim());
  }
}
