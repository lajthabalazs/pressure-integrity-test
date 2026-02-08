package ca.lajthabalazs.pressure_integity_test.config;

import ca.lajthabalazs.pressure_integity_test.io.ResourceTextFileReader;
import ca.lajthabalazs.pressure_integrity_test.config.TestConfig;
import ca.lajthabalazs.pressure_integrity_test.config.TestConfigReader;
import ca.lajthabalazs.pressure_integrity_test.config.TestConfigWriter;
import ca.lajthabalazs.pressure_integrity_test.config.TestConfigWriter.TestConfigWriteException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestConfigWriterTest {

  @Test
  public void write_fullConfig_producesJsonMatchingExpectedResource() throws Exception {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(TestConfigWriterTest.class);
    TestConfigReader reader = new TestConfigReader(fileReader);
    TestConfigWriter writer = new TestConfigWriter();

    TestConfig config = reader.read("/test-config/test-config-sample.json");
    String written = writer.writeToString(config);
    String expected = fileReader.readAllText("/test-config/test-config-write-expected-sample.json");
    Assertions.assertEquals(expected, written);
  }

  @Test
  public void write_eitvConfig_producesJsonMatchingExpectedResource() throws Exception {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(TestConfigWriterTest.class);
    TestConfigReader reader = new TestConfigReader(fileReader);
    TestConfigWriter writer = new TestConfigWriter();

    TestConfig config = reader.read("/test-config/test-config-eitv.json");
    String written = writer.writeToString(config);
    String expected = fileReader.readAllText("/test-config/test-config-write-expected-eitv.json");
    Assertions.assertEquals(expected, written);
  }

  @Test
  public void write_serializationFails_throwsTestConfigWriteException() {
    ObjectMapper failingMapper =
        new ObjectMapper() {
          @Override
          public String writeValueAsString(Object value) throws JsonProcessingException {
            throw new JsonProcessingException("mock write failure") {};
          }
        };
    TestConfigWriter writer = new TestConfigWriter(failingMapper);
    TestConfig config = new TestConfig();

    TestConfigWriteException exception =
        Assertions.assertThrows(TestConfigWriteException.class, () -> writer.writeToString(config));
    Assertions.assertTrue(
        exception.getMessage().contains("Failed to write test config"),
        "Expected message about write failure, got: " + exception.getMessage());
    Assertions.assertTrue(
        exception.getMessage().contains("mock write failure"),
        "Expected cause message in exception, got: " + exception.getMessage());
    Assertions.assertNotNull(exception.getCause());
    Assertions.assertTrue(exception.getCause() instanceof JsonProcessingException);
  }
}
