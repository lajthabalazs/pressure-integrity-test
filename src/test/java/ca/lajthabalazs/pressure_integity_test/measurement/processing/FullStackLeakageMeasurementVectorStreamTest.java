package ca.lajthabalazs.pressure_integity_test.measurement.processing;

import ca.lajthabalazs.pressure_integity_test.measurement.streaming.TestMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.config.CalibrationConfig;
import ca.lajthabalazs.pressure_integrity_test.config.HumiditySensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.LocationConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfig;
import ca.lajthabalazs.pressure_integrity_test.config.TemperatureSensorConfig;
import ca.lajthabalazs.pressure_integrity_test.measurement.ErrorSeverity;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementError;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.Temperature;
import ca.lajthabalazs.pressure_integrity_test.measurement.processing.FullStackLeakageMeasurementVectorStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link FullStackLeakageMeasurementVectorStream}. */
public class FullStackLeakageMeasurementVectorStreamTest {

  private TestMeasurementVectorStream source;
  private SiteConfig siteConfig;

  @BeforeEach
  public void setUp() {
    source = new TestMeasurementVectorStream();

    // Simple site config: one location with one temperature sensor
    LocationConfig loc = new LocationConfig();
    loc.setId("L1");
    loc.setVolumeFactor(new BigDecimal("1.0"));

    TemperatureSensorConfig t = new TemperatureSensorConfig();
    t.setId("T1");

    List<SensorConfig> sensors = new ArrayList<>();
    sensors.add(t);
    loc.setSensors(sensors);

    siteConfig = new SiteConfig();
    siteConfig.setLocations(List.of(loc));
  }

  @Test
  public void listSensors_returnsSiteConfigSensors() {
    FullStackLeakageMeasurementVectorStream chain =
        new FullStackLeakageMeasurementVectorStream(siteConfig, null, source);

    List<SensorConfig> sensors = chain.listSensors();
    Assertions.assertEquals(1, sensors.size());
    Assertions.assertEquals("T1", sensors.getFirst().getId());

    chain.stop();
  }

  @Test
  public void constructorWithCalibrationConfig_initializesAndStops() {
    // Site config with no locations (null list) to exercise null/empty paths
    SiteConfig cfg = new SiteConfig();
    cfg.setLocations(null);

    FullStackLeakageMeasurementVectorStream chain =
        new FullStackLeakageMeasurementVectorStream(cfg, new CalibrationConfig(), source);

    // listSensors should not throw and should be empty
    Assertions.assertNotNull(chain.listSensors());

    // stop() should be callable without exceptions
    chain.stop();
  }

  @Test
  public void humidityMappingBranch_exercised() {
    // Site config with a humidity sensor that has a paired temperature sensor
    LocationConfig loc = new LocationConfig();
    loc.setId("L1");
    loc.setVolumeFactor(new BigDecimal("1.0"));

    HumiditySensorConfig h = new HumiditySensorConfig();
    h.setId("H1");

    List<SensorConfig> sensors = new ArrayList<>();
    sensors.add(h);
    loc.setSensors(sensors);

    SiteConfig cfg = new SiteConfig();
    cfg.setLocations(List.of(loc));

    FullStackLeakageMeasurementVectorStream chain =
        new FullStackLeakageMeasurementVectorStream(cfg, null, source);

    Assertions.assertNotNull(chain.listSensors());
    chain.stop();
  }

  @Test
  public void severeErrorOnInput_passesThroughEntireChain() {
    FullStackLeakageMeasurementVectorStream chain =
        new FullStackLeakageMeasurementVectorStream(siteConfig, null, source);

    List<MeasurementVector> received = new ArrayList<>();
    chain.subscribe(received::add);

    Temperature t = new Temperature(1000L, "T1", new BigDecimal("20"));
    MeasurementError severe = new MeasurementError("T1", ErrorSeverity.SEVERE, "Test severe");
    MeasurementVector input = new MeasurementVector(1000L, List.of(t), List.of(severe));

    source.publishToSubscribers(input);

    Assertions.assertEquals(1, received.size());
    MeasurementVector out = received.getFirst();
    Assertions.assertSame(input, out);
    Assertions.assertEquals(List.of(severe), out.getErrors());

    chain.stop();
  }
}
