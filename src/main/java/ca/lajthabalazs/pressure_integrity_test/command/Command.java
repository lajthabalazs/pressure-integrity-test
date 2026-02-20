package ca.lajthabalazs.pressure_integrity_test.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    this.parameters =
        parameters == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(parameters));
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  /** Optional longer description (e.g. from device manual). */
  public String getDescription() {
    return description;
  }

  public List<Parameter> getParameters() {
    return parameters;
  }
}
