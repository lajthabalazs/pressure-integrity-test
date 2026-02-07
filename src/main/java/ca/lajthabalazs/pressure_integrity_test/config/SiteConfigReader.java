package ca.lajthabalazs.pressure_integrity_test.config;

import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader;
import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader.FailedToReadFileException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads a site configuration from a JSON file.
 *
 * <p>Uses a {@link TextFileReader} for file access so that tests can plug in resource or in-memory
 * implementations.
 */
public class SiteConfigReader {

  private final TextFileReader fileReader;
  private final ObjectMapper objectMapper;

  public SiteConfigReader(TextFileReader fileReader) {
    this.fileReader = fileReader;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Reads and parses the site configuration from the given path.
   *
   * @param path path to the JSON file (interpreted by the configured {@link TextFileReader})
   * @return the parsed site configuration
   * @throws FailedToReadFileException if the file cannot be read
   * @throws SiteConfigParseException if the file content is not valid JSON or does not match the
   *     expected structure
   */
  public SiteConfig read(String path) throws FailedToReadFileException, SiteConfigParseException {
    String json = fileReader.readAllText(path);
    try {
      SiteConfig config = objectMapper.readValue(json, SiteConfig.class);
      if (config == null) {
        throw new SiteConfigParseException("JSON did not produce a site config object");
      }
      return config;
    } catch (JsonProcessingException e) {
      throw new SiteConfigParseException("Invalid JSON or config: " + e.getMessage(), e);
    }
  }

  /** Thrown when the configuration file cannot be parsed (invalid JSON or structure). */
  public static class SiteConfigParseException extends Exception {
    public SiteConfigParseException(String message) {
      super(message);
    }

    public SiteConfigParseException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
