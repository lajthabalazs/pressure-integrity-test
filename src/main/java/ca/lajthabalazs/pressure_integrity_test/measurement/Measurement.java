package ca.lajthabalazs.pressure_integrity_test.measurement;

import java.math.BigDecimal;

public abstract class Measurement {
  private final long timeUtc;
  private final String sourceId;
  private final String defaultUnit;
  private final BigDecimal valueInDefaultUnit;

  protected Measurement(
      long timeUtc, String sourceId, String defaultUnit, BigDecimal valueInDefaultUnit) {
    this.timeUtc = timeUtc;
    this.sourceId = sourceId;
    this.defaultUnit = defaultUnit;
    this.valueInDefaultUnit = valueInDefaultUnit;
  }

  public long getTimeUtc() {
    return timeUtc;
  }

  public String getSourceId() {
    return sourceId;
  }

  public String getDefaultUnit() {
    return defaultUnit;
  }

  public BigDecimal getValueInDefaultUnit() {
    return valueInDefaultUnit;
  }

  /**
   * Creates a copy of this measurement with a new timestamp, preserving all other properties.
   *
   * @param newTimestamp the new timestamp in milliseconds since epoch
   * @return a new measurement instance with the adjusted timestamp
   */
  public abstract Measurement withNewTimestamp(long newTimestamp);

  @Override
  public String toString() {
    return String.format("%s%s", valueInDefaultUnit, defaultUnit);
  }
}
