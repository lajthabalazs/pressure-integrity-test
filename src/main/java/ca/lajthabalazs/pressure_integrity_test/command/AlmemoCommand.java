package ca.lajthabalazs.pressure_integrity_test.command;

import java.util.List;
import java.util.Optional;

/**
 * A {@link Command} that appends its single parameter directly after the id with no separating
 * character (e.g. G00, M95). Used for Almemo device selection (G) and measuring point selection
 * (M). Must be constructed with exactly one parameter.
 */
public class AlmemoCommand extends Command {

  public AlmemoCommand(String id, String name, String description, List<Parameter> parameters) {
    super(id, name, description, parameters);
    if (parameters.size() != 1) {
      throw new IllegalArgumentException("AlmemoCommand must have exactly one parameter");
    }
  }

  @Override
  public String getDisplayName() {
    return getId() + "xx - " + getName();
  }

  @Override
  public Optional<String> buildMessage(List<String> rawParameterValues) {
    Optional<String> result = super.buildMessage(rawParameterValues);
    if (result.isEmpty()) {
      return result;
    }
    // AlmemoCommand has exactly one parameter (enforced in constructor); send "idvalue" not
    // "id,value"
    Object normalized = getParameters().get(0).normalize(rawParameterValues.get(0));
    return Optional.of(getId() + normalized);
  }
}
