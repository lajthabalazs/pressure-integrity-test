package ca.lajthabalazs.pressure_integrity_test.io;

import ca.lajthabalazs.pressure_integrity_test.measurement.Humidity;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.Pressure;
import ca.lajthabalazs.pressure_integrity_test.measurement.Temperature;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads ITV (Integrális Tömörségvizsgálat) measurement files.
 *
 * <p>Dates and times in the file are interpreted as Paks, Hungary local time (Europe/Budapest).
 * Output timestamps are in UTC milliseconds since epoch.
 */
public class ItvFileReader {

  /** Paks, Hungary timezone. */
  private static final ZoneId PAKS_TIMEZONE = ZoneId.of("Europe/Budapest");

  private static final int COL_DATE = 1;
  private static final int COL_TIME = 2;
  private static final int COL_P_MAIN = 5;
  private static final int COL_T1_START = 29;
  private static final int COL_T_COUNT = 60;
  private static final int COL_FI1_START = 91; // empty col 90 between T60 and fi1
  private static final int COL_FI_COUNT = 10;
  private static final int COL_P1 = 102; // empty col 101 between fi10 and p1
  private static final int COL_P2 = 103;

  private final TextFileReader textFileReader;

  public ItvFileReader(TextFileReader textFileReader) {
    this.textFileReader = textFileReader;
  }

  /**
   * Reads an ITV file and returns the measurement vectors. Times are interpreted as Paks, Hungary
   * local time.
   *
   * @param path path to the ITV file
   * @return list of measurement vectors (may be empty)
   * @throws TextFileReader.FailedToReadFileException if the file cannot be read
   */
  public List<MeasurementVector> read(String path) throws TextFileReader.FailedToReadFileException {
    List<String> lines = textFileReader.readAllLines(path);
    return parseLines(lines);
  }

  private List<MeasurementVector> parseLines(List<String> lines) {
    List<MeasurementVector> result = new ArrayList<>();
    int dataStart = findDataStartLine(lines);
    for (int i = dataStart; i < lines.size(); i++) {
      String line = lines.get(i).trim();
      if (line.isEmpty() || line.startsWith("MÉRÉS VÉGE")) {
        break;
      }
      MeasurementVector vector = parseDataLine(line);
      if (vector != null) {
        result.add(vector);
      }
    }
    return result;
  }

  private int findDataStartLine(List<String> lines) {
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line.startsWith("1\t") && line.contains("\t")) {
        return i;
      }
    }
    return lines.size();
  }

  private MeasurementVector parseDataLine(String line) {
    String[] cols = line.split("\t", -1);
    if (cols.length <= COL_P2) {
      return null;
    }
    long timeUtc = parseTimestamp(cols[COL_DATE], cols[COL_TIME]);
    if (timeUtc < 0) {
      return null;
    }
    Map<String, Measurement> measurements = new LinkedHashMap<>();

    BigDecimal pMain = parseDecimal(cols[COL_P_MAIN]);
    if (pMain != null) {
      measurements.put("p", new Pressure(timeUtc, "p", pMain));
    }
    BigDecimal p1 = parseDecimal(cols[COL_P1]);
    if (p1 != null) {
      measurements.put("p1", new Pressure(timeUtc, "p1", p1));
    }
    BigDecimal p2 = parseDecimal(cols[COL_P2]);
    if (p2 != null) {
      measurements.put("p2", new Pressure(timeUtc, "p2", p2));
    }

    for (int t = 0; t < COL_T_COUNT; t++) {
      int idx = COL_T1_START + t;
      BigDecimal temp = parseDecimal(cols[idx]);
      if (temp != null) {
        String id = "T" + (t + 1);
        measurements.put(id, new Temperature(timeUtc, id, temp));
      }
    }

    for (int f = 0; f < COL_FI_COUNT; f++) {
      int idx = COL_FI1_START + f;
      BigDecimal hum = parseDecimal(cols[idx]);
      if (hum != null) {
        String id = "fi" + (f + 1);
        measurements.put(id, new Humidity(timeUtc, id, hum));
      }
    }

    if (measurements.isEmpty()) {
      return null;
    }
    return new MeasurementVector(timeUtc, measurements);
  }

  private long parseTimestamp(String dateStr, String timeStr) {
    try {
      LocalDate date = LocalDate.parse(dateStr.replace(".", "-"));
      LocalTime time = LocalTime.parse(timeStr);
      ZonedDateTime zdt = ZonedDateTime.of(date, time, PAKS_TIMEZONE);
      return zdt.toInstant().toEpochMilli();
    } catch (Exception e) {
      return -1;
    }
  }

  private BigDecimal parseDecimal(String s) {
    if (s == null || s.isBlank()) {
      return null;
    }
    if ("NaN".equalsIgnoreCase(s) || "Infinity".equalsIgnoreCase(s)) {
      return null;
    }
    try {
      return new BigDecimal(s.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
