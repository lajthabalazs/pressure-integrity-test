package ca.lajthabalazs.pressure_integrity_test.measurement.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.lajthabalazs.pressure_integrity_test.config.HumiditySensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.LocationConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfig;
import ca.lajthabalazs.pressure_integrity_test.config.TemperatureSensorConfig;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Direct tests for {@link
 * FullStackLeakageMeasurementVectorStream#buildHumidityToTemperatureSensorId}.
 */
public class FullStackLeakageHumidityMappingTest {

  @Test
  public void noLocations_returnsEmptyMap() {
    SiteConfig siteConfig = new SiteConfig();
    // default locations list is empty

    Map<String, String> result =
        FullStackLeakageMeasurementVectorStream.buildHumidityToTemperatureSensorId(siteConfig);

    assertTrue(result.isEmpty());
  }

  @Test
  public void locationWithNoSensors_returnsEmptyMap() {
    LocationConfig location = new LocationConfig();
    location.setId("L1");
    location.setVolumeFactor(new BigDecimal("1.0"));
    location.setSensors(new ArrayList<>());

    SiteConfig siteConfig = new SiteConfig();
    siteConfig.setLocations(List.of(location));

    Map<String, String> result =
        FullStackLeakageMeasurementVectorStream.buildHumidityToTemperatureSensorId(siteConfig);

    assertTrue(result.isEmpty());
  }

  @Test
  public void locationWithHumidityButNoTemperature_notPaired() {
    LocationConfig location = new LocationConfig();
    location.setId("L1");
    location.setVolumeFactor(new BigDecimal("1.0"));

    HumiditySensorConfig h1 = new HumiditySensorConfig();
    h1.setId("H1");

    List<SensorConfig> sensors = new ArrayList<>();
    sensors.add(h1);
    location.setSensors(sensors);

    SiteConfig siteConfig = new SiteConfig();
    siteConfig.setLocations(List.of(location));

    Map<String, String> result =
        FullStackLeakageMeasurementVectorStream.buildHumidityToTemperatureSensorId(siteConfig);

    assertTrue(result.isEmpty());
  }

  @Test
  public void multipleHumiditySensors_shareFirstTemperatureInLocation() {
    LocationConfig location = new LocationConfig();
    location.setId("L1");
    location.setVolumeFactor(new BigDecimal("1.0"));

    TemperatureSensorConfig t1 = new TemperatureSensorConfig();
    t1.setId("T1");

    HumiditySensorConfig h1 = new HumiditySensorConfig();
    h1.setId("H1");

    HumiditySensorConfig h2 = new HumiditySensorConfig();
    h2.setId("H2");

    List<SensorConfig> sensors = new ArrayList<>();
    // mix order to ensure implementation is order-insensitive for humidity collection
    sensors.add(h1);
    sensors.add(t1);
    sensors.add(h2);
    location.setSensors(sensors);

    SiteConfig siteConfig = new SiteConfig();
    siteConfig.setLocations(List.of(location));

    Map<String, String> result =
        FullStackLeakageMeasurementVectorStream.buildHumidityToTemperatureSensorId(siteConfig);

    assertEquals(2, result.size());
    assertEquals("T1", result.get("H1"));
    assertEquals("T1", result.get("H2"));
  }

  @Test
  public void multipleTemperatures_usesFirstTemperaturePerLocation() {
    LocationConfig loc1 = new LocationConfig();
    loc1.setId("L1");
    loc1.setVolumeFactor(new BigDecimal("1.0"));

    TemperatureSensorConfig t1a = new TemperatureSensorConfig();
    t1a.setId("T1A");
    TemperatureSensorConfig t1b = new TemperatureSensorConfig();
    t1b.setId("T1B");
    HumiditySensorConfig h1 = new HumiditySensorConfig();
    h1.setId("H1");

    List<SensorConfig> sensors1 = new ArrayList<>();
    sensors1.add(t1a);
    sensors1.add(h1);
    sensors1.add(t1b);
    loc1.setSensors(sensors1);

    LocationConfig loc2 = new LocationConfig();
    loc2.setId("L2");
    loc2.setVolumeFactor(new BigDecimal("1.0"));

    TemperatureSensorConfig t2 = new TemperatureSensorConfig();
    t2.setId("T2");
    HumiditySensorConfig h2 = new HumiditySensorConfig();
    h2.setId("H2");

    List<SensorConfig> sensors2 = new ArrayList<>();
    sensors2.add(h2);
    sensors2.add(t2);
    loc2.setSensors(sensors2);

    SiteConfig siteConfig = new SiteConfig();
    siteConfig.setLocations(List.of(loc1, loc2));

    Map<String, String> result =
        FullStackLeakageMeasurementVectorStream.buildHumidityToTemperatureSensorId(siteConfig);

    assertEquals(2, result.size());
    assertEquals("T1A", result.get("H1"));
    assertEquals("T2", result.get("H2"));
  }
}
