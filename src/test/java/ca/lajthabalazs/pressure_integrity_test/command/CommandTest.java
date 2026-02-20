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
  void getId_getName_getDescription_getParameters() {
    List<Parameter> params = List.of(new IntParameter(1, 4));
    Command cmd = new Command("UD", "User unit", "Description here", params);
    assertEquals("UD", cmd.getId());
    assertEquals("User unit", cmd.getName());
    assertEquals("Description here", cmd.getDescription());
    assertEquals(1, cmd.getParameters().size());
    assertFalse(cmd.getParameters().isEmpty());
  }
}
