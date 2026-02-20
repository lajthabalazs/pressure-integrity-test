package ca.lajthabalazs.pressure_integrity_test.command;

/** A {@link Parameter} that accepts a string parseable as a float. */
public class FloatParameter implements Parameter {

  @Override
  public boolean checkValue(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    try {
      Float.parseFloat(value.trim());
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  @Override
  public Object normalize(String value) {
    return Float.parseFloat(value.trim());
  }
}
