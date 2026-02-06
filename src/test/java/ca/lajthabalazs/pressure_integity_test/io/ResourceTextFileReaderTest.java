package ca.lajthabalazs.pressure_integity_test.io;

import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader.FailedToReadFileException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResourceTextFileReaderTest {

  @Test
  public void readAllLines_existingFile_returnsLines() throws Exception {
    ResourceTextFileReader reader = new ResourceTextFileReader(ResourceTextFileReaderTest.class);
    List<String> lines = reader.readAllLines("/test-file.txt");
    Assertions.assertNotNull(lines);
    Assertions.assertFalse(lines.isEmpty());
  }

  @Test
  public void readAllLines_nonExistentFile_throwsFailedToReadFileException() {
    ResourceTextFileReader reader = new ResourceTextFileReader(ResourceTextFileReaderTest.class);
    FailedToReadFileException exception =
        Assertions.assertThrows(
            FailedToReadFileException.class, () -> reader.readAllLines("/non-existent-file.txt"));
    Assertions.assertTrue(exception.getMessage().contains("Resource not found"));
    Assertions.assertNull(exception.getCause());
  }

  @Test
  public void readAllText_existingFile_returnsContent() throws Exception {
    ResourceTextFileReader reader = new ResourceTextFileReader(ResourceTextFileReaderTest.class);
    String content = reader.readAllText("/test-file.txt");
    Assertions.assertNotNull(content);
  }

  @Test
  public void readAllText_nonExistentFile_throwsFailedToReadFileException() {
    ResourceTextFileReader reader = new ResourceTextFileReader(ResourceTextFileReaderTest.class);
    FailedToReadFileException exception =
        Assertions.assertThrows(
            FailedToReadFileException.class, () -> reader.readAllText("/non-existent-file.txt"));
    Assertions.assertTrue(exception.getMessage().contains("Resource not found"));
    Assertions.assertNull(exception.getCause());
  }

  @Test
  public void exists_existingFile_returnsTrue() {
    ResourceTextFileReader reader = new ResourceTextFileReader(ResourceTextFileReaderTest.class);
    Assertions.assertTrue(reader.exists("/test-file.txt"));
  }

  @Test
  public void exists_nonExistentFile_returnsFalse() {
    ResourceTextFileReader reader = new ResourceTextFileReader(ResourceTextFileReaderTest.class);
    Assertions.assertFalse(reader.exists("/non-existent-file.txt"));
  }

  @Test
  public void failedToReadFileException_withMessage_only() {
    FailedToReadFileException exception = new FailedToReadFileException("Test message");
    Assertions.assertEquals("Test message", exception.getMessage());
    Assertions.assertNull(exception.getCause());
  }

  @Test
  public void failedToReadFileException_withMessageAndCause() {
    Throwable cause = new IOException("Underlying IO error");
    FailedToReadFileException exception = new FailedToReadFileException("Test message", cause);
    Assertions.assertEquals("Test message", exception.getMessage());
    Assertions.assertEquals(cause, exception.getCause());
  }

  @Test
  public void readAllLines_withCustomClass_usesCorrectClassLoader() throws Exception {
    ResourceTextFileReader reader = new ResourceTextFileReader(ResourceTextFileReader.class);
    List<String> lines = reader.readAllLines("/test-file.txt");
    Assertions.assertNotNull(lines);
  }

  @Test
  public void readAllLines_withDefaultConstructor_usesSelfAsClassLoader() throws Exception {
    ResourceTextFileReader reader = new ResourceTextFileReader();
    List<String> lines = reader.readAllLines("/test-file.txt");
    Assertions.assertNotNull(lines);
  }
}
