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
 * filtering based on the site's sensor configuration.
 *
 * <p>The chain is currently:
 *
 * <ol>
 *   <li>{@link BelievabilityFilteredMeasurementVectorStream}
 * </ol>
 *
 * <p>Subscribers register via {@link #subscribe(MeasurementVectorHandler)} and receive vectors
 * emitted by the believability-filtered stage. {@link #listSensors()} returns the site's sensor
 * list in order. {@link #stop()} unsubscribes from the tail and stops the wrapped stream so that
 * all subscriptions are cleared.
 */
public final class StackedMeasurementVectorStream extends MeasurementVectorStream {

  private final SiteConfig siteConfig;
  private MeasurementVectorStream.Subscription tailSubscription;

  private final BelievabilityFilteredMeasurementVectorStream believability;

  /**
   * Builds the chain and returns a stream that delegates to the believability-filtered stream and
   * exposes the site's sensor list.
   *
   * @param siteConfig site configuration (sensors); must not be null
   * @param source the measurement vector stream to feed the chain
   */
  public StackedMeasurementVectorStream(SiteConfig siteConfig, MeasurementVectorStream source) {
    this.siteConfig = siteConfig;

    Map<String, SensorConfig> sensorMapById = buildSensorMapById(siteConfig);

    this.believability = new BelievabilityFilteredMeasurementVectorStream(source, sensorMapById);
    this.tailSubscription = believability.subscribe(this::publish);
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
   * Stops the chain: unsubscribes from the tail, then stops the believability stream so that no
   * events are delivered and all subscriptions are cleared.
   */
  public void stop() {
    if (tailSubscription != null) {
      tailSubscription.unsubscribe();
      tailSubscription = null;
    }
    believability.stop();
    clearSubscribers();
  }
}
