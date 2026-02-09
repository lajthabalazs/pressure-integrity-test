package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
  private String locationId;
  private String type;
  private String units;
  private BigDecimal sigma;
  private String description;

  public SensorConfig() {}

  public String getId() {
    return id;
  }

  /** Location this sensor belongs to; null for pressure and humidity sensors. Not serialized. */
  @JsonIgnore
  public String getLocationId() {
    return locationId;
  }

  public void setLocationId(String locationId) {
    this.locationId = locationId;
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
