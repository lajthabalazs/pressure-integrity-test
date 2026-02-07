package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;

/** Design pressure and leak-rate limits for the integrity test. */
@JsonPropertyOrder({"overpressure_Pa", "leakLimit_percent_per_day"})
public class DesignPressure {

  /** Overpressure limit in Pascals. */
  private BigDecimal overpressure_Pa;

  /** Leak limit as percent per day. */
  private BigDecimal leakLimit_percent_per_day;

  public DesignPressure() {}

  public DesignPressure(BigDecimal overpressure_Pa, BigDecimal leakLimit_percent_per_day) {
    this.overpressure_Pa = overpressure_Pa;
    this.leakLimit_percent_per_day = leakLimit_percent_per_day;
  }

  public BigDecimal getOverpressure_Pa() {
    return overpressure_Pa;
  }

  public void setOverpressure_Pa(BigDecimal overpressure_Pa) {
    this.overpressure_Pa = overpressure_Pa;
  }

  public BigDecimal getLeakLimit_percent_per_day() {
    return leakLimit_percent_per_day;
  }

  public void setLeakLimit_percent_per_day(BigDecimal leakLimit_percent_per_day) {
    this.leakLimit_percent_per_day = leakLimit_percent_per_day;
  }
}
