package ca.lajthabalazs.pressure_integity_test.io;

import ca.lajthabalazs.pressure_integrity_test.io.ItvFileReader;
import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader;
import ca.lajthabalazs.pressure_integrity_test.measurement.Humidity;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.Pressure;
import ca.lajthabalazs.pressure_integrity_test.measurement.Temperature;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Tests for {@link ca.lajthabalazs.pressure_integrity_test.io.ItvFileReader}. */
public class ItvFileReaderTest {

  @Test
  public void read_parsesVectorsWithHungarianTimezone() throws Exception {
    TextFileReader reader = new ResourceTextFileReader(ItvFileReaderTest.class);
    ItvFileReader itvReader = new ItvFileReader(reader);
    List<MeasurementVector> vectors = itvReader.read("/itv/itv-sample.ITV");

    Assertions.assertEquals(10, vectors.size());

    // First row: 2026.02.08 00:00:00 in Paks (Europe/Budapest) — itv-sample.ITV (formula-based)
    MeasurementVector v0 = vectors.get(0);
    ZonedDateTime expectedLocal =
        ZonedDateTime.of(2026, 2, 8, 0, 0, 0, 0, ZoneId.of("Europe/Budapest"));
    long expectedUtc = expectedLocal.toInstant().toEpochMilli();
    Assertions.assertEquals(expectedUtc, v0.getTimeUtc());

    List<Measurement> m0 = v0.getMeasurements();
    Assertions.assertEquals(74, m0.size()); // p, p1, p2 + 61 temps + 10 humidity

    // Check main pressure (zeroed as computed field)
    Measurement pMain =
        m0.stream()
            .filter(m -> "p".equals(m.getSourceId()) && m instanceof Pressure)
            .findFirst()
            .orElse(null);
    Assertions.assertNotNull(pMain);
    Assertions.assertEquals(0, new BigDecimal("0.000000").compareTo(pMain.getValueInDefaultUnit()));

    // Check p1, p2 (formula: 100010 + line_index, 100020 + line_index; line 0)
    Measurement p1 =
        m0.stream()
            .filter(m -> "p1".equals(m.getSourceId()) && m instanceof Pressure)
            .findFirst()
            .orElse(null);
    Assertions.assertNotNull(p1);
    Assertions.assertEquals(
        0, new BigDecimal("100010.000000").compareTo(p1.getValueInDefaultUnit()));
    Measurement p2 =
        m0.stream()
            .filter(m -> "p2".equals(m.getSourceId()) && m instanceof Pressure)
            .findFirst()
            .orElse(null);
    Assertions.assertNotNull(p2);
    Assertions.assertEquals(
        0, new BigDecimal("100020.000000").compareTo(p2.getValueInDefaultUnit()));

    // Check temperatures (61 sensors T1–T61; T = 20 + sensor_index/10 + line_index/100)
    long tempCount = m0.stream().filter(m -> m instanceof Temperature).count();
    Assertions.assertEquals(61, tempCount);
    Measurement t1 =
        m0.stream()
            .filter(m -> "T1".equals(m.getSourceId()) && m instanceof Temperature)
            .findFirst()
            .orElse(null);
    Assertions.assertNotNull(t1);
    Assertions.assertEquals(0, new BigDecimal("20.100000").compareTo(t1.getValueInDefaultUnit()));
    Measurement t61 =
        m0.stream()
            .filter(m -> "T61".equals(m.getSourceId()) && m instanceof Temperature)
            .findFirst()
            .orElse(null);
    Assertions.assertNotNull(t61);
    Assertions.assertEquals(0, new BigDecimal("26.100000").compareTo(t61.getValueInDefaultUnit()));

    // Check humidity (H = 40 + sensor_index + line_index/10; fi1 line 0 = 41)
    long humCount = m0.stream().filter(m -> m instanceof Humidity).count();
    Assertions.assertEquals(10, humCount);
    Measurement fi1 =
        m0.stream()
            .filter(m -> "fi1".equals(m.getSourceId()) && m instanceof Humidity)
            .findFirst()
            .orElse(null);
    Assertions.assertNotNull(fi1);
    Assertions.assertEquals(0, new BigDecimal("41.000000").compareTo(fi1.getValueInDefaultUnit()));
  }
}
