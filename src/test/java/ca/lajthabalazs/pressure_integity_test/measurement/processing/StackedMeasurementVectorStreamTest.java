package ca.lajthabalazs.pressure_integity_test.measurement.processing;

import ca.lajthabalazs.pressure_integity_test.measurement.streaming.TestMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.config.LocationConfig;
import ca.lajthabalazs.pressure_integrity_test.config.PressureSensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfig;
import ca.lajthabalazs.pressure_integrity_test.config.TemperatureSensorConfig;
import ca.lajthabalazs.pressure_integrity_test.measurement.ErrorSeverity;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementError;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.Pressure;
import ca.lajthabalazs.pressure_integrity_test.measurement.Temperature;
import ca.lajthabalazs.pressure_integrity_test.measurement.processing.AveragePressureMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.measurement.processing.StackedMeasurementVectorStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link StackedMeasurementVectorStream}. */
public class StackedMeasurementVectorStreamTest {

  private TestMeasurementVectorStream source;
  private List<MeasurementVector> received;
  private SiteConfig siteConfig;

  @BeforeEach
  public void setUp() {
    source = new TestMeasurementVectorStream();
    received = new ArrayList<>();
    siteConfig = siteConfigWithPressureSensorIds("P1", "P2");
  }

  private static SiteConfig siteConfigWithPressureSensorIds(String... ids) {
    LocationConfig loc = new LocationConfig();
    loc.setId("L1");
    List<SensorConfig> sensors = new ArrayList<>();
    for (String id : ids) {
      PressureSensorConfig p = new PressureSensorConfig();
      p.setId(id);
      sensors.add(p);
    }
    loc.setSensors(sensors);
    SiteConfig cfg = new SiteConfig();
    cfg.setLocations(List.of(loc));
    return cfg;
  }

  /** listSensors() returns the site config sensors in order. */
  @Test
  public void listSensors_returnsSiteConfigSensors() {
    StackedMeasurementVectorStream stack = new StackedMeasurementVectorStream(siteConfig, source);

    List<SensorConfig> sensors = stack.listSensors();
    Assertions.assertEquals(2, sensors.size());
    Assertions.assertEquals("P1", sensors.get(0).getId());
    Assertions.assertEquals("P2", sensors.get(1).getId());

    stack.stop();
  }

  /** Subscriber receives vectors with believability applied and average pressure added. */
  @Test
  public void subscribe_receivesVectorsWithBelievabilityAndAveragePressure() {
    StackedMeasurementVectorStream stack = new StackedMeasurementVectorStream(siteConfig, source);
    stack.subscribe(received::add);

    Pressure p1 = new Pressure(1000L, "P1", new BigDecimal("101325"));
    Pressure p2 = new Pressure(1000L, "P2", new BigDecimal("101500"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(p1, p2)));

    Assertions.assertEquals(1, received.size());
    MeasurementVector out = received.getFirst();
    Assertions.assertEquals(1000L, out.getTimeUtc());
    Assertions.assertEquals(3, out.getMeasurements().size()); // P1, P2, AVG_PRESSURE
    Measurement avg =
        out.getMeasurements().stream()
            .filter(
                m ->
                    AveragePressureMeasurementVectorStream.AVG_PRESSURE_SOURCE_ID.equals(
                        m.getSourceId()))
            .findFirst()
            .orElseThrow();
    // (101325 + 101500) / 2 = 101412.5
    Assertions.assertEquals(0, new BigDecimal("101412.5").compareTo(avg.getValueInDefaultUnit()));

    stack.stop();
  }

  /** Vector with severe error is passed through unchanged (no believability or average). */
  @Test
  public void severeErrorOnInput_passesThroughUnchanged() {
    StackedMeasurementVectorStream stack = new StackedMeasurementVectorStream(siteConfig, source);
    stack.subscribe(received::add);

    Temperature t = new Temperature(1000L, "T1", new BigDecimal("20"));
    MeasurementError severe = new MeasurementError("T1", ErrorSeverity.SEVERE, "Test severe");
    MeasurementVector input = new MeasurementVector(1000L, List.of(t), List.of(severe));

    source.publishToSubscribers(input);

    Assertions.assertEquals(1, received.size());
    MeasurementVector out = received.getFirst();
    Assertions.assertSame(input, out);
    Assertions.assertEquals(List.of(severe), out.getErrors());

    stack.stop();
  }

  /** After stop(), no further vectors are delivered. */
  @Test
  public void stop_stopsDelivery() {
    StackedMeasurementVectorStream stack = new StackedMeasurementVectorStream(siteConfig, source);
    stack.subscribe(received::add);

    source.publishToSubscribers(
        new MeasurementVector(1000L, List.of(new Pressure(1000L, "P1", new BigDecimal("101325")))));
    Assertions.assertEquals(1, received.size());

    stack.stop();
    received.clear();
    source.publishToSubscribers(
        new MeasurementVector(2000L, List.of(new Pressure(2000L, "P1", new BigDecimal("101400")))));
    Assertions.assertEquals(0, received.size());
  }

  /** stop() is idempotent: second call does not throw. */
  @Test
  public void stop_idempotent_secondCallDoesNotThrow() {
    StackedMeasurementVectorStream stack = new StackedMeasurementVectorStream(siteConfig, source);
    stack.stop();
    Assertions.assertDoesNotThrow(stack::stop);
  }

  /** Empty site config: listSensors returns empty list. */
  @Test
  public void emptySiteConfig_listSensorsReturnsEmpty() {
    SiteConfig empty = new SiteConfig();
    empty.setLocations(List.of());

    StackedMeasurementVectorStream stack = new StackedMeasurementVectorStream(empty, source);

    List<SensorConfig> sensors = stack.listSensors();
    Assertions.assertNotNull(sensors);
    Assertions.assertTrue(sensors.isEmpty());

    stack.stop();
  }

  /** Vector with no pressure in site config is not published (average stage drops it). */
  @Test
  public void noPressureInSiteConfig_nothingPublished() {
    // Site config with only a temperature sensor, no pressure
    LocationConfig loc = new LocationConfig();
    loc.setId("L1");
    TemperatureSensorConfig t = new TemperatureSensorConfig();
    t.setId("T1");
    loc.setSensors(List.of(t));
    SiteConfig cfg = new SiteConfig();
    cfg.setLocations(List.of(loc));

    StackedMeasurementVectorStream stack = new StackedMeasurementVectorStream(cfg, source);
    stack.subscribe(received::add);

    Pressure p = new Pressure(1000L, "P1", new BigDecimal("101325"));
    source.publishToSubscribers(new MeasurementVector(1000L, List.of(p)));

    // P1 is not in site config, so filtered view has no pressures -> average stage does not publish
    Assertions.assertEquals(0, received.size());

    stack.stop();
  }

  /** Timestamp is preserved through the stack. */
  @Test
  public void timestampPreserved() {
    StackedMeasurementVectorStream stack = new StackedMeasurementVectorStream(siteConfig, source);
    stack.subscribe(received::add);

    long ts = 12345L;
    source.publishToSubscribers(
        new MeasurementVector(ts, List.of(new Pressure(ts, "P1", new BigDecimal("101325")))));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(ts, received.getFirst().getTimeUtc());

    stack.stop();
  }
}
