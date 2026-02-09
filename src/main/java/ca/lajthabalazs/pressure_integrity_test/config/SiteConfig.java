package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Root site configuration: containment, design pressure, and locations. Each location has a volume
 * factor and a list of sensors (pressure, temperature, humidity). Every sensor has an id and a
 * locationId (from its parent location).
 */
@JsonPropertyOrder({"id", "description", "containment", "designPressure", "locations"})
public class SiteConfig {

  private String id;
  private String description;
  private Containment containment;
  private DesignPressure designPressure;
  private List<LocationConfig> locations = List.of();

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

  public List<LocationConfig> getLocations() {
    return locations;
  }

  public void setLocations(List<LocationConfig> locations) {
    this.locations = locations != null ? List.copyOf(locations) : List.of();
  }

  /**
   * Returns all sensors in order: each location's sensors, in location order. For use by UI and
   * stream that need a single ordered list.
   */
  @JsonIgnore
  public List<SensorConfig> getSensors() {
    List<SensorConfig> out = new ArrayList<>();
    for (LocationConfig loc : getLocations()) {
      if (loc != null && loc.getSensors() != null) {
        out.addAll(loc.getSensors());
      }
    }
    return out;
  }
}
