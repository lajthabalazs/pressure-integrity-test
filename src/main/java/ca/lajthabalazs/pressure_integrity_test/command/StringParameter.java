package ca.lajthabalazs.pressure_integrity_test.command;

/** A {@link Parameter} that accepts any non-null string and normalizes by trimming. */
public class StringParameter implements Parameter {

  @Override
  public boolean checkValue(String value) {
    return value != null;
  }

  @Override
  public Object normalize(String value) {
    return value == null ? "" : value.trim();
  }
}
