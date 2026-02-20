package ca.lajthabalazs.pressure_integrity_test.command;

/**
 * A parameter of a {@link Command}. Validates and normalizes string input (e.g. from UI or CLI)
 * into a typed value for sending.
 */
public interface Parameter {

  /**
   * Returns whether the given string is valid for this parameter.
   *
   * @param value raw string value
   * @return true if the value is valid
   */
  boolean checkValue(String value);

  /**
   * Normalizes the given string into the parameter's typed value. Should only be called when {@link
   * #checkValue(String)} returns true for the same value.
   *
   * @param value raw string value
   * @return normalized value (String, Integer, Float, or Boolean depending on implementation)
   */
  Object normalize(String value);
}
