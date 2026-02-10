package ca.lajthabalazs.pressure_integity_test.measurement;

import ca.lajthabalazs.pressure_integrity_test.measurement.GasConstant;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GasConstantTest {

  @Test
  public void getTimeUtc() {
    var r = new GasConstant(1000L, "R1", new BigDecimal("286.9"));
    Assertions.assertEquals(1000L, r.getTimeUtc());
  }

  @Test
  public void getSourceId() {
    var r = new GasConstant(1000L, "R1", new BigDecimal("286.9"));
    Assertions.assertEquals("R1", r.getSourceId());
  }

  @Test
  public void getDefaultUnit() {
    var r = new GasConstant(1000L, "R1", new BigDecimal("286.9"));
    Assertions.assertEquals(GasConstant.NEWTON_METER_PER_KG_KELVIN, r.getDefaultUnit());
  }

  @Test
  public void getValueInDefaultUnit() {
    var r = new GasConstant(1000L, "R1", new BigDecimal("286.9"));
    Assertions.assertEquals(0, new BigDecimal("286.9").compareTo(r.getValueInDefaultUnit()));
  }

  @Test
  public void toStringTest() {
    var r = new GasConstant(1000L, "R1", new BigDecimal("286.9"));
    Assertions.assertEquals("286.9" + GasConstant.NEWTON_METER_PER_KG_KELVIN, r.toString());
  }

  @Test
  public void toStringTest_withDecimalPlaces() {
    var r = new GasConstant(1000L, "R1", new BigDecimal("286.912345"));
    Assertions.assertEquals("286.912345" + GasConstant.NEWTON_METER_PER_KG_KELVIN, r.toString());
  }

  @Test
  public void withNewTimestamp_returnsGasConstantWithNewTimestamp_preservesOtherFields() {
    var original = new GasConstant(1000L, "R1", new BigDecimal("286.9"));
    long newTimestamp = 2000L;

    Measurement copy = original.withNewTimestamp(newTimestamp);

    Assertions.assertInstanceOf(GasConstant.class, copy);
    Assertions.assertEquals(newTimestamp, copy.getTimeUtc());
    Assertions.assertEquals(original.getSourceId(), copy.getSourceId());
    Assertions.assertEquals(
        0, original.getValueInDefaultUnit().compareTo(copy.getValueInDefaultUnit()));
    Assertions.assertEquals(original.getDefaultUnit(), copy.getDefaultUnit());
  }

  @Test
  public void withNewTimestamp_doesNotModifyOriginal() {
    var original = new GasConstant(1000L, "R1", new BigDecimal("286.9"));
    long originalTime = original.getTimeUtc();

    original.withNewTimestamp(9999L);

    Assertions.assertEquals(originalTime, original.getTimeUtc());
  }

  @Test
  public void withNewValueInDefaultUnit_returnsGasConstantWithNewValue_preservesOtherFields() {
    var original = new GasConstant(1000L, "R1", new BigDecimal("286.9"));
    BigDecimal newValue = new BigDecimal("300.0");

    Measurement copy = original.withNewValueInDefaultUnit(newValue);

    Assertions.assertInstanceOf(GasConstant.class, copy);
    Assertions.assertEquals(original.getTimeUtc(), copy.getTimeUtc());
    Assertions.assertEquals(original.getSourceId(), copy.getSourceId());
    Assertions.assertEquals(0, newValue.compareTo(copy.getValueInDefaultUnit()));
    Assertions.assertEquals(original.getDefaultUnit(), copy.getDefaultUnit());
  }
}
