package ca.lajthabalazs.pressure_integrity_test.io;

import java.io.IOException;
import java.util.List;

/**
 * Interface for abstracting text file access.
 *
 * <p>This interface provides a way to read text files from different sources (file system,
 * classpath resources, etc.) without coupling the code to a specific implementation.
 */
public interface TextFileReader {

  /**
   * Reads all lines from a text file.
   *
   * @param path the path to the file
   * @return a list of lines from the file
   * @throws FailedToReadFileException if the file does not exist
   */
  List<String> readAllLines(String path) throws FailedToReadFileException;

  /**
   * Reads the entire contents of a text file as a single string.
   *
   * @param path the path to the file
   * @return the entire file contents as a string
   * @throws FailedToReadFileException if the file does not exist
   */
  String readAllText(String path) throws FailedToReadFileException;

  /**
   * Checks if a file exists.
   *
   * @param path the path to the file
   * @return true if the file exists, false otherwise
   */
  boolean exists(String path);

  /** Exception thrown when a file is not found. */
  class FailedToReadFileException extends IOException {
    public FailedToReadFileException(String message) {
      super(message);
    }

    public FailedToReadFileException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
