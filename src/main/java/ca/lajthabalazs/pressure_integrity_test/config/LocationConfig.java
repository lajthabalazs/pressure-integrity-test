package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;
import java.util.List;

/**
 * A location within a site with a volume factor (for ideal gas law) and sensors. The volume factor
 * applies to temperature sensors in this location; temperature sensors do not have their own volume
 * factor.
 */
@JsonPropertyOrder({"id", "volumeFactor", "sensors"})
public class LocationConfig {

  private String id;
  private BigDecimal volumeFactor;
  private List<SensorConfig> sensors = List.of();

  public LocationConfig() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public BigDecimal getVolumeFactor() {
    return volumeFactor;
  }

  public void setVolumeFactor(BigDecimal volumeFactor) {
    this.volumeFactor = volumeFactor;
  }

  public List<SensorConfig> getSensors() {
    return sensors;
  }

  public void setSensors(List<SensorConfig> sensors) {
    this.sensors = sensors != null ? List.copyOf(sensors) : List.of();
  }
}
