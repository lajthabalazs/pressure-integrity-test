package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;

/** Configuration for a pressure sensor. */
@JsonPropertyOrder({"id", "type", "units", "sigma", "description"})
public class PressureSensorConfig extends SensorConfig {

  public static final String TYPE = "pressure";

  public PressureSensorConfig() {
    setType(TYPE);
  }

  public PressureSensorConfig(String id, String units, BigDecimal sigma, String description) {
    super(id, TYPE, units, sigma, description);
  }
}
