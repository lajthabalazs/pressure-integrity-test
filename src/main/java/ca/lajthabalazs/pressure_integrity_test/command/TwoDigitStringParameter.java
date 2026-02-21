package ca.lajthabalazs.pressure_integrity_test.command;

/**
 * A {@link Parameter} that accepts a two-digit numeric string (00–99). Used for Almemo device and
 * measuring point selection (e.g. G00, M01) where the value is sent immediately after the command
 * id with no separating character.
 */
public class TwoDigitStringParameter implements Parameter {

  @Override
  public boolean checkValue(String value) {
    if (value == null) return false;
    String s = value.trim();
    if (s.length() != 2) return false;
    return Character.isDigit(s.charAt(0)) && Character.isDigit(s.charAt(1));
  }

  @Override
  public Object normalize(String value) {
    return value == null ? "00" : value.trim();
  }
}
