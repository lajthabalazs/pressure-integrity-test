package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** A single stage of a test: overpressure target and duration. */
@JsonPropertyOrder({"overpressure", "duration"})
public class TestConfigStage {

  private Quantity overpressure;
  private Duration duration;

  public TestConfigStage() {}

  public Quantity getOverpressure() {
    return overpressure;
  }

  public void setOverpressure(Quantity overpressure) {
    this.overpressure = overpressure;
  }

  public Duration getDuration() {
    return duration;
  }

  public void setDuration(Duration duration) {
    this.duration = duration;
  }
}
