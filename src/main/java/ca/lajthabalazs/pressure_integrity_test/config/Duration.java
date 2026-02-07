package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** Duration in hours and minutes. */
@JsonPropertyOrder({"hours", "minutes"})
public class Duration {

  private int hours;
  private int minutes;

  public Duration() {}

  public int getHours() {
    return hours;
  }

  public void setHours(int hours) {
    this.hours = hours;
  }

  public int getMinutes() {
    return minutes;
  }

  public void setMinutes(int minutes) {
    this.minutes = minutes;
  }
}
