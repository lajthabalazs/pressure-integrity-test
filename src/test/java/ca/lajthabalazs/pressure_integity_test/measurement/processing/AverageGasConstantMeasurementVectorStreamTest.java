package ca.lajthabalazs.pressure_integity_test.measurement.processing;

import ca.lajthabalazs.pressure_integity_test.measurement.streaming.TestMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.config.LocationConfig;
import ca.lajthabalazs.pressure_integrity_test.measurement.ErrorSeverity;
import ca.lajthabalazs.pressure_integrity_test.measurement.GasConstant;
import ca.lajthabalazs.pressure_integrity_test.measurement.Humidity;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementError;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.Pressure;
import ca.lajthabalazs.pressure_integrity_test.measurement.Temperature;
import ca.lajthabalazs.pressure_integrity_test.measurement.processing.AverageGasConstantMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.measurement.processing.AveragePressureMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.measurement.processing.AverageTemperatureMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.measurement.streaming.MeasurementVectorStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link AverageGasConstantMeasurementVectorStream}. */
public class AverageGasConstantMeasurementVectorStreamTest {

  private TestMeasurementVectorStream source;
  private List<MeasurementVector> received;
  private Map<String, LocationConfig> locationsBySensorId;
  private Map<String, String> humidityToTemperature;

  @BeforeEach
  public void setUp() {
    source = new TestMeasurementVectorStream();
    received = new ArrayList<>();
    locationsBySensorId = new HashMap<>();
    humidityToTemperature = new HashMap<>();
  }

  private LocationConfig location(String id, String volumeFactor) {
    LocationConfig loc = new LocationConfig();
    loc.setId(id);
    loc.setVolumeFactor(volumeFactor != null ? new BigDecimal(volumeFactor) : null);
    return loc;
  }

  private AverageGasConstantMeasurementVectorStream newStream(
      MeasurementVectorStream wrappedSource) {
    return new AverageGasConstantMeasurementVectorStream(
        wrappedSource, locationsBySensorId, humidityToTemperature);
  }

  /**
   * Simple happy path: one average pressure, one humidity/temperature pair, volume factor 1.0.
   *
   * <p>Verifies that:
   *
   * <ul>
   *   <li>Output vector is published
   *   <li>All originals are preserved
   *   <li>A synthetic {@link GasConstant} with source id {@link
   *       AverageGasConstantMeasurementVectorStream#AVG_R_SOURCE_ID} is present
   * </ul>
   */
  @Test
  public void singlePair_emitsAverageGasConstant() {
    // Base stream -> average pressure -> average temperature -> average gas constant
    AveragePressureMeasurementVectorStream avgPressure =
        new AveragePressureMeasurementVectorStream(source);
    AverageTemperatureMeasurementVectorStream avgTemp =
        new AverageTemperatureMeasurementVectorStream(avgPressure, locationsBySensorId);
    AverageGasConstantMeasurementVectorStream avgR = newStream(avgTemp);

    avgR.subscribe(received::add);

    // Configuration: temperature sensor T1 at location L1 with volume factor 1.0
    locationsBySensorId.put("T1", location("L1", "1.0"));
    humidityToTemperature.put("H1", "T1");

    // One cycle: pressure, temperature (20 °C), humidity (50 %)
    Pressure p = new Pressure(1000L, "P1", new BigDecimal("101325"));
    Temperature t = new Temperature(1000L, "T1", new BigDecimal("20.0"));
    Humidity h = new Humidity(1000L, "H1", new BigDecimal("50.0"));

    source.publishToSubscribers(new MeasurementVector(1000L, List.of(p, t, h)));

    Assertions.assertEquals(1, received.size());
    MeasurementVector out = received.getFirst();

    // Expect original 3 + 1 avg pressure + 1 avg temperature + 1 avg R = 6
    Assertions.assertEquals(6, out.getMeasurements().size());

    Measurement rMeasurement =
        out.getMeasurements().stream()
            .filter(
                m ->
                    AverageGasConstantMeasurementVectorStream.AVG_R_SOURCE_ID.equals(
                        m.getSourceId()))
            .findFirst()
            .orElseThrow();

    Assertions.assertInstanceOf(GasConstant.class, rMeasurement);
    Assertions.assertTrue(rMeasurement.getValueInDefaultUnit().doubleValue() > 0.0);

    avgR.stop();
    avgTemp.stop();
    avgPressure.stop();
  }

  /** No average pressure in the vector means no R is published. */
  @Test
  public void noAveragePressure_nothingPublished() {
    AverageGasConstantMeasurementVectorStream avgR = newStream(source);
    avgR.subscribe(received::add);

    // Only temperature and humidity; no average pressure present
    Temperature t = new Temperature(1000L, "T1", new BigDecimal("20.0"));
    Humidity h = new Humidity(1000L, "H1", new BigDecimal("50.0"));

    source.publishToSubscribers(new MeasurementVector(1000L, List.of(t, h)));

    Assertions.assertEquals(0, received.size());
    avgR.stop();
  }

  /**
   * Negative or zero volume factor causes the pair to be ignored, leading to no R being published
   * when it is the only pair.
   */
  @Test
  public void nonPositiveVolumeFactor_ignored_noAveragePublished() {
    // T1 has a non-positive volume factor
    locationsBySensorId.put("T1", location("L1", "0.0"));
    humidityToTemperature.put("H1", "T1");

    AverageGasConstantMeasurementVectorStream avgR = newStream(source);
    avgR.subscribe(received::add);

    Pressure avgP =
        new Pressure(
            1000L,
            AveragePressureMeasurementVectorStream.AVG_PRESSURE_SOURCE_ID,
            new BigDecimal("101325"));
    Temperature t = new Temperature(1000L, "T1", new BigDecimal("20.0"));
    Humidity h = new Humidity(1000L, "H1", new BigDecimal("50.0"));

    source.publishToSubscribers(new MeasurementVector(1000L, List.of(avgP, t, h)));

    // Non-positive volume factor means the only pair is ignored → no R
    Assertions.assertEquals(0, received.size());
    avgR.stop();
  }

  /** No matching temperature for a humidity sensor means that pair is ignored. */
  @Test
  public void humidityWithoutPairedTemperature_ignored() {
    locationsBySensorId.put("T1", location("L1", "1.0"));
    humidityToTemperature.put("H1", "T1");

    AverageGasConstantMeasurementVectorStream avgR = newStream(source);
    avgR.subscribe(received::add);

    // Have average pressure, but humidity is H2 which is not mapped to any temperature
    Pressure avgP =
        new Pressure(
            1000L,
            AveragePressureMeasurementVectorStream.AVG_PRESSURE_SOURCE_ID,
            new BigDecimal("101325"));
    Humidity h = new Humidity(1000L, "H2", new BigDecimal("50.0"));
    Temperature t = new Temperature(1000L, "T1", new BigDecimal("20.0"));

    source.publishToSubscribers(new MeasurementVector(1000L, List.of(avgP, t, h)));

    // No valid humidity/temperature pair => no R published
    Assertions.assertEquals(0, received.size());
    avgR.stop();
  }

  /**
   * Temperature value that makes the Antoine denominator zero (T = -C) results in saturation
   * pressure calculation returning null, so no R is published.
   */
  @Test
  public void saturationPressureDenominatorZero_noAveragePublished() {
    locationsBySensorId.put("T1", location("L1", "1.0"));
    humidityToTemperature.put("H1", "T1");

    AverageGasConstantMeasurementVectorStream avgR = newStream(source);
    avgR.subscribe(received::add);

    Pressure avgP =
        new Pressure(
            1000L,
            AveragePressureMeasurementVectorStream.AVG_PRESSURE_SOURCE_ID,
            new BigDecimal("101325"));
    // T = -C = -7.559 makes (T + C) = 0 in the Antoine equation
    Temperature t = new Temperature(1000L, "T1", new BigDecimal("-7.559"));
    Humidity h = new Humidity(1000L, "H1", new BigDecimal("50.0"));

    source.publishToSubscribers(new MeasurementVector(1000L, List.of(avgP, t, h)));

    // Saturation pressure cannot be calculated → pair ignored → no R
    Assertions.assertEquals(0, received.size());
    avgR.stop();
  }

  /** Constructor handles null maps by treating them as empty. */
  @Test
  public void constructor_nullMapsHandledAsEmpty() {
    AverageGasConstantMeasurementVectorStream stream =
        new AverageGasConstantMeasurementVectorStream(source, null, null);
    Assertions.assertNotNull(stream.listSensors());
    Assertions.assertTrue(stream.listSensors().isEmpty());
    stream.stop();
  }

  /** stop() is idempotent. */
  @Test
  public void stop_idempotent_secondCallDoesNotThrow() {
    AverageGasConstantMeasurementVectorStream stream = newStream(source);
    stream.stop();
    Assertions.assertDoesNotThrow(stream::stop);
  }

  /** Vector with severe error is passed through without computing average gas constant. */
  @Test
  public void severeError_passesThroughWithoutAverageR() {
    AverageGasConstantMeasurementVectorStream stream = newStream(source);
    stream.subscribe(received::add);

    Pressure p =
        new Pressure(
            1000L,
            AveragePressureMeasurementVectorStream.AVG_PRESSURE_SOURCE_ID,
            new BigDecimal("101325"));
    MeasurementError severe = new MeasurementError("P1", ErrorSeverity.SEVERE, "Test severe");
    MeasurementVector input = new MeasurementVector(1000L, List.of(p), List.of(severe));

    source.publishToSubscribers(input);

    Assertions.assertEquals(1, received.size());
    MeasurementVector out = received.getFirst();
    Assertions.assertSame(input, out);
    Assertions.assertEquals(List.of(severe), out.getErrors());
    stream.stop();
  }
}
