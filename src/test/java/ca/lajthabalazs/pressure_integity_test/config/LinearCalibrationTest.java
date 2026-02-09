package ca.lajthabalazs.pressure_integity_test.config;

import ca.lajthabalazs.pressure_integrity_test.config.LinearCalibration;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LinearCalibrationTest {

  @Test
  public void getCalibratedValue_nullInput_returnsNull() {
    LinearCalibration cal = new LinearCalibration();
    cal.setA(BigDecimal.ONE);
    cal.setB(BigDecimal.ZERO);
    Assertions.assertNull(cal.getCalibratedValue(null));
  }

  @Test
  public void getCalibratedValue_aNull_treatedAsOne() {
    LinearCalibration cal = new LinearCalibration();
    cal.setA(null);
    cal.setB(BigDecimal.ZERO);
    Assertions.assertEquals(
        0, new BigDecimal("10").compareTo(cal.getCalibratedValue(new BigDecimal("10"))));
  }

  @Test
  public void getCalibratedValue_bNull_treatedAsZero() {
    LinearCalibration cal = new LinearCalibration();
    cal.setA(BigDecimal.ONE);
    cal.setB(null);
    Assertions.assertEquals(
        0, new BigDecimal("10").compareTo(cal.getCalibratedValue(new BigDecimal("10"))));
  }

  @Test
  public void getCalibratedValue_bothSet_returnsAMeasuredPlusB() {
    LinearCalibration cal = new LinearCalibration();
    cal.setA(new BigDecimal("1.5"));
    cal.setB(new BigDecimal("-0.2"));
    BigDecimal result = cal.getCalibratedValue(new BigDecimal("10"));
    Assertions.assertNotNull(result);
    Assertions.assertEquals(0, new BigDecimal("14.8").compareTo(result));
  }

  @Test
  public void getCalibratedValue_zeroMeasured_returnsB() {
    LinearCalibration cal = new LinearCalibration();
    cal.setA(new BigDecimal("2"));
    cal.setB(new BigDecimal("0.5"));
    BigDecimal result = cal.getCalibratedValue(BigDecimal.ZERO);
    Assertions.assertNotNull(result);
    Assertions.assertEquals(0, new BigDecimal("0.5").compareTo(result));
  }

  @Test
  public void gettersAndSetters_roundTrip() {
    LinearCalibration cal = new LinearCalibration();
    BigDecimal a = new BigDecimal("0.998");
    BigDecimal b = new BigDecimal("0.1");
    cal.setA(a);
    cal.setB(b);
    Assertions.assertSame(a, cal.getA());
    Assertions.assertSame(b, cal.getB());
  }
}
