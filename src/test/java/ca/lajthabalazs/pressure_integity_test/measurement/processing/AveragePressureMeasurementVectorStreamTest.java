package ca.lajthabalazs.pressure_integity_test.measurement.processing;

import ca.lajthabalazs.pressure_integity_test.measurement.streaming.TestMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.measurement.Humidity;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.Pressure;
import ca.lajthabalazs.pressure_integrity_test.measurement.Temperature;
import ca.lajthabalazs.pressure_integrity_test.measurement.processing.AveragePressureMeasurementVectorStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link AveragePressureMeasurementVectorStream}. */
public class AveragePressureMeasurementVectorStreamTest {

  private TestMeasurementVectorStream source;
  private List<MeasurementVector> received;

  @BeforeEach
  public void setUp() {
    source = new TestMeasurementVectorStream();
    received = new ArrayList<>();
  }

  /** Single pressure: output contains original plus average. */
  @Test
  public void singlePressure_emittedAsAverage() {
    AveragePressureMeasurementVectorStream stream =
        new AveragePressureMeasurementVectorStream(source);
    stream.subscribe(received::add);

    Pressure p = new Pressure(1000L, "P1", new BigDecimal("101325"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(p)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(2, received.get(0).getMeasurements().size()); // original + average
    Measurement avg =
        received.get(0).getMeasurements().stream()
            .filter(
                m ->
                    AveragePressureMeasurementVectorStream.AVG_PRESSURE_SOURCE_ID.equals(
                        m.getSourceId()))
            .findFirst()
            .orElseThrow();
    Assertions.assertEquals(0, new BigDecimal("101325").compareTo(avg.getValueInDefaultUnit()));
    stream.stop();
  }

  /** Multiple pressure measurements: output contains originals plus average. */
  @Test
  public void multiplePressures_averageEmitted() {
    AveragePressureMeasurementVectorStream stream =
        new AveragePressureMeasurementVectorStream(source);
    stream.subscribe(received::add);

    Pressure p1 = new Pressure(2000L, "P1", new BigDecimal("100000"));
    Pressure p2 = new Pressure(2000L, "P2", new BigDecimal("102000"));
    source.publishToSubscribers(new MeasurementVector(2000L, List.of(p1, p2)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(3, received.get(0).getMeasurements().size()); // P1, P2, average
    Measurement avg =
        received.get(0).getMeasurements().stream()
            .filter(
                m ->
                    AveragePressureMeasurementVectorStream.AVG_PRESSURE_SOURCE_ID.equals(
                        m.getSourceId()))
            .findFirst()
            .orElseThrow();
    // (100000 + 102000) / 2 = 101000
    Assertions.assertEquals(0, new BigDecimal("101000").compareTo(avg.getValueInDefaultUnit()));
    stream.stop();
  }

  /** Vector with no pressure measurements does not publish. */
  @Test
  public void noPressure_nothingPublished() {
    AveragePressureMeasurementVectorStream stream =
        new AveragePressureMeasurementVectorStream(source);
    stream.subscribe(received::add);

    Temperature t = new Temperature(1000L, "T1", new BigDecimal("20"));
    Humidity h = new Humidity(1000L, "H1", new BigDecimal("50"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(t, h)));

    Assertions.assertEquals(0, received.size());
    stream.stop();
  }

  /** Vector with pressure and non-pressure: output contains originals plus average. */
  @Test
  public void pressureAndNonPressure_originalsPlusAverageInOutput() {
    AveragePressureMeasurementVectorStream stream =
        new AveragePressureMeasurementVectorStream(source);
    stream.subscribe(received::add);

    Pressure p = new Pressure(1000L, "P1", new BigDecimal("101325"));
    Temperature t = new Temperature(1000L, "T1", new BigDecimal("20"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(p, t)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(3, received.get(0).getMeasurements().size()); // P1, T1, average
    Measurement avg =
        received.get(0).getMeasurements().stream()
            .filter(
                m ->
                    AveragePressureMeasurementVectorStream.AVG_PRESSURE_SOURCE_ID.equals(
                        m.getSourceId()))
            .findFirst()
            .orElseThrow();
    Assertions.assertTrue(avg instanceof Pressure);
    Assertions.assertEquals(0, new BigDecimal("101325").compareTo(avg.getValueInDefaultUnit()));
    stream.stop();
  }

  /** Timestamp is preserved. */
  @Test
  public void timestampPreserved() {
    AveragePressureMeasurementVectorStream stream =
        new AveragePressureMeasurementVectorStream(source);
    stream.subscribe(received::add);

    long ts = 12345L;
    source.publishToSubscribers(
        new MeasurementVector(ts, List.of(new Pressure(ts, "P1", new BigDecimal("100000")))));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(2, received.get(0).getMeasurements().size());
    Assertions.assertEquals(ts, received.get(0).getTimeUtc());
    stream.stop();
  }

  /** listSensors() delegates to source. */
  @Test
  public void listSensors_delegatesToSource() {
    AveragePressureMeasurementVectorStream stream =
        new AveragePressureMeasurementVectorStream(source);
    Assertions.assertNotNull(stream.listSensors());
    Assertions.assertTrue(stream.listSensors().isEmpty());
    stream.stop();
  }

  /** stop() is idempotent. */
  @Test
  public void stop_idempotent_secondCallDoesNotThrow() {
    AveragePressureMeasurementVectorStream stream =
        new AveragePressureMeasurementVectorStream(source);
    stream.stop();
    Assertions.assertDoesNotThrow(stream::stop);
  }

  /** Pressure measurements with null value are skipped; if all null, nothing published. */
  @Test
  public void allPressureValuesNull_nothingPublished() {
    AveragePressureMeasurementVectorStream stream =
        new AveragePressureMeasurementVectorStream(source);
    stream.subscribe(received::add);

    Pressure pNull = new Pressure(1000L, "P1", null);
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(pNull)));

    Assertions.assertEquals(0, received.size());
    stream.stop();
  }

  /** Mix of null and non-null pressure values: average uses only non-null; originals included. */
  @Test
  public void somePressureValuesNull_averageUsesOnlyNonNull() {
    AveragePressureMeasurementVectorStream stream =
        new AveragePressureMeasurementVectorStream(source);
    stream.subscribe(received::add);

    Pressure p1 = new Pressure(1000L, "P1", null);
    Pressure p2 = new Pressure(1000L, "P2", new BigDecimal("100000"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(p1, p2)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(3, received.get(0).getMeasurements().size());
    Measurement avg =
        received.get(0).getMeasurements().stream()
            .filter(
                m ->
                    AveragePressureMeasurementVectorStream.AVG_PRESSURE_SOURCE_ID.equals(
                        m.getSourceId()))
            .findFirst()
            .orElseThrow();
    Assertions.assertEquals(0, new BigDecimal("100000").compareTo(avg.getValueInDefaultUnit()));
    stream.stop();
  }
}
