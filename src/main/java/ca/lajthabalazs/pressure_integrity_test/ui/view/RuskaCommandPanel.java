package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.command.RuskaReadCommands;
import ca.lajthabalazs.pressure_integrity_test.serial.SerialPortHandle;

/**
 * Command panel for Ruska devices. Delegates to {@link DeviceCommandPanel} with {@link
 * RuskaReadCommands#ALL}.
 */
public class RuskaCommandPanel extends DeviceCommandPanel {

  public RuskaCommandPanel(SerialPortHandle handle) {
    super(handle, RuskaReadCommands.ALL);
  }
}
