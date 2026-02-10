package ca.lajthabalazs.pressure_integrity_test.measurement.streaming;

import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract base for event-driven measurement vector streams.
 *
 * <p>Supports subscribing to measurement vector events and receiving them asynchronously. Each
 * event is a {@link MeasurementVector} (timestamp plus map of measurements). Subclasses implement
 * the source and call {@link #publish(MeasurementVector)} to deliver each vector to all
 * subscribers.
 */
public abstract class MeasurementVectorStream {

  private final List<MeasurementVectorHandler> subscribers = new CopyOnWriteArrayList<>();

  /**
   * Returns the list of sensors as defined by the site config, in order. Subclasses that are
   * configured with a site config should override this.
   *
   * @return list of sensor configs (never null; may be empty)
   */
  public List<SensorConfig> listSensors() {
    return List.of();
  }

  /** Returns the list of subscribed handlers. */
  public final List<MeasurementVectorHandler> getSubscribers() {
    return subscribers;
  }

  /**
   * Subscribes to measurement vector events.
   *
   * @param handler the callback to handle incoming measurement vectors
   * @return a subscription that can be used to unsubscribe
   */
  public final Subscription subscribe(MeasurementVectorHandler handler) {
    subscribers.add(handler);
    return () -> subscribers.remove(handler);
  }

  /** Clears all subscribers. For use by subclasses during shutdown. */
  protected final void clearSubscribers() {
    subscribers.clear();
  }

  /**
   * Publishes a measurement vector to all subscribers. For use by subclasses only (e.g. from a
   * scheduler).
   *
   * @param vector the measurement vector to publish (not null)
   */
  protected final void publish(MeasurementVector vector) {
    for (MeasurementVectorHandler handler : subscribers) {
      try {
        handler.handle(vector);
      } catch (Exception e) {
        System.err.println("Error in measurement vector handler: " + e.getMessage());
      }
    }
  }

  /** Subscription to measurement vector events; use to unsubscribe. */
  @FunctionalInterface
  public interface Subscription {
    /** Stops delivery of further events. */
    void unsubscribe();
  }

  /** Handler for measurement vector events. */
  @FunctionalInterface
  public interface MeasurementVectorHandler {
    /** Handles a measurement vector event. */
    void handle(MeasurementVector vector);
  }
}
