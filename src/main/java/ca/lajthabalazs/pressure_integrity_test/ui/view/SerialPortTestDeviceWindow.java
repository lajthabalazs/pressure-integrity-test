package ca.lajthabalazs.pressure_integrity_test.ui.view;

import com.fazecast.jSerialComm.SerialPort;
import java.awt.Frame;
import javax.swing.JFrame;

/**
 * Window for testing a single device type (Ruska or Almemo) on a chosen port: user configures
 * serial parameters then clicks Connect. Opened from {@link SerialPortTesterWindow} when the user
 * clicks Test.
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

  public SerialPortTestDeviceWindow(Frame parent, String preselectedPortName, boolean ruska) {
    super(ruska ? "Ruska – Serial Port Test" : "Ahlborn Almemo – Serial Port Test");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setSize(500, 120);
    if (parent != null) {
      setLocationRelativeTo(parent);
    } else {
      setLocationByPlatform(true);
    }

    SerialPortPanel panel =
        ruska
            ? new SerialPortPanel(RUSKA_BAUD, RUSKA_DATA_BITS, RUSKA_STOP_BITS, RUSKA_PARITY)
            : new SerialPortPanel(ALMEMO_BAUD, ALMEMO_DATA_BITS, ALMEMO_STOP_BITS, ALMEMO_PARITY);
    panel.refreshPorts();
    panel.setSelectedPort(preselectedPortName);

    getContentPane().add(panel);
  }
}
