package ca.lajthabalazs.pressure_integrity_test.measurement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A data stream that takes a sequence of measurements and plays them back in real time, altering
 * timestamps to reflect current times while preserving the time distance between measurements.
 *
 * <p>Example: If the original measurements were at times [1000, 2500, 4000] (deltas of 1500ms and
 * 1500ms), and playback starts at current time T, the measurements will be published at [T, T+1500,
 * T+3000], preserving the relative timing.
 */
public class MeasurementPlaybackStream extends MeasurementStream {
  private final ScheduledExecutorService scheduler;
  private final List<ScheduledFuture<?>> scheduledTasks;
  private final long shutdownAwaitMs;

  /** Creates a new MeasurementPlaybackStream. */
  public MeasurementPlaybackStream() {
    this(Executors.newSingleThreadScheduledExecutor(), 5000L);
  }

  /**
   * Creates a MeasurementPlaybackStream with the given scheduler and shutdown await time. For
   * testing or when the executor is managed externally.
   *
   * @param scheduler the executor used to schedule playback
   * @param shutdownAwaitMs how long to wait for termination before calling shutdownNow
   */
  public MeasurementPlaybackStream(ScheduledExecutorService scheduler, long shutdownAwaitMs) {
    this.scheduler = scheduler;
    this.scheduledTasks = new ArrayList<>();
    this.shutdownAwaitMs = shutdownAwaitMs;
  }

  /**
   * Starts playing back the given measurements in real time.
   *
   * <p>The first measurement will be published immediately with the specified start timestamp.
   * Subsequent measurements will be published at intervals that match the original time deltas
   * between measurements.
   *
   * @param measurements the measurements to play back
   * @param startTime the timestamp (in milliseconds since epoch) to use for the first measurement
   * @throws IllegalStateException if playback is already in progress
   */
  public void startPlayback(List<Measurement> measurements, long startTime) {
    if (!scheduledTasks.isEmpty()) {
      throw new IllegalStateException("Cannot start playback stream twice");
    }

    // Publish first measurement immediately with current timestamp
    long firstMeasurementTine = measurements.getFirst().getTimeUtc();
    for (Measurement measurement : measurements) {
      scheduleTask(measurement, startTime, firstMeasurementTine);
    }
  }

  private void scheduleTask(Measurement measurement, long startTime, long firstMeasurementTime) {
    long originalTime = measurement.getTimeUtc();
    long timeDelta = originalTime - firstMeasurementTime;
    long scheduledTime = startTime + timeDelta;
    final long newTimestamp = scheduledTime;
    long delay = Math.max(0, scheduledTime - System.currentTimeMillis());
    ScheduledFuture<?> task =
        scheduler.schedule(
            () -> {
              Measurement adjustedMeasurement = measurement.withNewTimestamp(newTimestamp);
              publish(adjustedMeasurement);
            },
            delay,
            TimeUnit.MILLISECONDS);
    scheduledTasks.add(task);
  }

  /** Stops the current playback if it is in progress. */
  public void stopPlayback() {
    for (ScheduledFuture<?> task : scheduledTasks) {
      if (task != null && !task.isDone()) {
        task.cancel(false);
      }
    }
    scheduledTasks.clear();
  }

  /** Shuts down the playback stream and releases resources. */
  public void shutdown() {
    stopPlayback();
    clearSubscribers();
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(shutdownAwaitMs, TimeUnit.MILLISECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
