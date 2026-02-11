package ca.lajthabalazs.pressure_integity_test.config;

import ca.lajthabalazs.pressure_integity_test.io.ResourceTextFileReader;
import ca.lajthabalazs.pressure_integrity_test.config.*;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfigReader.SiteConfigParseException;
import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader.FailedToReadFileException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SiteConfigReaderTest {

  @Test
  public void read_validConfig_returnsParsedSiteConfig() throws Exception {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(SiteConfigReaderTest.class);
    SiteConfigReader reader = new SiteConfigReader(fileReader);

    SiteConfig config = reader.read("/site-config/site-config-sample.json");

    Assertions.assertNotNull(config);
    Assertions.assertEquals("PAKS_BLOCK_2", config.getId());
    Assertions.assertEquals("Paks NPP – Block 2 containment", config.getDescription());

    Assertions.assertNotNull(config.getContainment());
    Assertions.assertEquals(
        0, new BigDecimal("51166").compareTo(config.getContainment().getNetVolume_m3()));

    Assertions.assertNotNull(config.getDesignPressure());
    Assertions.assertEquals(
        0, new BigDecimal("1.5").compareTo(config.getDesignPressure().getOverpressure_bar()));
    Assertions.assertEquals(
        0,
        new BigDecimal("14.7")
            .compareTo(config.getDesignPressure().getLeakLimit_percent_per_day()));

    Assertions.assertNotNull(config.getLocations());
    Assertions.assertEquals(1, config.getLocations().size());
    var location = config.getLocations().get(0);
    Assertions.assertEquals("A301", location.getId());
    Assertions.assertEquals(0, new BigDecimal("0.018993").compareTo(location.getVolumeFactor()));
    Assertions.assertNotNull(location.getSensors());
    Assertions.assertEquals(3, location.getSensors().size());

    List<SensorConfig> sensors = config.getSensors();
    Assertions.assertEquals(3, sensors.size());

    Assertions.assertInstanceOf(PressureSensorConfig.class, sensors.get(0));
    PressureSensorConfig p1 = (PressureSensorConfig) sensors.get(0);
    Assertions.assertEquals("p1", p1.getId());
    Assertions.assertEquals("A301", p1.getLocationId());
    Assertions.assertEquals("pressure", p1.getType());
    Assertions.assertEquals("Pa", p1.getUnits());
    Assertions.assertEquals(0, new BigDecimal("0.01").compareTo(p1.getSigma()));
    Assertions.assertEquals("Containment absolute pressure sensor A", p1.getDescription());

    Assertions.assertInstanceOf(TemperatureSensorConfig.class, sensors.get(1));
    TemperatureSensorConfig t24 = (TemperatureSensorConfig) sensors.get(1);
    Assertions.assertEquals("T24", t24.getId());
    Assertions.assertEquals("A301", t24.getLocationId());
    Assertions.assertEquals("temperature", t24.getType());
    Assertions.assertNotNull(t24.getValidRange());
    Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(t24.getValidRange().getMin()));
    Assertions.assertEquals(0, new BigDecimal("80").compareTo(t24.getValidRange().getMax()));

    Assertions.assertInstanceOf(HumiditySensorConfig.class, sensors.get(2));
    HumiditySensorConfig rh4 = (HumiditySensorConfig) sensors.get(2);
    Assertions.assertEquals("fi4", rh4.getId());
    Assertions.assertEquals("A301", rh4.getLocationId());
    Assertions.assertEquals("humidity", rh4.getType());
    Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(rh4.getValidRange().getMin()));
    Assertions.assertEquals(0, new BigDecimal("100").compareTo(rh4.getValidRange().getMax()));
  }

  @Test
  public void read_missingFile_throwsFailedToReadFileException() {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(SiteConfigReaderTest.class);
    SiteConfigReader reader = new SiteConfigReader(fileReader);

    FailedToReadFileException exception =
        Assertions.assertThrows(
            FailedToReadFileException.class,
            () -> reader.read("/site-config/non-existent-site-config.json"));
    Assertions.assertTrue(exception.getMessage().contains("Resource not found"));
  }

  @Test
  public void read_invalidJson_throwsSiteConfigParseException() {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(SiteConfigReaderTest.class);
    SiteConfigReader reader = new SiteConfigReader(fileReader);

    SiteConfigParseException exception =
        Assertions.assertThrows(
            SiteConfigParseException.class,
            () -> reader.read("/site-config/site-config-invalid.json"));
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
            () -> reader.read("/site-config/site-config-unknown-sensor-type.json"));
    String message = exception.getMessage();
    String causeMessage = exception.getCause() != null ? exception.getCause().getMessage() : "";
    Assertions.assertTrue(
        message.contains("unknown") || causeMessage.contains("unknown"),
        "Expected exception message to mention unknown type, got: " + message);
  }

  @Test
  public void read_emptyLocations_returnsEmptyLists() throws Exception {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(SiteConfigReaderTest.class);
    SiteConfigReader reader = new SiteConfigReader(fileReader);

    SiteConfig config = reader.read("/site-config/site-config-empty-sensors.json");

    Assertions.assertNotNull(config.getLocations());
    Assertions.assertTrue(config.getLocations().isEmpty());
    Assertions.assertTrue(config.getSensors().isEmpty());
  }

  @Test
  public void read_siteConfigWithoutLocations_parsesWithEmptyLocations() throws Exception {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(SiteConfigReaderTest.class);
    SiteConfigReader reader = new SiteConfigReader(fileReader);

    SiteConfig config = reader.read("/site-config/site-config-no-locations.json");

    Assertions.assertNotNull(config);
    Assertions.assertEquals("NO_LOC", config.getId());
    Assertions.assertNotNull(config.getLocations());
    Assertions.assertTrue(config.getLocations().isEmpty());
    Assertions.assertTrue(config.getSensors().isEmpty());
  }

  @Test
  public void read_siteConfigWithNullLocations_parsesWithEmptyLocations() throws Exception {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(SiteConfigReaderTest.class);
    SiteConfigReader reader = new SiteConfigReader(fileReader);

    SiteConfig config = reader.read("/site-config/site-config-null-locations.json");

    Assertions.assertNotNull(config);
    Assertions.assertEquals("NULL_LOC", config.getId());
    Assertions.assertNotNull(config.getLocations());
    Assertions.assertTrue(config.getLocations().isEmpty());
    Assertions.assertTrue(config.getSensors().isEmpty());
  }

  @Test
  public void read_locationWithoutSensors_parsesWithEmptySensorList() throws Exception {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(SiteConfigReaderTest.class);
    SiteConfigReader reader = new SiteConfigReader(fileReader);

    SiteConfig config = reader.read("/site-config/site-config-location-no-sensors.json");

    Assertions.assertNotNull(config);
    Assertions.assertNotNull(config.getLocations());
    Assertions.assertEquals(1, config.getLocations().size());
    LocationConfig loc = config.getLocations().get(0);
    Assertions.assertEquals("L1", loc.getId());
    Assertions.assertNotNull(loc.getSensors());
    Assertions.assertTrue(loc.getSensors().isEmpty());
    Assertions.assertTrue(config.getSensors().isEmpty());
  }

  @Test
  public void read_locationWithNullSensors_parsesWithEmptySensorList() throws Exception {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(SiteConfigReaderTest.class);
    SiteConfigReader reader = new SiteConfigReader(fileReader);

    SiteConfig config = reader.read("/site-config/site-config-location-null-sensors.json");

    Assertions.assertNotNull(config);
    Assertions.assertNotNull(config.getLocations());
    Assertions.assertEquals(1, config.getLocations().size());
    LocationConfig loc = config.getLocations().get(0);
    Assertions.assertEquals("L1", loc.getId());
    Assertions.assertNotNull(loc.getSensors());
    Assertions.assertTrue(loc.getSensors().isEmpty());
    Assertions.assertTrue(config.getSensors().isEmpty());
  }

  @Test
  public void read_parseReturnsNull_throwsSiteConfigParseException() {
    ObjectMapper nullReturningMapper =
        new ObjectMapper() {
          @Override
          public <T> T readValue(String content, Class<T> valueType)
              throws JsonProcessingException {
            if (valueType == SiteConfig.class) {
              return null;
            }
            return super.readValue(content, valueType);
          }
        };
    ResourceTextFileReader fileReader = new ResourceTextFileReader(SiteConfigReaderTest.class);
    SiteConfigReader reader = new SiteConfigReader(fileReader, nullReturningMapper);

    SiteConfigParseException exception =
        Assertions.assertThrows(
            SiteConfigParseException.class,
            () -> reader.read("/site-config/site-config-sample.json"));
    Assertions.assertTrue(
        exception.getMessage().contains("did not produce a site config object"),
        "Expected message about null parse result, got: " + exception.getMessage());
    Assertions.assertNull(exception.getCause());
  }

  @Test
  public void locationConfig_setSensorsNull_resultsInEmptySensorList() {
    LocationConfig loc = new LocationConfig();
    loc.setSensors(null);

    Assertions.assertNotNull(loc.getSensors());
    Assertions.assertTrue(loc.getSensors().isEmpty());
  }

  @Test
  public void siteConfig_setLocationsNull_resultsInEmptyLocationsList() {
    SiteConfig config = new SiteConfig();
    config.setLocations(null);

    Assertions.assertNotNull(config.getLocations());
    Assertions.assertTrue(config.getLocations().isEmpty());
    Assertions.assertTrue(config.getSensors().isEmpty());
  }

  /** Covers setLocationIdsOnSensors when config.getLocations() is null (early return). */
  @Test
  public void setLocationIdsOnSensors_configWithNullLocations_doesNotThrow() throws Exception {
    SiteConfig config = new SiteConfig();
    setField(config, "locations", null);

    invokeSetLocationIdsOnSensors(config);

    Assertions.assertNull(config.getLocations());
  }

  /** Covers setLocationIdsOnSensors when locations list contains null (skip null loc). */
  @Test
  public void setLocationIdsOnSensors_locationsListContainsNull_skipsNullAndProcessesRest()
      throws Exception {
    LocationConfig loc = new LocationConfig();
    loc.setId("L1");
    loc.setSensors(Collections.emptyList());

    List<LocationConfig> locations = new ArrayList<>();
    locations.add(null);
    locations.add(loc);

    SiteConfig config = new SiteConfig();
    setField(config, "locations", locations);

    invokeSetLocationIdsOnSensors(config);

    Assertions.assertEquals(2, config.getLocations().size());
    Assertions.assertNull(config.getLocations().get(0));
    Assertions.assertEquals("L1", config.getLocations().get(1).getId());
  }

  /** Covers setLocationIdsOnSensors when a location has null sensors (skip that location). */
  @Test
  public void setLocationIdsOnSensors_locationWithNullSensors_doesNotThrow() throws Exception {
    LocationConfig loc = new LocationConfig();
    loc.setId("L1");
    setField(loc, "sensors", null);

    SiteConfig config = new SiteConfig();
    config.setLocations(Collections.singletonList(loc));

    invokeSetLocationIdsOnSensors(config);

    Assertions.assertNull(loc.getSensors());
  }

  /** Covers setLocationIdsOnSensors when a sensor in the list is null (skip null sensor). */
  @Test
  public void setLocationIdsOnSensors_sensorsListContainsNull_skipsNullAndSetsIdOnRest()
      throws Exception {
    PressureSensorConfig p1 = new PressureSensorConfig();
    p1.setId("P1");

    List<SensorConfig> sensors = new ArrayList<>();
    sensors.add(null);
    sensors.add(p1);

    LocationConfig loc = new LocationConfig();
    loc.setId("L1");
    setField(loc, "sensors", sensors);

    SiteConfig config = new SiteConfig();
    config.setLocations(Collections.singletonList(loc));

    invokeSetLocationIdsOnSensors(config);

    Assertions.assertNull(loc.getSensors().get(0));
    Assertions.assertEquals("L1", p1.getLocationId());
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static void invokeSetLocationIdsOnSensors(SiteConfig config) throws Exception {
    Method method =
        SiteConfigReader.class.getDeclaredMethod("setLocationIdsOnSensors", SiteConfig.class);
    method.setAccessible(true);
    method.invoke(null, config);
  }
}
