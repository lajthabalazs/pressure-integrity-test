package ca.lajthabalazs.pressure_integrity_test.measurement.streaming;

import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A data stream that takes a sequence of {@link MeasurementVector}s and plays them back in real
 * time. Each vector is published as one event; the vector's timestamp drives scheduling and can be
 * adjusted to reflect current times while preserving the time distance between vectors.
 *
 * <p>Playback can be sped up or slowed down via {@link #setSpeed(double)}.
 *
 * <p>Example: If vectors have timestamps T0, T1, T2, playback at startTime will publish them at
 * [startTime, startTime+(T1-T0), startTime+(T2-T0)], preserving relative timing (when not using
 * original timestamps).
 */
public class MeasurementVectorPlaybackStream extends MeasurementVectorStream {
  private final ScheduledExecutorService scheduler;
  private final List<ScheduledFuture<?>> scheduledTasks;
  private final long shutdownAwaitMs;
  private volatile double speedFactor = 1.0;
  private volatile boolean useOriginalTimestamps;
  private volatile long startTime;
  private volatile long firstVectorTime;
  private volatile List<MeasurementVector> playbackVectors;
  private final AtomicInteger nextIndexToPublish = new AtomicInteger(0);
  private volatile boolean paused;

  // Virtual time tracking
  private final VirtualTimeTracker virtualTimeTracker = new VirtualTimeTracker();

  /** Creates a new MeasurementVectorPlaybackStream. */
  public MeasurementVectorPlaybackStream() {
    this(Executors.newSingleThreadScheduledExecutor(), 5000L);
  }

  /**
   * Creates a MeasurementVectorPlaybackStream with the given scheduler and shutdown await time. For
   * testing or when the executor is managed externally.
   *
   * @param scheduler the executor used to schedule playback
   * @param shutdownAwaitMs how long to wait for termination before calling shutdownNow
   */
  public MeasurementVectorPlaybackStream(ScheduledExecutorService scheduler, long shutdownAwaitMs) {
    this.scheduler = scheduler;
    this.scheduledTasks = new ArrayList<>();
    this.shutdownAwaitMs = shutdownAwaitMs;
  }

  /**
   * Sets the playback speed factor. Values &gt; 1 speed up delivery, values &lt; 1 slow it down. If
   * playback is in progress, the scheduler is cancelled and remaining vectors are rescheduled with
   * the new speed.
   *
   * @param factor speed factor (e.g. 2.0 = twice as fast, 0.5 = half speed). Must be positive.
   */
  public void setSpeed(double factor) {
    long now = System.currentTimeMillis();
    virtualTimeTracker.setSpeed(now, factor);
    this.speedFactor = factor;

    if (playbackVectors == null || playbackVectors.isEmpty() || paused) {
      return;
    }

    cancelAllScheduledTasks();
    rescheduleFromNextIndex();
  }

  /**
   * Starts playing back the given measurement vectors. The first vector is published immediately;
   * subsequent vectors are published at intervals that match the original time deltas between
   * vectors, scaled by the current speed factor.
   *
   * <p>Equivalent to {@code startPlayback(vectors, startTime, false)}.
   *
   * @param vectors the measurement vectors to play back (each must have non-empty measurements)
   * @param startTime the timestamp (ms since epoch) used for the first vector when not using
   *     original timestamps
   * @throws IllegalStateException if playback is already in progress
   * @throws IllegalArgumentException if vectors is null, empty, or any vector has empty
   *     measurements
   */
  public void startPlayback(List<MeasurementVector> vectors, long startTime) {
    startPlayback(vectors, startTime, false);
  }

  /**
   * Starts playing back the given measurement vectors.
   *
   * @param vectors the measurement vectors to play back (each must have non-empty measurements)
   * @param startTime the timestamp (ms since epoch) for the first vector when {@code
   *     useOriginalTimestamps} is false; ignored when true
   * @param useOriginalTimestamps if true, each vector is published unchanged; if false, vector
   *     timestamps are shifted so the first vector's time becomes {@code startTime} while
   *     preserving deltas, and each measurement's timestamp is adjusted by the same delta
   * @throws IllegalStateException if playback is already in progress
   * @throws IllegalArgumentException if vectors is null, empty, or any vector has empty
   *     measurements
   */
  public void startPlayback(
      List<MeasurementVector> vectors, long startTime, boolean useOriginalTimestamps) {
    if (!scheduledTasks.isEmpty()) {
      throw new IllegalStateException("Cannot start playback stream twice");
    }
    if (vectors == null || vectors.isEmpty()) {
      throw new IllegalArgumentException("vectors must be non-empty");
    }
    for (MeasurementVector v : vectors) {
      if (v == null || v.getMeasurementsMap() == null) {
        throw new IllegalArgumentException("Each vector must have non-empty measurements");
      }
    }

    this.firstVectorTime = vectors.getFirst().getTimeUtc();
    this.startTime = startTime;
    this.useOriginalTimestamps = useOriginalTimestamps;
    this.playbackVectors = new ArrayList<>(vectors);
    this.nextIndexToPublish.set(0);
    this.paused = false;

    // Initialize virtual time tracking
    virtualTimeTracker.start(System.currentTimeMillis());
    virtualTimeTracker.setSpeed(System.currentTimeMillis(), speedFactor);

    for (int i = 0; i < playbackVectors.size(); i++) {
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
    List<MeasurementVector> vectors = playbackVectors;
    int from = nextIndexToPublish.get();
    for (int i = from; i < vectors.size(); i++) {
      scheduleTask(i);
    }
  }

  private void scheduleTask(int index) {
    List<MeasurementVector> vectors = playbackVectors;
    MeasurementVector vector = vectors.get(index);
    long vectorTime = vector.getTimeUtc();
    long vectorVirtualTime = vectorTime - firstVectorTime;

    // Calculate delay using virtual time tracker
    long now = System.currentTimeMillis();
    long delay = virtualTimeTracker.calculateDelay(vectorVirtualTime, now);

    final long timestampBase = useOriginalTimestamps ? vectorTime : (startTime + vectorVirtualTime);
    final boolean useOriginal = useOriginalTimestamps;

    ScheduledFuture<?> task =
        scheduler.schedule(
            () -> {
              MeasurementVector toPublish;
              if (useOriginal) {
                toPublish = vector;
              } else {
                long refTime = vector.getTimeUtc();
                long delta = timestampBase - refTime;
                List<Measurement> adjusted = new ArrayList<>(vector.getMeasurementsMap().size());
                for (Measurement m : vector.getMeasurementsMap().values()) {
                  adjusted.add(m.withNewTimestamp(m.getTimeUtc() + delta));
                }
                toPublish = new MeasurementVector(timestampBase, adjusted, vector.getErrors());
              }
              publish(toPublish);
              nextIndexToPublish.compareAndSet(index, index + 1);
            },
            delay,
            TimeUnit.MILLISECONDS);
    scheduledTasks.add(task);
  }

  /** Pauses playback. Remaining vectors are not published until {@link #resume()} is called. */
  public void pause() {
    if (paused || playbackVectors == null) {
      return;
    }
    virtualTimeTracker.pause(System.currentTimeMillis());
    cancelAllScheduledTasks();
    paused = true;
  }

  /** Resumes playback from the next pending vector. */
  public void resume() {
    if (playbackVectors == null || !paused) {
      return;
    }
    int next = nextIndexToPublish.get();
    if (next >= playbackVectors.size()) {
      paused = false;
      return;
    }

    virtualTimeTracker.resume(System.currentTimeMillis());
    paused = false;
    rescheduleFromNextIndex();
  }

  /** Returns true if playback is paused. */
  public boolean isPaused() {
    return paused;
  }

  /** Stops the current playback if it is in progress. */
  public void stopPlayback() {
    cancelAllScheduledTasks();
    playbackVectors = null;
    paused = false;
    virtualTimeTracker.reset();
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
