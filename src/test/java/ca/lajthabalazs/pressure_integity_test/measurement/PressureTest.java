package ca.lajthabalazs.pressure_integity_test.measurement;

import ca.lajthabalazs.pressure_integrity_test.measurement.Pressure;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PressureTest {
  @Test
  public void getTimeUtc() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("101325"),
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertEquals(789, pressure.getTimeUtc());
  }

  @Test
  public void getSourceId() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("101325"),
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertEquals("P1", pressure.getSourceId());
  }

  @Test
  public void getDefaultUnit() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("101325"),
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertEquals("Pa", pressure.getDefaultUnit());
  }

  @Test
  public void getPascalValue() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("101325"),
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertEquals(0, new BigDecimal("101325").compareTo(pressure.getPascalValue()));
  }

  @Test
  public void getBarValue() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("100000"),
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertEquals(0, new BigDecimal("1").compareTo(pressure.getBarValue()));
  }

  @Test
  public void getBarValue_withDecimal() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("101325"),
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertEquals(0, new BigDecimal("1.01325").compareTo(pressure.getBarValue()));
  }

  @Test
  public void getKiloPascalValue() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("1000"),
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertEquals(0, new BigDecimal("1").compareTo(pressure.getKiloPascalValue()));
  }

  @Test
  public void getKiloPascalValue_withDecimal() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("101325"),
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertEquals(0, new BigDecimal("101.325").compareTo(pressure.getKiloPascalValue()));
  }

  @Test
  public void getValueInDefaultUnit() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("101325"),
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertEquals(
        0, new BigDecimal("101325").compareTo(pressure.getValueInDefaultUnit()));
  }

  @Test
  public void getSourceSigma() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("101325"),
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertEquals(0, new BigDecimal("10").compareTo(pressure.getSourceSigma()));
  }

  @Test
  public void isBelievable() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("101325"),
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertTrue(pressure.isBelievable());
  }

  @Test
  public void isBelievable_onRangeLow_isBelievable() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            BigDecimal.ZERO,
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertTrue(pressure.isBelievable());
  }

  @Test
  public void isBelievable_onRangeHigh_isBelievable() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("200000"),
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertTrue(pressure.isBelievable());
  }

  @Test
  public void isBelievable_outOfRangeLow_isNotBelievable() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("-1"),
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertFalse(pressure.isBelievable());
  }

  @Test
  public void isBelievable_outOfRangeHigh_isNotBelievable() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("200001"),
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertFalse(pressure.isBelievable());
  }

  @Test
  public void toStringTest() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("101325"),
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertEquals("101325Pa", pressure.toString());
  }

  @Test
  public void toStringTest_withDecimalPlaces() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("101325.12345"),
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertEquals("101325.12345Pa", pressure.toString());
  }

  @Test
  public void constructor_withUnitPascal() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("101325"),
            Pressure.PASCAL,
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertEquals(0, new BigDecimal("101325").compareTo(pressure.getPascalValue()));
    Assertions.assertEquals(0, new BigDecimal("1.01325").compareTo(pressure.getBarValue()));
    Assertions.assertEquals(0, new BigDecimal("101.325").compareTo(pressure.getKiloPascalValue()));
  }

  @Test
  public void constructor_withUnitBar() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("1.01325"),
            Pressure.BAR,
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertEquals(0, new BigDecimal("101325").compareTo(pressure.getPascalValue()));
    Assertions.assertEquals(0, new BigDecimal("1.01325").compareTo(pressure.getBarValue()));
    Assertions.assertEquals(0, new BigDecimal("101.325").compareTo(pressure.getKiloPascalValue()));
  }

  @Test
  public void constructor_withUnitKiloPascal() {
    var pressure =
        new Pressure(
            789L,
            "P1",
            new BigDecimal("101.325"),
            Pressure.KILO_PASCAL,
            new BigDecimal("10"),
            new BigDecimal("0"),
            new BigDecimal("200000"));
    Assertions.assertEquals(0, new BigDecimal("101325").compareTo(pressure.getPascalValue()));
    Assertions.assertEquals(0, new BigDecimal("1.01325").compareTo(pressure.getBarValue()));
    Assertions.assertEquals(0, new BigDecimal("101.325").compareTo(pressure.getKiloPascalValue()));
  }

  @Test
  public void constructor_withUnitNull_throwsException() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          new Pressure(
              789L,
              "P1",
              new BigDecimal("101325"),
              null,
              new BigDecimal("10"),
              new BigDecimal("0"),
              new BigDecimal("200000"));
        });
  }

  @Test
  public void constructor_withInvalidUnit_throwsException() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          new Pressure(
              789L,
              "P1",
              new BigDecimal("101325"),
              "psi",
              new BigDecimal("10"),
              new BigDecimal("0"),
              new BigDecimal("200000"));
        });
  }

  @Test
  public void constructor_withEmptyUnit_throwsException() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          new Pressure(
              789L,
              "P1",
              new BigDecimal("101325"),
              "",
              new BigDecimal("10"),
              new BigDecimal("0"),
              new BigDecimal("200000"));
        });
  }
}
