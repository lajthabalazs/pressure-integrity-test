package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;

/** Configuration for a temperature sensor (includes volume weight for ideal gas law). */
@JsonPropertyOrder({"id", "type", "units", "volumeWeight", "validRange", "sigma", "description"})
public class TemperatureSensorConfig extends SensorConfig {

  public static final String TYPE = "temperature";

  private ValidRange validRange;
  private BigDecimal volumeWeight;

  public TemperatureSensorConfig() {
    setType(TYPE);
  }

  public ValidRange getValidRange() {
    return validRange;
  }

  public void setValidRange(ValidRange validRange) {
    this.validRange = validRange;
  }

  public BigDecimal getVolumeWeight() {
    return volumeWeight;
  }

  public void setVolumeWeight(BigDecimal volumeWeight) {
    this.volumeWeight = volumeWeight;
  }
}
