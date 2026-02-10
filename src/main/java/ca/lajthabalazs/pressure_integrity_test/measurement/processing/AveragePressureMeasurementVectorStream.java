package ca.lajthabalazs.pressure_integrity_test.measurement.processing;

import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.Pressure;
import ca.lajthabalazs.pressure_integrity_test.measurement.streaming.MeasurementVectorStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * A measurement vector stream that adds an average pressure measurement to each incoming vector.
 *
 * <p>Subscribes to a source stream and, for each incoming {@link MeasurementVector}, collects all
 * {@link Pressure} measurements, computes the arithmetic mean of their values (in Pa), and
 * publishes a new vector with the same timestamp containing all original measurements plus one
 * {@link Pressure} measurement with {@link #AVG_PRESSURE_SOURCE_ID} and the average value. If a
 * vector has no pressure measurements, it is not published.
 *
 * <p>Sensor list is delegated to the source stream. Use {@link #stop()} to unsubscribe from the
 * source and stop publishing.
 */
public class AveragePressureMeasurementVectorStream extends MeasurementVectorStream {

  /** Source id of the synthetic average pressure measurement emitted by this stream. */
  public static final String AVG_PRESSURE_SOURCE_ID = "AVG_PRESSURE";

  private final MeasurementVectorStream source;
  private MeasurementVectorStream.Subscription sourceSubscription;

  /**
   * Creates an average-pressure stream that wraps the given source.
   *
   * @param source the stream to read measurement vectors from
   */
  public AveragePressureMeasurementVectorStream(MeasurementVectorStream source) {
    this.source = source;
    this.sourceSubscription =
        source.subscribe(
            vector -> {
              List<Pressure> pressures = new ArrayList<>();
              for (Measurement m : vector.getMeasurementsMap().values()) {
                if (m instanceof Pressure) {
                  pressures.add((Pressure) m);
                }
              }
              if (pressures.isEmpty()) {
                return;
              }
              BigDecimal sum = BigDecimal.ZERO;
              int count = 0;
              for (Pressure p : pressures) {
                BigDecimal v = p.getValueInDefaultUnit();
                if (v != null) {
                  sum = sum.add(v);
                  count++;
                }
              }
              if (count == 0) {
                return;
              }
              BigDecimal avg = sum.divide(BigDecimal.valueOf(count), 10, RoundingMode.HALF_UP);
              Pressure avgPressure = new Pressure(vector.getTimeUtc(), AVG_PRESSURE_SOURCE_ID, avg);
              List<Measurement> out = new ArrayList<>(vector.getMeasurements());
              out.add(avgPressure);
              publish(new MeasurementVector(vector.getTimeUtc(), out));
            });
  }

  @Override
  public List<SensorConfig> listSensors() {
    return source.listSensors();
  }

  /**
   * Stops publishing: unsubscribes from the source stream. No further vectors will be published.
   */
  public void stop() {
    if (sourceSubscription != null) {
      sourceSubscription.unsubscribe();
      sourceSubscription = null;
    }
    clearSubscribers();
  }
}
