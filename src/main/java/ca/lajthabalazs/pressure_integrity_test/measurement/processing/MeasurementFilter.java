package ca.lajthabalazs.pressure_integrity_test.measurement.processing;

import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfig;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility for filtering measurement vectors to only those measurements whose source id matches a
 * sensor in the site configuration.
 *
 * <p>Site config sensor ids must match ITV measurement keys (p1, p2, T1..T61, fi1..fi10).
 * Measurements whose source id is not in the site's sensor id set are dropped (e.g. main pressure
 * "p" when not in config).
 */
public final class MeasurementFilter {

  private MeasurementFilter() {
    // Utility class
  }

  /**
   * Returns a new vector containing only measurements whose source id is a sensor id in the given
   * site config. Severe-error vectors are returned unchanged.
   *
   * @param vector the measurement vector to filter
   * @param siteConfig site configuration; only sensors in this config are allowed
   * @return a new vector with the same timestamp and errors but only allowed measurements
   */
  public static MeasurementVector filter(MeasurementVector vector, SiteConfig siteConfig) {
    Set<String> allowed =
        siteConfig.getSensors().stream().map(SensorConfig::getId).collect(Collectors.toSet());
    List<Measurement> kept = new ArrayList<>();
    for (Measurement m : vector.getMeasurementsMap().values()) {
      if (allowed.contains(m.getSourceId())) {
        kept.add(m);
      }
    }
    return new MeasurementVector(vector.getTimeUtc(), kept, vector.getErrors());
  }
}
