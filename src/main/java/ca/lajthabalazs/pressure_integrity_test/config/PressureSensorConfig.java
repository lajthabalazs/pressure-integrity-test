package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** Configuration for a pressure sensor. */
@JsonPropertyOrder({"id", "type", "units", "sigma", "description"})
public class PressureSensorConfig extends SensorConfig {

  public static final String TYPE = "pressure";

  public PressureSensorConfig() {
    setType(TYPE);
  }
}
