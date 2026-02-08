package ca.lajthabalazs.pressure_integrity_test.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * File system based implementation of TextFileWriter.
 *
 * <p>Writes text files to the file system using standard Java NIO APIs.
 */
public class FileSystemTextFileWriter implements TextFileWriter {

  @Override
  public void writeAllLines(String path, List<String> lines)
      throws TextFileReader.FailedToReadFileException {
    Path filePath = Paths.get(path);
    try {
      Path parent = filePath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.write(filePath, lines, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new TextFileReader.FailedToReadFileException(
          "Failed to write file: " + path + ", " + e.getMessage(), e);
    }
  }

  @Override
  public void appendLines(String path, List<String> lines)
      throws TextFileReader.FailedToReadFileException {
    Path filePath = Paths.get(path);
    try {
      Path parent = filePath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.write(
          filePath,
          lines,
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    } catch (IOException e) {
      throw new TextFileReader.FailedToReadFileException(
          "Failed to append to file: " + path + ", " + e.getMessage(), e);
    }
  }
}
