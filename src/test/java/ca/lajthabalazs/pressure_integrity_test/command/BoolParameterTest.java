package ca.lajthabalazs.pressure_integrity_test.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BoolParameterTest {

  private final BoolParameter param = new BoolParameter();

  @Test
  void checkValue_null_returnsFalse() {
    assertFalse(param.checkValue(null));
  }

  @Test
  void checkValue_acceptsTrueFalseYesNo10() {
    assertTrue(param.checkValue("true"));
    assertTrue(param.checkValue("TRUE"));
    assertTrue(param.checkValue("false"));
    assertTrue(param.checkValue("yes"));
    assertTrue(param.checkValue("no"));
    assertTrue(param.checkValue("1"));
    assertTrue(param.checkValue("0"));
    assertTrue(param.checkValue("  true  "));
  }

  @Test
  void checkValue_rejectsInvalid() {
    assertFalse(param.checkValue(""));
    assertFalse(param.checkValue("2"));
    assertFalse(param.checkValue("maybe"));
  }

  @Test
  void normalize_trueYes1_returnsTrue() {
    assertEquals(Boolean.TRUE, param.normalize("true"));
    assertEquals(Boolean.TRUE, param.normalize("yes"));
    assertEquals(Boolean.TRUE, param.normalize("1"));
    assertEquals(Boolean.TRUE, param.normalize("  TRUE  "));
  }

  @Test
  void normalize_falseNo0_returnsFalse() {
    assertEquals(Boolean.FALSE, param.normalize("false"));
    assertEquals(Boolean.FALSE, param.normalize("no"));
    assertEquals(Boolean.FALSE, param.normalize("0"));
    assertEquals(Boolean.FALSE, param.normalize("  NO  "));
  }
}
