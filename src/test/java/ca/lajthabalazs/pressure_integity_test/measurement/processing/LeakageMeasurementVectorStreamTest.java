package ca.lajthabalazs.pressure_integity_test.measurement.processing;

import ca.lajthabalazs.pressure_integity_test.measurement.streaming.TestMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.measurement.GasConstant;
import ca.lajthabalazs.pressure_integrity_test.measurement.Leakage;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.Pressure;
import ca.lajthabalazs.pressure_integrity_test.measurement.Temperature;
import ca.lajthabalazs.pressure_integrity_test.measurement.processing.AverageGasConstantMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.measurement.processing.AveragePressureMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.measurement.processing.AverageTemperatureMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.measurement.processing.LeakageMeasurementVectorStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link LeakageMeasurementVectorStream}. */
public class LeakageMeasurementVectorStreamTest {

  private TestMeasurementVectorStream source;
  private List<MeasurementVector> received;

  /** Builds a vector with avg pressure (Pa), avg temp (°C), and avg R for leakage calculation. */
  private static MeasurementVector vectorWithPTR(
      long timeUtc, BigDecimal pressurePa, BigDecimal tempCelsius, BigDecimal r) {
    List<Measurement> m = new ArrayList<>();
    m.add(
        new Pressure(
            timeUtc, AveragePressureMeasurementVectorStream.AVG_PRESSURE_SOURCE_ID, pressurePa));
    m.add(
        new Temperature(
            timeUtc,
            AverageTemperatureMeasurementVectorStream.AVG_TEMPERATURE_SOURCE_ID,
            tempCelsius));
    m.add(new GasConstant(timeUtc, AverageGasConstantMeasurementVectorStream.AVG_R_SOURCE_ID, r));
    return new MeasurementVector(timeUtc, m);
  }

  @BeforeEach
  public void setUp() {
    source = new TestMeasurementVectorStream();
    received = new ArrayList<>();
  }

  @Test
  public void firstVector_emitsLeakageMinusOne() {
    LeakageMeasurementVectorStream stream = new LeakageMeasurementVectorStream(source);
    stream.subscribe(received::add);

    // P=101325 Pa, T=20°C (293.15 K), R=287 Nm/(kg·K) → rho > RHO_INF
    source.publishToSubscribers(
        vectorWithPTR(
            1000L, new BigDecimal("101325"), new BigDecimal("20"), new BigDecimal("287")));

    Assertions.assertEquals(1, received.size());
    Measurement leakage =
        received.get(0).getMeasurements().stream()
            .filter(m -> m instanceof Leakage)
            .findFirst()
            .orElseThrow();
    Assertions.assertEquals(
        LeakageMeasurementVectorStream.LEAKAGE_SOURCE_ID, leakage.getSourceId());
    Assertions.assertEquals(0, new BigDecimal("-1").compareTo(leakage.getValueInDefaultUnit()));
    stream.stop();
  }

  @Test
  public void secondVector_emitsDiffBasedLeakage() {
    LeakageMeasurementVectorStream stream = new LeakageMeasurementVectorStream(source);
    stream.subscribe(received::add);

    long t0 = 0L;
    long t1 = 3600_000L; // 1 hour later
    source.publishToSubscribers(
        vectorWithPTR(t0, new BigDecimal("101325"), new BigDecimal("20"), new BigDecimal("287")));
    source.publishToSubscribers(
        vectorWithPTR(t1, new BigDecimal("101000"), new BigDecimal("20"), new BigDecimal("287")));

    Assertions.assertEquals(2, received.size());
    // First already asserted as -1 in firstVector_emitsLeakageMinusOne
    Measurement leakage1 =
        received.get(0).getMeasurements().stream()
            .filter(m -> m instanceof Leakage)
            .findFirst()
            .orElseThrow();
    Assertions.assertEquals(0, new BigDecimal("-1").compareTo(leakage1.getValueInDefaultUnit()));

    Measurement leakage2 =
        received.get(1).getMeasurements().stream()
            .filter(m -> m instanceof Leakage)
            .findFirst()
            .orElseThrow();
    Assertions.assertEquals(
        LeakageMeasurementVectorStream.LEAKAGE_SOURCE_ID, leakage2.getSourceId());
    BigDecimal value2 = leakage2.getValueInDefaultUnit();
    Assertions.assertNotNull(value2);
    Assertions.assertTrue(
        value2.compareTo(BigDecimal.ONE.negate()) != 0,
        "Second leakage should be computed from diff, not -1");
    Assertions.assertTrue(value2.scale() >= 0, "Leakage value should be numeric");
    stream.stop();
  }

  @Test
  public void vectorWithoutAvgPressure_nothingPublished() {
    LeakageMeasurementVectorStream stream = new LeakageMeasurementVectorStream(source);
    stream.subscribe(received::add);

    List<Measurement> m = new ArrayList<>();
    m.add(
        new Temperature(
            1000L,
            AverageTemperatureMeasurementVectorStream.AVG_TEMPERATURE_SOURCE_ID,
            new BigDecimal("20")));
    m.add(
        new GasConstant(
            1000L,
            AverageGasConstantMeasurementVectorStream.AVG_R_SOURCE_ID,
            new BigDecimal("287")));
    source.publishToSubscribers(new MeasurementVector(1000L, m));

    Assertions.assertEquals(0, received.size());
    stream.stop();
  }

  @Test
  public void vectorWithoutAvgTemperature_nothingPublished() {
    LeakageMeasurementVectorStream stream = new LeakageMeasurementVectorStream(source);
    stream.subscribe(received::add);

    List<Measurement> m = new ArrayList<>();
    m.add(
        new Pressure(
            1000L,
            AveragePressureMeasurementVectorStream.AVG_PRESSURE_SOURCE_ID,
            new BigDecimal("101325")));
    m.add(
        new GasConstant(
            1000L,
            AverageGasConstantMeasurementVectorStream.AVG_R_SOURCE_ID,
            new BigDecimal("287")));
    source.publishToSubscribers(new MeasurementVector(1000L, m));

    Assertions.assertEquals(0, received.size());
    stream.stop();
  }

  @Test
  public void vectorWithoutAvgR_nothingPublished() {
    LeakageMeasurementVectorStream stream = new LeakageMeasurementVectorStream(source);
    stream.subscribe(received::add);

    List<Measurement> m = new ArrayList<>();
    m.add(
        new Pressure(
            1000L,
            AveragePressureMeasurementVectorStream.AVG_PRESSURE_SOURCE_ID,
            new BigDecimal("101325")));
    m.add(
        new Temperature(
            1000L,
            AverageTemperatureMeasurementVectorStream.AVG_TEMPERATURE_SOURCE_ID,
            new BigDecimal("20")));
    source.publishToSubscribers(new MeasurementVector(1000L, m));

    Assertions.assertEquals(0, received.size());
    stream.stop();
  }

  @Test
  public void timestampPreserved() {
    LeakageMeasurementVectorStream stream = new LeakageMeasurementVectorStream(source);
    stream.subscribe(received::add);

    long ts = 12345L;
    source.publishToSubscribers(
        vectorWithPTR(ts, new BigDecimal("101325"), new BigDecimal("20"), new BigDecimal("287")));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(ts, received.get(0).getTimeUtc());
    stream.stop();
  }

  @Test
  public void listSensors_delegatesToSource() {
    LeakageMeasurementVectorStream stream = new LeakageMeasurementVectorStream(source);
    Assertions.assertNotNull(stream.listSensors());
    Assertions.assertTrue(stream.listSensors().isEmpty());
    stream.stop();
  }

  @Test
  public void stop_idempotent_secondCallDoesNotThrow() {
    LeakageMeasurementVectorStream stream = new LeakageMeasurementVectorStream(source);
    stream.stop();
    Assertions.assertDoesNotThrow(stream::stop);
  }

  @Test
  public void outputContainsAllOriginalMeasurementsPlusLeakage() {
    LeakageMeasurementVectorStream stream = new LeakageMeasurementVectorStream(source);
    stream.subscribe(received::add);

    source.publishToSubscribers(
        vectorWithPTR(
            1000L, new BigDecimal("101325"), new BigDecimal("20"), new BigDecimal("287")));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(4, received.get(0).getMeasurements().size()); // P, T, R, Leakage
    Assertions.assertTrue(
        received.get(0).getMeasurements().stream().anyMatch(m -> m instanceof Leakage));
    stream.stop();
  }

  /**
   * When the second vector has the same or earlier timestamp, dtSec <= 0 so it is not published.
   */
  @Test
  public void sameTimestampForSecondVector_secondNotPublished() {
    LeakageMeasurementVectorStream stream = new LeakageMeasurementVectorStream(source);
    stream.subscribe(received::add);

    long t = 1000L;
    source.publishToSubscribers(
        vectorWithPTR(t, new BigDecimal("101325"), new BigDecimal("20"), new BigDecimal("287")));
    source.publishToSubscribers(
        vectorWithPTR(t, new BigDecimal("101000"), new BigDecimal("20"), new BigDecimal("287")));

    Assertions.assertEquals(
        1, received.size(), "Second vector with same timestamp should not publish");
    stream.stop();
  }

  /** When density ρ = P/(R·T) is not greater than ρ∞, the vector is not published. */
  @Test
  public void densityBelowRhoInf_nothingPublished() {
    LeakageMeasurementVectorStream stream = new LeakageMeasurementVectorStream(source);
    stream.subscribe(received::add);

    // Low P so that rho <= 1.16144 kg/m³ (e.g. P≈97000, T=20°C, R=287 → rho ≈ 1.155)
    source.publishToSubscribers(
        vectorWithPTR(1000L, new BigDecimal("97000"), new BigDecimal("20"), new BigDecimal("287")));

    Assertions.assertEquals(0, received.size());
    stream.stop();
  }

  /** When average pressure is zero or negative, density is invalid so nothing is published. */
  @Test
  public void zeroPressure_nothingPublished() {
    LeakageMeasurementVectorStream stream = new LeakageMeasurementVectorStream(source);
    stream.subscribe(received::add);

    source.publishToSubscribers(
        vectorWithPTR(1000L, BigDecimal.ZERO, new BigDecimal("20"), new BigDecimal("287")));

    Assertions.assertEquals(0, received.size());
    stream.stop();
  }

  /**
   * When average temperature is zero or negative (in K), density is invalid so nothing is
   * published.
   */
  @Test
  public void zeroTemperature_nothingPublished() {
    LeakageMeasurementVectorStream stream = new LeakageMeasurementVectorStream(source);
    stream.subscribe(received::add);

    // 0°C = 273.15 K; use -300°C so Kelvin is negative (invalid)
    source.publishToSubscribers(
        vectorWithPTR(
            1000L, new BigDecimal("101325"), new BigDecimal("-300"), new BigDecimal("287")));

    Assertions.assertEquals(0, received.size());
    stream.stop();
  }

  /** When average R is zero or negative, density is invalid so nothing is published. */
  @Test
  public void zeroR_nothingPublished() {
    LeakageMeasurementVectorStream stream = new LeakageMeasurementVectorStream(source);
    stream.subscribe(received::add);

    source.publishToSubscribers(
        vectorWithPTR(1000L, new BigDecimal("101325"), new BigDecimal("20"), BigDecimal.ZERO));

    Assertions.assertEquals(0, received.size());
    stream.stop();
  }

  /**
   * Vector that also contains other Pressure/Temperature/GasConstant (wrong source ids) still uses
   * avg P, T, R.
   */
  @Test
  public void vectorWithExtraMeasurements_usesAvgIdsOnly() {
    LeakageMeasurementVectorStream stream = new LeakageMeasurementVectorStream(source);
    stream.subscribe(received::add);

    List<Measurement> m = new ArrayList<>();
    m.add(new Pressure(1000L, "P1", new BigDecimal("99999")));
    m.add(
        new Pressure(
            1000L,
            AveragePressureMeasurementVectorStream.AVG_PRESSURE_SOURCE_ID,
            new BigDecimal("101325")));
    m.add(new Temperature(1000L, "T1", new BigDecimal("15")));
    m.add(
        new Temperature(
            1000L,
            AverageTemperatureMeasurementVectorStream.AVG_TEMPERATURE_SOURCE_ID,
            new BigDecimal("20")));
    m.add(new GasConstant(1000L, "R1", new BigDecimal("280")));
    m.add(
        new GasConstant(
            1000L,
            AverageGasConstantMeasurementVectorStream.AVG_R_SOURCE_ID,
            new BigDecimal("287")));
    source.publishToSubscribers(new MeasurementVector(1000L, m));

    Assertions.assertEquals(1, received.size());
    Measurement leakage =
        received.get(0).getMeasurements().stream()
            .filter(Leakage.class::isInstance)
            .findFirst()
            .orElseThrow();
    Assertions.assertEquals(0, new BigDecimal("-1").compareTo(leakage.getValueInDefaultUnit()));
    stream.stop();
  }
}
