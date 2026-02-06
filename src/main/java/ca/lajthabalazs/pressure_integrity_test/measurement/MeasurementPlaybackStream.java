package ca.lajthabalazs.pressure_integrity_test.measurement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A data stream that takes a sequence of measurements and plays them back in real time, altering
 * timestamps to reflect current times while preserving the time distance between measurements.
 *
 * <p>Playback can be sped up or slowed down via {@link #setSpeed(double)}. Timestamps in the stream
 * keep the original distance between them; only the delivery rate changes.
 *
 * <p>Example: If the original measurements were at times [1000, 2500, 4000] (deltas of 1500ms and
 * 1500ms), and playback starts at current time T, the measurements will be published at [T, T+1500,
 * T+3000], preserving the relative timing.
 */
public class MeasurementPlaybackStream extends MeasurementStream {
  private final ScheduledExecutorService scheduler;
  private final List<ScheduledFuture<?>> scheduledTasks;
  private final long shutdownAwaitMs;
  private volatile double speedFactor = 1.0;
  private volatile boolean useOriginalTimestamps;
  private volatile long playbackStartTime;
  private volatile long startTime;
  private volatile long firstMeasurementTime;
  private volatile List<Measurement> playbackMeasurements;
  private final AtomicInteger nextIndexToPublish = new AtomicInteger(0);

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
   * Sets the playback speed factor. Values &gt; 1 speed up delivery, values &lt; 1 slow it down. If
   * playback is in progress, the scheduler is cancelled and remaining measurements are rescheduled
   * with the new speed; no message is lost (duplicate delivery is allowed).
   *
   * @param factor speed factor (e.g. 2.0 = twice as fast, 0.5 = half speed). Must be positive.
   */
  public void setSpeed(double factor) {
    if (factor <= 0 || !Double.isFinite(factor)) {
      throw new IllegalArgumentException("Speed factor must be positive and finite");
    }
    this.speedFactor = factor;
    if (playbackMeasurements == null || playbackMeasurements.isEmpty()) {
      return;
    }
    cancelAllScheduledTasks();
    rescheduleFromNextIndex();
  }

  /**
   * Starts playing back the given measurements in real time, using updated timestamps (first
   * measurement at {@code startTime}, subsequent ones at {@code startTime + timeDelta}).
   *
   * <p>Equivalent to {@code startPlayback(measurements, startTime, false)}.
   *
   * @param measurements the measurements to play back
   * @param startTime the timestamp (in milliseconds since epoch) to use for the first measurement
   * @throws IllegalStateException if playback is already in progress
   */
  public void startPlayback(List<Measurement> measurements, long startTime) {
    startPlayback(measurements, startTime, false);
  }

  /**
   * Starts playing back the given measurements in real time.
   *
   * <p>The first measurement will be published immediately. Subsequent measurements will be
   * published at intervals that match the original time deltas between measurements, scaled by the
   * current speed factor.
   *
   * @param measurements the measurements to play back
   * @param startTime the timestamp (in milliseconds since epoch) for the first measurement when
   *     using updated timestamps; ignored when {@code useOriginalTimestamps} is true
   * @param useOriginalTimestamps if true, each measurement is published with its original timestamp
   *     ({@link Measurement#getTimeUtc()}); if false, timestamps are updated to start at {@code
   *     startTime} while preserving deltas between measurements
   * @throws IllegalStateException if playback is already in progress
   */
  public void startPlayback(
      List<Measurement> measurements, long startTime, boolean useOriginalTimestamps) {
    if (!scheduledTasks.isEmpty()) {
      throw new IllegalStateException("Cannot start playback stream twice");
    }

    long first = measurements.getFirst().getTimeUtc();
    this.startTime = startTime;
    this.useOriginalTimestamps = useOriginalTimestamps;
    this.playbackStartTime = System.currentTimeMillis();
    this.firstMeasurementTime = first;
    this.playbackMeasurements = new ArrayList<>(measurements);
    this.nextIndexToPublish.set(0);

    for (int i = 0; i < playbackMeasurements.size(); i++) {
      scheduleTask(i);
    }
  }

  private void cancelAllScheduledTasks() {
    for (ScheduledFuture<?> task : scheduledTasks) {
      if (task != null && !task.isDone()) {
        task.cancel(false);
      }
    }
    scheduledTasks.clear();
  }

  private void rescheduleFromNextIndex() {
    List<Measurement> list = playbackMeasurements;

    int from = nextIndexToPublish.get();
    for (int i = from; i < list.size(); i++) {
      scheduleTask(i);
    }
  }

  private void scheduleTask(int index) {
    List<Measurement> list = playbackMeasurements;
    Measurement measurement = list.get(index);
    long timeDelta = measurement.getTimeUtc() - firstMeasurementTime;
    long timestampToPublish =
        useOriginalTimestamps ? measurement.getTimeUtc() : (startTime + timeDelta);
    double factor = speedFactor;
    long scheduledRealTime = playbackStartTime + (long) (timeDelta / factor);
    long delay = Math.max(0, scheduledRealTime - System.currentTimeMillis());
    final long finalTimestamp = timestampToPublish;

    ScheduledFuture<?> task =
        scheduler.schedule(
            () -> {
              Measurement adjusted = measurement.withNewTimestamp(finalTimestamp);
              publish(adjusted);
              nextIndexToPublish.compareAndSet(index, index + 1);
            },
            delay,
            TimeUnit.MILLISECONDS);
    scheduledTasks.add(task);
  }

  /** Stops the current playback if it is in progress. */
  public void stopPlayback() {
    cancelAllScheduledTasks();
    playbackMeasurements = null;
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
