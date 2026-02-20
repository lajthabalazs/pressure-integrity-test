package ca.lajthabalazs.pressure_integrity_test.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A command that can be sent to a device, with an id, display name, and ordered parameters. */
public class Command {

  private final String id;
  private final String name;
  private final List<Parameter> parameters;

  public Command(String id, String name, List<Parameter> parameters) {
    this.id = id;
    this.name = name;
    this.parameters =
        parameters == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(parameters));
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public List<Parameter> getParameters() {
    return parameters;
  }
}
