package ca.lajthabalazs.pressure_integrity_test.measurement.processing;

import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfig;
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
 * <p>Uses {@link MeasurementFilter#filter} to consider only site-config sensors before computation.
 * Collects {@link Pressure} measurements from the filtered view, computes the arithmetic mean (in
 * Pa), and publishes the <em>original</em> vector with one added {@link Pressure} ({@link
 * #AVG_PRESSURE_SOURCE_ID}). If the filtered vector has no pressure measurements, the vector is not
 * published.
 *
 * <p>Sensor list is delegated to the source stream. Use {@link #stop()} to unsubscribe from the
 * source and stop publishing.
 */
public class AveragePressureMeasurementVectorStream extends MeasurementVectorStream {

  /** Source id of the synthetic average pressure measurement emitted by this stream. */
  public static final String AVG_PRESSURE_SOURCE_ID = "AVG_PRESSURE";

  private final MeasurementVectorStream source;
  private final SiteConfig siteConfig;
  private MeasurementVectorStream.Subscription sourceSubscription;

  /**
   * Creates an average-pressure stream that wraps the given source and uses only site-config
   * sensors for the average (via {@link MeasurementFilter#filter}).
   *
   * @param source the stream to read measurement vectors from
   * @param siteConfig site configuration; only its pressure sensors are used for the average
   */
  public AveragePressureMeasurementVectorStream(
      MeasurementVectorStream source, SiteConfig siteConfig) {
    this.source = source;
    this.siteConfig = siteConfig;
    this.sourceSubscription = source.subscribe(this::computeAndPublish);
  }

  private void computeAndPublish(MeasurementVector vector) {
    if (vector.hasSevereError()) {
      publish(vector);
      return;
    }
    MeasurementVector filtered = MeasurementFilter.filter(vector, siteConfig);
    List<Pressure> pressures = new ArrayList<>();
    for (Measurement m : filtered.getMeasurementsMap().values()) {
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
    publish(new MeasurementVector(vector.getTimeUtc(), out, vector.getErrors()));
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
