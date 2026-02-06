package ca.lajthabalazs.pressure_integity_test.io;

import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Resource file based implementation of TextFileReader.
 *
 * <p>Reads text files from the classpath resources, typically used for test data files located in
 * src/test/resources.
 */
public class ResourceTextFileReader implements TextFileReader {

  private final Class<?> resourceClass;

  /**
   * Creates a ResourceTextFileReader that loads resources relative to the given class.
   *
   * @param resourceClass the class to use for loading resources
   */
  public ResourceTextFileReader(Class<?> resourceClass) {
    this.resourceClass = resourceClass;
  }

  /**
   * Creates a ResourceTextFileReader that loads resources relative to this class.
   *
   * <p>This is useful when resources are in the same package as the test class.
   */
  public ResourceTextFileReader() {
    this.resourceClass = ResourceTextFileReader.class;
  }

  @Override
  public List<String> readAllLines(String path) throws FailedToReadFileException {
    InputStream inputStream = resourceClass.getResourceAsStream(path);
    if (inputStream == null) {
      throw new FailedToReadFileException("Resource not found: " + path);
    }

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      List<String> lines = new ArrayList<>();
      String line;
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }
      return lines;
    } catch (IOException e) {
      throw new FailedToReadFileException("Failed to read resource " + path, e);
    }
  }

  @Override
  public String readAllText(String path) throws FailedToReadFileException {
    InputStream inputStream = resourceClass.getResourceAsStream(path);
    if (inputStream == null) {
      throw new FailedToReadFileException("Resource not found: " + path);
    }

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      StringBuilder content = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        if (!content.isEmpty()) {
          content.append(System.lineSeparator());
        }
        content.append(line);
      }
      return content.toString();
    } catch (IOException e) {
      throw new FailedToReadFileException("Failed to read resource " + path, e);
    }
  }

  @Override
  public boolean exists(String path) {
    InputStream inputStream = resourceClass.getResourceAsStream(path);
    if (inputStream == null) {
      return false;
    }
    try {
      inputStream.close();
    } catch (IOException e) {
    }
    return true;
  }
}
