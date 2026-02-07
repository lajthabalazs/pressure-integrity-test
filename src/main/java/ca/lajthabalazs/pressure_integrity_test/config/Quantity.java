package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;

/** A numeric value with a unit (e.g. pressure in bar or Pa). */
@JsonPropertyOrder({"value", "unit"})
public class Quantity {

  private BigDecimal value;
  private String unit;

  public Quantity() {}

  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }
}
