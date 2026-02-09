package ca.lajthabalazs.pressure_integrity_test.config;

import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader;
import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader.FailedToReadFileException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads a calibration configuration from a JSON file.
 *
 * <p>The JSON is an object mapping sensor ids to linear calibration objects (each with A and B).
 * Uses a {@link TextFileReader} for file access so that tests can plug in resource or in-memory
 * implementations.
 */
public class CalibrationConfigReader {

  private final TextFileReader fileReader;
  private final ObjectMapper objectMapper;

  public CalibrationConfigReader(TextFileReader fileReader) {
    this(fileReader, new ObjectMapper());
  }

  /**
   * Constructor for tests or when using a custom {@link ObjectMapper}.
   *
   * @param fileReader reader used to load file content
   * @param objectMapper mapper used to parse JSON
   */
  public CalibrationConfigReader(TextFileReader fileReader, ObjectMapper objectMapper) {
    this.fileReader = fileReader;
    this.objectMapper = objectMapper;
  }

  /**
   * Reads and parses the calibration configuration from the given path.
   *
   * @param path path to the JSON file (interpreted by the configured {@link TextFileReader})
   * @return the parsed calibration config
   * @throws FailedToReadFileException if the file cannot be read
   * @throws CalibrationConfigParseException if the content is not valid JSON or structure
   */
  public CalibrationConfig read(String path)
      throws FailedToReadFileException, CalibrationConfigParseException {
    String json = fileReader.readAllText(path);
    try {
      CalibrationConfig config = objectMapper.readValue(json, CalibrationConfig.class);
      return config != null ? config : new CalibrationConfig();
    } catch (JsonProcessingException e) {
      throw new CalibrationConfigParseException(
          "Invalid JSON or calibration config: " + e.getMessage(), e);
    }
  }

  /** Thrown when the calibration file cannot be parsed. */
  public static class CalibrationConfigParseException extends Exception {
    public CalibrationConfigParseException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
