package ca.lajthabalazs.pressure_integrity_test.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CommandTest {

  @Test
  void buildMessage_noParameters_emptyList_returnsId() {
    Command cmd = new Command("PA", "Pressure", "desc", List.of());
    assertEquals(Optional.of("PA"), cmd.buildMessage(List.of()));
  }

  @Test
  void buildMessage_noParameters_null_returnsEmpty() {
    Command cmd = new Command("PA", "Pressure", "desc", List.of());
    assertEquals(Optional.empty(), cmd.buildMessage(null));
  }

  @Test
  void buildMessage_noParameters_wrongSize_returnsEmpty() {
    Command cmd = new Command("PA", "Pressure", "desc", List.of());
    assertTrue(cmd.buildMessage(List.of("x")).isEmpty());
  }

  @Test
  void buildMessage_oneParameter_valid_returnsIdCommaValue() {
    Command cmd = new Command("UD", "User unit", "desc", List.of(new IntParameter(1, 4)));
    assertEquals(Optional.of("UD,2"), cmd.buildMessage(List.of("2")));
  }

  @Test
  void buildMessage_oneParameter_invalid_returnsEmpty() {
    Command cmd = new Command("UD", "User unit", "desc", List.of(new IntParameter(1, 4)));
    assertTrue(cmd.buildMessage(List.of("99")).isEmpty());
    assertTrue(cmd.buildMessage(List.of("x")).isEmpty());
  }

  @Test
  void buildMessage_oneParameter_wrongListSize_returnsEmpty() {
    Command cmd = new Command("UD", "User unit", "desc", List.of(new IntParameter(1, 4)));
    assertTrue(cmd.buildMessage(List.of()).isEmpty());
    assertTrue(cmd.buildMessage(List.of("1", "2")).isEmpty());
  }

  @Test
  void buildMessage_twoParameters_valid_returnsIdCommaSeparated() {
    Command cmd =
        new Command("XY", "Two", "desc", List.of(new IntParameter(), new StringParameter()));
    assertEquals(Optional.of("XY,42,hello"), cmd.buildMessage(List.of("42", "hello")));
  }

  @Test
  void buildMessage_almemoCommand_oneParameter_returnsIdPlusValue() {
    Command cmd =
        new AlmemoCommand("G", "Select device", "desc", List.of(new TwoDigitStringParameter()));
    assertEquals(Optional.of("G00"), cmd.buildMessage(List.of("00")));
    assertEquals(Optional.of("G99"), cmd.buildMessage(List.of("99")));
    assertTrue(cmd.buildMessage(List.of("9")).isEmpty());
    assertTrue(cmd.buildMessage(List.of("123")).isEmpty());
  }

  @Test
  void getId_getName_getDescription_getParameters() {
    List<Parameter> params = List.of(new IntParameter(1, 4));
    Command cmd = new Command("UD", "User unit", "Description here", params);
    assertEquals("UD", cmd.getId());
    assertEquals("User unit", cmd.getName());
    assertEquals("Description here", cmd.getDescription());
    assertEquals(1, cmd.getParameters().size());
    assertFalse(cmd.getParameters().isEmpty());
  }

  @Test
  void getDisplayName_returnsIdDashName() {
    Command cmd = new Command("PA", "Pressure", "desc", List.of());
    assertEquals("PA - Pressure", cmd.getDisplayName());
  }

  @Test
  void getDisplayName_withParameterCommand_returnsIdDashName() {
    Command cmd =
        new Command("P15", "Device and sensors overview", "desc", List.of(new IntParameter(1, 4)));
    assertEquals("P15 - Device and sensors overview", cmd.getDisplayName());
  }
}
