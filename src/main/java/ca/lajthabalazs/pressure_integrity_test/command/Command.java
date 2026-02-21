package ca.lajthabalazs.pressure_integrity_test.command;

import java.util.List;
import java.util.Optional;

/**
 * A command that can be sent to a device, with an id, display name, description, and ordered
 * parameters.
 */
public class Command {

  private final String id;
  private final String name;
  private final String description;
  private final List<Parameter> parameters;

  public Command(String id, String name, String description, List<Parameter> parameters) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.parameters = List.copyOf(parameters);
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  /**
   * Label for UI (e.g. command dropdown): command id and name, e.g. "P15 - Device and sensors
   * overview".
   */
  public String getDisplayName() {
    return id + " - " + name;
  }

  /** Optional longer description (e.g. from device manual). */
  public String getDescription() {
    return description;
  }

  public List<Parameter> getParameters() {
    return parameters;
  }

  /**
   * Builds the message string to send (e.g. "PA" or "UD,2") from raw parameter values. Validates
   * each value with the corresponding parameter's {@link Parameter#checkValue(String)} and
   * normalizes with {@link Parameter#normalize(String)}. Values are appended comma-separated after
   * the command id.
   *
   * @param rawParameterValues list of raw strings, one per parameter; must match {@link
   *     #getParameters()}.size(). Use empty list for no-parameter commands.
   * @return the message string if validation passes, or empty if size mismatch or any value invalid
   */
  public Optional<String> buildMessage(List<String> rawParameterValues) {
    if (rawParameterValues == null || rawParameterValues.size() != parameters.size()) {
      return Optional.empty();
    }
    for (int i = 0; i < parameters.size(); i++) {
      if (!parameters.get(i).checkValue(rawParameterValues.get(i))) {
        return Optional.empty();
      }
    }
    if (parameters.isEmpty()) {
      return Optional.of(id);
    }
    StringBuilder sb = new StringBuilder(id);
    for (int i = 0; i < parameters.size(); i++) {
      Object normalized = parameters.get(i).normalize(rawParameterValues.get(i));
      sb.append(",").append(normalized);
    }
    return Optional.of(sb.toString());
  }
}
