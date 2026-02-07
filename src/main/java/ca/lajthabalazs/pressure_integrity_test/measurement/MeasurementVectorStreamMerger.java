package ca.lajthabalazs.pressure_integrity_test.measurement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Merges a data measurement stream into a timing stream.
 *
 * <p>Subscribes to two streams: a <em>timing</em> stream and a <em>data</em> stream. Whenever the
 * timing stream emits a {@link MeasurementVector}, this merger publishes a new vector containing
 * the latest available value from the data stream for every sensor (sourceId) that has received at
 * least one measurement so far. The merged vector uses the timing vector's timestamp. Measurements
 * in the merged vector have their timestamps set to the timing vector's timestamp.
 *
 * <p>Latest is defined as the most recently received measurement per sensor from the data stream.
 * If the data stream has not yet sent any measurement for a sensor, that sensor is omitted. If no
 * sensor has any data yet, a vector with an empty measurement list is published.
 */
public class MeasurementVectorStreamMerger extends MeasurementVectorStream {

  private final Map<String, Measurement> latestBySourceId = new ConcurrentHashMap<>();

  private MeasurementVectorStream.Subscription timingSubscription;
  private MeasurementVectorStream.Subscription dataSubscription;

  /**
   * Creates a merger that subscribes to the given timing and data streams. Merged vectors are
   * published whenever the timing stream emits. The merger subscribes to both streams immediately;
   * use {@link #stop()} to unsubscribe.
   *
   * @param timingStream stream that drives when merged vectors are emitted
   * @param dataStream stream from which latest per-sensor values are taken
   */
  public MeasurementVectorStreamMerger(
      MeasurementVectorStream timingStream, MeasurementVectorStream dataStream) {
    this.dataSubscription =
        dataStream.subscribe(
            vector -> {
              for (Measurement m : vector.getMeasurements()) {
                latestBySourceId.put(m.getSourceId(), m);
              }
            });

    this.timingSubscription =
        timingStream.subscribe(
            timingVector -> {
              long timingTimestamp = timingVector.getTimeUtc();

              List<Measurement> merged = new ArrayList<>();
              for (Measurement m : latestBySourceId.values()) {
                merged.add(m.withNewTimestamp(timingTimestamp));
              }
              merged.sort((a, b) -> a.getSourceId().compareTo(b.getSourceId()));

              publish(new MeasurementVector(timingTimestamp, merged));
            });
  }

  /**
   * Stops merging: unsubscribes from both the timing and data streams. Merged output will no longer
   * be published.
   */
  public void stop() {
    if (timingSubscription != null) {
      timingSubscription.unsubscribe();
      timingSubscription = null;
    }
    if (dataSubscription != null) {
      dataSubscription.unsubscribe();
      dataSubscription = null;
    }
    latestBySourceId.clear();
    clearSubscribers();
  }
}
