package ca.lajthabalazs.pressure_integity_test.measurement;

import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementStream;

/**
 * Concrete MeasurementStream for testing base class behaviour. Exposes {@link
 * #publishToSubscribers} and {@link #clearForTest()} so tests can drive subscription and delivery
 * without a real stream implementation.
 */
public class TestMeasurementStream extends MeasurementStream {

  /** Publishes a measurement to all subscribers. For use by tests only. */
  public void publishToSubscribers(Measurement measurement) {
    publish(measurement);
  }

  /** Clears all subscribers. For use by tests only. */
  public void clearForTest() {
    clearSubscribers();
  }
}
