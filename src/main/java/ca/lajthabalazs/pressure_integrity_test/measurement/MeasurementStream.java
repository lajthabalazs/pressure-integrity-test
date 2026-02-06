package ca.lajthabalazs.pressure_integrity_test.measurement;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract base for event-driven measurement streams.
 *
 * <p>Supports subscribing to measurement events and receiving them asynchronously. Subclasses
 * implement the source of measurements and call {@link #publish(Measurement)} to deliver to all
 * subscribers.
 */
public abstract class MeasurementStream {

  private final List<MeasurementHandler> subscribers = new CopyOnWriteArrayList<>();

  /** Returns the list of subscribed handlers. */
  public final List<MeasurementHandler> getSubscribers() {
    return subscribers;
  }

  /**
   * Subscribes to measurement events.
   *
   * @param handler the callback to handle incoming measurements
   * @return a subscription that can be used to unsubscribe
   */
  public final Subscription subscribe(MeasurementHandler handler) {
    subscribers.add(handler);
    return () -> subscribers.remove(handler);
  }

  /** Clears all subscribers. For use by subclasses during shutdown. */
  protected final void clearSubscribers() {
    subscribers.clear();
  }

  /**
   * Publishes a measurement to all subscribers. For use by subclasses only (e.g. from a scheduler).
   *
   * @param measurement the measurement to publish
   */
  protected final void publish(Measurement measurement) {
    for (MeasurementHandler handler : subscribers) {
      try {
        handler.handle(measurement);
      } catch (Exception e) {
        System.err.println("Error in measurement handler: " + e.getMessage());
      }
    }
  }

  /** Subscription to measurement events; use to unsubscribe. */
  @FunctionalInterface
  public interface Subscription {
    /** Stops delivery of further events. */
    void unsubscribe();
  }

  /** Handler for measurement events. */
  @FunctionalInterface
  public interface MeasurementHandler {
    /** Handles a measurement event. */
    void handle(Measurement measurement);
  }
}
