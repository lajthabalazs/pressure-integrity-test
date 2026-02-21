package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.command.AlmemoCommands;
import ca.lajthabalazs.pressure_integrity_test.command.Command;
import ca.lajthabalazs.pressure_integrity_test.command.RuskaReadCommands;
import com.fazecast.jSerialComm.SerialPort;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Window for testing a single device type (Ruska or Almemo) on a chosen port: user configures
 * serial parameters then clicks Connect. When connected, a command/plain-text panel and log are
 * shown for both device types.
 */
public class SerialPortTestDeviceWindow extends JFrame {

  private static final int[] RUSKA_BAUD = {1200, 2400, 9600, 19200};
  private static final int[] RUSKA_DATA_BITS = {7, 8};
  private static final int[] RUSKA_STOP_BITS = {SerialPort.ONE_STOP_BIT, SerialPort.TWO_STOP_BITS};
  private static final int[] RUSKA_PARITY = {
    SerialPort.NO_PARITY, SerialPort.EVEN_PARITY, SerialPort.ODD_PARITY
  };

  private static final int[] ALMEMO_BAUD = {300, 600, 1200, 2400, 9600, 57600, 115200, 230400};
  private static final int[] ALMEMO_DATA_BITS = {8};
  private static final int[] ALMEMO_STOP_BITS = {SerialPort.ONE_STOP_BIT};
  private static final int[] ALMEMO_PARITY = {SerialPort.NO_PARITY};

  private DeviceCommandPanel deviceCommandPanel;

  public SerialPortTestDeviceWindow(Frame parent, String portName, boolean ruska) {
    super((ruska ? "Ruska" : "Ahlborn Almemo") + " – " + portName);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setSize(820, 520);
    if (parent != null) {
      setLocationRelativeTo(parent);
    } else {
      setLocationByPlatform(true);
    }

    SerialPortPanel serialPanel =
        ruska
            ? new SerialPortPanel(
                portName, RUSKA_BAUD, RUSKA_DATA_BITS, RUSKA_STOP_BITS, RUSKA_PARITY)
            : new SerialPortPanel(
                portName, ALMEMO_BAUD, ALMEMO_DATA_BITS, ALMEMO_STOP_BITS, ALMEMO_PARITY);

    JPanel main = new JPanel(new BorderLayout(0, 0));
    main.add(serialPanel, BorderLayout.NORTH);
    JPanel centerPlaceholder = new JPanel();
    main.add(centerPlaceholder, BorderLayout.CENTER);
    getContentPane().add(main);

    List<Command> commands = ruska ? RuskaReadCommands.ALL : AlmemoCommands.ALL;

    serialPanel.setConnectionListener(
        handle -> {
          main.remove(centerPlaceholder);
          if (deviceCommandPanel != null) {
            deviceCommandPanel.dispose();
          }
          deviceCommandPanel = new DeviceCommandPanel(handle, commands);
          main.add(deviceCommandPanel, BorderLayout.CENTER);
          main.revalidate();
          main.repaint();
        });
  }

  @Override
  public void dispose() {
    if (deviceCommandPanel != null) {
      deviceCommandPanel.dispose();
    }
    super.dispose();
  }
}
