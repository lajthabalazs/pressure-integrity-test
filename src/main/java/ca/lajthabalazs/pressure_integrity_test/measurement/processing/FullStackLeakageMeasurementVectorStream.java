package ca.lajthabalazs.pressure_integrity_test.measurement.processing;

import ca.lajthabalazs.pressure_integrity_test.config.CalibrationConfig;
import ca.lajthabalazs.pressure_integrity_test.config.HumiditySensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.LocationConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfig;
import ca.lajthabalazs.pressure_integrity_test.config.TemperatureSensorConfig;
import ca.lajthabalazs.pressure_integrity_test.measurement.streaming.MeasurementVectorStream;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A measurement vector stream that wraps a source stream and chains site-config–driven processing
 * steps: believability (add errors), optional calibration, average pressure, average temperature,
 * average gas constant (R), and leakage. Each step that needs site-config sensors uses {@link
 * MeasurementFilter#filter} before computation and adds results/errors to the input vector, leaving
 * all values intact.
 *
 * <p>Construction requires a {@link SiteConfig}, an optional {@link CalibrationConfig}, and a
 * source {@link MeasurementVectorStream}. The chain is:
 *
 * <ol>
 *   <li>{@link BelievabilityFilteredMeasurementVectorStream} – adds errors for out-of-range
 *   <li>{@link CalibratedMeasurementVectorStream} – only when calibration config is non-null
 *   <li>{@link AveragePressureMeasurementVectorStream}
 *   <li>{@link AverageTemperatureMeasurementVectorStream}
 *   <li>{@link AverageGasConstantMeasurementVectorStream}
 *   <li>{@link LeakageMeasurementVectorStream}
 * </ol>
 *
 * <p>Subscribers register via {@link #subscribe(MeasurementVectorHandler)} and receive vectors
 * emitted by the final (leakage) stage. {@link #listSensors()} returns the site's sensor list in
 * order. {@link #stop()} unsubscribes from the tail and stops every stream in the chain from tail
 * to head so that all subscriptions are cleared.
 */
public final class FullStackLeakageMeasurementVectorStream extends MeasurementVectorStream {

  private final SiteConfig siteConfig;
  private MeasurementVectorStream.Subscription tailSubscription;

  private final BelievabilityFilteredMeasurementVectorStream believability;
  private final CalibratedMeasurementVectorStream calibrated; // null when no calibration config
  private final AveragePressureMeasurementVectorStream averagePressure;
  private final AverageTemperatureMeasurementVectorStream averageTemperature;
  private final AverageGasConstantMeasurementVectorStream averageGasConstant;
  private final LeakageMeasurementVectorStream leakage;

  /**
   * Builds the chain and returns a stream that delegates to the leakage stream and exposes the
   * site's sensor list.
   *
   * @param siteConfig site configuration (sensors, locations, humidity pairing); must not be null
   * @param calibrationConfig optional calibration; if null, no calibration step is applied
   * @param source the measurement vector stream to feed the chain
   */
  public FullStackLeakageMeasurementVectorStream(
      SiteConfig siteConfig, CalibrationConfig calibrationConfig, MeasurementVectorStream source) {
    this.siteConfig = siteConfig;

    Map<String, LocationConfig> locationBySensorId = buildLocationBySensorId(siteConfig);
    Map<String, String> humidityToTemperatureSensorId =
        buildHumidityToTemperatureSensorId(siteConfig);

    this.believability = new BelievabilityFilteredMeasurementVectorStream(source, siteConfig);
    MeasurementVectorStream afterBelievability;
    if (calibrationConfig != null) {
      this.calibrated = new CalibratedMeasurementVectorStream(believability, calibrationConfig);
      afterBelievability = calibrated;
    } else {
      this.calibrated = null;
      afterBelievability = believability;
    }
    this.averagePressure =
        new AveragePressureMeasurementVectorStream(afterBelievability, siteConfig);
    this.averageTemperature =
        new AverageTemperatureMeasurementVectorStream(
            averagePressure, locationBySensorId, siteConfig);
    this.averageGasConstant =
        new AverageGasConstantMeasurementVectorStream(
            averageTemperature, locationBySensorId, humidityToTemperatureSensorId, siteConfig);
    this.leakage = new LeakageMeasurementVectorStream(averageGasConstant);
    this.tailSubscription = leakage.subscribe(this::publish);
  }

  private static Map<String, LocationConfig> buildLocationBySensorId(SiteConfig siteConfig) {
    Map<String, LocationConfig> map = new LinkedHashMap<>();
    List<LocationConfig> locations = siteConfig.getLocations();
    for (LocationConfig loc : locations) {
      for (SensorConfig s : loc.getSensors()) {
        map.put(s.getId(), loc);
      }
    }
    return map;
  }

  /**
   * Builds a humidity→temperature pairing map based on sensor locations.
   *
   * <p>For each location, all humidity sensors are paired with the first temperature sensor found
   * in the same location. If a location has no temperature sensors, its humidity sensors are not
   * paired.
   */
  @VisibleForTesting
  static Map<String, String> buildHumidityToTemperatureSensorId(SiteConfig siteConfig) {
    Map<String, String> map = new LinkedHashMap<>();
    for (LocationConfig loc : siteConfig.getLocations()) {
      String primaryTempId = null;
      List<String> humidityIds = new ArrayList<>();
      for (SensorConfig s : loc.getSensors()) {
        if (s instanceof TemperatureSensorConfig && primaryTempId == null) {
          primaryTempId = s.getId();
        } else if (s instanceof HumiditySensorConfig) {
          humidityIds.add(s.getId());
        }
      }
      if (primaryTempId == null) {
        continue;
      }
      for (String hId : humidityIds) {
        map.put(hId, primaryTempId);
      }
    }
    return map;
  }

  @Override
  public List<SensorConfig> listSensors() {
    // SiteConfig#getSensors never returns null; always provide a mutable copy.
    return new ArrayList<>(siteConfig.getSensors());
  }

  /**
   * Stops the entire chain: unsubscribes from the tail, then stops each stream from tail to head so
   * that no events are delivered and all subscriptions are cleared.
   */
  public void stop() {
    tailSubscription.unsubscribe();
    tailSubscription = null;
    leakage.stop();
    averageGasConstant.stop();
    averageTemperature.stop();
    averagePressure.stop();
    if (calibrated != null) {
      calibrated.stop();
    }
    believability.stop();
    clearSubscribers();
  }
}
