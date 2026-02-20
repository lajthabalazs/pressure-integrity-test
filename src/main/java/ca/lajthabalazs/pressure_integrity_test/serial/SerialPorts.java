package ca.lajthabalazs.pressure_integrity_test.serial;

import com.fazecast.jSerialComm.SerialPort;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for listing available serial ports and obtaining a {@link SerialPortHandle} (real or
 * demo).
 */
public final class SerialPorts {

  private static final String DEMO_PORT_NAME = "DemoPort";

  private SerialPorts() {}

  /**
   * Returns descriptors for all available real serial ports from the system. Does not include
   * DemoPort; the UI may add it to the list. Use {@link #getHandle(String)} with "DemoPort" to
   * obtain a demo handle.
   */
  public static List<PortDescriptor> getAvailablePorts() {
    List<PortDescriptor> list = new ArrayList<>();
    SerialPort[] ports = SerialPort.getCommPorts();
    for (SerialPort port : ports) {
      list.add(
          new PortDescriptor(
              port.getSystemPortName(),
              port.getPortDescription() != null ? port.getPortDescription() : ""));
    }
    return list;
  }

  /**
   * Returns a handle for the given port name. Use {@link #getAvailablePorts()} to get valid names.
   * For "DemoPort" returns a {@link DemoPortHandle}; otherwise wraps a jSerialComm {@link
   * SerialPort}.
   */
  public static SerialPortHandle getHandle(String systemPortName) {
    if (DEMO_PORT_NAME.equals(systemPortName)) {
      return new DemoPortHandle();
    }
    SerialPort port = SerialPort.getCommPort(systemPortName);
    return new JSerialCommPortHandle(port);
  }
}
