package ca.lajthabalazs.pressure_integity_test.measurement;

import ca.lajthabalazs.pressure_integity_test.measurement.streaming.TestMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.config.HumiditySensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.PressureSensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.io.FileSystemTextFileReader;
import ca.lajthabalazs.pressure_integrity_test.io.FileSystemTextFileWriter;
import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader;
import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader.FailedToReadFileException;
import ca.lajthabalazs.pressure_integrity_test.io.TextFileWriter;
import ca.lajthabalazs.pressure_integrity_test.measurement.CsvMeasurementWriter;
import ca.lajthabalazs.pressure_integrity_test.measurement.Humidity;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.Pressure;
import ca.lajthabalazs.pressure_integrity_test.measurement.streaming.MeasurementVectorStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link CsvMeasurementWriter}. */
public class CsvMeasurementWriterTest {

  @TempDir Path tempDir;

  @Test
  public void handle_existingFileWithZeroLines_treatsAsEmptyAndWritesFromScratch()
      throws Exception {
    Path csvPath = tempDir.resolve("measurements.csv");
    Files.write(csvPath, List.of());

    TextFileReader reader = new FileSystemTextFileReader();
    TextFileWriter writer = new FileSystemTextFileWriter();
    CsvMeasurementWriter csvWriter =
        new CsvMeasurementWriter(
            csvPath.toString(), reader, writer, new TestMeasurementVectorStream());

    MeasurementVector v =
        new MeasurementVector(
            1000000L,
            List.of(
                new Humidity(1000000L, "H1", new BigDecimal("45.2")),
                new Pressure(1000000L, "p1", new BigDecimal("98567"))));

    csvWriter.handle(v);

    List<String> lines = Files.readAllLines(csvPath);
    Assertions.assertEquals(2, lines.size());
    Assertions.assertEquals("timestamp,H1,p1", lines.get(0));
    Assertions.assertEquals("1000000,45.2,98567", lines.get(1));
  }

  @Test
  public void handle_existingFileWithSingleEmptyLine_treatsAsEmptyAndWritesFromScratch()
      throws Exception {
    Path csvPath = tempDir.resolve("measurements.csv");
    Files.write(csvPath, List.of("   "));

    TextFileReader reader = new FileSystemTextFileReader();
    TextFileWriter writer = new FileSystemTextFileWriter();
    CsvMeasurementWriter csvWriter =
        new CsvMeasurementWriter(
            csvPath.toString(), reader, writer, new TestMeasurementVectorStream());

    MeasurementVector v =
        new MeasurementVector(
            1000000L,
            List.of(
                new Humidity(1000000L, "H1", new BigDecimal("45.2")),
                new Pressure(1000000L, "p1", new BigDecimal("98567"))));

    csvWriter.handle(v);

    List<String> lines = Files.readAllLines(csvPath);
    Assertions.assertEquals(2, lines.size());
    Assertions.assertEquals("timestamp,H1,p1", lines.get(0));
    Assertions.assertEquals("1000000,45.2,98567", lines.get(1));
  }

  @Test
  public void handle_emptyFile_writesHeaderAndRow() throws Exception {
    Path csvPath = tempDir.resolve("measurements.csv");
    TextFileReader reader = new FileSystemTextFileReader();
    TextFileWriter writer = new FileSystemTextFileWriter();
    CsvMeasurementWriter csvWriter =
        new CsvMeasurementWriter(
            csvPath.toString(), reader, writer, new TestMeasurementVectorStream());

    MeasurementVector v =
        new MeasurementVector(
            1000000L,
            List.of(
                new Humidity(1000000L, "H1", new BigDecimal("45.2")),
                new Pressure(1000000L, "p1", new BigDecimal("98567"))));

    csvWriter.handle(v);

    List<String> lines = Files.readAllLines(csvPath);
    Assertions.assertEquals(2, lines.size());
    Assertions.assertEquals("timestamp,H1,p1", lines.get(0));
    Assertions.assertEquals("1000000,45.2,98567", lines.get(1));
  }

  @Test
  public void handle_emptyFileWithStreamProvidingSensors_usesSensorOrderInHeader()
      throws Exception {
    Path csvPath = tempDir.resolve("measurements.csv");
    TextFileReader reader = new FileSystemTextFileReader();
    TextFileWriter writer = new FileSystemTextFileWriter();
    MeasurementVectorStream streamWithSensors =
        new TestMeasurementVectorStream() {
          @Override
          public List<SensorConfig> listSensors() {
            PressureSensorConfig p1 = new PressureSensorConfig();
            p1.setId("p1");
            HumiditySensorConfig h1 = new HumiditySensorConfig();
            h1.setId("H1");
            return List.of(p1, h1);
          }
        };
    CsvMeasurementWriter csvWriter =
        new CsvMeasurementWriter(csvPath.toString(), reader, writer, streamWithSensors);

    MeasurementVector v =
        new MeasurementVector(
            1000000L,
            List.of(
                new Humidity(1000000L, "H1", new BigDecimal("45.2")),
                new Pressure(1000000L, "p1", new BigDecimal("98567"))));

    csvWriter.handle(v);

    List<String> lines = Files.readAllLines(csvPath);
    Assertions.assertEquals(2, lines.size());
    Assertions.assertEquals("timestamp,p1,H1", lines.get(0));
    Assertions.assertEquals("1000000,98567,45.2", lines.get(1));
  }

  @Test
  public void handle_missingMeasurements_writesEmptyStrings() throws Exception {
    Path csvPath = tempDir.resolve("measurements.csv");
    TextFileReader reader = new FileSystemTextFileReader();
    TextFileWriter writer = new FileSystemTextFileWriter();
    CsvMeasurementWriter csvWriter =
        new CsvMeasurementWriter(
            csvPath.toString(), reader, writer, new TestMeasurementVectorStream());

    MeasurementVector v1 =
        new MeasurementVector(
            1000000L,
            List.of(
                new Humidity(1000000L, "H1", new BigDecimal("45.2")),
                new Pressure(1000000L, "p1", new BigDecimal("98567"))));
    csvWriter.handle(v1);

    MeasurementVector v2 =
        new MeasurementVector(
            1000100L, List.of(new Humidity(1000100L, "H1", new BigDecimal("45.5"))));
    csvWriter.handle(v2);

    List<String> lines = Files.readAllLines(csvPath);
    Assertions.assertEquals(3, lines.size());
    Assertions.assertEquals("timestamp,H1,p1", lines.get(0));
    Assertions.assertEquals("1000000,45.2,98567", lines.get(1));
    Assertions.assertEquals("1000100,45.5,", lines.get(2));
  }

  @Test
  public void handle_existingFileWithMatchingHeaderAndRecentTimestamp_appends() throws Exception {
    Path csvPath = tempDir.resolve("measurements.csv");
    long now = System.currentTimeMillis();
    long recent = now - 10 * 60 * 1000; // 10 minutes ago
    Files.write(csvPath, List.of("timestamp,H1,p1", recent + ",45.0,98500"));

    TextFileReader reader = new FileSystemTextFileReader();
    TextFileWriter writer = new FileSystemTextFileWriter();
    CsvMeasurementWriter csvWriter =
        new CsvMeasurementWriter(
            csvPath.toString(), reader, writer, new TestMeasurementVectorStream());

    MeasurementVector v =
        new MeasurementVector(
            now,
            List.of(
                new Humidity(now, "H1", new BigDecimal("45.2")),
                new Pressure(now, "p1", new BigDecimal("98567"))));
    csvWriter.handle(v);

    List<String> lines = Files.readAllLines(csvPath);
    Assertions.assertEquals(3, lines.size());
    Assertions.assertTrue(lines.get(2).startsWith(String.valueOf(now)));
  }

  @Test
  public void handle_existingFileWithOldTimestamp_throws() throws Exception {
    Path csvPath = tempDir.resolve("measurements.csv");
    long oldTimestamp = System.currentTimeMillis() - 2 * 60 * 60 * 1000; // 2 hours ago
    Files.write(csvPath, List.of("timestamp,H1,p1", oldTimestamp + ",45.0,98500"));

    TextFileReader reader = new FileSystemTextFileReader();
    TextFileWriter writer = new FileSystemTextFileWriter();
    CsvMeasurementWriter csvWriter =
        new CsvMeasurementWriter(
            csvPath.toString(), reader, writer, new TestMeasurementVectorStream());

    MeasurementVector v =
        new MeasurementVector(
            System.currentTimeMillis(),
            List.of(
                new Humidity(0L, "H1", new BigDecimal("45.2")),
                new Pressure(0L, "p1", new BigDecimal("98567"))));

    Assertions.assertThrows(IllegalArgumentException.class, () -> csvWriter.handle(v));
  }

  @Test
  public void handle_existingFileWithMismatchedHeader_throws() throws Exception {
    Path csvPath = tempDir.resolve("measurements.csv");
    long now = System.currentTimeMillis();
    Files.write(csvPath, List.of("timestamp,H1", (now - 1000) + ",45.0"));

    TextFileReader reader = new FileSystemTextFileReader();
    TextFileWriter writer = new FileSystemTextFileWriter();
    CsvMeasurementWriter csvWriter =
        new CsvMeasurementWriter(
            csvPath.toString(), reader, writer, new TestMeasurementVectorStream());

    MeasurementVector v =
        new MeasurementVector(
            now,
            List.of(
                new Humidity(now, "H1", new BigDecimal("45.2")),
                new Pressure(now, "p1", new BigDecimal("98567"))));

    Assertions.assertThrows(IllegalArgumentException.class, () -> csvWriter.handle(v));
  }

  @Test
  public void handle_existingFileWithHeaderOrderMismatch_throwsWithExpectedAndFoundInMessage()
      throws Exception {
    Path csvPath = tempDir.resolve("measurements.csv");
    long now = System.currentTimeMillis();
    // Existing file: header order p1,H1 (alphabetically p before H)
    Files.write(csvPath, List.of("timestamp,p1,H1", (now - 1000) + ",98567,45.0"));

    TextFileReader reader = new FileSystemTextFileReader();
    TextFileWriter writer = new FileSystemTextFileWriter();
    CsvMeasurementWriter csvWriter =
        new CsvMeasurementWriter(
            csvPath.toString(), reader, writer, new TestMeasurementVectorStream());

    MeasurementVector v =
        new MeasurementVector(
            now,
            List.of(
                new Humidity(now, "H1", new BigDecimal("45.2")),
                new Pressure(now, "p1", new BigDecimal("98567"))));
    // Expected schema from TreeSet: timestamp,H1,p1 (H before p alphabetically)

    IllegalArgumentException ex =
        Assertions.assertThrows(IllegalArgumentException.class, () -> csvWriter.handle(v));
    Assertions.assertTrue(
        ex.getMessage().contains("header does not match"),
        "Message should mention header mismatch: " + ex.getMessage());
    Assertions.assertTrue(
        ex.getMessage().contains("timestamp,H1,p1"),
        "Message should include expected header: " + ex.getMessage());
    Assertions.assertTrue(
        ex.getMessage().contains("timestamp,p1,H1"),
        "Message should include found header: " + ex.getMessage());
  }

  @Test
  public void handle_existingFileWithInsufficientContent_throws() throws Exception {
    Path csvPath = tempDir.resolve("measurements.csv");
    long now = System.currentTimeMillis();
    Files.write(csvPath, List.of("timestamp,H1,p1"));

    TextFileReader reader = new FileSystemTextFileReader();
    TextFileWriter writer = new FileSystemTextFileWriter();
    CsvMeasurementWriter csvWriter =
        new CsvMeasurementWriter(
            csvPath.toString(), reader, writer, new TestMeasurementVectorStream());

    MeasurementVector v =
        new MeasurementVector(
            now,
            List.of(
                new Humidity(now, "H1", new BigDecimal("45.2")),
                new Pressure(now, "p1", new BigDecimal("98567"))));

    IllegalArgumentException ex =
        Assertions.assertThrows(IllegalArgumentException.class, () -> csvWriter.handle(v));
    Assertions.assertTrue(
        ex.getMessage().contains("insufficient content"),
        "Message should mention insufficient content: " + ex.getMessage());
  }

  @Test
  public void handle_existingFileWithNoValidTimestamp_throws() throws Exception {
    Path csvPath = tempDir.resolve("measurements.csv");
    Files.write(csvPath, List.of("timestamp,H1,p1", "not-a-number,45.0,98500"));

    TextFileReader reader = new FileSystemTextFileReader();
    TextFileWriter writer = new FileSystemTextFileWriter();
    CsvMeasurementWriter csvWriter =
        new CsvMeasurementWriter(
            csvPath.toString(), reader, writer, new TestMeasurementVectorStream());

    MeasurementVector v =
        new MeasurementVector(
            System.currentTimeMillis(),
            List.of(
                new Humidity(0L, "H1", new BigDecimal("45.2")),
                new Pressure(0L, "p1", new BigDecimal("98567"))));

    IllegalArgumentException ex =
        Assertions.assertThrows(IllegalArgumentException.class, () -> csvWriter.handle(v));
    Assertions.assertTrue(
        ex.getMessage().contains("No valid timestamp"),
        "Message should mention no valid timestamp: " + ex.getMessage());
  }

  @Test
  public void
      handle_existingFileWhenReaderThrowsFailedToReadFileException_throwsIllegalArgumentException()
          throws Exception {
    Path csvPath = tempDir.resolve("measurements.csv");
    long now = System.currentTimeMillis();
    List<String> fileContent = List.of("timestamp,H1,p1", (now - 1000) + ",45.0,98500");
    Files.write(csvPath, fileContent);

    TextFileReader reader =
        new TextFileReader() {
          private int readCount = 0;

          @Override
          public List<String> readAllLines(String path) throws FailedToReadFileException {
            readCount++;
            if (readCount > 1) {
              throw new FailedToReadFileException("Simulated read failure on append");
            }
            return fileContent;
          }

          @Override
          public String readAllText(String path) throws FailedToReadFileException {
            throw new FailedToReadFileException("Simulated read failure");
          }

          @Override
          public boolean exists(String path) {
            return true;
          }
        };
    TextFileWriter writer = new FileSystemTextFileWriter();
    CsvMeasurementWriter csvWriter =
        new CsvMeasurementWriter(
            csvPath.toString(), reader, writer, new TestMeasurementVectorStream());

    MeasurementVector v =
        new MeasurementVector(
            now,
            List.of(
                new Humidity(now, "H1", new BigDecimal("45.2")),
                new Pressure(now, "p1", new BigDecimal("98567"))));

    IllegalArgumentException ex =
        Assertions.assertThrows(IllegalArgumentException.class, () -> csvWriter.handle(v));
    Assertions.assertTrue(
        ex.getMessage().contains("Cannot read existing file"),
        "Message should mention cannot read: " + ex.getMessage());
    Assertions.assertNotNull(ex.getCause());
    Assertions.assertInstanceOf(FailedToReadFileException.class, ex.getCause());
  }

  @Test
  public void handle_existingEmptyFileWhenReaderThrowsAndWriterThrows_throwsRuntimeException()
      throws Exception {
    Path csvPath = tempDir.resolve("measurements.csv");
    Files.write(csvPath, List.of()); // existing empty file
    TextFileReader reader =
        new TextFileReader() {
          @Override
          public List<String> readAllLines(String path) throws FailedToReadFileException {
            throw new FailedToReadFileException("Simulated read failure");
          }

          @Override
          public String readAllText(String path) throws FailedToReadFileException {
            throw new FailedToReadFileException("Simulated read failure");
          }

          @Override
          public boolean exists(String path) {
            return true;
          }
        };
    TextFileWriter writer =
        new TextFileWriter() {
          @Override
          public void writeAllLines(String path, List<String> lines)
              throws FailedToReadFileException {
            throw new FailedToReadFileException("Simulated write failure");
          }

          @Override
          public void appendLines(String path, List<String> lines)
              throws FailedToReadFileException {
            throw new FailedToReadFileException("Simulated append failure");
          }
        };
    CsvMeasurementWriter csvWriter =
        new CsvMeasurementWriter(
            csvPath.toString(), reader, writer, new TestMeasurementVectorStream());

    MeasurementVector v =
        new MeasurementVector(
            1000000L,
            List.of(
                new Humidity(1000000L, "H1", new BigDecimal("45.2")),
                new Pressure(1000000L, "p1", new BigDecimal("98567"))));

    RuntimeException ex =
        Assertions.assertThrows(RuntimeException.class, () -> csvWriter.handle(v));
    Assertions.assertTrue(
        ex.getMessage().contains("Failed to write CSV"),
        "Message should mention failed to write: " + ex.getMessage());
    Assertions.assertNotNull(ex.getCause());
    Assertions.assertInstanceOf(FailedToReadFileException.class, ex.getCause());
  }

  @Test
  public void handle_whenWriterThrowsFailedToReadFileException_throwsRuntimeException()
      throws Exception {
    Path csvPath = tempDir.resolve("measurements.csv");
    TextFileReader reader = new FileSystemTextFileReader();
    TextFileWriter writer =
        new TextFileWriter() {
          @Override
          public void writeAllLines(String path, List<String> lines)
              throws FailedToReadFileException {
            throw new FailedToReadFileException("Simulated write failure");
          }

          @Override
          public void appendLines(String path, List<String> lines)
              throws FailedToReadFileException {
            throw new FailedToReadFileException("Simulated append failure");
          }
        };
    CsvMeasurementWriter csvWriter =
        new CsvMeasurementWriter(
            csvPath.toString(), reader, writer, new TestMeasurementVectorStream());

    MeasurementVector v =
        new MeasurementVector(
            1000000L,
            List.of(
                new Humidity(1000000L, "H1", new BigDecimal("45.2")),
                new Pressure(1000000L, "p1", new BigDecimal("98567"))));

    RuntimeException ex =
        Assertions.assertThrows(RuntimeException.class, () -> csvWriter.handle(v));
    Assertions.assertTrue(
        ex.getMessage().contains("Failed to write CSV"),
        "Message should mention failed to write: " + ex.getMessage());
    Assertions.assertNotNull(ex.getCause());
    Assertions.assertInstanceOf(FailedToReadFileException.class, ex.getCause());
  }
}
