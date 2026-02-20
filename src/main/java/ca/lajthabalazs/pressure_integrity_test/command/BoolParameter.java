package ca.lajthabalazs.pressure_integrity_test.command;

/** A {@link Parameter} that accepts common boolean representations (true/false, yes/no, 1/0). */
public class BoolParameter implements Parameter {

  @Override
  public boolean checkValue(String value) {
    if (value == null) return false;
    String v = value.trim().toLowerCase();
    return "true".equals(v)
        || "false".equals(v)
        || "yes".equals(v)
        || "no".equals(v)
        || "1".equals(v)
        || "0".equals(v);
  }

  @Override
  public Object normalize(String value) {
    String v = value.trim().toLowerCase();
    return "true".equals(v) || "yes".equals(v) || "1".equals(v);
  }
}
