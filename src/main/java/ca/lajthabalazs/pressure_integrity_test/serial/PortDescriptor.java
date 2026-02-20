package ca.lajthabalazs.pressure_integrity_test.serial;

/** Describes an available serial port (real or virtual) for listing in UI. */
public record PortDescriptor(String systemPortName, String portDescription) {

  /** Display string: "COM3 (Description)" or "COM3" if description is blank. */
  public String toDisplayString() {
    if (portDescription != null && !portDescription.isBlank()) {
      return systemPortName + " (" + portDescription + ")";
    }
    return systemPortName;
  }
}
