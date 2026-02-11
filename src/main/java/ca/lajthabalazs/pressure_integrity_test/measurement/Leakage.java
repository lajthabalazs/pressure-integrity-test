package ca.lajthabalazs.pressure_integrity_test.measurement;

import java.math.BigDecimal;

/**
 * Leakage rate measurement (e.g. from containment integrity test).
 *
 * <p>The default unit is {@code v/v%/d} (volume per volume percent per day), matching the ILRT
 * reference. A value of {@code -1} is used as a sentinel when no leakage can be computed yet (e.g.
 * first measurement).
 */
public class Leakage extends Measurement {

  public static final String V_V_PERCENT_PER_DAY = "v/v%/d";

  public Leakage(long timeUtc, String sourceId, BigDecimal value) {
    super(timeUtc, sourceId, V_V_PERCENT_PER_DAY, value);
  }

  @Override
  public Measurement withNewTimestamp(long newTimestamp) {
    return new Leakage(newTimestamp, getSourceId(), getValueInDefaultUnit());
  }

  @Override
  public Measurement withNewValueInDefaultUnit(BigDecimal newValueInDefaultUnit) {
    return new Leakage(getTimeUtc(), getSourceId(), newValueInDefaultUnit);
  }
}
