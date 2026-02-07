package ca.lajthabalazs.pressure_integity_test.measurement;

import ca.lajthabalazs.pressure_integrity_test.measurement.Humidity;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVectorStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link MeasurementVectorStream} subscription and delivery using a test subclass. */
public class MeasurementVectorStreamTest {

  private TestMeasurementVectorStream stream;

  @BeforeEach
  public void setUp() {
    stream = new TestMeasurementVectorStream();
  }

  @Test
  public void subscribe_receivesPublishedVector() {
    List<MeasurementVector> received = new ArrayList<>();
    stream.subscribe(received::add);

    List<Measurement> measurements =
        List.of(
            new Humidity(1000L, "H1", new BigDecimal("50")),
            new Humidity(1000L, "H2", new BigDecimal("60")));
    MeasurementVector vector = new MeasurementVector(1000L, measurements);
    stream.publishToSubscribers(vector);

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(1000L, received.get(0).getTimeUtc());
    Assertions.assertEquals(2, received.get(0).getMeasurements().size());
    Assertions.assertEquals(
        measurements.get(0).getSourceId(), received.get(0).getMeasurements().get(0).getSourceId());
    Assertions.assertEquals(
        measurements.get(1).getSourceId(), received.get(0).getMeasurements().get(1).getSourceId());
  }

  @Test
  public void multipleSubscribers_allReceiveSameVector() {
    List<MeasurementVector> first = new ArrayList<>();
    List<MeasurementVector> second = new ArrayList<>();
    stream.subscribe(first::add);
    stream.subscribe(second::add);

    MeasurementVector vector =
        new MeasurementVector(2000L, List.of(new Humidity(2000L, "H2", new BigDecimal("60"))));
    stream.publishToSubscribers(vector);

    Assertions.assertEquals(1, first.size());
    Assertions.assertEquals(1, second.size());
    Assertions.assertEquals(1, first.get(0).getMeasurements().size());
    Assertions.assertEquals(1, second.get(0).getMeasurements().size());
  }

  @Test
  public void unsubscribe_stopsReceivingVectors() {
    List<MeasurementVector> received = new ArrayList<>();
    MeasurementVectorStream.Subscription sub = stream.subscribe(received::add);

    MeasurementVector v1 =
        new MeasurementVector(1000L, List.of(new Humidity(1000L, "H1", new BigDecimal("50"))));
    stream.publishToSubscribers(v1);
    Assertions.assertEquals(1, received.size());

    sub.unsubscribe();

    MeasurementVector v2 =
        new MeasurementVector(2000L, List.of(new Humidity(2000L, "H1", new BigDecimal("51"))));
    stream.publishToSubscribers(v2);

    Assertions.assertEquals(1, received.size(), "Unsubscribed handler should not receive more");
  }

  @Test
  public void getSubscribers_reflectsActiveSubscriptions() {
    Assertions.assertTrue(stream.getSubscribers().isEmpty());

    MeasurementVectorStream.Subscription sub1 = stream.subscribe(v -> {});
    Assertions.assertEquals(1, stream.getSubscribers().size());

    MeasurementVectorStream.Subscription sub2 = stream.subscribe(v -> {});
    Assertions.assertEquals(2, stream.getSubscribers().size());

    sub1.unsubscribe();
    Assertions.assertEquals(1, stream.getSubscribers().size());

    sub2.unsubscribe();
    Assertions.assertTrue(stream.getSubscribers().isEmpty());
  }

  @Test
  public void publishedVectorMeasurementsAreImmutable() {
    List<MeasurementVector> received = new ArrayList<>();
    stream.subscribe(received::add);

    List<Measurement> mutable = new ArrayList<>();
    mutable.add(new Humidity(1000L, "H1", new BigDecimal("50")));
    MeasurementVector vector = new MeasurementVector(1000L, mutable);
    stream.publishToSubscribers(vector);

    List<Measurement> got = received.get(0).getMeasurements();
    Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> got.add(new Humidity(2000L, "H2", new BigDecimal("60"))));
  }

  @Test
  public void publish_emptyMeasurements_deliversVectorWithEmptyMeasurements() {
    List<MeasurementVector> received = new ArrayList<>();
    stream.subscribe(received::add);

    stream.publishToSubscribers(new MeasurementVector(1000L, List.of()));

    Assertions.assertEquals(1, received.size());
    Assertions.assertEquals(1000L, received.get(0).getTimeUtc());
    Assertions.assertTrue(received.get(0).getMeasurements().isEmpty());
  }

  @Test
  public void handlerException_doesNotBlockOtherHandlersOrStopEventHandling() {
    List<MeasurementVector> received = new ArrayList<>();
    stream.subscribe(
        vector -> {
          throw new RuntimeException("handler failed");
        });
    stream.subscribe(received::add);

    MeasurementVector vector =
        new MeasurementVector(1000L, List.of(new Humidity(1000L, "H1", new BigDecimal("50"))));
    stream.publishToSubscribers(vector);

    Assertions.assertEquals(1, received.size(), "Other handler should still receive the event");
    Assertions.assertEquals(
        vector.getMeasurements().get(0).getSourceId(),
        received.get(0).getMeasurements().get(0).getSourceId(),
        "Received vector should match published");

    stream.publishToSubscribers(vector);

    Assertions.assertEquals(
        2, received.size(), "Subsequent events should still be delivered after handler error");
  }
}
