package ca.lajthabalazs.pressure_integity_test.measurement.streaming;

import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.streaming.MeasurementVectorStream;

/**
 * Concrete MeasurementVectorStream for testing base class behaviour. Exposes {@link
 * #publishToSubscribers(MeasurementVector)} and {@link #clearForTest()} so tests can drive
 * subscription and delivery without a real stream implementation.
 */
public class TestMeasurementVectorStream extends MeasurementVectorStream {

  /** Publishes a measurement vector to all subscribers. For use by tests only. */
  public void publishToSubscribers(MeasurementVector vector) {
    publish(vector);
  }

  /** Clears all subscribers. For use by tests only. */
  public void clearForTest() {
    clearSubscribers();
  }
}
