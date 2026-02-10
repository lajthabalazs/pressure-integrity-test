package ca.lajthabalazs.pressure_integity_test.measurement.processing;

import ca.lajthabalazs.pressure_integity_test.measurement.streaming.TestMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.config.CalibrationConfig;
import ca.lajthabalazs.pressure_integrity_test.config.LinearCalibration;
import ca.lajthabalazs.pressure_integrity_test.measurement.Humidity;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.Pressure;
import ca.lajthabalazs.pressure_integrity_test.measurement.Temperature;
import ca.lajthabalazs.pressure_integrity_test.measurement.processing.CalibratedMeasurementVectorStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CalibratedMeasurementVectorStream} to verify calibration is applied correctly.
 */
public class CalibratedMeasurementVectorStreamTest {

  private TestMeasurementVectorStream source;
  private List<MeasurementVector> received;

  @BeforeEach
  public void setUp() {
    source = new TestMeasurementVectorStream();
    received = new ArrayList<>();
  }

  @AfterEach
  public void tearDown() {
    // No-op; tests that create a calibrated stream call stop() where needed
  }

  /** When a sensor has calibration (real = A * measured + B), published value is calibrated. */
  @Test
  public void calibrationApplied_whenSensorHasCalibration_publishesCalibratedValue() {
    CalibrationConfig config = new CalibrationConfig();
    LinearCalibration cal = new LinearCalibration();
    cal.setA(new BigDecimal("2"));
    cal.setB(new BigDecimal("1"));
    config.setSensorCalibration("T1", cal);

    CalibratedMeasurementVectorStream calibrated =
        new CalibratedMeasurementVectorStream(source, config);
    calibrated.subscribe(received::add);

    Temperature raw = new Temperature(1000L, "T1", new BigDecimal("10"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(raw)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(1000L, received.get(0).getTimeUtc());
    Assertions.assertEquals(1, received.get(0).getMeasurements().size());
    // 2 * 10 + 1 = 21
    Assertions.assertEquals(
        0,
        new BigDecimal("21")
            .compareTo(received.get(0).getMeasurements().get(0).getValueInDefaultUnit()),
        "Calibrated value should be A*measured+B = 21");
    Assertions.assertEquals("T1", received.get(0).getMeasurements().get(0).getSourceId());
    calibrated.stop();
  }

  /** When a sensor has no calibration, measurement is passed through unchanged. */
  @Test
  public void noCalibrationForSensor_passesThroughOriginalValue() {
    CalibrationConfig config = new CalibrationConfig();
    LinearCalibration cal = new LinearCalibration();
    cal.setA(new BigDecimal("1.5"));
    cal.setB(BigDecimal.ZERO);
    config.setSensorCalibration("T1", cal);
    // T2 has no calibration

    CalibratedMeasurementVectorStream calibrated =
        new CalibratedMeasurementVectorStream(source, config);
    calibrated.subscribe(received::add);

    Temperature t1 = new Temperature(2000L, "T1", new BigDecimal("20"));
    Temperature t2 = new Temperature(2000L, "T2", new BigDecimal("25"));
    source.publishToSubscribers(new MeasurementVector(2000L, List.of(t1, t2)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(2, received.get(0).getMeasurements().size());
    // T1: 1.5 * 20 + 0 = 30
    BigDecimal t1Value =
        received.get(0).getMeasurements().stream()
            .filter(m -> "T1".equals(m.getSourceId()))
            .findFirst()
            .orElseThrow()
            .getValueInDefaultUnit();
    Assertions.assertEquals(0, new BigDecimal("30").compareTo(t1Value));
    // T2: unchanged 25
    BigDecimal t2Value =
        received.get(0).getMeasurements().stream()
            .filter(m -> "T2".equals(m.getSourceId()))
            .findFirst()
            .orElseThrow()
            .getValueInDefaultUnit();
    Assertions.assertEquals(0, new BigDecimal("25").compareTo(t2Value));
    calibrated.stop();
  }

  /** Timestamp of the vector is preserved in the published calibrated vector. */
  @Test
  public void timestampPreserved_inPublishedVector() {
    CalibrationConfig config = new CalibrationConfig();
    LinearCalibration cal = new LinearCalibration();
    cal.setB(new BigDecimal("5"));
    config.setSensorCalibration("T1", cal);

    CalibratedMeasurementVectorStream calibrated =
        new CalibratedMeasurementVectorStream(source, config);
    calibrated.subscribe(received::add);

    long ts = 12345L;
    source.publishToSubscribers(
        new MeasurementVector(ts, List.of(new Temperature(ts, "T1", new BigDecimal("10")))));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(ts, received.get(0).getTimeUtc());
    Assertions.assertEquals(ts, received.get(0).getMeasurements().get(0).getTimeUtc());
    calibrated.stop();
  }

  /** Null calibration config is treated as empty: all values passed through. */
  @Test
  public void nullCalibrationConfig_passesThroughAllValues() {
    CalibratedMeasurementVectorStream calibrated =
        new CalibratedMeasurementVectorStream(source, null);
    calibrated.subscribe(received::add);

    Temperature t = new Temperature(1000L, "T1", new BigDecimal("42"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(t)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(1, received.get(0).getMeasurements().size());
    Assertions.assertEquals(
        0,
        new BigDecimal("42")
            .compareTo(received.get(0).getMeasurements().get(0).getValueInDefaultUnit()));
    calibrated.stop();
  }

  /** Empty calibration config: all values passed through. */
  @Test
  public void emptyCalibrationConfig_passesThroughAllValues() {
    CalibratedMeasurementVectorStream calibrated =
        new CalibratedMeasurementVectorStream(source, new CalibrationConfig());
    calibrated.subscribe(received::add);

    Temperature t = new Temperature(1000L, "T1", new BigDecimal("37"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(t)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(
        0,
        new BigDecimal("37")
            .compareTo(received.get(0).getMeasurements().get(0).getValueInDefaultUnit()));
    calibrated.stop();
  }

  /** After stop(), no more vectors are published. */
  @Test
  public void afterStop_noMoreVectorsPublished() {
    CalibrationConfig config = new CalibrationConfig();
    config.setSensorCalibration("T1", new LinearCalibration());

    CalibratedMeasurementVectorStream calibrated =
        new CalibratedMeasurementVectorStream(source, config);
    calibrated.subscribe(received::add);

    source.publishToSubscribers(
        new MeasurementVector(1000L, List.of(new Temperature(1000L, "T1", new BigDecimal("10")))));
    Assertions.assertEquals(1, received.size());

    calibrated.stop();

    source.publishToSubscribers(
        new MeasurementVector(2000L, List.of(new Temperature(2000L, "T1", new BigDecimal("20")))));
    Assertions.assertEquals(1, received.size(), "No new vector after stop");
  }

  /** listSensors() delegates to source stream. */
  @Test
  public void listSensors_delegatesToSource() {
    CalibratedMeasurementVectorStream calibrated =
        new CalibratedMeasurementVectorStream(source, new CalibrationConfig());
    Assertions.assertNotNull(calibrated.listSensors());
    Assertions.assertTrue(
        calibrated.listSensors().isEmpty(), "TestMeasurementVectorStream returns empty list");
    calibrated.stop();
  }

  /** Pressure measurements are calibrated when sensor has calibration (value in Pa). */
  @Test
  public void calibrationApplied_toPressure_publishesCalibratedValue() {
    CalibrationConfig config = new CalibrationConfig();
    LinearCalibration cal = new LinearCalibration();
    cal.setA(new BigDecimal("0.001"));
    cal.setB(new BigDecimal("100"));
    config.setSensorCalibration("P1", cal);

    CalibratedMeasurementVectorStream calibrated =
        new CalibratedMeasurementVectorStream(source, config);
    calibrated.subscribe(received::add);

    Pressure raw = new Pressure(1000L, "P1", new BigDecimal("101325"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(raw)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(1, received.get(0).getMeasurements().size());
    Pressure calibratedPressure = (Pressure) received.get(0).getMeasurements().get(0);
    // 0.001 * 101325 + 100 = 101.325 + 100 = 201.325
    Assertions.assertEquals(
        0,
        new BigDecimal("201.325").compareTo(calibratedPressure.getPascalValue()),
        "Calibrated pressure should be A*measured+B");
    Assertions.assertEquals("P1", calibratedPressure.getSourceId());
    calibrated.stop();
  }

  /** Humidity measurements are calibrated when sensor has calibration (value in %). */
  @Test
  public void calibrationApplied_toHumidity_publishesCalibratedValue() {
    CalibrationConfig config = new CalibrationConfig();
    LinearCalibration cal = new LinearCalibration();
    cal.setA(new BigDecimal("1.2"));
    cal.setB(new BigDecimal("-5"));
    config.setSensorCalibration("H1", cal);

    CalibratedMeasurementVectorStream calibrated =
        new CalibratedMeasurementVectorStream(source, config);
    calibrated.subscribe(received::add);

    Humidity raw = new Humidity(1000L, "H1", new BigDecimal("50"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(raw)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(1, received.get(0).getMeasurements().size());
    Humidity calibratedHumidity = (Humidity) received.get(0).getMeasurements().get(0);
    // 1.2 * 50 - 5 = 60 - 5 = 55
    Assertions.assertEquals(
        0,
        new BigDecimal("55").compareTo(calibratedHumidity.getValueInDefaultUnit()),
        "Calibrated humidity should be A*measured+B");
    Assertions.assertEquals("H1", calibratedHumidity.getSourceId());
    calibrated.stop();
  }

  /** When calibration returns null, measurement is passed through unchanged. */
  @Test
  public void calibrationReturnsNull_passesThroughOriginalMeasurement() {
    CalibrationConfig config = new CalibrationConfig();
    LinearCalibration nullReturningCal =
        new LinearCalibration() {
          @Override
          public BigDecimal getCalibratedValue(BigDecimal measuredValue) {
            return null;
          }
        };
    config.setSensorCalibration("T1", nullReturningCal);

    CalibratedMeasurementVectorStream calibrated =
        new CalibratedMeasurementVectorStream(source, config);
    calibrated.subscribe(received::add);

    Temperature raw = new Temperature(1000L, "T1", new BigDecimal("10"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(raw)));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(1, received.get(0).getMeasurements().size());
    Assertions.assertEquals(
        0,
        new BigDecimal("10")
            .compareTo(received.get(0).getMeasurements().get(0).getValueInDefaultUnit()),
        "When calibration returns null, original value should be passed through");
    calibrated.stop();
  }

  /** stop() called twice does not throw; second call only clears subscribers. */
  @Test
  public void stopCalledTwice_doesNotThrow() {
    CalibratedMeasurementVectorStream calibrated =
        new CalibratedMeasurementVectorStream(source, new CalibrationConfig());
    calibrated.subscribe(received::add);
    source.publishToSubscribers(
        new MeasurementVector(1000L, List.of(new Temperature(1000L, "T1", new BigDecimal("10")))));
    Assertions.assertEquals(1, received.size());

    calibrated.stop();
    calibrated.stop();
  }
}
