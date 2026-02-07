package ca.lajthabalazs.pressure_integity_test.measurement;

import ca.lajthabalazs.pressure_integrity_test.measurement.Humidity;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HumidityTest {
  @Test
  public void getTimeUtc() {
    var Humidity = new Humidity(123L, "H1", new BigDecimal("56"));
    Assertions.assertEquals(123, Humidity.getTimeUtc());
  }

  @Test
  public void getSourceId() {
    var Humidity = new Humidity(123L, "H1", new BigDecimal("56"));
    Assertions.assertEquals("H1", Humidity.getSourceId());
  }

  @Test
  public void getDefaultUnit() {
    var Humidity = new Humidity(123L, "H1", new BigDecimal("56"));
    Assertions.assertEquals("%", Humidity.getDefaultUnit());
  }

  @Test
  public void getValueInDefaultUnit() {
    var Humidity = new Humidity(123L, "H1", new BigDecimal("56"));
    Assertions.assertEquals(0, new BigDecimal("56").compareTo(Humidity.getValueInDefaultUnit()));
  }

  @Test
  public void toStringTest() {
    var Humidity = new Humidity(123L, "H1", new BigDecimal("47"));
    Assertions.assertEquals("47%", Humidity.toString());
  }

  @Test
  public void toStringTest_withDecimalPlaces() {
    var Humidity = new Humidity(123L, "H1", new BigDecimal("47.33434"));
    Assertions.assertEquals("47.33434%", Humidity.toString());
  }

  @Test
  public void withNewTimestamp_returnsHumidityWithNewTimestamp_preservesOtherFields() {
    var original = new Humidity(1000L, "H1", new BigDecimal("50"));
    long newTimestamp = 2000L;

    Measurement copy = original.withNewTimestamp(newTimestamp);

    Assertions.assertInstanceOf(Humidity.class, copy);
    Assertions.assertEquals(newTimestamp, copy.getTimeUtc());
    Assertions.assertEquals(original.getSourceId(), copy.getSourceId());
    Assertions.assertEquals(
        0, original.getValueInDefaultUnit().compareTo(copy.getValueInDefaultUnit()));
    Assertions.assertEquals(original.getDefaultUnit(), copy.getDefaultUnit());
  }

  @Test
  public void withNewTimestamp_doesNotModifyOriginal() {
    var original = new Humidity(1000L, "H1", new BigDecimal("50"));
    long originalTime = original.getTimeUtc();

    original.withNewTimestamp(9999L);

    Assertions.assertEquals(originalTime, original.getTimeUtc());
  }
}
