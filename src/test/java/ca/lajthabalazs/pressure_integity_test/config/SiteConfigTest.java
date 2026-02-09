package ca.lajthabalazs.pressure_integity_test.config;

import ca.lajthabalazs.pressure_integrity_test.config.LocationConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfig;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SiteConfigTest {

  @Test
  public void getSensors_locationWithNullSensors_skipsAndReturnsEmpty() throws Exception {
    SiteConfig config = new SiteConfig();
    LocationConfig loc = new LocationConfig();
    loc.setId("L1");
    loc.setVolumeFactor(new BigDecimal("0.019"));
    setSensorsViaReflection(loc, null);
    config.setLocations(List.of(loc));

    List<?> sensors = config.getSensors();

    Assertions.assertNotNull(sensors);
    Assertions.assertTrue(sensors.isEmpty());
  }

  @Test
  public void getSensors_locationsContainsNull_skipsNullAndReturnsEmpty() throws Exception {
    SiteConfig config = new SiteConfig();
    setLocationsViaReflection(config, Collections.singletonList(null));

    List<?> sensors = config.getSensors();

    Assertions.assertNotNull(sensors);
    Assertions.assertTrue(sensors.isEmpty());
  }

  private static void setSensorsViaReflection(LocationConfig loc, List<?> value) throws Exception {
    Field f = LocationConfig.class.getDeclaredField("sensors");
    f.setAccessible(true);
    f.set(loc, value);
  }

  private static void setLocationsViaReflection(SiteConfig config, List<LocationConfig> value)
      throws Exception {
    Field f = SiteConfig.class.getDeclaredField("locations");
    f.setAccessible(true);
    f.set(config, value);
  }
}
