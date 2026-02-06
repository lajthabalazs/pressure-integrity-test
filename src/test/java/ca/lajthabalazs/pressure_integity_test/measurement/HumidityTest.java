package ca.lajthabalazs.pressure_integity_test.measurement;

import ca.lajthabalazs.pressure_integrity_test.measurement.Humidity;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HumidityTest {
  @Test
  public void getTimeUtc() {
    var Humidity =
        new Humidity(
            123L,
            "H1",
            new BigDecimal("56"),
            new BigDecimal("0.1"),
            BigDecimal.ZERO,
            new BigDecimal("100"));
    Assertions.assertEquals(123, Humidity.getTimeUtc());
  }

  @Test
  public void getSourceId() {
    var Humidity =
        new Humidity(
            123L,
            "H1",
            new BigDecimal("56"),
            new BigDecimal("0.1"),
            BigDecimal.ZERO,
            new BigDecimal("100"));
    Assertions.assertEquals("H1", Humidity.getSourceId());
  }

  @Test
  public void getDefaultUnit() {
    var Humidity =
        new Humidity(
            123L,
            "H1",
            new BigDecimal("56"),
            new BigDecimal("0.1"),
            BigDecimal.ZERO,
            new BigDecimal("100"));
    Assertions.assertEquals("%", Humidity.getDefaultUnit());
  }

  @Test
  public void getValueInDefaultUnit() {
    var Humidity =
        new Humidity(
            123L,
            "H1",
            new BigDecimal("56"),
            new BigDecimal("0.1"),
            BigDecimal.ZERO,
            new BigDecimal("100"));
    Assertions.assertEquals(0, new BigDecimal("56").compareTo(Humidity.getValueInDefaultUnit()));
  }

  @Test
  public void getSourceSigma() {
    var Humidity =
        new Humidity(
            123L,
            "H1",
            new BigDecimal("56"),
            new BigDecimal("0.1"),
            BigDecimal.ZERO,
            new BigDecimal("100"));
    Assertions.assertEquals(0, new BigDecimal("0.1").compareTo(Humidity.getSourceSigma()));
  }

  @Test
  public void isBelievable() {
    var Humidity =
        new Humidity(
            123L,
            "H1",
            new BigDecimal("56"),
            new BigDecimal("0.1"),
            BigDecimal.ZERO,
            new BigDecimal("100"));
    Assertions.assertTrue(Humidity.isBelievable());
  }

  @Test
  public void isBelievable_onRangeLow_isBelievable() {
    var Humidity =
        new Humidity(
            123L,
            "H1",
            BigDecimal.ZERO,
            new BigDecimal("0.1"),
            BigDecimal.ZERO,
            new BigDecimal("100"));
    Assertions.assertTrue(Humidity.isBelievable());
  }

  @Test
  public void isBelievable_onRangeHigh_isBelievable() {
    var Humidity =
        new Humidity(
            123L,
            "H1",
            new BigDecimal("100"),
            new BigDecimal("0.1"),
            BigDecimal.ZERO,
            new BigDecimal("100"));
    Assertions.assertTrue(Humidity.isBelievable());
  }

  @Test
  public void isBelievable_outOfRangeLow_isNotBelievable() {
    var Humidity =
        new Humidity(
            123L,
            "H1",
            new BigDecimal("-3"),
            new BigDecimal("0.1"),
            BigDecimal.ZERO,
            new BigDecimal("100"));
    Assertions.assertFalse(Humidity.isBelievable());
  }

  @Test
  public void isBelievable_outOfRangeHigh_isNotBelievable() {
    var Humidity =
        new Humidity(
            123L,
            "H1",
            new BigDecimal("2500"),
            new BigDecimal("0.1"),
            BigDecimal.ZERO,
            new BigDecimal("100"));
    Assertions.assertFalse(Humidity.isBelievable());
  }

  @Test
  public void toStringTest() {
    var Humidity =
        new Humidity(
            123L,
            "H1",
            new BigDecimal("47"),
            new BigDecimal("0.1"),
            BigDecimal.ZERO,
            new BigDecimal("100"));
    Assertions.assertEquals("47%", Humidity.toString());
  }

  @Test
  public void toStringTest_withDecimalPlaces() {
    var Humidity =
        new Humidity(
            123L,
            "H1",
            new BigDecimal("47.33434"),
            new BigDecimal("0.1"),
            BigDecimal.ZERO,
            new BigDecimal("100"));
    Assertions.assertEquals("47.33434%", Humidity.toString());
  }

  @Test
  public void withNewTimestamp_returnsHumidityWithNewTimestamp_preservesOtherFields() {
    var original =
        new Humidity(
            1000L,
            "H1",
            new BigDecimal("50"),
            new BigDecimal("0.1"),
            new BigDecimal("0"),
            new BigDecimal("100"));
    long newTimestamp = 2000L;

    Measurement copy = original.withNewTimestamp(newTimestamp);

    Assertions.assertInstanceOf(Humidity.class, copy);
    Assertions.assertEquals(newTimestamp, copy.getTimeUtc());
    Assertions.assertEquals(original.getSourceId(), copy.getSourceId());
    Assertions.assertEquals(
        0, original.getValueInDefaultUnit().compareTo(copy.getValueInDefaultUnit()));
    Assertions.assertEquals(0, original.getSourceSigma().compareTo(copy.getSourceSigma()));
    Assertions.assertEquals(
        0, original.getLowerBelievableBound().compareTo(copy.getLowerBelievableBound()));
    Assertions.assertEquals(
        0, original.getUpperBelievableBound().compareTo(copy.getUpperBelievableBound()));
    Assertions.assertEquals(original.getDefaultUnit(), copy.getDefaultUnit());
  }

  @Test
  public void withNewTimestamp_doesNotModifyOriginal() {
    var original =
        new Humidity(
            1000L,
            "H1",
            new BigDecimal("50"),
            new BigDecimal("0.1"),
            BigDecimal.ZERO,
            new BigDecimal("100"));
    long originalTime = original.getTimeUtc();

    original.withNewTimestamp(9999L);

    Assertions.assertEquals(originalTime, original.getTimeUtc());
  }
}
