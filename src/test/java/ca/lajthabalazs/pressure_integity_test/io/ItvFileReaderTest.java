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
  public void read_parsesVectorsWithPaksTimezone() throws Exception {
    TextFileReader reader = new ResourceTextFileReader(ItvFileReaderTest.class);
    ItvFileReader itvReader = new ItvFileReader(reader);
    List<MeasurementVector> vectors = itvReader.read("/itv-sample.ITV");

    Assertions.assertEquals(2, vectors.size());

    // First row: 2024.03.27 13:00:15 in Paks (Europe/Budapest)
    MeasurementVector v0 = vectors.get(0);
    ZonedDateTime expectedLocal =
        ZonedDateTime.of(2024, 3, 27, 13, 0, 15, 0, ZoneId.of("Europe/Budapest"));
    long expectedUtc = expectedLocal.toInstant().toEpochMilli();
    Assertions.assertEquals(expectedUtc, v0.getTimeUtc());

    List<Measurement> m0 = v0.getMeasurements();
    Assertions.assertEquals(73, m0.size()); // p, p1, p2 + 60 temps + 10 humidity

    // Check main pressure
    Measurement pMain =
        m0.stream()
            .filter(m -> "p".equals(m.getSourceId()) && m instanceof Pressure)
            .findFirst()
            .orElse(null);
    Assertions.assertNotNull(pMain);
    Assertions.assertEquals(
        0, new BigDecimal("98567.000000").compareTo(pMain.getValueInDefaultUnit()));

    // Check p1, p2
    Measurement p1 =
        m0.stream()
            .filter(m -> "p1".equals(m.getSourceId()) && m instanceof Pressure)
            .findFirst()
            .orElse(null);
    Assertions.assertNotNull(p1);
    Assertions.assertEquals(
        0, new BigDecimal("98567.000000").compareTo(p1.getValueInDefaultUnit()));
    Measurement p2 =
        m0.stream()
            .filter(m -> "p2".equals(m.getSourceId()) && m instanceof Pressure)
            .findFirst()
            .orElse(null);
    Assertions.assertNotNull(p2);
    Assertions.assertEquals(
        0, new BigDecimal("98571.000000").compareTo(p2.getValueInDefaultUnit()));

    // Check temperatures
    long tempCount = m0.stream().filter(m -> m instanceof Temperature).count();
    Assertions.assertEquals(60, tempCount);
    Measurement t1 =
        m0.stream()
            .filter(m -> "T1".equals(m.getSourceId()) && m instanceof Temperature)
            .findFirst()
            .orElse(null);
    Assertions.assertNotNull(t1);
    Assertions.assertEquals(0, new BigDecimal("22.800000").compareTo(t1.getValueInDefaultUnit()));

    // Check humidity
    long humCount = m0.stream().filter(m -> m instanceof Humidity).count();
    Assertions.assertEquals(10, humCount);
    Measurement fi1 =
        m0.stream()
            .filter(m -> "fi1".equals(m.getSourceId()) && m instanceof Humidity)
            .findFirst()
            .orElse(null);
    Assertions.assertNotNull(fi1);
    Assertions.assertEquals(0, new BigDecimal("38.540000").compareTo(fi1.getValueInDefaultUnit()));
  }
}
