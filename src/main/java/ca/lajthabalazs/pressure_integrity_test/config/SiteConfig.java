package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
    return sensors;
  }

  public void setSensors(List<SensorConfig> sensors) {
    this.sensors = List.copyOf(sensors);
  }
}
