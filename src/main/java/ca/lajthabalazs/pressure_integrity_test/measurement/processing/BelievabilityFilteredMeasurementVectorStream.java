package ca.lajthabalazs.pressure_integrity_test.measurement.processing;

import ca.lajthabalazs.pressure_integrity_test.config.HumiditySensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfig;
import ca.lajthabalazs.pressure_integrity_test.config.TemperatureSensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.ValidRange;
import ca.lajthabalazs.pressure_integrity_test.measurement.ErrorSeverity;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementError;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.streaming.MeasurementVectorStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A measurement vector stream that adds errors for site-config sensors whose value is outside the
 * valid range, leaving all measurements intact.
 *
 * <p>For each incoming {@link MeasurementVector}, only sensors in the given map (from site config)
 * are checked. If a measurement's value lies outside the sensor's {@link ValidRange}, a {@link
 * MeasurementError} (WARNING) is added. All measurements are kept; no values are dropped. Severe
 * error vectors are passed through unchanged.
 *
 * <p>Sensor list is delegated to the source stream. Use {@link #stop()} to unsubscribe from the
 * source and stop publishing.
 */
public class BelievabilityFilteredMeasurementVectorStream extends MeasurementVectorStream {

  private final MeasurementVectorStream source;
  private MeasurementVectorStream.Subscription sourceSubscription;
  private Map<String, SensorConfig> sensorsById;

  /**
   * Creates a believability-filtered stream that wraps the given source and applies valid-range
   * filtering using the given site config.
   *
   * @param source the stream to read measurement vectors from
   * @param siteConfig site configuration containing sensors; if null, no sensors are checked (all
   *     measurements passed through)
   */
  public BelievabilityFilteredMeasurementVectorStream(
      MeasurementVectorStream source, SiteConfig siteConfig) {
    this.source = source;
    this.sourceSubscription = source.subscribe(this::computeAndPublish);
    this.sensorsById =
        siteConfig.getSensors().stream()
            .collect(Collectors.toMap(SensorConfig::getId, sensorConfig -> sensorConfig));
  }

  private void computeAndPublish(MeasurementVector vector) {
    if (vector.hasSevereError()) {
      publish(vector);
      return;
    }
    List<MeasurementError> errors = new ArrayList<>(vector.getErrors());
    for (Measurement m : vector.getMeasurementsMap().values()) {
      if (sensorsById.containsKey(m.getSourceId()) && !isWithinValidRange(m)) {
        errors.add(
            new MeasurementError(m.getSourceId(), ErrorSeverity.WARNING, "Out of valid range"));
      }
    }
    publish(new MeasurementVector(vector.getTimeUtc(), vector.getMeasurements(), errors));
  }

  private boolean isWithinValidRange(Measurement m) {
    SensorConfig sensor = sensorsById.get(m.getSourceId());
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
