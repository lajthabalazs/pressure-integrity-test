package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;

/** Configuration for a humidity sensor (paired with a temperature sensor for dew-point etc.). */
@JsonPropertyOrder({
  "id",
  "type",
  "units",
  "pairedTemperatureSensor",
  "validRange",
  "sigma",
  "description"
})
public class HumiditySensorConfig extends SensorConfig {

  public static final String TYPE = "humidity";

  private ValidRange validRange;
  private String pairedTemperatureSensor;

  public HumiditySensorConfig() {
    setType(TYPE);
  }

  public HumiditySensorConfig(
      String id,
      String units,
      BigDecimal sigma,
      String description,
      ValidRange validRange,
      String pairedTemperatureSensor) {
    super(id, TYPE, units, sigma, description);
    this.validRange = validRange;
    this.pairedTemperatureSensor = pairedTemperatureSensor;
  }

  public ValidRange getValidRange() {
    return validRange;
  }

  public void setValidRange(ValidRange validRange) {
    this.validRange = validRange;
  }

  public String getPairedTemperatureSensor() {
    return pairedTemperatureSensor;
  }

  public void setPairedTemperatureSensor(String pairedTemperatureSensor) {
    this.pairedTemperatureSensor = pairedTemperatureSensor;
  }
}
