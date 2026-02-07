package ca.lajthabalazs.pressure_integity_test.config;

import ca.lajthabalazs.pressure_integity_test.io.ResourceTextFileReader;
import ca.lajthabalazs.pressure_integrity_test.config.*;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfigReader.SiteConfigParseException;
import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader.FailedToReadFileException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SiteConfigReaderTest {

  @Test
  public void read_validConfig_returnsParsedSiteConfig() throws Exception {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(SiteConfigReaderTest.class);
    SiteConfigReader reader = new SiteConfigReader(fileReader);

    SiteConfig config = reader.read("/site-config-sample.json");

    Assertions.assertNotNull(config);
    Assertions.assertEquals("PAKS_BLOCK_2", config.getId());
    Assertions.assertEquals("Paks NPP – Block 2 containment", config.getDescription());

    Assertions.assertNotNull(config.getContainment());
    Assertions.assertEquals(
        0, new BigDecimal("51166").compareTo(config.getContainment().getNetVolume_m3()));

    Assertions.assertNotNull(config.getDesignPressure());
    Assertions.assertEquals(
        0, new BigDecimal("150000").compareTo(config.getDesignPressure().getOverpressure_Pa()));
    Assertions.assertEquals(
        0,
        new BigDecimal("14.7")
            .compareTo(config.getDesignPressure().getLeakLimit_percent_per_day()));

    List<SensorConfig> sensors = config.getSensors();
    Assertions.assertEquals(3, sensors.size());

    Assertions.assertInstanceOf(PressureSensorConfig.class, sensors.get(0));
    PressureSensorConfig p1 = (PressureSensorConfig) sensors.get(0);
    Assertions.assertEquals("P1", p1.getId());
    Assertions.assertEquals("pressure", p1.getType());
    Assertions.assertEquals("Pa", p1.getUnits());
    Assertions.assertEquals(0, new BigDecimal("0.01").compareTo(p1.getSigma()));
    Assertions.assertEquals("Containment absolute pressure sensor A", p1.getDescription());

    Assertions.assertInstanceOf(TemperatureSensorConfig.class, sensors.get(1));
    TemperatureSensorConfig t24 = (TemperatureSensorConfig) sensors.get(1);
    Assertions.assertEquals("T24", t24.getId());
    Assertions.assertEquals("temperature", t24.getType());
    Assertions.assertEquals(0, new BigDecimal("0.018993").compareTo(t24.getVolumeWeight()));
    Assertions.assertNotNull(t24.getValidRange());
    Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(t24.getValidRange().getMin()));
    Assertions.assertEquals(0, new BigDecimal("80").compareTo(t24.getValidRange().getMax()));

    Assertions.assertInstanceOf(HumiditySensorConfig.class, sensors.get(2));
    HumiditySensorConfig rh4 = (HumiditySensorConfig) sensors.get(2);
    Assertions.assertEquals("RH4", rh4.getId());
    Assertions.assertEquals("humidity", rh4.getType());
    Assertions.assertEquals("T24", rh4.getPairedTemperatureSensor());
    Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(rh4.getValidRange().getMin()));
    Assertions.assertEquals(0, new BigDecimal("100").compareTo(rh4.getValidRange().getMax()));
  }

  @Test
  public void read_missingFile_throwsFailedToReadFileException() {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(SiteConfigReaderTest.class);
    SiteConfigReader reader = new SiteConfigReader(fileReader);

    FailedToReadFileException exception =
        Assertions.assertThrows(
            FailedToReadFileException.class, () -> reader.read("/non-existent-site-config.json"));
    Assertions.assertTrue(exception.getMessage().contains("Resource not found"));
  }

  @Test
  public void read_invalidJson_throwsSiteConfigParseException() {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(SiteConfigReaderTest.class);
    SiteConfigReader reader = new SiteConfigReader(fileReader);

    SiteConfigParseException exception =
        Assertions.assertThrows(
            SiteConfigParseException.class, () -> reader.read("/site-config-invalid.json"));
    Assertions.assertTrue(
        exception.getMessage().contains("Invalid JSON")
            || exception.getMessage().contains("config"));
    Assertions.assertNotNull(exception.getCause());
  }

  @Test
  public void read_unknownSensorType_throwsSiteConfigParseException() {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(SiteConfigReaderTest.class);
    SiteConfigReader reader = new SiteConfigReader(fileReader);

    SiteConfigParseException exception =
        Assertions.assertThrows(
            SiteConfigParseException.class,
            () -> reader.read("/site-config-unknown-sensor-type.json"));
    String message = exception.getMessage();
    String causeMessage = exception.getCause() != null ? exception.getCause().getMessage() : "";
    Assertions.assertTrue(
        message.contains("unknown") || causeMessage.contains("unknown"),
        "Expected exception message to mention unknown type, got: " + message);
  }

  @Test
  public void read_emptySensors_returnsEmptyList() throws Exception {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(SiteConfigReaderTest.class);
    SiteConfigReader reader = new SiteConfigReader(fileReader);

    SiteConfig config = reader.read("/site-config-empty-sensors.json");

    Assertions.assertNotNull(config.getSensors());
    Assertions.assertTrue(config.getSensors().isEmpty());
  }
}
