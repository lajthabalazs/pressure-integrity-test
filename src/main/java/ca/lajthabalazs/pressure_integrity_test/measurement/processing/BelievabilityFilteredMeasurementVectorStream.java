package ca.lajthabalazs.pressure_integrity_test.measurement.processing;

import ca.lajthabalazs.pressure_integrity_test.config.HumiditySensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.TemperatureSensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.ValidRange;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.streaming.MeasurementVectorStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A measurement vector stream that filters measurements by sensor believability (valid range).
 *
 * <p>Subscribes to a source stream and, for each incoming {@link MeasurementVector}, keeps only
 * measurements whose value lies within the sensor's lower and upper thresholds (valid range). The
 * sensor map is keyed by sensor id; only sensors that define a {@link ValidRange} (e.g. {@link
 * TemperatureSensorConfig}, {@link HumiditySensorConfig}) are filtered. Sensors without a valid
 * range are passed through. The resulting vector is published to subscribers of this stream.
 *
 * <p>Sensor list is delegated to the source stream. Use {@link #stop()} to unsubscribe from the
 * source and stop publishing.
 */
public class BelievabilityFilteredMeasurementVectorStream extends MeasurementVectorStream {

  private final MeasurementVectorStream source;
  private final Map<String, SensorConfig> sensorMapById;
  private MeasurementVectorStream.Subscription sourceSubscription;

  /**
   * Creates a believability-filtered stream that wraps the given source and applies valid-range
   * filtering using the given sensor map.
   *
   * @param source the stream to read measurement vectors from
   * @param sensorMapById sensors by id; used to obtain lower/upper thresholds (valid range) per
   *     sensor. Sensors without a valid range are not filtered (their measurements are always
   *     included).
   */
  public BelievabilityFilteredMeasurementVectorStream(
      MeasurementVectorStream source, Map<String, SensorConfig> sensorMapById) {
    this.source = source;
    this.sensorMapById = sensorMapById != null ? sensorMapById : Map.of();
    this.sourceSubscription =
        source.subscribe(
            vector -> {
              List<Measurement> withinRange = new ArrayList<>();
              for (Measurement m : vector.getMeasurementsMap().values()) {
                if (isWithinValidRange(m)) {
                  withinRange.add(m);
                }
              }
              publish(new MeasurementVector(vector.getTimeUtc(), withinRange));
            });
  }

  private boolean isWithinValidRange(Measurement m) {
    SensorConfig sensor = sensorMapById.get(m.getSourceId());
    ValidRange range = getValidRange(sensor);
    if (range == null || range.getMin() == null || range.getMax() == null) {
      return true;
    }
    BigDecimal value = m.getValueInDefaultUnit();
    if (value == null) {
      return false;
    }
    return value.compareTo(range.getMin()) >= 0 && value.compareTo(range.getMax()) <= 0;
  }

  private static ValidRange getValidRange(SensorConfig sensor) {
    if (sensor == null) {
      return null;
    }
    if (sensor instanceof TemperatureSensorConfig) {
      return ((TemperatureSensorConfig) sensor).getValidRange();
    }
    if (sensor instanceof HumiditySensorConfig) {
      return ((HumiditySensorConfig) sensor).getValidRange();
    }
    return null;
  }

  @Override
  public List<SensorConfig> listSensors() {
    return source.listSensors();
  }

  /** Stops filtering: unsubscribes from the source stream. No further vectors will be published. */
  public void stop() {
    if (sourceSubscription != null) {
      sourceSubscription.unsubscribe();
      sourceSubscription = null;
    }
    clearSubscribers();
  }
}
