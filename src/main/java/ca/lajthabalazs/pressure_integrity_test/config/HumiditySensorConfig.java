package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

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
