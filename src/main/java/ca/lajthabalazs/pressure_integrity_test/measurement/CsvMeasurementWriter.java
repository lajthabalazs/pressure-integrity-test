package ca.lajthabalazs.pressure_integrity_test.measurement;

import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader;
import ca.lajthabalazs.pressure_integrity_test.io.TextFileWriter;
import ca.lajthabalazs.pressure_integrity_test.measurement.streaming.MeasurementVectorStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * A {@link MeasurementVectorStream.MeasurementVectorHandler} that writes measurement vectors to a
 * CSV file.
 *
 * <p>Each row contains a UTC timestamp (milliseconds since epoch) followed by sensor values.
 * Missing measurements are written as empty strings. The header line lists column names: timestamp
 * plus sensor IDs in the order defined by {@link MeasurementVectorStream#listSensors()}, or from
 * the first measurement vector if the stream returns no sensors.
 *
 * <p>If the file is empty or does not exist, the header and measurements are written from scratch.
 * If the file already has content, the writer reads the header and checks that: (1) it matches the
 * sensor list in the same order, and (2) the last timestamp in the file is within one hour of the
 * incoming measurement's timestamp. If both hold, new rows are appended. Otherwise, an {@link
 * IllegalArgumentException} is thrown.
 */
public class CsvMeasurementWriter implements MeasurementVectorStream.MeasurementVectorHandler {

  private static final long MAX_TIMESTAMP_AGE_MS = 60 * 60 * 1000; // 1 hour

  private final String filePath;
  private final TextFileReader reader;
  private final TextFileWriter writer;
  private final MeasurementVectorStream stream;

  private List<String> schema; // null until first write; then ["timestamp", "p1", "p2", ...]
  private final Object lock = new Object();

  /**
   * Creates a CSV measurement writer.
   *
   * @param filePath path to the CSV file
   * @param reader used to read existing file content when appending
   * @param writer used to write the CSV file
   * @param stream the measurement stream; its {@link MeasurementVectorStream#listSensors()} defines
   *     the header order when non-empty
   */
  public CsvMeasurementWriter(
      String filePath,
      TextFileReader reader,
      TextFileWriter writer,
      MeasurementVectorStream stream) {
    this.filePath = filePath;
    this.reader = reader;
    this.writer = writer;
    this.stream = stream;
  }

  @Override
  public void handle(MeasurementVector vector) {
    synchronized (lock) {
      try {
        if (schema == null) {
          initializeOrAppend(vector);
        } else {
          writer.appendLines(filePath, List.of(toCsvRow(vector)));
        }
      } catch (TextFileReader.FailedToReadFileException e) {
        throw new RuntimeException("Failed to write CSV: " + filePath, e);
      }
    }
  }

  private void initializeOrAppend(MeasurementVector vector)
      throws TextFileReader.FailedToReadFileException {
    boolean fileEmpty = isFileEmpty();

    if (fileEmpty) {
      schema = buildSchema(vector);
      String header = String.join(",", schema);
      String firstRow = toCsvRow(vector);
      writer.writeAllLines(filePath, List.of(header, firstRow));
    } else {
      List<String> lines;
      try {
        lines = reader.readAllLines(filePath);
      } catch (TextFileReader.FailedToReadFileException e) {
        throw new IllegalArgumentException("Cannot read existing file: " + filePath, e);
      }
      if (lines.size() < 2) {
        throw new IllegalArgumentException(
            "Existing file has insufficient content to append: " + filePath);
      }

      List<String> existingHeader = parseCsvLine(lines.get(0));
      schema = buildSchema(vector);
      if (!headersMatch(existingHeader, schema)) {
        throw new IllegalArgumentException(
            "Cannot append: CSV header does not match current sensors. "
                + "Expected: "
                + String.join(",", schema)
                + "; found: "
                + String.join(",", existingHeader));
      }

      long lastTimestamp = parseLastTimestamp(lines);
      long measurementTimestamp = vector.getTimeUtc();
      if (Math.abs(measurementTimestamp - lastTimestamp) > MAX_TIMESTAMP_AGE_MS) {
        throw new IllegalArgumentException(
            "Cannot append: last timestamp in file ("
                + lastTimestamp
                + ") is more than one hour away from incoming measurement timestamp ("
                + measurementTimestamp
                + ")");
      }

      writer.appendLines(filePath, List.of(toCsvRow(vector)));
    }
  }

  private boolean isFileEmpty() {
    if (!reader.exists(filePath)) {
      return true;
    }
    try {
      List<String> lines = reader.readAllLines(filePath);
      return lines.isEmpty() || (lines.size() == 1 && lines.getFirst().trim().isEmpty());
    } catch (TextFileReader.FailedToReadFileException e) {
      return true;
    }
  }

  private List<String> buildSchema(MeasurementVector vector) {
    List<SensorConfig> sensors = stream.listSensors();
    List<String> s = new ArrayList<>();
    s.add("timestamp");
    if (!sensors.isEmpty()) {
      for (SensorConfig sensor : sensors) {
        if (sensor.getId() != null) {
          s.add(sensor.getId());
        }
      }
    } else {
      s.addAll(new TreeSet<>(vector.getMeasurementsMap().keySet()));
    }
    return List.copyOf(s);
  }

  private boolean headersMatch(List<String> existing, List<String> expected) {
    if (existing.size() != expected.size()) {
      return false;
    }
    for (int i = 0; i < existing.size(); i++) {
      if (!existing.get(i).trim().equalsIgnoreCase(expected.get(i))) {
        return false;
      }
    }
    return true;
  }

  private long parseLastTimestamp(List<String> lines) {
    for (int i = lines.size() - 1; i >= 1; i--) {
      String line = lines.get(i).trim();
      List<String> cols = parseCsvLine(line);
      if (!cols.isEmpty()) {
        try {
          return Long.parseLong(cols.getFirst().trim());
        } catch (NumberFormatException e) {
          // Skip non-data rows (e.g. comments)
        }
      }
    }
    throw new IllegalArgumentException("No valid timestamp found in existing file: " + filePath);
  }

  private List<String> parseCsvLine(String line) {
    List<String> result = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == ',') {
        result.add(current.toString());
        current = new StringBuilder();
      } else {
        current.append(c);
      }
    }
    result.add(current.toString());
    return result;
  }

  private String toCsvRow(MeasurementVector vector) {
    Map<String, String> valuesBySource = new LinkedHashMap<>();
    for (Map.Entry<String, Measurement> e : vector.getMeasurementsMap().entrySet()) {
      valuesBySource.put(e.getKey(), e.getValue().getValueInDefaultUnit().toPlainString());
    }

    StringBuilder row = new StringBuilder();
    for (int i = 0; i < schema.size(); i++) {
      if (i > 0) {
        row.append(',');
      }
      String col = schema.get(i);
      if ("timestamp".equals(col)) {
        row.append(vector.getTimeUtc());
      } else {
        String value = valuesBySource.get(col);
        row.append(value != null ? value : "");
      }
    }
    return row.toString();
  }
}
