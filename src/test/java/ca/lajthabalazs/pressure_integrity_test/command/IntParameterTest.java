package ca.lajthabalazs.pressure_integrity_test.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IntParameterTest {

  @Test
  void noArgConstructor_acceptsAnyValidInteger() {
    IntParameter param = new IntParameter();
    assertTrue(param.checkValue("0"));
    assertTrue(param.checkValue("-1"));
    assertTrue(param.checkValue("99999"));
    assertEquals(42, param.normalize("42"));
  }

  @Test
  void checkValue_nullOrBlank_returnsFalse() {
    IntParameter param = new IntParameter();
    assertFalse(param.checkValue(null));
    assertFalse(param.checkValue(""));
    assertFalse(param.checkValue("   "));
  }

  @Test
  void checkValue_nonNumeric_returnsFalse() {
    IntParameter param = new IntParameter();
    assertFalse(param.checkValue("abc"));
    assertFalse(param.checkValue("12.3"));
  }

  @Test
  void normalize_parsesInteger() {
    IntParameter param = new IntParameter();
    assertEquals(0, param.normalize("0"));
    assertEquals(-100, param.normalize("-100"));
    assertEquals(123, param.normalize("  123  "));
  }

  @Test
  void boundedConstructor_rejectsOutOfRange() {
    IntParameter param = new IntParameter(1, 4);
    assertFalse(param.checkValue("0"));
    assertFalse(param.checkValue("5"));
    assertTrue(param.checkValue("1"));
    assertTrue(param.checkValue("4"));
    assertTrue(param.checkValue("2"));
  }

  @Test
  void boundedConstructor_normalizeReturnsInteger() {
    IntParameter param = new IntParameter(1, 4);
    assertEquals(3, param.normalize("3"));
  }
}
