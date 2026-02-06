package ca.lajthabalazs.pressure_integrity_test.measurement;

/**
 * Interface for event-driven streaming of measurement data.
 *
 * <p>This interface supports subscribing to measurement events and receiving them asynchronously as
 * they become available. Routing and filtering of measurements is handled at the streamer
 * implementation level.
 */
public interface MeasurementStream {

  /**
   * Subscribes to measurement events.
   *
   * @param handler the callback function to handle incoming measurements
   * @return a subscription that can be used to unsubscribe
   */
  Subscription subscribe(MeasurementHandler handler);

  /**
   * Publishes a measurement event to all relevant subscribers.
   *
   * @param measurement the measurement to publish
   */
  void publish(Measurement measurement);

  /** Represents a subscription to measurement events. Can be used to unsubscribe. */
  interface Subscription {
    /** Unsubscribes from the measurement stream, stopping further events from being delivered. */
    void unsubscribe();
  }

  /** Functional interface for handling measurement events. */
  @FunctionalInterface
  interface MeasurementHandler {
    /**
     * Handles a measurement event.
     *
     * @param measurement the measurement that was received
     */
    void handle(Measurement measurement);
  }
}
