package ca.lajthabalazs.pressure_integrity_test.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TwoDigitStringParameterTest {

  private final TwoDigitStringParameter param = new TwoDigitStringParameter();

  @Test
  void checkValue_null_returnsFalse() {
    assertFalse(param.checkValue(null));
  }

  @Test
  void checkValue_emptyOrSingleDigit_returnsFalse() {
    assertFalse(param.checkValue(""));
    assertFalse(param.checkValue("0"));
    assertFalse(param.checkValue("9"));
    assertFalse(param.checkValue(" 5 "));
  }

  @Test
  void checkValue_threeOrMoreDigits_returnsFalse() {
    assertFalse(param.checkValue("000"));
    assertFalse(param.checkValue("123"));
    assertFalse(param.checkValue("999"));
  }

  @Test
  void checkValue_nonDigit_returnsFalse() {
    assertFalse(param.checkValue("0a"));
    assertFalse(param.checkValue("a0"));
    assertFalse(param.checkValue("xx"));
    assertFalse(param.checkValue("  "));
  }

  @Test
  void checkValue_twoDigits_returnsTrue() {
    assertTrue(param.checkValue("00"));
    assertTrue(param.checkValue("01"));
    assertTrue(param.checkValue("99"));
    assertTrue(param.checkValue("15"));
    assertTrue(param.checkValue("  42  "));
  }

  @Test
  void normalize_validValue_returnsTrimmed() {
    assertEquals("00", param.normalize("00"));
    assertEquals("99", param.normalize("99"));
    assertEquals("42", param.normalize("  42  "));
  }

  @Test
  void normalize_null_returns00() {
    assertEquals("00", param.normalize(null));
  }
}
