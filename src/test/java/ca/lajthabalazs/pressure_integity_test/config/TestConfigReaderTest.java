package ca.lajthabalazs.pressure_integity_test.config;

import ca.lajthabalazs.pressure_integity_test.io.ResourceTextFileReader;
import ca.lajthabalazs.pressure_integrity_test.config.TestConfig;
import ca.lajthabalazs.pressure_integrity_test.config.TestConfigReader;
import ca.lajthabalazs.pressure_integrity_test.config.TestConfigReader.TestConfigParseException;
import ca.lajthabalazs.pressure_integrity_test.config.TestConfigStage;
import ca.lajthabalazs.pressure_integrity_test.config.TestType;
import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader.FailedToReadFileException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestConfigReaderTest {

  @Test
  public void read_validConfig_returnsParsedTestConfig() throws Exception {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(TestConfigReaderTest.class);
    TestConfigReader reader = new TestConfigReader(fileReader);

    TestConfig config = reader.read("/test-config/test-config-sample.json");

    Assertions.assertNotNull(config);
    Assertions.assertEquals("2026-05-TITV", config.getId());
    Assertions.assertEquals(TestType.TITV, config.getType());
    Assertions.assertEquals("Containment absolute pressure sensor A", config.getDescription());

    Assertions.assertNotNull(config.getAmbientPressure());
    Assertions.assertEquals(
        0, new BigDecimal("101000").compareTo(config.getAmbientPressure().getValue()));
    Assertions.assertEquals("Pa", config.getAmbientPressure().getUnit());

    List<TestConfigStage> stages = config.getStages();
    Assertions.assertEquals(3, stages.size());

    TestConfigStage stage0 = stages.get(0);
    Assertions.assertNotNull(stage0.getOverpressure());
    Assertions.assertEquals(
        0, new BigDecimal("0.2").compareTo(stage0.getOverpressure().getValue()));
    Assertions.assertEquals("bar", stage0.getOverpressure().getUnit());
    Assertions.assertNotNull(stage0.getDuration());
    Assertions.assertEquals(6, stage0.getDuration().getHours());
    Assertions.assertEquals(0, stage0.getDuration().getMinutes());

    TestConfigStage stage1 = stages.get(1);
    Assertions.assertEquals(
        0, new BigDecimal("0.7").compareTo(stage1.getOverpressure().getValue()));
    Assertions.assertEquals(2, stage1.getDuration().getHours());
    Assertions.assertEquals(0, stage1.getDuration().getMinutes());

    TestConfigStage stage2 = stages.get(2);
    Assertions.assertEquals(
        0, new BigDecimal("1.5").compareTo(stage2.getOverpressure().getValue()));
    Assertions.assertEquals(1, stage2.getDuration().getHours());
    Assertions.assertEquals(0, stage2.getDuration().getMinutes());
  }

  @Test
  public void read_missingFile_throwsFailedToReadFileException() {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(TestConfigReaderTest.class);
    TestConfigReader reader = new TestConfigReader(fileReader);

    FailedToReadFileException exception =
        Assertions.assertThrows(
            FailedToReadFileException.class,
            () -> reader.read("/test-config/non-existent-test-config.json"));
    Assertions.assertTrue(exception.getMessage().contains("Resource not found"));
  }

  @Test
  public void read_invalidJson_throwsTestConfigParseException() {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(TestConfigReaderTest.class);
    TestConfigReader reader = new TestConfigReader(fileReader);

    TestConfigParseException exception =
        Assertions.assertThrows(
            TestConfigParseException.class,
            () -> reader.read("/test-config/test-config-invalid.json"));
    Assertions.assertTrue(
        exception.getMessage().contains("Invalid JSON")
            || exception.getMessage().contains("config"));
    Assertions.assertNotNull(exception.getCause());
  }

  @Test
  public void read_nullType_throwsTestConfigParseException() {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(TestConfigReaderTest.class);
    TestConfigReader reader = new TestConfigReader(fileReader);

    TestConfigParseException exception =
        Assertions.assertThrows(
            TestConfigParseException.class,
            () -> reader.read("/test-config/test-config-null-type.json"));
    Assertions.assertTrue(
        exception.getMessage().contains("Test type is required"),
        "Expected message about test type required, got: " + exception.getMessage());
  }

  @Test
  public void read_nullStages_throwsTestConfigParseException() {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(TestConfigReaderTest.class);
    TestConfigReader reader = new TestConfigReader(fileReader);

    TestConfigParseException exception =
        Assertions.assertThrows(
            TestConfigParseException.class,
            () -> reader.read("/test-config/test-config-null-stages.json"));
    Assertions.assertTrue(
        exception.getMessage().contains("Stages are required"),
        "Expected message about stages required, got: " + exception.getMessage());
  }

  @Test
  public void read_wrongStageCount_throwsTestConfigParseException() {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(TestConfigReaderTest.class);
    TestConfigReader reader = new TestConfigReader(fileReader);

    TestConfigParseException exception =
        Assertions.assertThrows(
            TestConfigParseException.class,
            () -> reader.read("/test-config/test-config-wrong-stage-count.json"));
    Assertions.assertTrue(
        exception.getMessage().contains("requires 3 stage(s)"),
        "Expected message about 3 stages required, got: " + exception.getMessage());
    Assertions.assertTrue(
        exception.getMessage().contains("2 were provided"),
        "Expected message about 2 provided, got: " + exception.getMessage());
  }

  @Test
  public void read_emptyStages_forTITV_throwsTestConfigParseException() {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(TestConfigReaderTest.class);
    TestConfigReader reader = new TestConfigReader(fileReader);

    TestConfigParseException exception =
        Assertions.assertThrows(
            TestConfigParseException.class,
            () -> reader.read("/test-config/test-config-empty-stages.json"));
    Assertions.assertTrue(
        exception.getMessage().contains("requires 3 stage(s)"),
        "Expected message about 3 stages required, got: " + exception.getMessage());
    Assertions.assertTrue(
        exception.getMessage().contains("0 were provided"),
        "Expected message about 0 provided, got: " + exception.getMessage());
  }

  @Test
  public void read_parseReturnsNull_throwsTestConfigParseException() {
    ObjectMapper nullReturningMapper =
        new ObjectMapper() {
          @Override
          public <T> T readValue(String content, Class<T> valueType)
              throws JsonProcessingException {
            if (valueType == TestConfig.class) {
              return null;
            }
            return super.readValue(content, valueType);
          }
        };
    ResourceTextFileReader fileReader = new ResourceTextFileReader(TestConfigReaderTest.class);
    TestConfigReader reader = new TestConfigReader(fileReader, nullReturningMapper);

    TestConfigParseException exception =
        Assertions.assertThrows(
            TestConfigParseException.class,
            () -> reader.read("/test-config/test-config-sample.json"));
    Assertions.assertTrue(
        exception.getMessage().contains("did not produce a test config object"),
        "Expected message about null parse result, got: " + exception.getMessage());
    Assertions.assertNull(exception.getCause());
  }
}
