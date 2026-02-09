package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** Configuration for a temperature sensor. Volume factor is on the location, not the sensor. */
@JsonPropertyOrder({"id", "type", "units", "validRange", "sigma", "description"})
public class TemperatureSensorConfig extends SensorConfig {

  public static final String TYPE = "temperature";

  private ValidRange validRange;

  public TemperatureSensorConfig() {
    setType(TYPE);
  }

  public ValidRange getValidRange() {
    return validRange;
  }

  public void setValidRange(ValidRange validRange) {
    this.validRange = validRange;
  }
}
