package ca.lajthabalazs.pressure_integrity_test.measurement;

import java.math.BigDecimal;

/**
 * Specific gas constant of humid air \(R\).
 *
 * <p>The default unit is {@code Nm/(kg·K)}, matching the ILRT reference implementation.
 */
public class GasConstant extends Measurement {

  public static final String NEWTON_METER_PER_KG_KELVIN = "Nm/(kg*K)";

  public GasConstant(long timeUtc, String sourceId, BigDecimal value) {
    super(timeUtc, sourceId, NEWTON_METER_PER_KG_KELVIN, value);
  }

  @Override
  public Measurement withNewTimestamp(long newTimestamp) {
    return new GasConstant(newTimestamp, getSourceId(), getValueInDefaultUnit());
  }

  @Override
  public Measurement withNewValueInDefaultUnit(BigDecimal newValueInDefaultUnit) {
    return new GasConstant(getTimeUtc(), getSourceId(), newValueInDefaultUnit);
  }
}
