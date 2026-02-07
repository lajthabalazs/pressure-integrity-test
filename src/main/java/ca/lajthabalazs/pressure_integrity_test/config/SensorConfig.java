package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.math.BigDecimal;

/**
 * Base configuration for a sensor (pressure, temperature, or humidity).
 *
 * <p>Subclasses add type-specific fields. The {@code type} field is used by Jackson to deserialize
 * JSON into the correct subclass.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type",
    include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = PressureSensorConfig.class, name = "pressure"),
  @JsonSubTypes.Type(value = TemperatureSensorConfig.class, name = "temperature"),
  @JsonSubTypes.Type(value = HumiditySensorConfig.class, name = "humidity")
})
public abstract class SensorConfig {

  private String id;
  private String type;
  private String units;
  private BigDecimal sigma;
  private String description;

  public SensorConfig() {}

  public SensorConfig(String id, String type, String units, BigDecimal sigma, String description) {
    this.id = id;
    this.type = type;
    this.units = units;
    this.sigma = sigma;
    this.description = description;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getUnits() {
    return units;
  }

  public void setUnits(String units) {
    this.units = units;
  }

  public BigDecimal getSigma() {
    return sigma;
  }

  public void setSigma(BigDecimal sigma) {
    this.sigma = sigma;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
