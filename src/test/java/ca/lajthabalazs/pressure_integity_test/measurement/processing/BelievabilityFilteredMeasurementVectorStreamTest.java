package ca.lajthabalazs.pressure_integity_test.measurement.processing;

import ca.lajthabalazs.pressure_integity_test.measurement.streaming.TestMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.config.HumiditySensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.PressureSensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.TemperatureSensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.ValidRange;
import ca.lajthabalazs.pressure_integrity_test.measurement.ErrorSeverity;
import ca.lajthabalazs.pressure_integrity_test.measurement.Humidity;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementError;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.Pressure;
import ca.lajthabalazs.pressure_integrity_test.measurement.Temperature;
import ca.lajthabalazs.pressure_integrity_test.measurement.processing.BelievabilityFilteredMeasurementVectorStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link BelievabilityFilteredMeasurementVectorStream}. */
public class BelievabilityFilteredMeasurementVectorStreamTest {

  private TestMeasurementVectorStream source;
  private List<MeasurementVector> received;

  @BeforeEach
  public void setUp() {
    source = new TestMeasurementVectorStream();
    received = new ArrayList<>();
  }

  private static Map<String, SensorConfig> sensorMap(
      TemperatureSensorConfig t24, HumiditySensorConfig rh4) {
    Map<String, SensorConfig> map = new LinkedHashMap<>();
    map.put("T24", t24);
    map.put("RH4", rh4);
    return map;
  }

  private static TemperatureSensorConfig temperatureSensorWithRange(
      BigDecimal min, BigDecimal max) {
    TemperatureSensorConfig s = new TemperatureSensorConfig();
    s.setId("T24");
    ValidRange r = new ValidRange();
    r.setMin(min);
    r.setMax(max);
    s.setValidRange(r);
    return s;
  }

  private static HumiditySensorConfig humiditySensorWithRange(BigDecimal min, BigDecimal max) {
    HumiditySensorConfig s = new HumiditySensorConfig();
    s.setId("RH4");
    ValidRange r = new ValidRange();
    r.setMin(min);
    r.setMax(max);
    s.setValidRange(r);
    return s;
  }

  /** When a measurement is within the sensor's valid range, it is included. */
  @Test
  public void valueWithinRange_includedInOutput() {
    TemperatureSensorConfig t24 =
        temperatureSensorWithRange(new BigDecimal("0"), new BigDecimal("80"));
    Map<String, SensorConfig> sensorMap =
        sensorMap(t24, humiditySensorWithRange(BigDecimal.ZERO, new BigDecimal("100")));

    BelievabilityFilteredMeasurementVectorStream filter =
        new BelievabilityFilteredMeasurementVectorStream(source, sensorMap);
    filter.subscribe(received::add);

    Temperature m = new Temperature(1000L, "T24", new BigDecimal("50"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(m)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(1, received.get(0).getMeasurements().size());
    Assertions.assertEquals("T24", received.get(0).getMeasurements().get(0).getSourceId());
    Assertions.assertEquals(
        0,
        new BigDecimal("50")
            .compareTo(received.get(0).getMeasurements().get(0).getValueInDefaultUnit()));
    filter.stop();
  }

  /** When a measurement is below the minimum, it is excluded. */
  @Test
  public void valueBelowMin_excludedFromOutput() {
    TemperatureSensorConfig t24 =
        temperatureSensorWithRange(new BigDecimal("0"), new BigDecimal("80"));
    Map<String, SensorConfig> sensorMap =
        sensorMap(t24, humiditySensorWithRange(BigDecimal.ZERO, new BigDecimal("100")));

    BelievabilityFilteredMeasurementVectorStream filter =
        new BelievabilityFilteredMeasurementVectorStream(source, sensorMap);
    filter.subscribe(received::add);

    Temperature m = new Temperature(1000L, "T24", new BigDecimal("-10"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(m)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(0, received.get(0).getMeasurements().size());
    filter.stop();
  }

  /** When a measurement is above the maximum, it is excluded. */
  @Test
  public void valueAboveMax_excludedFromOutput() {
    TemperatureSensorConfig t24 =
        temperatureSensorWithRange(new BigDecimal("0"), new BigDecimal("80"));
    Map<String, SensorConfig> sensorMap =
        sensorMap(t24, humiditySensorWithRange(BigDecimal.ZERO, new BigDecimal("100")));

    BelievabilityFilteredMeasurementVectorStream filter =
        new BelievabilityFilteredMeasurementVectorStream(source, sensorMap);
    filter.subscribe(received::add);

    Temperature m = new Temperature(1000L, "T24", new BigDecimal("90"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(m)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(0, received.get(0).getMeasurements().size());
    filter.stop();
  }

  /** Value exactly at min or max is included (closed interval). */
  @Test
  public void valueAtMinAndMax_included() {
    TemperatureSensorConfig t24 =
        temperatureSensorWithRange(new BigDecimal("0"), new BigDecimal("80"));
    Map<String, SensorConfig> sensorMap =
        sensorMap(t24, humiditySensorWithRange(BigDecimal.ZERO, new BigDecimal("100")));

    BelievabilityFilteredMeasurementVectorStream filter =
        new BelievabilityFilteredMeasurementVectorStream(source, sensorMap);
    filter.subscribe(received::add);

    Temperature atMin = new Temperature(1000L, "T24", BigDecimal.ZERO);
    Humidity atMax = new Humidity(1000L, "RH4", new BigDecimal("100"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(atMin, atMax)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(2, received.get(0).getMeasurements().size());
    filter.stop();
  }

  /** Multiple sensors: only those within range are included. */
  @Test
  public void multipleSensors_onlyInRangeIncluded() {
    TemperatureSensorConfig t24 =
        temperatureSensorWithRange(new BigDecimal("0"), new BigDecimal("80"));
    HumiditySensorConfig rh4 = humiditySensorWithRange(BigDecimal.ZERO, new BigDecimal("100"));
    Map<String, SensorConfig> sensorMap = sensorMap(t24, rh4);

    BelievabilityFilteredMeasurementVectorStream filter =
        new BelievabilityFilteredMeasurementVectorStream(source, sensorMap);
    filter.subscribe(received::add);

    Temperature tIn = new Temperature(2000L, "T24", new BigDecimal("20"));
    Humidity hOut = new Humidity(2000L, "RH4", new BigDecimal("150")); // above 100
    source.publishToSubscribers(new MeasurementVector(2000L, List.of(tIn, hOut)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(1, received.get(0).getMeasurements().size());
    Assertions.assertEquals("T24", received.get(0).getMeasurements().get(0).getSourceId());
    filter.stop();
  }

  /** Sensor not in map (no valid range) is passed through. */
  @Test
  public void sensorNotInMap_passedThrough() {
    Map<String, SensorConfig> sensorMap = new LinkedHashMap<>(); // empty or only some sensors

    BelievabilityFilteredMeasurementVectorStream filter =
        new BelievabilityFilteredMeasurementVectorStream(source, sensorMap);
    filter.subscribe(received::add);

    Temperature m = new Temperature(1000L, "T1", new BigDecimal("1000"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(m)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(1, received.get(0).getMeasurements().size());
    Assertions.assertEquals("T1", received.get(0).getMeasurements().get(0).getSourceId());
    filter.stop();
  }

  /** Timestamp is preserved. */
  @Test
  public void timestampPreserved() {
    TemperatureSensorConfig t24 = temperatureSensorWithRange(BigDecimal.ZERO, new BigDecimal("80"));
    Map<String, SensorConfig> sensorMap =
        sensorMap(t24, humiditySensorWithRange(BigDecimal.ZERO, new BigDecimal("100")));

    BelievabilityFilteredMeasurementVectorStream filter =
        new BelievabilityFilteredMeasurementVectorStream(source, sensorMap);
    filter.subscribe(received::add);

    long ts = 12345L;
    source.publishToSubscribers(
        new MeasurementVector(ts, List.of(new Temperature(ts, "T24", new BigDecimal("50")))));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(ts, received.get(0).getTimeUtc());
    filter.stop();
  }

  /** listSensors() delegates to source. */
  @Test
  public void listSensors_delegatesToSource() {
    BelievabilityFilteredMeasurementVectorStream filter =
        new BelievabilityFilteredMeasurementVectorStream(source, Map.of());
    Assertions.assertNotNull(filter.listSensors());
    Assertions.assertTrue(filter.listSensors().isEmpty());
    filter.stop();
  }

  /** Vector with a severe error is passed through unchanged (no filtering). */
  @Test
  public void severeError_passesThroughUnchanged() {
    TemperatureSensorConfig t24 =
        temperatureSensorWithRange(new BigDecimal("0"), new BigDecimal("80"));
    Map<String, SensorConfig> sensorMap =
        sensorMap(t24, humiditySensorWithRange(BigDecimal.ZERO, new BigDecimal("100")));

    BelievabilityFilteredMeasurementVectorStream filter =
        new BelievabilityFilteredMeasurementVectorStream(source, sensorMap);
    filter.subscribe(received::add);

    Temperature t = new Temperature(1000L, "T24", new BigDecimal("200"));
    MeasurementError severe = new MeasurementError("T24", ErrorSeverity.SEVERE, "Test severe");
    MeasurementVector input = new MeasurementVector(1000L, List.of(t), List.of(severe));

    source.publishToSubscribers(input);

    Assertions.assertEquals(1, received.size());
    MeasurementVector out = received.getFirst();
    Assertions.assertSame(input, out);
    Assertions.assertEquals(List.of(severe), out.getErrors());
    filter.stop();
  }

  /** Constructor with null sensorMapById uses empty map; all measurements passed through. */
  @Test
  public void nullSensorMapById_usesEmptyMap_allMeasurementsPassedThrough() {
    BelievabilityFilteredMeasurementVectorStream filter =
        new BelievabilityFilteredMeasurementVectorStream(source, null);
    filter.subscribe(received::add);

    Temperature m = new Temperature(1000L, "T24", new BigDecimal("50"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(m)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(1, received.get(0).getMeasurements().size());
    filter.stop();
  }

  /** When valid range has null min, measurement is passed through (no range check). */
  @Test
  public void validRangeWithNullMin_passesThrough() {
    TemperatureSensorConfig t24 = new TemperatureSensorConfig();
    t24.setId("T24");
    ValidRange r = new ValidRange();
    r.setMin(null);
    r.setMax(new BigDecimal("80"));
    t24.setValidRange(r);
    Map<String, SensorConfig> sensorMap = new LinkedHashMap<>();
    sensorMap.put("T24", t24);

    BelievabilityFilteredMeasurementVectorStream filter =
        new BelievabilityFilteredMeasurementVectorStream(source, sensorMap);
    filter.subscribe(received::add);

    Temperature m = new Temperature(1000L, "T24", new BigDecimal("50"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(m)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(1, received.get(0).getMeasurements().size());
    filter.stop();
  }

  /** When valid range has null max, measurement is passed through (no range check). */
  @Test
  public void validRangeWithNullMax_passesThrough() {
    TemperatureSensorConfig t24 = new TemperatureSensorConfig();
    t24.setId("T24");
    ValidRange r = new ValidRange();
    r.setMin(BigDecimal.ZERO);
    r.setMax(null);
    t24.setValidRange(r);
    Map<String, SensorConfig> sensorMap = new LinkedHashMap<>();
    sensorMap.put("T24", t24);

    BelievabilityFilteredMeasurementVectorStream filter =
        new BelievabilityFilteredMeasurementVectorStream(source, sensorMap);
    filter.subscribe(received::add);

    Temperature m = new Temperature(1000L, "T24", new BigDecimal("50"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(m)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(1, received.get(0).getMeasurements().size());
    filter.stop();
  }

  /** When measurement value is null and sensor has valid range, measurement is excluded. */
  @Test
  public void measurementWithNullValue_excludedWhenSensorHasRange() {
    TemperatureSensorConfig t24 = temperatureSensorWithRange(BigDecimal.ZERO, new BigDecimal("80"));
    Map<String, SensorConfig> sensorMap =
        sensorMap(t24, humiditySensorWithRange(BigDecimal.ZERO, new BigDecimal("100")));

    BelievabilityFilteredMeasurementVectorStream filter =
        new BelievabilityFilteredMeasurementVectorStream(source, sensorMap);
    filter.subscribe(received::add);

    Measurement nullValueMeasurement = new Temperature(1000L, "T24", null);
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(nullValueMeasurement)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(0, received.get(0).getMeasurements().size());
    filter.stop();
  }

  /**
   * Sensor type without valid range (e.g. PressureSensorConfig) yields null range; measurement
   * passed through.
   */
  @Test
  public void pressureSensorInMap_noValidRange_measurementPassedThrough() {
    PressureSensorConfig p1 = new PressureSensorConfig();
    p1.setId("P1");
    Map<String, SensorConfig> sensorMap = new LinkedHashMap<>();
    sensorMap.put("P1", p1);

    BelievabilityFilteredMeasurementVectorStream filter =
        new BelievabilityFilteredMeasurementVectorStream(source, sensorMap);
    filter.subscribe(received::add);

    Pressure m = new Pressure(1000L, "P1", new BigDecimal("101325"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(m)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(1, received.get(0).getMeasurements().size());
    Assertions.assertEquals("P1", received.get(0).getMeasurements().get(0).getSourceId());
    filter.stop();
  }

  /** stop() is idempotent: second call does not throw. */
  @Test
  public void stop_idempotent_secondCallDoesNotThrow() {
    BelievabilityFilteredMeasurementVectorStream filter =
        new BelievabilityFilteredMeasurementVectorStream(source, Map.of());
    filter.stop();
    Assertions.assertDoesNotThrow(filter::stop);
  }
}
