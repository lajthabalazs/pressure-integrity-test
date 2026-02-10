package ca.lajthabalazs.pressure_integrity_test.measurement;

import java.math.BigDecimal;

public class Temperature extends Measurement {
  public static final String CELSIUS = "C";
  public static final String KELVIN = "K";
  private static final BigDecimal CELSIUS_TO_KELVIN = new BigDecimal("273.15");

  public Temperature(long timeUtc, String sourceId, BigDecimal celsiusValue) {
    super(timeUtc, sourceId, CELSIUS, celsiusValue);
  }

  public Temperature(long timeUtc, String sourceId, BigDecimal value, String unit) {
    super(timeUtc, sourceId, CELSIUS, convertToCelsius(value, unit));
  }

  private static BigDecimal convertToCelsius(BigDecimal value, String unit) {
    if (unit == null) {
      throw new IllegalArgumentException("Unit cannot be null");
    }
    return switch (unit) {
      case CELSIUS -> value;
      case KELVIN -> value.subtract(CELSIUS_TO_KELVIN);
      default ->
          throw new IllegalArgumentException(
              "Invalid unit: " + unit + ". Valid units are: " + CELSIUS + ", " + KELVIN);
    };
  }

  public BigDecimal getCelsiusValue() {
    return super.getValueInDefaultUnit();
  }

  public BigDecimal getKelvinValue() {
    return super.getValueInDefaultUnit().add(CELSIUS_TO_KELVIN);
  }

  @Override
  public Measurement withNewTimestamp(long newTimestamp) {
    return new Temperature(newTimestamp, getSourceId(), getCelsiusValue());
  }

  @Override
  public Measurement withNewValueInDefaultUnit(java.math.BigDecimal newValueInDefaultUnit) {
    return new Temperature(getTimeUtc(), getSourceId(), newValueInDefaultUnit);
  }
}
