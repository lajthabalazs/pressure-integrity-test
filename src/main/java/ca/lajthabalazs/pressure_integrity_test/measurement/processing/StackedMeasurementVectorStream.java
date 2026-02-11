package ca.lajthabalazs.pressure_integrity_test.measurement.processing;

import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfig;
import ca.lajthabalazs.pressure_integrity_test.measurement.streaming.MeasurementVectorStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple stacked measurement vector stream that wraps a source stream and applies believability
 * filtering and average pressure calculation based on the site's sensor configuration.
 *
 * <p>Each step uses {@link MeasurementFilter#filter} when it needs site-config sensors for
 * computation; the input vector is left intact and results/errors are added. Chain:
 *
 * <ol>
 *   <li>{@link BelievabilityFilteredMeasurementVectorStream} – adds errors for out-of-range
 *   <li>{@link AveragePressureMeasurementVectorStream} – averages site-config pressures, adds
 *       result
 * </ol>
 *
 * <p>Subscribers register via {@link #subscribe(MeasurementVectorHandler)} and receive vectors
 * emitted by the final stage. {@link #listSensors()} returns the site's sensor list in order.
 * {@link #stop()} unsubscribes from the tail and stops the wrapped streams so that all
 * subscriptions are cleared.
 */
public final class StackedMeasurementVectorStream extends MeasurementVectorStream {

  private final SiteConfig siteConfig;
  private MeasurementVectorStream.Subscription tailSubscription;

  private final BelievabilityFilteredMeasurementVectorStream believability;
  private final AveragePressureMeasurementVectorStream averagePressure;

  /**
   * Builds the chain and returns a stream that delegates to the final stage and exposes the site's
   * sensor list.
   *
   * @param siteConfig site configuration (sensors); must not be null
   * @param source the measurement vector stream to feed the chain
   */
  public StackedMeasurementVectorStream(SiteConfig siteConfig, MeasurementVectorStream source) {
    this.siteConfig = siteConfig;

    Map<String, SensorConfig> sensorMapById = buildSensorMapById(siteConfig);

    this.believability = new BelievabilityFilteredMeasurementVectorStream(source, siteConfig);
    this.averagePressure = new AveragePressureMeasurementVectorStream(believability, siteConfig);
    this.tailSubscription = averagePressure.subscribe(this::publish);
  }

  private static Map<String, SensorConfig> buildSensorMapById(SiteConfig siteConfig) {
    Map<String, SensorConfig> map = new LinkedHashMap<>();
    for (SensorConfig s : siteConfig.getSensors()) {
      map.put(s.getId(), s);
    }
    return map;
  }

  @Override
  public List<SensorConfig> listSensors() {
    // SiteConfig#getSensors never returns null; always provide a mutable copy.
    return new ArrayList<>(siteConfig.getSensors());
  }

  /**
   * Stops the chain: unsubscribes from the tail, then stops each stage so that no events are
   * delivered and all subscriptions are cleared.
   */
  public void stop() {
    if (tailSubscription != null) {
      tailSubscription.unsubscribe();
      tailSubscription = null;
    }
    averagePressure.stop();
    believability.stop();
    clearSubscribers();
  }
}
