package ca.lajthabalazs.pressure_integrity_test.measurement;

import java.util.List;

/**
 * A timestamped vector of measurements. The timestamp applies to the vector as a whole (e.g. the
 * logical time of the event); individual measurements may carry their own timestamps for reference.
 */
public final class MeasurementVector {

  private final long timeUtc;
  private final List<Measurement> measurements;

  /**
   * Creates a measurement vector with the given timestamp and list of measurements. The list is
   * copied and the copy is stored unmodifiably.
   *
   * @param timeUtc timestamp in milliseconds since epoch for this vector
   * @param measurements list of measurements (not null; can be empty)
   */
  public MeasurementVector(long timeUtc, List<Measurement> measurements) {
    this.timeUtc = timeUtc;
    this.measurements = List.copyOf(measurements);
  }

  /** Returns the timestamp for this vector in milliseconds since epoch. */
  public long getTimeUtc() {
    return timeUtc;
  }

  /** Returns an unmodifiable list of measurements in this vector. */
  public List<Measurement> getMeasurements() {
    return measurements;
  }
}
