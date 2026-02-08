package ca.lajthabalazs.pressure_integrity_test.io;

import java.util.List;

/**
 * Interface for writing text files.
 *
 * <p>Provides methods to write and append content to files, abstracting the underlying storage.
 */
public interface TextFileWriter {

  /**
   * Writes all lines to a file, overwriting any existing content.
   *
   * @param path the path to the file
   * @param lines the lines to write
   * @throws TextFileReader.FailedToReadFileException if the file cannot be written
   */
  void writeAllLines(String path, List<String> lines)
      throws TextFileReader.FailedToReadFileException;

  /**
   * Appends lines to the end of a file. Creates the file if it does not exist.
   *
   * @param path the path to the file
   * @param lines the lines to append
   * @throws TextFileReader.FailedToReadFileException if the file cannot be written
   */
  void appendLines(String path, List<String> lines) throws TextFileReader.FailedToReadFileException;
}
