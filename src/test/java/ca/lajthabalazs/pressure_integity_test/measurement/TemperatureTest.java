package ca.lajthabalazs.pressure_integity_test.measurement;

import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.Temperature;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TemperatureTest {
  @Test
  public void getTimeUtc() {
    var temperature = new Temperature(456L, "T1", new BigDecimal("25.5"));
    Assertions.assertEquals(456, temperature.getTimeUtc());
  }

  @Test
  public void getSourceId() {
    var temperature = new Temperature(456L, "T1", new BigDecimal("25.5"));
    Assertions.assertEquals("T1", temperature.getSourceId());
  }

  @Test
  public void getDefaultUnit() {
    var temperature = new Temperature(456L, "T1", new BigDecimal("25.5"));
    Assertions.assertEquals("C", temperature.getDefaultUnit());
  }

  @Test
  public void getCelsiusValue() {
    var temperature = new Temperature(456L, "T1", new BigDecimal("25.5"));
    Assertions.assertEquals(0, new BigDecimal("25.5").compareTo(temperature.getCelsiusValue()));
  }

  @Test
  public void getKelvinValue() {
    var temperature = new Temperature(456L, "T1", new BigDecimal("25.5"));
    Assertions.assertEquals(0, new BigDecimal("298.65").compareTo(temperature.getKelvinValue()));
  }

  @Test
  public void getKelvinValue_zeroCelsius() {
    var temperature = new Temperature(456L, "T1", BigDecimal.ZERO);
    Assertions.assertEquals(0, new BigDecimal("273.15").compareTo(temperature.getKelvinValue()));
  }

  @Test
  public void getKelvinValue_negativeCelsius() {
    var temperature = new Temperature(456L, "T1", new BigDecimal("-40"));
    Assertions.assertEquals(0, new BigDecimal("233.15").compareTo(temperature.getKelvinValue()));
  }

  @Test
  public void getValueInDefaultUnit() {
    var temperature = new Temperature(456L, "T1", new BigDecimal("25.5"));
    Assertions.assertEquals(
        0, new BigDecimal("25.5").compareTo(temperature.getValueInDefaultUnit()));
  }

  @Test
  public void toStringTest() {
    var temperature = new Temperature(456L, "T1", new BigDecimal("25.5"));
    Assertions.assertEquals("25.5C", temperature.toString());
  }

  @Test
  public void toStringTest_withDecimalPlaces() {
    var temperature = new Temperature(456L, "T1", new BigDecimal("25.123456"));
    Assertions.assertEquals("25.123456C", temperature.toString());
  }

  @Test
  public void constructor_withUnitCelsius() {
    var temperature = new Temperature(456L, "T1", new BigDecimal("25.5"), Temperature.CELSIUS);
    Assertions.assertEquals(0, new BigDecimal("25.5").compareTo(temperature.getCelsiusValue()));
    Assertions.assertEquals(0, new BigDecimal("298.65").compareTo(temperature.getKelvinValue()));
  }

  @Test
  public void constructor_withUnitKelvin() {
    var temperature = new Temperature(456L, "T1", new BigDecimal("298.65"), Temperature.KELVIN);
    Assertions.assertEquals(0, new BigDecimal("25.5").compareTo(temperature.getCelsiusValue()));
    Assertions.assertEquals(0, new BigDecimal("298.65").compareTo(temperature.getKelvinValue()));
  }

  @Test
  public void constructor_withUnitKelvin_zeroKelvin() {
    var temperature = new Temperature(456L, "T1", new BigDecimal("273.15"), Temperature.KELVIN);
    Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(temperature.getCelsiusValue()));
    Assertions.assertEquals(0, new BigDecimal("273.15").compareTo(temperature.getKelvinValue()));
  }

  @Test
  public void constructor_withUnitKelvin_negativeCelsius() {
    var temperature = new Temperature(456L, "T1", new BigDecimal("233.15"), Temperature.KELVIN);
    Assertions.assertEquals(0, new BigDecimal("-40").compareTo(temperature.getCelsiusValue()));
    Assertions.assertEquals(0, new BigDecimal("233.15").compareTo(temperature.getKelvinValue()));
  }

  @Test
  public void constructor_withUnitNull_throwsException() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          new Temperature(456L, "T1", new BigDecimal("25.5"), null);
        });
  }

  @Test
  public void constructor_withInvalidUnit_throwsException() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          new Temperature(456L, "T1", new BigDecimal("25.5"), "F");
        });
  }

  @Test
  public void constructor_withEmptyUnit_throwsException() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          new Temperature(456L, "T1", new BigDecimal("25.5"), "");
        });
  }

  @Test
  public void withNewTimestamp_returnsTemperatureWithNewTimestamp_preservesOtherFields() {
    var original = new Temperature(456L, "T1", new BigDecimal("25.5"));
    long newTimestamp = 3000L;

    Measurement copy = original.withNewTimestamp(newTimestamp);

    Assertions.assertInstanceOf(Temperature.class, copy);
    Assertions.assertEquals(newTimestamp, copy.getTimeUtc());
    Assertions.assertEquals(original.getSourceId(), copy.getSourceId());
    Assertions.assertEquals(
        0, original.getValueInDefaultUnit().compareTo(copy.getValueInDefaultUnit()));
    Assertions.assertEquals(original.getDefaultUnit(), copy.getDefaultUnit());
    Assertions.assertEquals(
        0, original.getCelsiusValue().compareTo(((Temperature) copy).getCelsiusValue()));
  }

  @Test
  public void withNewTimestamp_doesNotModifyOriginal() {
    var original = new Temperature(456L, "T1", new BigDecimal("25.5"));
    long originalTime = original.getTimeUtc();

    original.withNewTimestamp(9999L);

    Assertions.assertEquals(originalTime, original.getTimeUtc());
  }
}
