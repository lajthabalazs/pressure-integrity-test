package ca.lajthabalazs.pressure_integrity_test.measurement.processing;

import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.measurement.GasConstant;
import ca.lajthabalazs.pressure_integrity_test.measurement.Leakage;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.Pressure;
import ca.lajthabalazs.pressure_integrity_test.measurement.Temperature;
import ca.lajthabalazs.pressure_integrity_test.measurement.streaming.MeasurementVectorStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * A measurement vector stream that adds a leakage rate to each incoming vector, based on the
 * difference between consecutive measurement vectors.
 *
 * <p>Implements the two-point leakage calculation from the old-code reference: density is computed
 * as P/(R·T), then the regression variable yi = ln(ρ/ρ∞ − 1), and the leakage rate L in v/v%/d is
 * derived from the slope between the previous and current (yi, time) points.
 *
 * <ul>
 *   <li>First vector: publishes a {@link Leakage} measurement with value {@code -1} (no previous
 *       point to diff against).
 *   <li>Subsequent vectors: publishes the diff-based leakage rate L.
 * </ul>
 *
 * <p>Expects the source stream to provide {@link
 * AveragePressureMeasurementVectorStream#AVG_PRESSURE_SOURCE_ID}, {@link
 * AverageTemperatureMeasurementVectorStream#AVG_TEMPERATURE_SOURCE_ID}, and {@link
 * AverageGasConstantMeasurementVectorStream#AVG_R_SOURCE_ID}. If any of these are missing or
 * invalid, that vector is not published.
 */
public class LeakageMeasurementVectorStream extends MeasurementVectorStream {

  /** Source id of the synthetic leakage measurement emitted by this stream. */
  public static final String LEAKAGE_SOURCE_ID = "LEAKAGE";

  /** Reference density ρ∞ [kg/m³] near P_inf = 0.1 MPa, T_inf = 300 K, from the ILRT reference. */
  private static final double RHO_INF = 1.16144;

  /** Factor for leakage rate in v/v%/d: 8640000 = 86400 s/day × 100 for percent, from Calc_L. */
  private static final double L_SCALE = 8640000.0;

  private final MeasurementVectorStream source;
  private MeasurementVectorStream.Subscription sourceSubscription;

  /** Previous vector's timestamp (ms) and density (kg/m³); null until first vector is processed. */
  private Long prevTimeUtc;

  private Double prevRho;

  /**
   * Creates a leakage stream that wraps the given source (e.g. output of {@link
   * AverageGasConstantMeasurementVectorStream}).
   *
   * @param source the stream that provides average pressure, average temperature, and average gas
   *     constant per vector
   */
  public LeakageMeasurementVectorStream(MeasurementVectorStream source) {
    this.source = source;
    this.sourceSubscription =
        source.subscribe(
            vector -> {
              if (vector.hasSevereError()) {
                // Do not attempt leakage calculation on invalid vectors – pass through.
                publish(vector);
                return;
              }
              Double rho = densityFromVector(vector);
              if (rho == null) {
                return;
              }
              long t = vector.getTimeUtc();
              BigDecimal leakageValue;
              if (prevTimeUtc == null) {
                leakageValue = BigDecimal.ONE.negate();
              } else {
                Double L = leakageFromTwoPoints(prevRho, prevTimeUtc, rho, t);
                if (L == null) {
                  return;
                }
                leakageValue = BigDecimal.valueOf(L).setScale(6, RoundingMode.HALF_UP);
              }
              prevTimeUtc = t;
              prevRho = rho;
              Leakage leakage = new Leakage(vector.getTimeUtc(), LEAKAGE_SOURCE_ID, leakageValue);
              List<Measurement> out = new ArrayList<>(vector.getMeasurements());
              out.add(leakage);
              publish(new MeasurementVector(vector.getTimeUtc(), out, vector.getErrors()));
            });
  }

  /**
   * Computes density ρ = P/(R·T) from the vector's average pressure (Pa), average temperature (K),
   * and average gas constant R (Nm/(kg·K)). Returns null if any value is missing or invalid.
   */
  private static Double densityFromVector(MeasurementVector vector) {
    BigDecimal pPa = null;
    BigDecimal tK = null;
    BigDecimal r = null;
    for (Measurement m : vector.getMeasurementsMap().values()) {
      if (m instanceof Pressure
          && AveragePressureMeasurementVectorStream.AVG_PRESSURE_SOURCE_ID.equals(
              m.getSourceId())) {
        pPa = ((Pressure) m).getPascalValue();
      } else if (m instanceof Temperature
          && AverageTemperatureMeasurementVectorStream.AVG_TEMPERATURE_SOURCE_ID.equals(
              m.getSourceId())) {
        tK = ((Temperature) m).getKelvinValue();
      } else if (m instanceof GasConstant
          && AverageGasConstantMeasurementVectorStream.AVG_R_SOURCE_ID.equals(m.getSourceId())) {
        r = ((GasConstant) m).getValueInDefaultUnit();
      }
    }
    if (pPa == null
        || tK == null
        || r == null
        || pPa.signum() <= 0
        || tK.signum() <= 0
        || r.signum() <= 0) {
      return null;
    }
    double rho = pPa.doubleValue() / (r.doubleValue() * tK.doubleValue());
    return rho > RHO_INF ? rho : null;
  }

  /**
   * Two-point leakage rate L in v/v%/d: yi = ln(ρ/ρ∞ − 1), Arn = (yi_curr − yi_prev)/dt_sec, Brn =
   * yi_prev, L = 8640000·Arn·(1/(1+exp(Brn)) − 1). Returns null if the formula is undefined.
   */
  private static Double leakageFromTwoPoints(
      double rhoPrev, long tPrev, double rhoCurr, long tCurr) {
    double yPrev = Math.log(rhoPrev / RHO_INF - 1.0);
    double yCurr = Math.log(rhoCurr / RHO_INF - 1.0);
    double dtSec = (tCurr - tPrev) / 1000.0;
    if (dtSec <= 0 || !Double.isFinite(yPrev) || !Double.isFinite(yCurr)) {
      return null;
    }
    double Arn = (yCurr - yPrev) / dtSec;
    double Brn = yPrev;
    double term = 1.0 / (1.0 + Math.exp(Brn)) - 1.0;
    return L_SCALE * Arn * term;
  }

  @Override
  public List<SensorConfig> listSensors() {
    return source.listSensors();
  }

  /** Stops publishing: unsubscribes from the source stream and clears stored previous state. */
  public void stop() {
    if (sourceSubscription != null) {
      sourceSubscription.unsubscribe();
      sourceSubscription = null;
    }
    prevTimeUtc = null;
    prevRho = null;
    clearSubscribers();
  }
}
