package ca.lajthabalazs.pressure_integrity_test.measurement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A timestamped vector of measurements. The timestamp applies to the vector as a whole (e.g. the
 * logical time of the event); individual measurements may carry their own timestamps for reference.
 *
 * <p>Measurements are stored as a map from sensor ID to {@link Measurement}. In addition, a vector
 * can carry a list of {@link MeasurementError}s describing issues detected during processing (per
 * sensor or derived quantity). Streams should treat vectors that contain at least one {@link
 * ErrorSeverity#SEVERE} error as invalid and pass them through unchanged without further
 * processing.
 */
public final class MeasurementVector {

  private final long timeUtc;
  private final Map<String, Measurement> measurements;
  private final List<MeasurementError> errors;

  /**
   * Creates a measurement vector with the given timestamp, map of measurements (sensor ID →
   * measurement), and list of errors.
   *
   * @param timeUtc timestamp in milliseconds since epoch for this vector
   * @param measurements map of sensor ID to measurement (not null; can be empty)
   * @param errors list of associated errors (not null; can be empty)
   */
  public MeasurementVector(
      long timeUtc, Map<String, Measurement> measurements, List<MeasurementError> errors) {
    this.timeUtc = timeUtc;
    this.measurements = Collections.unmodifiableMap(new LinkedHashMap<>(measurements));
    this.errors = errors != null ? List.copyOf(errors) : List.of();
  }

  /**
   * Creates a measurement vector with the given timestamp and map of measurements (sensor ID →
   * measurement) and no errors.
   *
   * @param timeUtc timestamp in milliseconds since epoch for this vector
   * @param measurements map of sensor ID to measurement (not null; can be empty)
   */
  public MeasurementVector(long timeUtc, Map<String, Measurement> measurements) {
    this(timeUtc, measurements, List.of());
  }

  /**
   * Creates a measurement vector with the given timestamp, list of measurements, and list of
   * errors. Measurements are stored by their source ID; duplicates overwrite.
   *
   * @param timeUtc timestamp in milliseconds since epoch for this vector
   * @param measurements list of measurements (not null; can be empty)
   * @param errors list of associated errors (not null; can be empty)
   */
  public MeasurementVector(
      long timeUtc, List<Measurement> measurements, List<MeasurementError> errors) {
    this.timeUtc = timeUtc;
    Map<String, Measurement> map = new LinkedHashMap<>();
    for (Measurement m : measurements) {
      map.put(m.getSourceId(), m);
    }
    this.measurements = Collections.unmodifiableMap(map);
    this.errors = errors != null ? List.copyOf(errors) : List.of();
  }

  /**
   * Creates a measurement vector with the given timestamp and list of measurements and no errors.
   * Measurements are stored by their source ID; duplicates overwrite.
   *
   * @param timeUtc timestamp in milliseconds since epoch for this vector
   * @param measurements list of measurements (not null; can be empty)
   */
  public MeasurementVector(long timeUtc, List<Measurement> measurements) {
    this(timeUtc, measurements, List.of());
  }

  /** Returns the timestamp for this vector in milliseconds since epoch. */
  public long getTimeUtc() {
    return timeUtc;
  }

  /** Returns an unmodifiable map from sensor ID to measurement. */
  public Map<String, Measurement> getMeasurementsMap() {
    return measurements;
  }

  /** Returns measurements as an unmodifiable list (order follows map iteration order). */
  public List<Measurement> getMeasurements() {
    return Collections.unmodifiableList(new ArrayList<>(measurements.values()));
  }

  /** Returns an unmodifiable list of errors associated with this vector (may be empty). */
  public List<MeasurementError> getErrors() {
    return errors;
  }

  /** Returns true if this vector contains at least one severe error. */
  public boolean hasSevereError() {
    for (MeasurementError e : errors) {
      if (e.severity() == ErrorSeverity.SEVERE) {
        return true;
      }
    }
    return false;
  }
}
