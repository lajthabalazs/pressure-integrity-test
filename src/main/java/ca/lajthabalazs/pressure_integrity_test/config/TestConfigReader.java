package ca.lajthabalazs.pressure_integrity_test.config;

import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader;
import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader.FailedToReadFileException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads a test configuration from a JSON file.
 *
 * <p>Uses a {@link TextFileReader} for file access so that tests can plug in resource or in-memory
 * implementations.
 */
public class TestConfigReader {

  private final TextFileReader fileReader;
  private final ObjectMapper objectMapper;

  public TestConfigReader(TextFileReader fileReader) {
    this(fileReader, new ObjectMapper());
  }

  /**
   * Constructor for tests or when using a custom {@link ObjectMapper}.
   *
   * @param fileReader reader used to load file content
   * @param objectMapper mapper used to parse JSON; may be configured or a test double
   */
  public TestConfigReader(TextFileReader fileReader, ObjectMapper objectMapper) {
    this.fileReader = fileReader;
    this.objectMapper = objectMapper;
  }

  /**
   * Reads and parses the test configuration from the given path.
   *
   * @param path path to the JSON file (interpreted by the configured {@link TextFileReader})
   * @return the parsed test configuration
   * @throws FailedToReadFileException if the file cannot be read
   * @throws TestConfigParseException if the file content is not valid JSON or does not match the
   *     expected structure
   */
  public TestConfig read(String path) throws FailedToReadFileException, TestConfigParseException {
    String json = fileReader.readAllText(path);
    try {
      TestConfig config = objectMapper.readValue(json, TestConfig.class);
      if (config == null) {
        throw new TestConfigParseException("JSON did not produce a test config object");
      }
      validateStages(config);
      return config;
    } catch (JsonProcessingException e) {
      throw new TestConfigParseException("Invalid JSON or config: " + e.getMessage(), e);
    }
  }

  private void validateStages(TestConfig config) throws TestConfigParseException {
    if (config.getType() == null) {
      throw new TestConfigParseException("Test type is required");
    }
    if (config.getStages() == null) {
      throw new TestConfigParseException("Stages are required");
    }
    int expected = config.getType().getExpectedStageCount();
    int actual = config.getStages().size();
    if (actual != expected) {
      throw new TestConfigParseException(
          String.format(
              "Test type %s requires %d stage(s), but %d were provided",
              config.getType(), expected, actual));
    }
  }

  /** Thrown when the configuration file cannot be parsed (invalid JSON or structure). */
  public static class TestConfigParseException extends Exception {
    public TestConfigParseException(String message) {
      super(message);
    }

    public TestConfigParseException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
