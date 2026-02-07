package ca.lajthabalazs.pressure_integrity_test.measurement;

import java.math.BigDecimal;

public class Humidity extends Measurement {

  private static final String PERCENTAGE = "%";

  public Humidity(long timeUtc, String sourceId, BigDecimal percentage) {
    super(timeUtc, sourceId, PERCENTAGE, percentage);
  }

  @Override
  public Measurement withNewTimestamp(long newTimestamp) {
    return new Humidity(newTimestamp, getSourceId(), getValueInDefaultUnit());
  }
}
