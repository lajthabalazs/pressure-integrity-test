package ca.lajthabalazs.pressure_integrity_test.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FloatParameterTest {

  private final FloatParameter param = new FloatParameter();

  @Test
  void checkValue_nullOrBlank_returnsFalse() {
    assertFalse(param.checkValue(null));
    assertFalse(param.checkValue(""));
    assertFalse(param.checkValue("   "));
  }

  @Test
  void checkValue_validFloat_returnsTrue() {
    assertTrue(param.checkValue("0"));
    assertTrue(param.checkValue("1.5"));
    assertTrue(param.checkValue("-3.14"));
    assertTrue(param.checkValue("  2.0  "));
  }

  @Test
  void checkValue_nonNumeric_returnsFalse() {
    assertFalse(param.checkValue("abc"));
    assertFalse(param.checkValue("1.2.3"));
  }

  @Test
  void normalize_parsesFloat() {
    assertEquals(0f, param.normalize("0"));
    assertEquals(1.5f, param.normalize("1.5"));
    assertEquals(-3.14f, param.normalize("  -3.14  "));
  }
}
