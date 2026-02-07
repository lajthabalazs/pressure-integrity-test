package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;

/** Valid range (min/max) for a sensor, used by temperature and humidity sensors. */
@JsonPropertyOrder({"min", "max"})
public class ValidRange {

  private BigDecimal min;
  private BigDecimal max;

  public ValidRange() {}

  public ValidRange(BigDecimal min, BigDecimal max) {
    this.min = min;
    this.max = max;
  }

  public BigDecimal getMin() {
    return min;
  }

  public void setMin(BigDecimal min) {
    this.min = min;
  }

  public BigDecimal getMax() {
    return max;
  }

  public void setMax(BigDecimal max) {
    this.max = max;
  }
}
