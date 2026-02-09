package ca.lajthabalazs.pressure_integity_test.config;

import ca.lajthabalazs.pressure_integrity_test.config.CalibrationConfig;
import ca.lajthabalazs.pressure_integrity_test.config.LinearCalibration;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CalibrationConfigTest {

  @Test
  public void getSensorCalibrations_empty_initiallyEmpty() {
    CalibrationConfig config = new CalibrationConfig();
    Assertions.assertNotNull(config.getSensorCalibrations());
    Assertions.assertTrue(config.getSensorCalibrations().isEmpty());
  }

  @Test
  public void setSensorCalibration_addsEntry_getCalibrationForSensorReturnsIt() {
    CalibrationConfig config = new CalibrationConfig();
    LinearCalibration cal = new LinearCalibration();
    cal.setA(BigDecimal.ONE);
    cal.setB(new BigDecimal("0.5"));
    config.setSensorCalibration("T1", cal);
    Assertions.assertEquals(1, config.getSensorCalibrations().size());
    LinearCalibration retrieved = config.getCalibrationForSensor("T1");
    Assertions.assertNotNull(retrieved);
    Assertions.assertSame(cal, retrieved);
  }

  @Test
  public void setSensorCalibration_nullSensorId_ignored() {
    CalibrationConfig config = new CalibrationConfig();
    config.setSensorCalibration(null, new LinearCalibration());
    Assertions.assertTrue(config.getSensorCalibrations().isEmpty());
  }

  @Test
  public void getCalibrationForSensor_unknownId_returnsNull() {
    CalibrationConfig config = new CalibrationConfig();
    config.setSensorCalibration("T1", new LinearCalibration());
    Assertions.assertNull(config.getCalibrationForSensor("T2"));
  }

  @Test
  public void multipleSensors_insertionOrderPreserved() {
    CalibrationConfig config = new CalibrationConfig();
    LinearCalibration c1 = new LinearCalibration();
    LinearCalibration c2 = new LinearCalibration();
    config.setSensorCalibration("T2", c2);
    config.setSensorCalibration("T1", c1);
    var map = config.getSensorCalibrations();
    var keys = map.keySet().toArray();
    Assertions.assertEquals("T2", keys[0]);
    Assertions.assertEquals("T1", keys[1]);
  }
}
