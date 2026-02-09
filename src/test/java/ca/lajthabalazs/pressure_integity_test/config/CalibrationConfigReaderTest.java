package ca.lajthabalazs.pressure_integity_test.config;

import ca.lajthabalazs.pressure_integity_test.io.ResourceTextFileReader;
import ca.lajthabalazs.pressure_integrity_test.config.CalibrationConfig;
import ca.lajthabalazs.pressure_integrity_test.config.CalibrationConfigReader;
import ca.lajthabalazs.pressure_integrity_test.config.CalibrationConfigReader.CalibrationConfigParseException;
import ca.lajthabalazs.pressure_integrity_test.config.LinearCalibration;
import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader.FailedToReadFileException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CalibrationConfigReaderTest {

  @Test
  public void read_validConfig_returnsParsedConfig() throws Exception {
    ResourceTextFileReader fileReader =
        new ResourceTextFileReader(CalibrationConfigReaderTest.class);
    CalibrationConfigReader reader = new CalibrationConfigReader(fileReader);

    CalibrationConfig config = reader.read("/calibration/calibration-sample.json");

    Assertions.assertNotNull(config);
    Assertions.assertEquals(3, config.getSensorCalibrations().size());

    LinearCalibration t1 = config.getCalibrationForSensor("T1");
    Assertions.assertNotNull(t1);
    Assertions.assertEquals(0, new BigDecimal("1").compareTo(t1.getA()));
    Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(t1.getB()));

    LinearCalibration t2 = config.getCalibrationForSensor("T2");
    Assertions.assertNotNull(t2);
    Assertions.assertEquals(0, new BigDecimal("1.001").compareTo(t2.getA()));
    Assertions.assertEquals(0, new BigDecimal("-0.05").compareTo(t2.getB()));

    LinearCalibration t24 = config.getCalibrationForSensor("T24");
    Assertions.assertNotNull(t24);
    Assertions.assertEquals(0, new BigDecimal("0.998").compareTo(t24.getA()));
    Assertions.assertEquals(0, new BigDecimal("0.1").compareTo(t24.getB()));
  }

  @Test
  public void read_emptyObject_returnsEmptyConfig() throws Exception {
    ResourceTextFileReader fileReader =
        new ResourceTextFileReader(CalibrationConfigReaderTest.class);
    CalibrationConfigReader reader = new CalibrationConfigReader(fileReader);

    CalibrationConfig config = reader.read("/calibration/calibration-empty.json");

    Assertions.assertNotNull(config);
    Assertions.assertTrue(config.getSensorCalibrations().isEmpty());
  }

  @Test
  public void read_missingFile_throwsFailedToReadFileException() {
    ResourceTextFileReader fileReader =
        new ResourceTextFileReader(CalibrationConfigReaderTest.class);
    CalibrationConfigReader reader = new CalibrationConfigReader(fileReader);

    FailedToReadFileException exception =
        Assertions.assertThrows(
            FailedToReadFileException.class, () -> reader.read("/calibration/non-existent.json"));
    Assertions.assertTrue(exception.getMessage().contains("Resource not found"));
  }

  @Test
  public void read_invalidJson_throwsCalibrationConfigParseException() {
    ResourceTextFileReader fileReader =
        new ResourceTextFileReader(CalibrationConfigReaderTest.class);
    CalibrationConfigReader reader = new CalibrationConfigReader(fileReader);

    CalibrationConfigParseException exception =
        Assertions.assertThrows(
            CalibrationConfigParseException.class,
            () -> reader.read("/calibration/calibration-invalid.json"));
    Assertions.assertTrue(
        exception.getMessage().contains("Invalid JSON")
            || exception.getMessage().contains("calibration config"));
    Assertions.assertNotNull(exception.getCause());
  }

  @Test
  public void read_parseReturnsNull_returnsEmptyConfig() throws Exception {
    ObjectMapper nullReturningMapper =
        new ObjectMapper() {
          @Override
          public <T> T readValue(String content, Class<T> valueType)
              throws JsonProcessingException {
            if (valueType == CalibrationConfig.class) {
              return null;
            }
            return super.readValue(content, valueType);
          }
        };
    ResourceTextFileReader fileReader =
        new ResourceTextFileReader(CalibrationConfigReaderTest.class);
    CalibrationConfigReader reader = new CalibrationConfigReader(fileReader, nullReturningMapper);

    CalibrationConfig config = reader.read("/calibration/calibration-empty.json");

    Assertions.assertNotNull(config);
    Assertions.assertTrue(config.getSensorCalibrations().isEmpty());
  }
}
