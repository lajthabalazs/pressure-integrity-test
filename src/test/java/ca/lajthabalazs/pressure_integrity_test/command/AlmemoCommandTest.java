package ca.lajthabalazs.pressure_integrity_test.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AlmemoCommandTest {

  private static final TwoDigitStringParameter TWO_DIGIT = new TwoDigitStringParameter();

  @Test
  void constructor_zeroParameters_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new AlmemoCommand("G", "Select device", "desc", List.of()));
  }

  @Test
  void constructor_twoParameters_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AlmemoCommand(
                "G",
                "Select device",
                "desc",
                List.of(new TwoDigitStringParameter(), new TwoDigitStringParameter())));
  }

  @Test
  void getDisplayName_returnsIdXxAndName() {
    AlmemoCommand cmd = new AlmemoCommand("G", "Select device", "desc", List.of(TWO_DIGIT));
    assertEquals("Gxx - Select device", cmd.getDisplayName());

    AlmemoCommand mCmd =
        new AlmemoCommand("M", "Select measuring point", "desc", List.of(TWO_DIGIT));
    assertEquals("Mxx - Select measuring point", mCmd.getDisplayName());
  }

  @Test
  void getId_getName_getDescription_inheritedFromCommand() {
    AlmemoCommand cmd =
        new AlmemoCommand("G", "Select device", "Description here", List.of(TWO_DIGIT));
    assertEquals("G", cmd.getId());
    assertEquals("Select device", cmd.getName());
    assertEquals("Description here", cmd.getDescription());
    assertEquals(1, cmd.getParameters().size());
  }

  @Test
  void buildMessage_validTwoDigits_returnsIdConcatenatedWithValue() {
    AlmemoCommand cmd = new AlmemoCommand("G", "Select device", "desc", List.of(TWO_DIGIT));
    assertEquals(Optional.of("G00"), cmd.buildMessage(List.of("00")));
    assertEquals(Optional.of("G99"), cmd.buildMessage(List.of("99")));
    assertEquals(Optional.of("G15"), cmd.buildMessage(List.of("15")));

    AlmemoCommand mCmd =
        new AlmemoCommand("M", "Select measuring point", "desc", List.of(TWO_DIGIT));
    assertEquals(Optional.of("M01"), mCmd.buildMessage(List.of("01")));
    assertEquals(Optional.of("M95"), mCmd.buildMessage(List.of("95")));
  }

  @Test
  void buildMessage_invalidParameter_returnsEmpty() {
    AlmemoCommand cmd = new AlmemoCommand("G", "Select device", "desc", List.of(TWO_DIGIT));
    assertTrue(cmd.buildMessage(List.of("9")).isEmpty());
    assertTrue(cmd.buildMessage(List.of("123")).isEmpty());
    assertTrue(cmd.buildMessage(List.of("x0")).isEmpty());
  }

  @Test
  void buildMessage_nullOrWrongSize_returnsEmpty() {
    AlmemoCommand cmd = new AlmemoCommand("G", "Select device", "desc", List.of(TWO_DIGIT));
    assertTrue(cmd.buildMessage(null).isEmpty());
    assertTrue(cmd.buildMessage(List.of()).isEmpty());
    assertTrue(cmd.buildMessage(List.of("00", "01")).isEmpty());
  }
}
