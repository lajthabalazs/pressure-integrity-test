package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;

/** Design pressure and leak-rate limits for the integrity test. */
@JsonPropertyOrder({"overpressure_bar", "leakLimit_percent_per_day"})
public class DesignPressure {

  /** Overpressure limit in bar. */
  private BigDecimal overpressure_bar;

  /** Leak limit as percent per day. */
  private BigDecimal leakLimit_percent_per_day;

  public DesignPressure() {}

  public BigDecimal getOverpressure_bar() {
    return overpressure_bar;
  }

  public void setOverpressure_bar(BigDecimal overpressure_bar) {
    this.overpressure_bar = overpressure_bar;
  }

  public BigDecimal getLeakLimit_percent_per_day() {
    return leakLimit_percent_per_day;
  }

  public void setLeakLimit_percent_per_day(BigDecimal leakLimit_percent_per_day) {
    this.leakLimit_percent_per_day = leakLimit_percent_per_day;
  }
}
