package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/** Test configuration: id, type, stages, ambient pressure, and description. */
@JsonPropertyOrder({"id", "type", "stages", "ambientPressure", "description"})
public class TestConfig {

  private String id;
  private TestType type;
  private List<TestConfigStage> stages = List.of();
  private Quantity ambientPressure;
  private String description;

  public TestConfig() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public TestType getType() {
    return type;
  }

  public void setType(TestType type) {
    this.type = type;
  }

  public List<TestConfigStage> getStages() {
    return stages;
  }

  public void setStages(List<TestConfigStage> stages) {
    this.stages = stages;
  }

  public Quantity getAmbientPressure() {
    return ambientPressure;
  }

  public void setAmbientPressure(Quantity ambientPressure) {
    this.ambientPressure = ambientPressure;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
