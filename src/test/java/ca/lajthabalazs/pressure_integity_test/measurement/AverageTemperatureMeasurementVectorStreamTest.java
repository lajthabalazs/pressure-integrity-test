package ca.lajthabalazs.pressure_integity_test.measurement;

import ca.lajthabalazs.pressure_integrity_test.config.LocationConfig;
import ca.lajthabalazs.pressure_integrity_test.measurement.AveragePressureMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.measurement.AverageTemperatureMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.measurement.Humidity;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.Pressure;
import ca.lajthabalazs.pressure_integrity_test.measurement.Temperature;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link AverageTemperatureMeasurementVectorStream}. */
public class AverageTemperatureMeasurementVectorStreamTest {

  private TestMeasurementVectorStream source;
  private List<MeasurementVector> received;
  private Map<String, LocationConfig> locationsBySensorId;

  @BeforeEach
  public void setUp() {
    source = new TestMeasurementVectorStream();
    received = new ArrayList<>();
    locationsBySensorId = new HashMap<>();
  }

  private LocationConfig location(String id, String volumeFactor) {
    LocationConfig loc = new LocationConfig();
    loc.setId(id);
    loc.setVolumeFactor(volumeFactor != null ? new BigDecimal(volumeFactor) : null);
    return loc;
  }

  private AverageTemperatureMeasurementVectorStream newStream() {
    return new AverageTemperatureMeasurementVectorStream(source, locationsBySensorId);
  }

  /** Single credible temperature: output contains original plus average (same value). */
  @Test
  public void singleTemperature_emittedAsAverage() {
    locationsBySensorId.put("T1", location("L1", "1.0"));
    AverageTemperatureMeasurementVectorStream stream = newStream();
    stream.subscribe(received::add);

    Temperature t = new Temperature(1000L, "T1", new BigDecimal("20.0"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(t)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(2, received.getFirst().getMeasurements().size()); // original + average
    Measurement avg =
        received.getFirst().getMeasurements().stream()
            .filter(
                m ->
                    AverageTemperatureMeasurementVectorStream.AVG_TEMPERATURE_SOURCE_ID.equals(
                        m.getSourceId()))
            .findFirst()
            .orElseThrow();
    Assertions.assertInstanceOf(Temperature.class, avg);
    Assertions.assertEquals(0, new BigDecimal("20.0").compareTo(avg.getValueInDefaultUnit()));
    stream.stop();
  }

  /**
   * Multiple temperatures with different volume factors: average is volume‑weighted harmonic mean,
   * and all originals are preserved.
   */
  @Test
  public void multipleTemperatures_volumeWeightedHarmonicMeanEmitted() {
    // Two sensors, different volume factors
    locationsBySensorId.put("T1", location("L1", "2.0"));
    locationsBySensorId.put("T2", location("L2", "1.0"));

    AverageTemperatureMeasurementVectorStream stream = newStream();
    stream.subscribe(received::add);

    // Values in °C
    Temperature t1 = new Temperature(2000L, "T1", new BigDecimal("20.0"));
    Temperature t2 = new Temperature(2000L, "T2", new BigDecimal("40.0"));

    source.publishToSubscribers(new MeasurementVector(2000L, List.of(t1, t2)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(3, received.getFirst().getMeasurements().size()); // T1, T2, average

    Measurement avg =
        received.getFirst().getMeasurements().stream()
            .filter(
                m ->
                    AverageTemperatureMeasurementVectorStream.AVG_TEMPERATURE_SOURCE_ID.equals(
                        m.getSourceId()))
            .findFirst()
            .orElseThrow();

    // Harmonic mean: Vsum = 2 + 1 = 3; denom = 2/20 + 1/40 = 0.1 + 0.025 = 0.125; Tavg = 3 / 0.125
    // = 24
    Assertions.assertEquals(0, new BigDecimal("24.0").compareTo(avg.getValueInDefaultUnit()));
    stream.stop();
  }

  /** Temperatures outside [0, 100] °C are ignored; if all out of range, nothing is published. */
  @Test
  public void allTemperaturesOutOfRange_nothingPublished() {
    locationsBySensorId.put("T1", location("L1", "1.0"));
    AverageTemperatureMeasurementVectorStream stream = newStream();
    stream.subscribe(received::add);

    Temperature tLow = new Temperature(1000L, "T1", new BigDecimal("-5.0"));
    Temperature tHigh = new Temperature(1000L, "T1", new BigDecimal("150.0"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(tLow, tHigh)));

    Assertions.assertEquals(0, received.size());
    stream.stop();
  }

  /** Mix of in-range and out-of-range temperatures: only in-range participate in the average. */
  @Test
  public void someTemperaturesOutOfRange_onlyInRangeUsedForAverage() {
    locationsBySensorId.put("T1", location("L1", "1.0"));
    locationsBySensorId.put("T2", location("L2", "1.0"));
    AverageTemperatureMeasurementVectorStream stream = newStream();
    stream.subscribe(received::add);

    Temperature tLow = new Temperature(1000L, "T1", new BigDecimal("-5.0")); // ignored
    Temperature tOk = new Temperature(1000L, "T2", new BigDecimal("25.0")); // used
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(tLow, tOk)));

    Assertions.assertEquals(1, received.size());
    Measurement avg =
        received.getFirst().getMeasurements().stream()
            .filter(
                m ->
                    AverageTemperatureMeasurementVectorStream.AVG_TEMPERATURE_SOURCE_ID.equals(
                        m.getSourceId()))
            .findFirst()
            .orElseThrow();
    Assertions.assertEquals(0, new BigDecimal("25.0").compareTo(avg.getValueInDefaultUnit()));
    stream.stop();
  }

  /** Vector with no temperatures does not publish anything. */
  @Test
  public void noTemperature_nothingPublished() {
    AverageTemperatureMeasurementVectorStream stream = newStream();
    stream.subscribe(received::add);

    Pressure p = new Pressure(1000L, "P1", new BigDecimal("101325"));
    Humidity h = new Humidity(1000L, "H1", new BigDecimal("50"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(p, h)));

    Assertions.assertEquals(0, received.size());
    stream.stop();
  }

  /**
   * Missing location or null volume factor falls back to weight 1, ensuring the stream still works
   * without full config.
   */
  @Test
  public void missingLocationOrVolumeFactor_fallsBackToUnitWeight() {
    // T1 has no entry; T2 has location with null volumeFactor
    locationsBySensorId.put("T2", location("L2", null));
    AverageTemperatureMeasurementVectorStream stream = newStream();
    stream.subscribe(received::add);

    Temperature t1 = new Temperature(1000L, "T1", new BigDecimal("10.0"));
    Temperature t2 = new Temperature(1000L, "T2", new BigDecimal("30.0"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(t1, t2)));

    Measurement avg =
        received.getFirst().getMeasurements().stream()
            .filter(
                m ->
                    AverageTemperatureMeasurementVectorStream.AVG_TEMPERATURE_SOURCE_ID.equals(
                        m.getSourceId()))
            .findFirst()
            .orElseThrow();

    // Both default to weight 1: harmonic mean of 10 and 30 => Vsum=2, denom=1/10+1/30=0.1333...,
    // Tavg=15
    Assertions.assertEquals(0, new BigDecimal("15.0").compareTo(avg.getValueInDefaultUnit()));
    stream.stop();
  }

  /** Timestamp of the vector is preserved. */
  @Test
  public void timestampPreserved() {
    locationsBySensorId.put("T1", location("L1", "1.0"));
    AverageTemperatureMeasurementVectorStream stream = newStream();
    stream.subscribe(received::add);

    long ts = 12345L;
    source.publishToSubscribers(
        new MeasurementVector(ts, List.of(new Temperature(ts, "T1", new BigDecimal("20.0")))));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(2, received.getFirst().getMeasurements().size());
    Assertions.assertEquals(ts, received.getFirst().getTimeUtc());
    stream.stop();
  }

  /** listSensors() delegates to source. */
  @Test
  public void listSensors_delegatesToSource() {
    AverageTemperatureMeasurementVectorStream stream = newStream();
    Assertions.assertNotNull(stream.listSensors());
    Assertions.assertTrue(stream.listSensors().isEmpty());
    stream.stop();
  }

  /** Constructor handles null location map by falling back to an empty map. */
  @Test
  public void constructor_nullLocationMapHandledAsEmpty() {
    AverageTemperatureMeasurementVectorStream stream =
        new AverageTemperatureMeasurementVectorStream(source, null);
    Assertions.assertNotNull(stream.listSensors());
    Assertions.assertTrue(stream.listSensors().isEmpty());
    stream.stop();
  }

  /** stop() is idempotent. */
  @Test
  public void stop_idempotent_secondCallDoesNotThrow() {
    AverageTemperatureMeasurementVectorStream stream = newStream();
    stream.stop();
    Assertions.assertDoesNotThrow(stream::stop);
  }

  /** All temperature values null: treated as unusable, so nothing is published. */
  @Test
  public void allTemperatureValuesNull_nothingPublished() {
    locationsBySensorId.put("T1", location("L1", "1.0"));
    AverageTemperatureMeasurementVectorStream stream = newStream();
    stream.subscribe(received::add);

    Temperature tNull = new Temperature(1000L, "T1", null);
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(tNull)));

    Assertions.assertEquals(0, received.size());
    stream.stop();
  }

  /**
   * Zero-degree temperature within valid range leads to vSum &gt; 0 but denom == 0, so result is
   * not published.
   */
  @Test
  public void zeroDegreeTemperature_denominatorZero_noAveragePublished() {
    locationsBySensorId.put("T1", location("L1", "1.0"));
    AverageTemperatureMeasurementVectorStream stream = newStream();
    stream.subscribe(received::add);

    // 0 °C is within [0, 100], but contributes nothing to the denominator; vSum &gt; 0, denom == 0
    Temperature tZero = new Temperature(1000L, "T1", new BigDecimal("0.0"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(tZero)));

    Assertions.assertEquals(0, received.size());
    stream.stop();
  }

  /**
   * The average temperature stream works correctly when its source is the average-pressure stream
   * output, i.e. it ignores non-temperature measurements and synthetic average pressure.
   */
  @Test
  public void worksOnAveragePressureStreamOutput() {
    locationsBySensorId.put("T1", location("L1", "1.0"));
    locationsBySensorId.put("T2", location("L2", "1.0"));

    // Wrap source with average pressure first, then with average temperature
    AveragePressureMeasurementVectorStream avgPressureStream =
        new AveragePressureMeasurementVectorStream(source);
    AverageTemperatureMeasurementVectorStream avgTempStream =
        new AverageTemperatureMeasurementVectorStream(avgPressureStream, locationsBySensorId);

    avgTempStream.subscribe(received::add);

    Pressure p1 = new Pressure(1000L, "P1", new BigDecimal("100000"));
    Pressure p2 = new Pressure(1000L, "P2", new BigDecimal("102000"));
    Temperature t1 = new Temperature(1000L, "T1", new BigDecimal("20.0"));
    Temperature t2 = new Temperature(1000L, "T2", new BigDecimal("40.0"));

    // Publish into the base source; avgPressureStream will add AVG_PRESSURE, then avgTempStream
    // will add AVG_TEMPERATURE, ignoring pressures.
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(p1, p2, t1, t2)));

    Assertions.assertEquals(1, received.size());
    MeasurementVector out = received.getFirst();

    // Expect original 4 + 1 avg pressure + 1 avg temperature = 6 measurements
    Assertions.assertEquals(6, out.getMeasurements().size());

    Measurement avgTemp =
        out.getMeasurements().stream()
            .filter(
                m ->
                    AverageTemperatureMeasurementVectorStream.AVG_TEMPERATURE_SOURCE_ID.equals(
                        m.getSourceId()))
            .findFirst()
            .orElseThrow();

    // Harmonic mean of 20 and 40 with equal weights => 26.666..., check with reasonable precision
    BigDecimal expected =
        new BigDecimal("26.6666666667"); // matches 2 / (1/20 + 1/40) to 10 decimal places
    BigDecimal actual = avgTemp.getValueInDefaultUnit();

    Assertions.assertEquals(expected.floatValue(), actual.floatValue(), 0.001f);

    avgTempStream.stop();
    avgPressureStream.stop();
  }
}
