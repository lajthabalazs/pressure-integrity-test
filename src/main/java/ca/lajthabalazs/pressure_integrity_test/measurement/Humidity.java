package ca.lajthabalazs.pressure_integrity_test.measurement;

import java.math.BigDecimal;

public class Humidity extends Measurement {

  private static final String PERCENTAGE = "%";

  public Humidity(
      long timeUtc,
      String sourceId,
      BigDecimal percentage,
      BigDecimal sourceSigma,
      BigDecimal lowerBelievableBound,
      BigDecimal upperBelievableBound) {
    super(
        timeUtc,
        sourceId,
        PERCENTAGE,
        percentage,
        sourceSigma,
        lowerBelievableBound,
        upperBelievableBound);
  }

  @Override
  public Measurement withNewTimestamp(long newTimestamp) {
    return new Humidity(
        newTimestamp,
        getSourceId(),
        getValueInDefaultUnit(),
        getSourceSigma(),
        getLowerBelievableBound(),
        getUpperBelievableBound());
  }
}
