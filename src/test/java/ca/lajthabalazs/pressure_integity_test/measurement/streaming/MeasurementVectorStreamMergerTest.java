package ca.lajthabalazs.pressure_integity_test.measurement.streaming;

import ca.lajthabalazs.pressure_integrity_test.measurement.Humidity;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.Pressure;
import ca.lajthabalazs.pressure_integrity_test.measurement.streaming.MeasurementVectorStreamMerger;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link MeasurementVectorStreamMerger} to verify merging behaviour. */
public class MeasurementVectorStreamMergerTest {

  private TestMeasurementVectorStream timingStream;
  private TestMeasurementVectorStream dataStream;
  private MeasurementVectorStreamMerger merger;
  private List<MeasurementVector> received;

  @BeforeEach
  public void setUp() {
    timingStream = new TestMeasurementVectorStream();
    dataStream = new TestMeasurementVectorStream();
    received = new ArrayList<>();
  }

  @AfterEach
  public void tearDown() {
    if (merger != null) {
      merger.stop();
    }
  }

  /**
   * When timing fires before any data, merged vector has timing timestamp and empty measurements.
   */
  @Test
  public void timingBeforeData_publishesEmptyMeasurementsWithTimingTimestamp() {
    merger = new MeasurementVectorStreamMerger(timingStream, dataStream);
    merger.subscribe(received::add);

    long timingTime = 5000L;
    timingStream.publishToSubscribers(new MeasurementVector(timingTime, List.of()));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(timingTime, received.get(0).getTimeUtc());
    Assertions.assertTrue(received.get(0).getMeasurements().isEmpty());
  }

  /** Data then timing: merged vector contains data measurements with timing timestamp. */
  @Test
  public void dataThenTiming_publishesLatestDataWithTimingTimestamp() {
    merger = new MeasurementVectorStreamMerger(timingStream, dataStream);
    merger.subscribe(received::add);

    Humidity h1 = new Humidity(1000L, "H1", new BigDecimal("50"));
    dataStream.publishToSubscribers(new MeasurementVector(1000L, List.of(h1)));

    long timingTime = 2000L;
    timingStream.publishToSubscribers(new MeasurementVector(timingTime, List.of()));

    Assertions.assertEquals(1, received.size());
    MeasurementVector merged = received.get(0);
    Assertions.assertEquals(timingTime, merged.getTimeUtc());
    Assertions.assertEquals(1, merged.getMeasurements().size());
    Assertions.assertEquals("H1", merged.getMeasurements().get(0).getSourceId());
    Assertions.assertEquals(
        0, new BigDecimal("50").compareTo(merged.getMeasurements().get(0).getValueInDefaultUnit()));
    Assertions.assertEquals(timingTime, merged.getMeasurements().get(0).getTimeUtc());
  }

  /** Multiple sensors in data: merged contains all, sorted by sourceId, with timing timestamp. */
  @Test
  public void multipleSensors_mergedSortedBySourceIdWithTimingTimestamp() {
    merger = new MeasurementVectorStreamMerger(timingStream, dataStream);
    merger.subscribe(received::add);

    Humidity h = new Humidity(1000L, "H1", new BigDecimal("50"));
    Pressure p = new Pressure(2000L, "P1", new BigDecimal("101325"));
    dataStream.publishToSubscribers(new MeasurementVector(1500L, List.of(h, p)));

    long timingTime = 3000L;
    timingStream.publishToSubscribers(new MeasurementVector(timingTime, List.of()));

    Assertions.assertEquals(1, received.size());
    MeasurementVector merged = received.get(0);
    Assertions.assertEquals(timingTime, merged.getTimeUtc());
    Assertions.assertEquals(2, merged.getMeasurements().size());
    // Sorted by sourceId: H1 before P1
    Assertions.assertEquals("H1", merged.getMeasurements().get(0).getSourceId());
    Assertions.assertEquals("P1", merged.getMeasurements().get(1).getSourceId());
    Assertions.assertEquals(timingTime, merged.getMeasurements().get(0).getTimeUtc());
    Assertions.assertEquals(timingTime, merged.getMeasurements().get(1).getTimeUtc());
  }

  /** When data updates (same sensor), next timing event gets the latest value. */
  @Test
  public void dataUpdated_thenTiming_publishesLatestValuePerSensor() {
    merger = new MeasurementVectorStreamMerger(timingStream, dataStream);
    merger.subscribe(received::add);

    dataStream.publishToSubscribers(
        new MeasurementVector(1000L, List.of(new Humidity(1000L, "H1", new BigDecimal("50")))));
    dataStream.publishToSubscribers(
        new MeasurementVector(2000L, List.of(new Humidity(2000L, "H1", new BigDecimal("60")))));

    timingStream.publishToSubscribers(new MeasurementVector(2500L, List.of()));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(1, received.get(0).getMeasurements().size());
    Assertions.assertEquals(
        0,
        new BigDecimal("60")
            .compareTo(received.get(0).getMeasurements().get(0).getValueInDefaultUnit()),
        "Should have latest value 60, not 50");
    Assertions.assertEquals(2500L, received.get(0).getMeasurements().get(0).getTimeUtc());
  }

  /** Multiple timing events each receive current latest from data. */
  @Test
  public void multipleTimingEvents_eachGetsCurrentLatestFromData() {
    merger = new MeasurementVectorStreamMerger(timingStream, dataStream);
    merger.subscribe(received::add);

    timingStream.publishToSubscribers(new MeasurementVector(1000L, List.of()));
    Assertions.assertEquals(1, received.size());
    Assertions.assertTrue(received.get(0).getMeasurements().isEmpty());

    dataStream.publishToSubscribers(
        new MeasurementVector(1500L, List.of(new Humidity(1500L, "H1", new BigDecimal("40")))));
    timingStream.publishToSubscribers(new MeasurementVector(2000L, List.of()));
    Assertions.assertEquals(2, received.size());
    Assertions.assertEquals(1, received.get(1).getMeasurements().size());
    Assertions.assertEquals(
        0,
        new BigDecimal("40")
            .compareTo(received.get(1).getMeasurements().get(0).getValueInDefaultUnit()));

    dataStream.publishToSubscribers(
        new MeasurementVector(2500L, List.of(new Humidity(2500L, "H1", new BigDecimal("55")))));
    timingStream.publishToSubscribers(new MeasurementVector(3000L, List.of()));
    Assertions.assertEquals(3, received.size());
    Assertions.assertEquals(
        0,
        new BigDecimal("55")
            .compareTo(received.get(2).getMeasurements().get(0).getValueInDefaultUnit()));
  }

  /** After stop(), no more merged vectors are published. */
  @Test
  public void afterStop_noMoreMergedVectorsPublished() {
    merger = new MeasurementVectorStreamMerger(timingStream, dataStream);
    merger.subscribe(received::add);

    dataStream.publishToSubscribers(
        new MeasurementVector(1000L, List.of(new Humidity(1000L, "H1", new BigDecimal("50")))));
    timingStream.publishToSubscribers(new MeasurementVector(2000L, List.of()));
    Assertions.assertEquals(1, received.size());

    merger.stop();

    timingStream.publishToSubscribers(new MeasurementVector(3000L, List.of()));
    Assertions.assertEquals(1, received.size(), "No new merged vector after stop");
  }
}
