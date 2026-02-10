package ca.lajthabalazs.pressure_integrity_test.measurement;

import java.math.BigDecimal;

public class Pressure extends Measurement {

  public static final String PASCAL = "Pa";
  public static final String BAR = "bar";
  public static final String KILO_PASCAL = "KPa";
  private static final BigDecimal PASCAL_TO_BAR = new BigDecimal("0.00001");
  private static final BigDecimal PASCAL_TO_KILO_PASCAL = new BigDecimal("0.001");
  private static final BigDecimal BAR_TO_PASCAL = new BigDecimal("100000");
  private static final BigDecimal KILO_PASCAL_TO_PASCAL = new BigDecimal("1000");

  public Pressure(long timeUtc, String sourceId, BigDecimal pascalValue) {
    super(timeUtc, sourceId, PASCAL, pascalValue);
  }

  public Pressure(long timeUtc, String sourceId, BigDecimal value, String unit) {
    super(timeUtc, sourceId, PASCAL, convertToPascal(value, unit));
  }

  private static BigDecimal convertToPascal(BigDecimal value, String unit) {
    if (unit == null) {
      throw new IllegalArgumentException("Unit cannot be null");
    }
    return switch (unit) {
      case PASCAL -> value;
      case BAR -> value.multiply(BAR_TO_PASCAL);
      case KILO_PASCAL -> value.multiply(KILO_PASCAL_TO_PASCAL);
      default ->
          throw new IllegalArgumentException(
              "Invalid unit: "
                  + unit
                  + ". Valid units are: "
                  + PASCAL
                  + ", "
                  + BAR
                  + ", "
                  + KILO_PASCAL);
    };
  }

  public BigDecimal getPascalValue() {
    return super.getValueInDefaultUnit();
  }

  public BigDecimal getBarValue() {
    return super.getValueInDefaultUnit().multiply(PASCAL_TO_BAR);
  }

  public BigDecimal getKiloPascalValue() {
    return super.getValueInDefaultUnit().multiply(PASCAL_TO_KILO_PASCAL);
  }

  @Override
  public Measurement withNewTimestamp(long newTimestamp) {
    return new Pressure(newTimestamp, getSourceId(), getPascalValue());
  }

  @Override
  public Measurement withNewValueInDefaultUnit(java.math.BigDecimal newValueInDefaultUnit) {
    return new Pressure(getTimeUtc(), getSourceId(), newValueInDefaultUnit);
  }
}
