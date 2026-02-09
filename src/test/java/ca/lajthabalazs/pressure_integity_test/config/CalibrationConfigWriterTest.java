package ca.lajthabalazs.pressure_integity_test.config;

import ca.lajthabalazs.pressure_integity_test.io.ResourceTextFileReader;
import ca.lajthabalazs.pressure_integrity_test.config.CalibrationConfig;
import ca.lajthabalazs.pressure_integrity_test.config.CalibrationConfigReader;
import ca.lajthabalazs.pressure_integrity_test.config.CalibrationConfigWriter;
import ca.lajthabalazs.pressure_integrity_test.config.CalibrationConfigWriter.CalibrationConfigWriteException;
import ca.lajthabalazs.pressure_integrity_test.config.LinearCalibration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CalibrationConfigWriterTest {

  @Test
  public void writeThenRead_roundTripsCorrectly() throws Exception {
    ResourceTextFileReader fileReader =
        new ResourceTextFileReader(CalibrationConfigWriterTest.class);
    CalibrationConfigReader reader = new CalibrationConfigReader(fileReader);
    CalibrationConfigWriter writer = new CalibrationConfigWriter();

    CalibrationConfig original = reader.read("/calibration/calibration-sample.json");
    String written = writer.writeToString(original);
    Assertions.assertNotNull(written);
    Assertions.assertTrue(written.contains("T1"));
    Assertions.assertTrue(written.contains("T2"));
    Assertions.assertTrue(written.contains("T24"));

    CalibrationConfig readBack = new ObjectMapper().readValue(written, CalibrationConfig.class);
    Assertions.assertEquals(
        original.getSensorCalibrations().size(), readBack.getSensorCalibrations().size());
    for (String sensorId : original.getSensorCalibrations().keySet()) {
      LinearCalibration origCal = original.getCalibrationForSensor(sensorId);
      LinearCalibration readCal = readBack.getCalibrationForSensor(sensorId);
      Assertions.assertNotNull(readCal);
      Assertions.assertEquals(
          0,
          (origCal.getA() == null ? BigDecimal.ONE : origCal.getA())
              .compareTo(readCal.getA() == null ? BigDecimal.ONE : readCal.getA()));
      Assertions.assertEquals(
          0,
          (origCal.getB() == null ? BigDecimal.ZERO : origCal.getB())
              .compareTo(readCal.getB() == null ? BigDecimal.ZERO : readCal.getB()));
    }
  }

  @Test
  public void write_configReadBackViaObjectMapper_equalsOriginal() throws Exception {
    CalibrationConfig config = new CalibrationConfig();
    LinearCalibration c1 = new LinearCalibration();
    c1.setA(new BigDecimal("1.001"));
    c1.setB(new BigDecimal("-0.05"));
    config.setSensorCalibration("T2", c1);
    LinearCalibration c2 = new LinearCalibration();
    c2.setA(new BigDecimal("0.998"));
    c2.setB(new BigDecimal("0.1"));
    config.setSensorCalibration("T24", c2);

    CalibrationConfigWriter writer = new CalibrationConfigWriter();
    String written = writer.writeToString(config);
    CalibrationConfig readBack = new ObjectMapper().readValue(written, CalibrationConfig.class);
    Assertions.assertEquals(2, readBack.getSensorCalibrations().size());
    LinearCalibration t2 = readBack.getCalibrationForSensor("T2");
    Assertions.assertNotNull(t2);
    Assertions.assertEquals(0, new BigDecimal("1.001").compareTo(t2.getA()));
    Assertions.assertEquals(0, new BigDecimal("-0.05").compareTo(t2.getB()));
    LinearCalibration t24 = readBack.getCalibrationForSensor("T24");
    Assertions.assertNotNull(t24);
    Assertions.assertEquals(0, new BigDecimal("0.998").compareTo(t24.getA()));
    Assertions.assertEquals(0, new BigDecimal("0.1").compareTo(t24.getB()));
  }

  @Test
  public void write_calibrationWithNullAOrB_serializesNullLiteral() throws Exception {
    CalibrationConfig config = new CalibrationConfig();
    LinearCalibration calWithNullB = new LinearCalibration();
    calWithNullB.setA(new BigDecimal("1.0"));
    calWithNullB.setB(null);
    config.setSensorCalibration("T1", calWithNullB);
    LinearCalibration calWithNullA = new LinearCalibration();
    calWithNullA.setA(null);
    calWithNullA.setB(new BigDecimal("0.5"));
    config.setSensorCalibration("T2", calWithNullA);

    CalibrationConfigWriter writer = new CalibrationConfigWriter();
    String written = writer.writeToString(config);

    Assertions.assertTrue(written.contains("\"B\" : null") || written.contains("\"B\": null"));
    Assertions.assertTrue(written.contains("\"A\" : null") || written.contains("\"A\": null"));
  }

  @Test
  public void formatBigDecimalForJson_null_returnsNullLiteral() {
    Assertions.assertEquals("null", CalibrationConfigWriter.formatBigDecimalForJson(null));
  }

  @Test
  public void formatBigDecimalForJson_nonNull_returnsPlainString() {
    Assertions.assertEquals(
        "1.5", CalibrationConfigWriter.formatBigDecimalForJson(new BigDecimal("1.5")));
  }

  @Test
  public void write_serializationFails_throwsCalibrationConfigWriteException() {
    ObjectMapper failingMapper = new ObjectMapper();
    failingMapper.registerModule(
        new com.fasterxml.jackson.databind.module.SimpleModule()
            .addSerializer(
                CalibrationConfig.class,
                new com.fasterxml.jackson.databind.JsonSerializer<CalibrationConfig>() {
                  @Override
                  public void serialize(
                      CalibrationConfig value,
                      com.fasterxml.jackson.core.JsonGenerator gen,
                      com.fasterxml.jackson.databind.SerializerProvider serializers)
                      throws java.io.IOException {
                    throw new JsonProcessingException("mock write failure") {};
                  }
                }));
    CalibrationConfigWriter writer = new CalibrationConfigWriter(failingMapper);
    CalibrationConfig config = new CalibrationConfig();

    CalibrationConfigWriteException exception =
        Assertions.assertThrows(
            CalibrationConfigWriteException.class, () -> writer.writeToString(config));
    Assertions.assertTrue(
        exception.getMessage().contains("Failed to write calibration config"),
        "Expected message about write failure, got: " + exception.getMessage());
    Assertions.assertTrue(
        exception.getMessage().contains("mock write failure"),
        "Expected cause message in exception, got: " + exception.getMessage());
    Assertions.assertNotNull(exception.getCause());
    Assertions.assertTrue(exception.getCause() instanceof JsonProcessingException);
  }
}
