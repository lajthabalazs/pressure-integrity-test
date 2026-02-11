package ca.lajthabalazs.pressure_integity_test.measurement;

import ca.lajthabalazs.pressure_integrity_test.measurement.Leakage;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Tests for {@link Leakage}. */
public class LeakageTest {

  @Test
  public void getTimeUtc() {
    var l = new Leakage(1000L, "LEAKAGE", new BigDecimal("0.5"));
    Assertions.assertEquals(1000L, l.getTimeUtc());
  }

  @Test
  public void getSourceId() {
    var l = new Leakage(1000L, "LEAKAGE", new BigDecimal("0.5"));
    Assertions.assertEquals("LEAKAGE", l.getSourceId());
  }

  @Test
  public void getDefaultUnit() {
    var l = new Leakage(1000L, "LEAKAGE", new BigDecimal("0.5"));
    Assertions.assertEquals(Leakage.V_V_PERCENT_PER_DAY, l.getDefaultUnit());
  }

  @Test
  public void getValueInDefaultUnit() {
    var l = new Leakage(1000L, "LEAKAGE", new BigDecimal("0.5"));
    Assertions.assertEquals(0, new BigDecimal("0.5").compareTo(l.getValueInDefaultUnit()));
  }

  @Test
  public void sentinelMinusOne() {
    var l = new Leakage(1000L, "LEAKAGE", new BigDecimal("-1"));
    Assertions.assertEquals(0, new BigDecimal("-1").compareTo(l.getValueInDefaultUnit()));
  }

  @Test
  public void toStringTest() {
    var l = new Leakage(1000L, "LEAKAGE", new BigDecimal("0.5"));
    Assertions.assertEquals("0.5" + Leakage.V_V_PERCENT_PER_DAY, l.toString());
  }

  @Test
  public void withNewTimestamp_returnsLeakageWithNewTimestamp_preservesOtherFields() {
    var original = new Leakage(1000L, "LEAKAGE", new BigDecimal("0.5"));
    long newTimestamp = 2000L;

    Measurement copy = original.withNewTimestamp(newTimestamp);

    Assertions.assertInstanceOf(Leakage.class, copy);
    Assertions.assertEquals(newTimestamp, copy.getTimeUtc());
    Assertions.assertEquals(original.getSourceId(), copy.getSourceId());
    Assertions.assertEquals(
        0, original.getValueInDefaultUnit().compareTo(copy.getValueInDefaultUnit()));
    Assertions.assertEquals(original.getDefaultUnit(), copy.getDefaultUnit());
  }

  @Test
  public void withNewTimestamp_doesNotModifyOriginal() {
    var original = new Leakage(1000L, "LEAKAGE", new BigDecimal("0.5"));
    long originalTime = original.getTimeUtc();

    original.withNewTimestamp(9999L);

    Assertions.assertEquals(originalTime, original.getTimeUtc());
  }

  @Test
  public void withNewValueInDefaultUnit_returnsLeakageWithNewValue_preservesOtherFields() {
    var original = new Leakage(1000L, "LEAKAGE", new BigDecimal("0.5"));
    BigDecimal newValue = new BigDecimal("-1");

    Measurement copy = original.withNewValueInDefaultUnit(newValue);

    Assertions.assertInstanceOf(Leakage.class, copy);
    Assertions.assertEquals(original.getTimeUtc(), copy.getTimeUtc());
    Assertions.assertEquals(original.getSourceId(), copy.getSourceId());
    Assertions.assertEquals(0, newValue.compareTo(copy.getValueInDefaultUnit()));
    Assertions.assertEquals(original.getDefaultUnit(), copy.getDefaultUnit());
  }
}
