package ca.lajthabalazs.pressure_integrity_test.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StringParameterTest {

  private final StringParameter param = new StringParameter();

  @Test
  void checkValue_null_returnsFalse() {
    assertFalse(param.checkValue(null));
  }

  @Test
  void checkValue_nonNull_returnsTrue() {
    assertTrue(param.checkValue(""));
    assertTrue(param.checkValue("x"));
    assertTrue(param.checkValue("  "));
  }

  @Test
  void normalize_null_returnsEmptyString() {
    assertEquals("", param.normalize(null));
  }

  @Test
  void normalize_trimsWhitespace() {
    assertEquals("hello", param.normalize("  hello  "));
    assertEquals("a", param.normalize(" a "));
  }

  @Test
  void normalize_noWhitespace_returnsSame() {
    assertEquals("hello", param.normalize("hello"));
  }
}
