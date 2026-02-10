package ca.lajthabalazs.pressure_integrity_test.measurement.processing;

import ca.lajthabalazs.pressure_integrity_test.config.CalibrationConfig;
import ca.lajthabalazs.pressure_integrity_test.config.LinearCalibration;
import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.streaming.MeasurementVectorStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A measurement vector stream that applies a calibration configuration to another stream.
 *
 * <p>Subscribes to a source stream and, for each incoming {@link MeasurementVector}, applies the
 * configured {@link LinearCalibration} per sensor (by source id). If a sensor has no calibration,
 * its measurement is passed through unchanged. The resulting vector is published to subscribers of
 * this stream.
 *
 * <p>Sensor list is delegated to the source stream. Use {@link #stop()} to unsubscribe from the
 * source and stop publishing.
 */
public class CalibratedMeasurementVectorStream extends MeasurementVectorStream {

  private final MeasurementVectorStream source;
  private final CalibrationConfig calibrationConfig;
  private MeasurementVectorStream.Subscription sourceSubscription;

  /**
   * Creates a calibrated stream that wraps the given source and applies the given calibration
   * config.
   *
   * @param source the stream to read measurement vectors from
   * @param calibrationConfig calibration to apply per sensor (by source id); may be null or empty
   *     (values are then passed through)
   */
  public CalibratedMeasurementVectorStream(
      MeasurementVectorStream source, CalibrationConfig calibrationConfig) {
    this.source = source;
    this.calibrationConfig =
        calibrationConfig != null ? calibrationConfig : new CalibrationConfig();
    this.sourceSubscription =
        source.subscribe(
            vector -> {
              Map<String, Measurement> raw = vector.getMeasurementsMap();
              List<Measurement> calibrated = new ArrayList<>(raw.size());
              for (Measurement m : raw.values()) {
                LinearCalibration cal =
                    this.calibrationConfig.getCalibrationForSensor(m.getSourceId());
                if (cal != null) {
                  BigDecimal calibratedValue = cal.getCalibratedValue(m.getValueInDefaultUnit());
                  if (calibratedValue != null) {
                    calibrated.add(m.withNewValueInDefaultUnit(calibratedValue));
                  } else {
                    calibrated.add(m);
                  }
                } else {
                  calibrated.add(m);
                }
              }
              publish(new MeasurementVector(vector.getTimeUtc(), calibrated));
            });
  }

  @Override
  public List<SensorConfig> listSensors() {
    return source.listSensors();
  }

  /**
   * Stops applying calibration: unsubscribes from the source stream. No further vectors will be
   * published.
   */
  public void stop() {
    if (sourceSubscription != null) {
      sourceSubscription.unsubscribe();
      sourceSubscription = null;
    }
    clearSubscribers();
  }
}
