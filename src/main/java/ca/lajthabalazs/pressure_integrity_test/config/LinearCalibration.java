package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;

/**
 * Linear calibration for a sensor: real = A * measured + B.
 *
 * <p>A is a scalar; B is in the same unit as the measured value (e.g. °C for thermometers).
 */
@JsonPropertyOrder({"A", "B"})
public class LinearCalibration {

  private BigDecimal A;
  private BigDecimal B;

  public LinearCalibration() {}

  @JsonProperty("A")
  public BigDecimal getA() {
    return A;
  }

  @JsonProperty("A")
  public void setA(BigDecimal A) {
    this.A = A;
  }

  @JsonProperty("B")
  public BigDecimal getB() {
    return B;
  }

  @JsonProperty("B")
  public void setB(BigDecimal B) {
    this.B = B;
  }

  /**
   * Returns the calibrated value: A * measuredValue + B. Uses the same scale as the input for the
   * result. If measuredValue is null, returns null. If A is null it is treated as 1; if B is null
   * it is treated as 0.
   *
   * @param measuredValue the raw measured value (in the sensor's default unit)
   * @return the calibrated value in the same unit
   */
  public BigDecimal getCalibratedValue(BigDecimal measuredValue) {
    if (measuredValue == null) {
      return null;
    }
    BigDecimal a = A != null ? A : BigDecimal.ONE;
    BigDecimal b = B != null ? B : BigDecimal.ZERO;
    return a.multiply(measuredValue).add(b);
  }
}
