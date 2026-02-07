package ca.lajthabalazs.pressure_integity_test.measurement;

import ca.lajthabalazs.pressure_integrity_test.measurement.Humidity;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link MeasurementStream} subscription and delivery using a test subclass. */
public class MeasurementStreamTest {

  private TestMeasurementStream stream;

  @BeforeEach
  public void setUp() {
    stream = new TestMeasurementStream();
  }

  @Test
  public void subscribe_receivesPublishedMeasurement() {
    List<Measurement> received = new ArrayList<>();
    stream.subscribe(received::add);

    Measurement m = new Humidity(1000L, "H1", new BigDecimal("50"));
    stream.publishToSubscribers(m);

    Assertions.assertEquals(1, received.size());
    Assertions.assertSame(m, received.get(0));
  }

  @Test
  public void multipleSubscribers_allReceiveSameMeasurement() {
    List<Measurement> first = new ArrayList<>();
    List<Measurement> second = new ArrayList<>();
    stream.subscribe(first::add);
    stream.subscribe(second::add);

    Measurement m = new Humidity(2000L, "H2", new BigDecimal("60"));
    stream.publishToSubscribers(m);

    Assertions.assertEquals(1, first.size());
    Assertions.assertEquals(1, second.size());
    Assertions.assertSame(m, first.get(0));
    Assertions.assertSame(m, second.get(0));
  }

  @Test
  public void unsubscribe_stopsReceivingMeasurements() {
    List<Measurement> received = new ArrayList<>();
    MeasurementStream.Subscription sub = stream.subscribe(received::add);

    Measurement m1 = new Humidity(1000L, "H1", new BigDecimal("50"));
    stream.publishToSubscribers(m1);
    Assertions.assertEquals(1, received.size());

    sub.unsubscribe();

    Measurement m2 = new Humidity(2000L, "H1", new BigDecimal("51"));
    stream.publishToSubscribers(m2);

    Assertions.assertEquals(1, received.size(), "Unsubscribed handler should not receive more");
  }

  @Test
  public void getSubscribers_reflectsActiveSubscriptions() {
    Assertions.assertTrue(stream.getSubscribers().isEmpty());

    MeasurementStream.Subscription sub1 = stream.subscribe(m -> {});
    Assertions.assertEquals(1, stream.getSubscribers().size());

    MeasurementStream.Subscription sub2 = stream.subscribe(m -> {});
    Assertions.assertEquals(2, stream.getSubscribers().size());

    sub1.unsubscribe();
    Assertions.assertEquals(1, stream.getSubscribers().size());

    sub2.unsubscribe();
    Assertions.assertTrue(stream.getSubscribers().isEmpty());
  }

  @Test
  public void clearForTest_removesAllSubscribers() {
    List<Measurement> received = new ArrayList<>();
    stream.subscribe(received::add);
    stream.clearForTest();

    Measurement m = new Humidity(1000L, "H1", new BigDecimal("50"));
    stream.publishToSubscribers(m);

    Assertions.assertTrue(received.isEmpty());
    Assertions.assertTrue(stream.getSubscribers().isEmpty());
  }

  @Test
  public void handlerException_doesNotBlockOtherHandlers() {
    List<Measurement> received = new ArrayList<>();
    stream.subscribe(
        m -> {
          throw new RuntimeException("handler failed");
        });
    stream.subscribe(received::add);

    Measurement m = new Humidity(1000L, "H1", new BigDecimal("50"));
    stream.publishToSubscribers(m);

    Assertions.assertEquals(1, received.size());
    Assertions.assertSame(m, received.get(0));
  }
}
