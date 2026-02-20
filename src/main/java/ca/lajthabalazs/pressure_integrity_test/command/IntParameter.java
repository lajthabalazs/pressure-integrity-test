package ca.lajthabalazs.pressure_integrity_test.command;

/** A {@link Parameter} that accepts a string parseable as an integer. */
public class IntParameter implements Parameter {

  @Override
  public boolean checkValue(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    try {
      Integer.parseInt(value.trim());
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  @Override
  public Object normalize(String value) {
    return Integer.parseInt(value.trim());
  }
}
