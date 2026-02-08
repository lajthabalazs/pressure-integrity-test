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
 * <p>Measurements are stored as a map from sensor ID to {@link Measurement}.
 */
public final class MeasurementVector {

  private final long timeUtc;
  private final Map<String, Measurement> measurements;

  /**
   * Creates a measurement vector with the given timestamp and map of measurements (sensor ID →
   * measurement).
   *
   * @param timeUtc timestamp in milliseconds since epoch for this vector
   * @param measurements map of sensor ID to measurement (not null; can be empty)
   */
  public MeasurementVector(long timeUtc, Map<String, Measurement> measurements) {
    this.timeUtc = timeUtc;
    this.measurements = Collections.unmodifiableMap(new LinkedHashMap<>(measurements));
  }

  /**
   * Creates a measurement vector with the given timestamp and list of measurements. Measurements
   * are stored by their source ID; duplicates overwrite.
   *
   * @param timeUtc timestamp in milliseconds since epoch for this vector
   * @param measurements list of measurements (not null; can be empty)
   */
  public MeasurementVector(long timeUtc, List<Measurement> measurements) {
    this.timeUtc = timeUtc;
    Map<String, Measurement> map = new LinkedHashMap<>();
    for (Measurement m : measurements) {
      map.put(m.getSourceId(), m);
    }
    this.measurements = Collections.unmodifiableMap(map);
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
}
