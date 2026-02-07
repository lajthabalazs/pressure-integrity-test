package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Collections;
import java.util.List;

/** Root site configuration: containment, design pressure, and sensor definitions. */
@JsonPropertyOrder({"id", "description", "containment", "designPressure", "sensors"})
public class SiteConfig {

  private String id;
  private String description;
  private Containment containment;
  private DesignPressure designPressure;
  private List<SensorConfig> sensors = List.of();

  public SiteConfig() {}

  public SiteConfig(
      String id,
      String description,
      Containment containment,
      DesignPressure designPressure,
      List<SensorConfig> sensors) {
    this.id = id;
    this.description = description;
    this.containment = containment;
    this.designPressure = designPressure;
    this.sensors = sensors == null ? List.of() : List.copyOf(sensors);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Containment getContainment() {
    return containment;
  }

  public void setContainment(Containment containment) {
    this.containment = containment;
  }

  public DesignPressure getDesignPressure() {
    return designPressure;
  }

  public void setDesignPressure(DesignPressure designPressure) {
    this.designPressure = designPressure;
  }

  public List<SensorConfig> getSensors() {
    return sensors == null ? Collections.emptyList() : sensors;
  }

  public void setSensors(List<SensorConfig> sensors) {
    this.sensors = sensors == null ? List.of() : List.copyOf(sensors);
  }
}
