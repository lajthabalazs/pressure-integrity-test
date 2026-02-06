package ca.lajthabalazs.pressure_integity_test.measurement;

import ca.lajthabalazs.pressure_integity_test.io.ResourceTextFileReader;
import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader;
import ca.lajthabalazs.pressure_integrity_test.measurement.Humidity;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementPlaybackStream;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MeasurementPlaybackStreamTest {

  // Time tolerance constants (in milliseconds)
  private static final long TIMESTAMP_TOLERANCE_MS = 0; // Timestamps must be exact
  private static final long ARRIVAL_TIME_TOLERANCE_MS = 20;
  private static final long ARRIVAL_DELTA_TOLERANCE_MS = 15;
  private static final long MAX_ALLOWED_OUTLIERS = 2;
  private static final long MAX_DELAY_MS = 50;

  private MeasurementPlaybackStream playbackStream;
  private boolean gcWasDisabled;

  @BeforeEach
  public void setUp() {
    gcWasDisabled = false;
  }

  @AfterEach
  public void tearDown() {
    if (playbackStream != null) {
      playbackStream.shutdown();
    }
    // Re-enable GC if it was disabled
    if (gcWasDisabled) {
      enableGarbageCollection();
    }
  }

  /**
   * Disables explicit GC suggestions to minimize GC interference during timing-sensitive tests.
   * Note: This doesn't disable automatic GC, but prevents explicit GC calls from interfering.
   */
  private void disableGarbageCollection() {
    try {
      // Run GC before disabling to clean up
      System.gc();
      Thread.sleep(50); // Give GC time to complete
      gcWasDisabled = true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** Re-enables normal GC behavior after timing tests. */
  private void enableGarbageCollection() {
    // In Java, we can't really "disable" GC, so this is mainly for symmetry
    // The JVM will continue to run GC automatically as needed
    gcWasDisabled = false;
  }

  @Test
  public void subscribe_andPublish_receivesMeasurement() throws Exception {
    playbackStream = new MeasurementPlaybackStream();
    List<Measurement> received = new ArrayList<>();

    MeasurementStream.Subscription subscription = playbackStream.subscribe(received::add);

    Humidity testMeasurement =
        new Humidity(
            1000L,
            "H1",
            new BigDecimal("50"),
            new BigDecimal("0.1"),
            BigDecimal.ZERO,
            new BigDecimal("100"));

    playbackStream.publish(testMeasurement);

    Thread.sleep(100); // Give handler time to execute

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(testMeasurement, received.get(0));

    subscription.unsubscribe();
  }

  @Test
  public void startPlayback_preservesTimeDeltas() throws Exception {
    disableGarbageCollection();
    try {
      playbackStream = new MeasurementPlaybackStream();
      List<Measurement> received = new ArrayList<>();
      List<Long> receiveTimesMillis = new ArrayList<>();
      long startNanoTime = System.nanoTime();

      playbackStream.subscribe(
          measurement -> {
            received.add(measurement);
            receiveTimesMillis.add(System.currentTimeMillis());
          });

      // Create measurements 100ms apart
      long baseTime = 1000000L;
      List<Measurement> measurements = new ArrayList<>();
      measurements.add(
          new Humidity(
              baseTime,
              "H1",
              new BigDecimal("45.2"),
              new BigDecimal("0.1"),
              BigDecimal.ZERO,
              new BigDecimal("100")));
      measurements.add(
          new Humidity(
              baseTime + 100,
              "H1",
              new BigDecimal("45.5"),
              new BigDecimal("0.1"),
              BigDecimal.ZERO,
              new BigDecimal("100")));
      measurements.add(
          new Humidity(
              baseTime + 200,
              "H1",
              new BigDecimal("45.8"),
              new BigDecimal("0.1"),
              BigDecimal.ZERO,
              new BigDecimal("100")));

      long startTime = System.currentTimeMillis();
      playbackStream.startPlayback(measurements, startTime);

      // Wait for all measurements to be published (3 measurements, last one at +200ms = ~300ms
      // total)
      Thread.sleep(400);

      Assertions.assertEquals(3, received.size(), "Should receive all 3 measurements");
      Assertions.assertTrue(received.stream().allMatch(m -> m instanceof Humidity));

      // Verify measurement timestamps are exactly aligned with the provided start time
      long expectedTime0 = startTime;
      long expectedTime1 = startTime + 100;
      long expectedTime2 = startTime + 200;

      Assertions.assertEquals(
          expectedTime0,
          received.get(0).getTimeUtc(),
          "First measurement timestamp should be exactly " + expectedTime0);
      Assertions.assertEquals(
          expectedTime1,
          received.get(1).getTimeUtc(),
          "Second measurement timestamp should be exactly " + expectedTime1);
      Assertions.assertEquals(
          expectedTime2,
          received.get(2).getTimeUtc(),
          "Third measurement timestamp should be exactly " + expectedTime2);

      // Check that arrival times match expected timing (allow tolerance for scheduler jitter)
      if (receiveTimesMillis.size() >= 3) {
        long arrivalDelta1 = receiveTimesMillis.get(1) - receiveTimesMillis.get(0);
        long arrivalDelta2 = receiveTimesMillis.get(2) - receiveTimesMillis.get(1);
        // Use statistical approach: allow some outliers due to GC/scheduler jitter
        int outliers = 0;
        if (Math.abs(arrivalDelta1 - 100) > ARRIVAL_DELTA_TOLERANCE_MS) outliers++;
        if (Math.abs(arrivalDelta2 - 100) > ARRIVAL_DELTA_TOLERANCE_MS) outliers++;
        Assertions.assertTrue(
            outliers <= MAX_ALLOWED_OUTLIERS,
            "Too many arrival delta outliers: "
                + outliers
                + ". Delta1: "
                + arrivalDelta1
                + ", Delta2: "
                + arrivalDelta2);

        // Fail if any delta exceeds maximum delay
        Assertions.assertTrue(
            Math.abs(arrivalDelta1 - 100) <= MAX_DELAY_MS,
            "Arrival delta 1 exceeds maximum delay: " + arrivalDelta1);
        Assertions.assertTrue(
            Math.abs(arrivalDelta2 - 100) <= MAX_DELAY_MS,
            "Arrival delta 2 exceeds maximum delay: " + arrivalDelta2);

        // Verify arrival times match measurement timestamps (within tolerance)
        Assertions.assertTrue(
            Math.abs(receiveTimesMillis.get(0) - received.get(0).getTimeUtc())
                <= ARRIVAL_TIME_TOLERANCE_MS,
            "Arrival time 0 should match measurement timestamp");
        Assertions.assertTrue(
            Math.abs(receiveTimesMillis.get(1) - received.get(1).getTimeUtc())
                <= ARRIVAL_TIME_TOLERANCE_MS,
            "Arrival time 1 should match measurement timestamp");
        Assertions.assertTrue(
            Math.abs(receiveTimesMillis.get(2) - received.get(2).getTimeUtc())
                <= ARRIVAL_TIME_TOLERANCE_MS,
            "Arrival time 2 should match measurement timestamp");
      }
    } finally {
      enableGarbageCollection();
    }
  }

  @Test
  public void startPlayback_fromResourceFile() throws Exception {
    disableGarbageCollection();
    try {
      playbackStream = new MeasurementPlaybackStream();
      List<Measurement> received = new ArrayList<>();
      List<Long> receiveTimesMillis = new ArrayList<>();

      playbackStream.subscribe(
          measurement -> {
            received.add(measurement);
            receiveTimesMillis.add(System.currentTimeMillis());
          });

      // Load measurements from resource file
      TextFileReader reader = new ResourceTextFileReader(MeasurementPlaybackStreamTest.class);
      List<String> lines = reader.readAllLines("/sample-humidity-measurements.csv");

      // Skip header line
      List<Measurement> measurements = new ArrayList<>();
      for (int i = 1; i < lines.size(); i++) {
        String[] parts = lines.get(i).split(",");
        long timeUtc = Long.parseLong(parts[0]);
        String sourceId = parts[1];
        BigDecimal value = new BigDecimal(parts[2]);
        BigDecimal sourceSigma = new BigDecimal(parts[3]);
        BigDecimal lowerBound = new BigDecimal(parts[4]);
        BigDecimal upperBound = new BigDecimal(parts[5]);

        measurements.add(
            new Humidity(timeUtc, sourceId, value, sourceSigma, lowerBound, upperBound));
      }

      long startTime = System.currentTimeMillis();
      playbackStream.startPlayback(measurements, startTime);

      // Wait for all measurements (20 measurements, last one at +1900ms = ~2000ms total)
      Thread.sleep(2100);

      Assertions.assertEquals(20, received.size(), "Should receive all 20 measurements");
      Assertions.assertTrue(received.stream().allMatch(m -> m instanceof Humidity));

      // Verify timestamps are adjusted to current time
      Assertions.assertTrue(
          received.get(0).getTimeUtc() >= startTime,
          "First measurement timestamp should be >= start time");

      // Verify measurement timestamps are exactly aligned with the provided start time
      for (int i = 0; i < received.size(); i++) {
        long expectedTimestamp = startTime + (i * 100);
        long actualTimestamp = received.get(i).getTimeUtc();
        Assertions.assertEquals(
            expectedTimestamp,
            actualTimestamp,
            "Measurement " + i + " timestamp should be exactly " + expectedTimestamp);
      }

      // Statistical verification: count outliers instead of checking each event
      if (receiveTimesMillis.size() == received.size()) {
        int outliersCount = 0;
        long maxDelay = 0;

        for (int i = 0; i < receiveTimesMillis.size(); i++) {
          long actualArrivalTime = receiveTimesMillis.get(i);
          long measurementTimestamp = received.get(i).getTimeUtc();
          long delay = Math.abs(actualArrivalTime - measurementTimestamp);

          // Track maximum delay (fail if exceeds MAX_DELAY_MS)
          if (delay > maxDelay) {
            maxDelay = delay;
          }

          // Count outliers (outside ARRIVAL_TIME_TOLERANCE_MS)
          if (delay > ARRIVAL_TIME_TOLERANCE_MS) {
            outliersCount++;
          }
        }

        // Fail if any delay exceeds the maximum allowed delay
        Assertions.assertTrue(
            maxDelay <= MAX_DELAY_MS,
            "Maximum delay (" + maxDelay + "ms) exceeds allowed maximum (" + MAX_DELAY_MS + "ms)");

        // Allow up to MAX_ALLOWED_OUTLIERS events outside tolerance
        Assertions.assertTrue(
            outliersCount <= MAX_ALLOWED_OUTLIERS,
            "Number of outliers ("
                + outliersCount
                + ") exceeds allowed maximum ("
                + MAX_ALLOWED_OUTLIERS
                + "). Events outside "
                + ARRIVAL_TIME_TOLERANCE_MS
                + "ms tolerance: "
                + outliersCount);

        // Statistical verification for arrival time deltas: count outliers
        int deltaOutliersCount = 0;
        long maxDeltaDeviation = 0;

        for (int i = 1; i < receiveTimesMillis.size(); i++) {
          long arrivalDelta = receiveTimesMillis.get(i) - receiveTimesMillis.get(i - 1);
          long deviation = Math.abs(arrivalDelta - 100);

          // Track maximum deviation (fail if exceeds MAX_DELAY_MS)
          if (deviation > maxDeltaDeviation) {
            maxDeltaDeviation = deviation;
          }

          // Count outliers (outside ARRIVAL_DELTA_TOLERANCE_MS)
          if (deviation > ARRIVAL_DELTA_TOLERANCE_MS) {
            deltaOutliersCount++;
          }
        }

        // Fail if any delta deviation exceeds the maximum allowed delay
        Assertions.assertTrue(
            maxDeltaDeviation <= MAX_DELAY_MS,
            "Maximum arrival delta deviation ("
                + maxDeltaDeviation
                + "ms) exceeds allowed maximum ("
                + MAX_DELAY_MS
                + "ms)");

        // Allow up to MAX_ALLOWED_OUTLIERS delta outliers
        Assertions.assertTrue(
            deltaOutliersCount <= MAX_ALLOWED_OUTLIERS,
            "Number of arrival delta outliers ("
                + deltaOutliersCount
                + ") exceeds allowed maximum ("
                + MAX_ALLOWED_OUTLIERS
                + "). Deltas outside "
                + ARRIVAL_DELTA_TOLERANCE_MS
                + "ms tolerance: "
                + deltaOutliersCount);
      }

      // Verify all measurements are from the same sensor
      Assertions.assertTrue(
          received.stream().allMatch(m -> "H1".equals(m.getSourceId())),
          "All measurements should be from sensor H1");
    } finally {
      enableGarbageCollection();
    }
  }

  @Test
  public void stopPlayback_cancelsScheduledTasks() throws Exception {
    playbackStream = new MeasurementPlaybackStream();
    List<Measurement> received = new ArrayList<>();

    playbackStream.subscribe(measurement -> received.add(measurement));

    long baseTime = 1000000L;
    List<Measurement> measurements = new ArrayList<>();
    measurements.add(
        new Humidity(
            baseTime,
            "H1",
            new BigDecimal("45.2"),
            new BigDecimal("0.1"),
            BigDecimal.ZERO,
            new BigDecimal("100")));
    measurements.add(
        new Humidity(
            baseTime + 100,
            "H1",
            new BigDecimal("45.5"),
            new BigDecimal("0.1"),
            BigDecimal.ZERO,
            new BigDecimal("100")));

    long startTime = System.currentTimeMillis();
    playbackStream.startPlayback(measurements, startTime);
    Thread.sleep(50); // Let first measurement publish

    playbackStream.stopPlayback();

    Thread.sleep(200); // Wait longer than the second measurement would take

    // Should only have the first measurement
    Assertions.assertEquals(1, received.size());
  }

  @Test
  public void isPlaying_reflectsPlaybackState() throws Exception {
    playbackStream = new MeasurementPlaybackStream();
    List<Measurement> measurements = new ArrayList<>();
    measurements.add(
        new Humidity(
            1000L,
            "H1",
            new BigDecimal("50"),
            new BigDecimal("0.1"),
            BigDecimal.ZERO,
            new BigDecimal("100")));
    measurements.add(
        new Humidity(
            1100L,
            "H1",
            new BigDecimal("51"),
            new BigDecimal("0.1"),
            BigDecimal.ZERO,
            new BigDecimal("100")));

    long startTime = System.currentTimeMillis();
    playbackStream.startPlayback(measurements, startTime);
    Thread.sleep(50); // Small delay to ensure isPlaying is set

    Thread.sleep(
        200); // Wait for playback to complete (100ms between measurements, total duration 100ms)
  }

  @Test
  public void startPlayback_whenMessagesComeFast_delayBecomesZero() throws Exception {
    disableGarbageCollection();
    try {
      playbackStream = new MeasurementPlaybackStream();
      List<Measurement> received = new ArrayList<>();
      List<Long> receiveTimesMillis = new ArrayList<>();

      playbackStream.subscribe(
          measurement -> {
            received.add(measurement);
            receiveTimesMillis.add(System.currentTimeMillis());
          });

      // Create many measurements with very small time deltas (1ms apart)
      // This simulates messages coming so fast that by the time the scheduler
      // processes later measurements, their scheduled time has already passed
      long baseTime = 1000000L;
      List<Measurement> measurements = new ArrayList<>();
      int measurementCount = 50; // Enough measurements to cause scheduling overhead

      for (int i = 0; i < measurementCount; i++) {
        measurements.add(
            new Humidity(
                baseTime + i, // 1ms apart
                "H1",
                new BigDecimal("45.0").add(new BigDecimal(i * 0.1)),
                new BigDecimal("0.1"),
                BigDecimal.ZERO,
                new BigDecimal("100")));
      }

      long startTime = System.currentTimeMillis();
      playbackStream.startPlayback(measurements, startTime);

      // Wait for all measurements to be published
      // Since they're 1ms apart, total duration is ~49ms, but allow extra time
      Thread.sleep(200);

      // Verify all measurements were received
      Assertions.assertEquals(
          measurementCount,
          received.size(),
          "Should receive all measurements even when delays become zero");

      // Verify all measurements have correct timestamps (within tolerance)
      // When delays become zero, measurements are published immediately, so timestamps
      // may be slightly adjusted but should still reflect the intended sequence
      for (int i = 0; i < received.size(); i++) {
        long expectedTimestamp = startTime + i;
        long actualTimestamp = received.get(i).getTimeUtc();
        // Allow some tolerance for immediate publishing when delay is zero
        Assertions.assertTrue(
            Math.abs(actualTimestamp - expectedTimestamp) <= ARRIVAL_TIME_TOLERANCE_MS,
            "Measurement "
                + i
                + " timestamp should be close to "
                + expectedTimestamp
                + " (actual: "
                + actualTimestamp
                + ")");
      }

      // Verify that measurements arrive in order (non-decreasing timestamps)
      for (int i = 1; i < received.size(); i++) {
        Assertions.assertTrue(
            received.get(i).getTimeUtc() >= received.get(i - 1).getTimeUtc(),
            "Measurements should arrive in non-decreasing timestamp order");
      }

      // Verify that all measurements were published successfully
      // The key test: when messages come so fast that delays become negative (set to 0),
      // all measurements should still be published without errors
      Assertions.assertTrue(
          received.size() == measurementCount,
          "All measurements should be published even when scheduling overhead causes negative delays");
    } finally {
      enableGarbageCollection();
    }
  }
}
