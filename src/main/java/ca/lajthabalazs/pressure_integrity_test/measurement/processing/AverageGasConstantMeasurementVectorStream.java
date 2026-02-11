package ca.lajthabalazs.pressure_integrity_test.measurement.processing;

import ca.lajthabalazs.pressure_integrity_test.config.LocationConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfig;
import ca.lajthabalazs.pressure_integrity_test.measurement.GasConstant;
import ca.lajthabalazs.pressure_integrity_test.measurement.Humidity;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.Pressure;
import ca.lajthabalazs.pressure_integrity_test.measurement.Temperature;
import ca.lajthabalazs.pressure_integrity_test.measurement.streaming.MeasurementVectorStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A measurement vector stream that adds a volume‑weighted harmonic mean of the specific gas
 * constant of humid air (R) to each incoming vector.
 *
 * <p>This stream expects to wrap the output of the {@link AveragePressureMeasurementVectorStream}
 * and {@link AverageTemperatureMeasurementVectorStream} chain. For every incoming {@link
 * MeasurementVector} it:
 *
 * <ul>
 *   <li>Reads the global average pressure {@link Pressure} with source id {@link
 *       AveragePressureMeasurementVectorStream#AVG_PRESSURE_SOURCE_ID}
 *   <li>Pairs each {@link Humidity} measurement with its temperature measurement using the provided
 *       humidity → temperature sensor mapping
 *   <li>Computes saturation pressure from temperature using the Antoine equation, ported from the
 *       ILRT reference implementation
 *   <li>Computes a specific gas constant R for each humidity/temperature pair using the ILRT
 *       formula for humid air
 *   <li>Applies a credibility filter equivalent to the original CF_Hum (0–100 % humidity and 0–100
 *       °C temperature)
 *   <li>Computes a volume‑weighted harmonic mean of the per‑location R values using the location's
 *       volume factor as weight
 *   <li>Uses {@link MeasurementFilter#filter} for humidity/temperature (site-config only); reads
 *       {@link AveragePressureMeasurementVectorStream#AVG_PRESSURE_SOURCE_ID} from the original
 *       vector. Publishes the <em>original</em> vector plus one synthetic {@link GasConstant}
 *       ({@link #AVG_R_SOURCE_ID}).
 * </ul>
 *
 * <p>If there is no credible data (missing average pressure, no valid humidity/temperature pairs,
 * or zero total weight), the vector is not published.
 */
public class AverageGasConstantMeasurementVectorStream extends MeasurementVectorStream {

  /** Source id of the synthetic average gas constant measurement emitted by this stream. */
  public static final String AVG_R_SOURCE_ID = "AVG_R";

  /** Gas constant of dry air [Nm/(kg·K)] in ILRT reference implementation. */
  private static final BigDecimal R_L = new BigDecimal("286.9");

  /** Gas constant of water vapour [Nm/(kg·K)] in ILRT reference implementation. */
  private static final BigDecimal R_G = new BigDecimal("460.7");

  /** Math context for all BigDecimal operations. */
  private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);

  private final MeasurementVectorStream source;
  private final SiteConfig siteConfig;

  /**
   * Mapping from sensor id to its {@link LocationConfig}. Used to obtain the volume factor for the
   * location of each paired humidity/temperature measurement.
   */
  private final Map<String, LocationConfig> locationBySensorId;

  /**
   * Mapping from humidity sensor id to the id of its paired temperature sensor (for the same
   * physical location).
   */
  private final Map<String, String> humidityToTemperatureSensorId;

  private MeasurementVectorStream.Subscription sourceSubscription;

  /**
   * Creates an average‑R stream that wraps the given source and uses only site-config sensors for
   * humidity/temperature (via {@link MeasurementFilter#filter}); average pressure is read from the
   * incoming vector.
   *
   * @param source the underlying measurement stream (typically the output of the
   *     AveragePressure→AverageTemperature chain)
   * @param locationBySensorId mapping from sensor source id to its {@link LocationConfig}; if
   *     missing or its volume factor is null, a volume factor of {@code 1} is assumed
   * @param humidityToTemperatureSensorId mapping from humidity sensor id to its paired temperature
   *     sensor id
   * @param siteConfig site configuration; only its sensors are used for humidity/temperature in R
   */
  public AverageGasConstantMeasurementVectorStream(
      MeasurementVectorStream source,
      Map<String, LocationConfig> locationBySensorId,
      Map<String, String> humidityToTemperatureSensorId,
      SiteConfig siteConfig) {
    this.source = source;
    this.locationBySensorId = locationBySensorId != null ? locationBySensorId : Map.of();
    this.humidityToTemperatureSensorId =
        humidityToTemperatureSensorId != null ? humidityToTemperatureSensorId : Map.of();
    this.siteConfig = siteConfig;
    this.sourceSubscription = source.subscribe(this::computeAndPublish);
  }

  private void computeAndPublish(MeasurementVector vector) {
    if (vector.hasSevereError()) {
      publish(vector);
      return;
    }
    MeasurementVector filtered = MeasurementFilter.filter(vector, siteConfig);
    GasConstant avg = computeAverageGasConstant(vector, filtered);
    if (avg == null) {
      return;
    }
    List<Measurement> out = new ArrayList<>(vector.getMeasurements());
    out.add(avg);
    publish(new MeasurementVector(vector.getTimeUtc(), out, vector.getErrors()));
  }

  private GasConstant computeAverageGasConstant(
      MeasurementVector vectorWithAvgPressure, MeasurementVector filteredSiteSensors) {
    Pressure avgPressure =
        vectorWithAvgPressure.getMeasurementsMap().values().stream()
            .filter(m -> m instanceof Pressure)
            .map(m -> (Pressure) m)
            .filter(
                p ->
                    AveragePressureMeasurementVectorStream.AVG_PRESSURE_SOURCE_ID.equals(
                        p.getSourceId()))
            .findFirst()
            .orElse(null);

    if (avgPressure == null) {
      return null;
    }

    BigDecimal presAver = avgPressure.getPascalValue();

    // Index temperature measurements by sensor id for fast lookup (from site-config filtered view).
    Map<String, Temperature> temperatureById = new HashMap<>();
    for (Measurement m : filteredSiteSensors.getMeasurementsMap().values()) {
      if (m instanceof Temperature) {
        temperatureById.put(m.getSourceId(), (Temperature) m);
      }
    }

    BigDecimal sumWeights = BigDecimal.ZERO;
    BigDecimal sumWeightsOverR = BigDecimal.ZERO;

    for (Measurement m : filteredSiteSensors.getMeasurementsMap().values()) {
      if (!(m instanceof Humidity humidity)) {
        continue;
      }

      BigDecimal phi = humidity.getValueInDefaultUnit(); // percentage

      String humidityId = humidity.getSourceId();
      String tempId = humidityToTemperatureSensorId.get(humidityId);

      Temperature temp = temperatureById.get(tempId);

      BigDecimal tempC = temp.getCelsiusValue();

      // Volume factor from the location of the paired temperature sensor; default to 1 if missing.
      LocationConfig location = locationBySensorId.get(tempId);
      BigDecimal volumeFactor = location.getVolumeFactor();
      if (volumeFactor.signum() <= 0) {
        continue;
      }

      BigDecimal presSat = calcSaturationPressure(tempC);

      BigDecimal r = calcSpecificGasConstant(presAver, phi, presSat);

      sumWeights = sumWeights.add(volumeFactor, MC);
      sumWeightsOverR = sumWeightsOverR.add(volumeFactor.divide(r, MC), MC);
    }

    BigDecimal rMean = sumWeights.divide(sumWeightsOverR, MC);
    return new GasConstant(vectorWithAvgPressure.getTimeUtc(), AVG_R_SOURCE_ID, rMean);
  }

  /**
   * Calculates saturation pressure of water vapour using the Antoine equation from the ILRT code.
   *
   * <p>Applicable range: 20–70 °C. Outside this range, the formula is still evaluated but the
   * caller is expected to apply its own validity checks.
   */
  private static BigDecimal calcSaturationPressure(BigDecimal tempC) {
    // Antoine coefficients for water, temperature in Celsius.
    BigDecimal A = new BigDecimal("6.20963");
    BigDecimal B = new BigDecimal("2354.731");
    BigDecimal C = new BigDecimal("7.559");

    BigDecimal denominator = tempC.add(C, MC);
    if (denominator.signum() == 0) {
      return null;
    }

    BigDecimal exponent = A.subtract(B.divide(denominator, MC), MC); // A - B / (T + C)

    // 10^exponent using BigDecimal: 10^x = e^(x * ln 10)
    double expDouble = exponent.doubleValue();
    double value = Math.pow(10.0, expDouble);
    return new BigDecimal(value, MC);
  }

  /**
   * Calculates the specific gas constant of humid air for a single location, ported from the ILRT
   * implementation:
   *
   * <pre>
   * R_nl = (R_L * P_aver) / (P_aver - (1 - R_L / R_G) * (phi / 100) * P_sat)
   * </pre>
   *
   * @param presAver average absolute pressure (Pa)
   * @param phi relative humidity (%)
   * @param presSat saturation pressure of water vapour at the local temperature (same units as
   *     presAver)
   */
  private static BigDecimal calcSpecificGasConstant(
      BigDecimal presAver, BigDecimal phi, BigDecimal presSat) {

    BigDecimal oneMinusRatio = BigDecimal.ONE.subtract(R_L.divide(R_G, MC), MC);
    BigDecimal phiFraction = phi.divide(new BigDecimal("100.0"), MC);
    BigDecimal correction = oneMinusRatio.multiply(phiFraction, MC).multiply(presSat, MC);
    BigDecimal denominator = presAver.subtract(correction, MC);
    return R_L.multiply(presAver, MC).divide(denominator, MC);
  }

  @Override
  public List<ca.lajthabalazs.pressure_integrity_test.config.SensorConfig> listSensors() {
    // Delegate to the wrapped source; the synthetic average R does not currently appear in the
    // sensor list, mirroring the other average streams.
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
