package ca.lajthabalazs.pressure_integrity_test.io;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * File system based implementation of TextFileReader.
 *
 * <p>Reads text files from the file system using standard Java NIO APIs. By default uses UTF-8; use
 * the constructor that takes a {@link Charset} for files in another encoding (e.g. Windows-1250 for
 * Central European ITV files).
 */
public class FileSystemTextFileReader implements TextFileReader {

  private final Charset charset;

  /** Creates a reader that uses UTF-8 encoding. */
  public FileSystemTextFileReader() {
    this(StandardCharsets.UTF_8);
  }

  /**
   * Creates a reader that uses the given encoding.
   *
   * @param charset encoding to use (e.g. Windows-1250 for Central European .ITV files)
   */
  public FileSystemTextFileReader(Charset charset) {
    this.charset = charset != null ? charset : StandardCharsets.UTF_8;
  }

  private static Path toPath(String path) throws FailedToReadFileException {
    if (path == null || path.isBlank()) {
      throw new FailedToReadFileException("Path is null or empty");
    }
    return Paths.get(path.trim()).toAbsolutePath().normalize();
  }

  @Override
  public List<String> readAllLines(String path) throws FailedToReadFileException {
    Path filePath = toPath(path);
    if (!Files.exists(filePath)) {
      throw new FailedToReadFileException("File not found: " + path);
    }
    if (Files.isDirectory(filePath)) {
      throw new FailedToReadFileException(
          "Path is a directory, not a file. Please select an ITV or JSON file: " + path);
    }
    if (!Files.isRegularFile(filePath)) {
      throw new FailedToReadFileException("Path is not a regular file: " + path);
    }
    try {
      return Files.readAllLines(filePath, charset);
    } catch (IOException e) {
      throw new FailedToReadFileException(
          "Failed to read file: " + path + " — " + e.getMessage(), e);
    }
  }

  @Override
  public String readAllText(String path) throws FailedToReadFileException {
    Path filePath = toPath(path);
    if (!Files.exists(filePath)) {
      throw new FailedToReadFileException("File not found: " + path);
    }
    if (Files.isDirectory(filePath)) {
      throw new FailedToReadFileException(
          "Path is a directory, not a file. Please select a file: " + path);
    }
    if (!Files.isRegularFile(filePath)) {
      throw new FailedToReadFileException("Path is not a regular file: " + path);
    }
    try {
      return Files.readString(filePath, charset);
    } catch (IOException e) {
      throw new FailedToReadFileException(
          "Failed to read file: " + path + " — " + e.getMessage(), e);
    }
  }

  @Override
  public boolean exists(String path) {
    try {
      Path filePath = toPath(path);
      return Files.exists(filePath) && Files.isRegularFile(filePath);
    } catch (FailedToReadFileException e) {
      return false;
    }
  }
}
