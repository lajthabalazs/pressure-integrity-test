package ca.lajthabalazs.pressure_integrity_test.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * File system based implementation of TextFileReader.
 *
 * <p>Reads text files from the file system using standard Java NIO APIs.
 */
public class FileSystemTextFileReader implements TextFileReader {

  @Override
  public List<String> readAllLines(String path) throws FailedToReadFileException {
    Path filePath = Paths.get(path);
    if (!Files.exists(filePath)) {
      throw new FailedToReadFileException("File not found: " + path);
    }
    if (!Files.isRegularFile(filePath)) {
      throw new FailedToReadFileException("Path is not a regular file: " + path);
    }
    try {
      return Files.readAllLines(filePath, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new FailedToReadFileException(
          "Path is not a regular file: " + path + ", " + e.getMessage(), e);
    }
  }

  @Override
  public String readAllText(String path) throws FailedToReadFileException {
    Path filePath = Paths.get(path);
    if (!Files.exists(filePath)) {
      throw new FailedToReadFileException("File not found: " + path);
    }
    if (!Files.isRegularFile(filePath)) {
      throw new FailedToReadFileException("Path is not a regular file: " + path);
    }
    try {
      return Files.readString(filePath, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new FailedToReadFileException(
          "Path is not a regular file: " + path + ", " + e.getMessage(), e);
    }
  }

  @Override
  public boolean exists(String path) {
    Path filePath = Paths.get(path);
    return Files.exists(filePath) && Files.isRegularFile(filePath);
  }
}
