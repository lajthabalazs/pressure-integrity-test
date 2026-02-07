package ca.lajthabalazs.pressure_integity_test.measurement;

import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.Pressure;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PressureTest {
  @Test
  public void getTimeUtc() {
    var pressure = new Pressure(789L, "P1", new BigDecimal("101325"));
    Assertions.assertEquals(789, pressure.getTimeUtc());
  }

  @Test
  public void getSourceId() {
    var pressure = new Pressure(789L, "P1", new BigDecimal("101325"));
    Assertions.assertEquals("P1", pressure.getSourceId());
  }

  @Test
  public void getDefaultUnit() {
    var pressure = new Pressure(789L, "P1", new BigDecimal("101325"));
    Assertions.assertEquals("Pa", pressure.getDefaultUnit());
  }

  @Test
  public void getPascalValue() {
    var pressure = new Pressure(789L, "P1", new BigDecimal("101325"));
    Assertions.assertEquals(0, new BigDecimal("101325").compareTo(pressure.getPascalValue()));
  }

  @Test
  public void getBarValue() {
    var pressure = new Pressure(789L, "P1", new BigDecimal("100000"));
    Assertions.assertEquals(0, new BigDecimal("1").compareTo(pressure.getBarValue()));
  }

  @Test
  public void getBarValue_withDecimal() {
    var pressure = new Pressure(789L, "P1", new BigDecimal("101325"));
    Assertions.assertEquals(0, new BigDecimal("1.01325").compareTo(pressure.getBarValue()));
  }

  @Test
  public void getKiloPascalValue() {
    var pressure = new Pressure(789L, "P1", new BigDecimal("1000"));
    Assertions.assertEquals(0, new BigDecimal("1").compareTo(pressure.getKiloPascalValue()));
  }

  @Test
  public void getKiloPascalValue_withDecimal() {
    var pressure = new Pressure(789L, "P1", new BigDecimal("101325"));
    Assertions.assertEquals(0, new BigDecimal("101.325").compareTo(pressure.getKiloPascalValue()));
  }

  @Test
  public void getValueInDefaultUnit() {
    var pressure = new Pressure(789L, "P1", new BigDecimal("101325"));
    Assertions.assertEquals(
        0, new BigDecimal("101325").compareTo(pressure.getValueInDefaultUnit()));
  }

  @Test
  public void toStringTest() {
    var pressure = new Pressure(789L, "P1", new BigDecimal("101325"));
    Assertions.assertEquals("101325Pa", pressure.toString());
  }

  @Test
  public void toStringTest_withDecimalPlaces() {
    var pressure = new Pressure(789L, "P1", new BigDecimal("101325.12345"), "Pa");
    Assertions.assertEquals("101325.12345Pa", pressure.toString());
  }

  @Test
  public void constructor_withUnitPascal() {
    var pressure = new Pressure(789L, "P1", new BigDecimal("101325"), Pressure.PASCAL);
    Assertions.assertEquals(0, new BigDecimal("101325").compareTo(pressure.getPascalValue()));
    Assertions.assertEquals(0, new BigDecimal("1.01325").compareTo(pressure.getBarValue()));
    Assertions.assertEquals(0, new BigDecimal("101.325").compareTo(pressure.getKiloPascalValue()));
  }

  @Test
  public void constructor_withUnitBar() {
    var pressure = new Pressure(789L, "P1", new BigDecimal("1.01325"), Pressure.BAR);
    Assertions.assertEquals(0, new BigDecimal("101325").compareTo(pressure.getPascalValue()));
    Assertions.assertEquals(0, new BigDecimal("1.01325").compareTo(pressure.getBarValue()));
    Assertions.assertEquals(0, new BigDecimal("101.325").compareTo(pressure.getKiloPascalValue()));
  }

  @Test
  public void constructor_withUnitKiloPascal() {
    var pressure = new Pressure(789L, "P1", new BigDecimal("101.325"), Pressure.KILO_PASCAL);
    Assertions.assertEquals(0, new BigDecimal("101325").compareTo(pressure.getPascalValue()));
    Assertions.assertEquals(0, new BigDecimal("1.01325").compareTo(pressure.getBarValue()));
    Assertions.assertEquals(0, new BigDecimal("101.325").compareTo(pressure.getKiloPascalValue()));
  }

  @Test
  public void constructor_withUnitNull_throwsException() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          new Pressure(789L, "P1", new BigDecimal("101325"), null);
        });
  }

  @Test
  public void constructor_withInvalidUnit_throwsException() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          new Pressure(789L, "P1", new BigDecimal("101325"), "psi");
        });
  }

  @Test
  public void constructor_withEmptyUnit_throwsException() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          new Pressure(789L, "P1", new BigDecimal("101325"), "");
        });
  }

  @Test
  public void withNewTimestamp_returnsPressureWithNewTimestamp_preservesOtherFields() {
    var original = new Pressure(789L, "P1", new BigDecimal("101325"));
    long newTimestamp = 5000L;

    Measurement copy = original.withNewTimestamp(newTimestamp);

    Assertions.assertInstanceOf(Pressure.class, copy);
    Assertions.assertEquals(newTimestamp, copy.getTimeUtc());
    Assertions.assertEquals(original.getSourceId(), copy.getSourceId());
    Assertions.assertEquals(
        0, original.getValueInDefaultUnit().compareTo(copy.getValueInDefaultUnit()));
    Assertions.assertEquals(original.getDefaultUnit(), copy.getDefaultUnit());
    Assertions.assertEquals(
        0, original.getPascalValue().compareTo(((Pressure) copy).getPascalValue()));
  }

  @Test
  public void withNewTimestamp_doesNotModifyOriginal() {
    var original = new Pressure(789L, "P1", new BigDecimal("101325"));
    long originalTime = original.getTimeUtc();

    original.withNewTimestamp(9999L);

    Assertions.assertEquals(originalTime, original.getTimeUtc());
  }
}
