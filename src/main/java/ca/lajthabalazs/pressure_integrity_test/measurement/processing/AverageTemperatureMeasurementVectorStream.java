package ca.lajthabalazs.pressure_integrity_test.measurement.processing;

import ca.lajthabalazs.pressure_integrity_test.config.LocationConfig;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.Temperature;
import ca.lajthabalazs.pressure_integrity_test.measurement.streaming.MeasurementVectorStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A measurement vector stream that adds a volume‑weighted average temperature to each incoming
 * vector.
 *
 * <p>For every {@link MeasurementVector} from the wrapped {@link #source} stream, this class:
 *
 * <ul>
 *   <li>Collects all {@link Temperature} measurements
 *   <li>Looks up a volume factor for each sensor from its {@link LocationConfig} via a location map
 *   <li>Filters out temperatures outside the {@code [0, 100]} °C range (credibility check)
 *   <li>Computes a volume‑weighted harmonic mean in Celsius using the algorithm from the original
 *       ILRT code:
 *       <pre>
 *       Vsum   = Σ (CF_j * v_j)
 *       denom  = Σ (CF_j * v_j / T_j)
 *       T_aver = Vsum / denom
 *       </pre>
 *   <li>Publishes a new vector containing all original measurements plus one synthetic {@link
 *       Temperature} measurement with source id {@link #AVG_TEMPERATURE_SOURCE_ID} and value {@code
 *       T_aver} (in °C)
 * </ul>
 *
 * <p>If a vector has no credible temperature measurements or no usable volume factors, it is not
 * published.
 *
 * <p>Note: callers can obtain the Kelvin value via {@link Temperature#getKelvinValue()} on the
 * synthetic measurement, mirroring the original ILRT behaviour of returning temperature in Kelvin.
 */
public class AverageTemperatureMeasurementVectorStream extends MeasurementVectorStream {

  /** Source id of the synthetic average temperature measurement emitted by this stream. */
  public static final String AVG_TEMPERATURE_SOURCE_ID = "AVG_TEMPERATURE";

  /** Credible temperature range in Celsius, matching the original ILRT implementation. */
  private static final BigDecimal MIN_C = BigDecimal.ZERO;

  private static final BigDecimal MAX_C = new BigDecimal("100.0");

  /** Math context used for BigDecimal operations (sufficient precision for engineering use). */
  private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);

  private final MeasurementVectorStream source;

  /**
   * Mapping from sensor id to its {@link LocationConfig}. The {@code volumeFactor} on the location
   * is used as the volumetric weight for all temperature sensors in that location.
   */
  private final Map<String, LocationConfig> locationBySensorId;

  private MeasurementVectorStream.Subscription sourceSubscription;

  /**
   * Creates an average‑temperature stream that wraps the given source.
   *
   * @param source the underlying measurement stream
   * @param locationBySensorId mapping from sensor source id to its {@link LocationConfig}; if a
   *     sensor id is missing or its location has a null volumeFactor, it is treated as having
   *     volume factor {@code 1}
   */
  public AverageTemperatureMeasurementVectorStream(
      MeasurementVectorStream source, Map<String, LocationConfig> locationBySensorId) {
    this.source = source;
    this.locationBySensorId = locationBySensorId != null ? locationBySensorId : Map.of();
    this.sourceSubscription =
        source.subscribe(
            vector -> {
              if (vector.hasSevereError()) {
                // Propagate invalid vectors without further processing.
                publish(vector);
                return;
              }
              Temperature avg = computeAverageTemperature(vector);
              if (avg == null) {
                // No credible temperatures or no usable weights – skip publishing
                return;
              }
              List<Measurement> out = new ArrayList<>(vector.getMeasurements());
              out.add(avg);
              publish(new MeasurementVector(vector.getTimeUtc(), out, vector.getErrors()));
            });
  }

  private Temperature computeAverageTemperature(MeasurementVector vector) {
    List<Temperature> temps = new ArrayList<>();
    for (Measurement m : vector.getMeasurementsMap().values()) {
      if (m instanceof Temperature) {
        temps.add((Temperature) m);
      }
    }
    if (temps.isEmpty()) {
      return null;
    }

    BigDecimal vSum = BigDecimal.ZERO;
    BigDecimal denom = BigDecimal.ZERO;

    for (Temperature t : temps) {
      BigDecimal tempC = t.getCelsiusValue();
      if (tempC == null) {
        continue;
      }
      // Credibility check: 0 <= T <= 100 °C, like the original CF_Temp filter
      if (tempC.compareTo(MIN_C) < 0 || tempC.compareTo(MAX_C) > 0) {
        continue;
      }
      LocationConfig location = locationBySensorId.get(t.getSourceId());
      BigDecimal v = location != null ? location.getVolumeFactor() : null;
      if (v == null) {
        v = BigDecimal.ONE; // fallback weight if missing
      }
      vSum = vSum.add(v, MC);
      // denom += v_j / T_j
      if (tempC.signum() != 0) {
        denom = denom.add(v.divide(tempC, MC), MC);
      }
    }

    if (vSum.signum() == 0 || denom.signum() == 0) {
      return null;
    }

    // Harmonic mean in Celsius (equivalent to original ILRT algorithm before +273.15)
    BigDecimal avgC = vSum.divide(denom, MC);
    return new Temperature(vector.getTimeUtc(), AVG_TEMPERATURE_SOURCE_ID, avgC);
  }

  @Override
  public java.util.List<ca.lajthabalazs.pressure_integrity_test.config.SensorConfig> listSensors() {
    // Delegate to the wrapped source; the synthetic average temperature does not currently appear
    // in the sensor list, mirroring AveragePressureMeasurementVectorStream.
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
