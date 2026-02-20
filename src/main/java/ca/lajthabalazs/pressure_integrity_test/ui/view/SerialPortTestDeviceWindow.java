package ca.lajthabalazs.pressure_integrity_test.ui.view;

import com.fazecast.jSerialComm.SerialPort;
import java.awt.BorderLayout;
import java.awt.Frame;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Window for testing a single device type (Ruska or Almemo) on a chosen port: user configures
 * serial parameters then clicks Connect. For Ruska, when connected a command chooser and log are
 * shown.
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

  private final boolean ruska;
  private RuskaCommandPanel ruskaCommandPanel;

  public SerialPortTestDeviceWindow(Frame parent, String portName, boolean ruska) {
    super((ruska ? "Ruska" : "Ahlborn Almemo") + " – " + portName);
    this.ruska = ruska;
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setSize(820, ruska ? 520 : 120);
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

    if (ruska) {
      JPanel main = new JPanel(new BorderLayout(0, 0));
      main.add(serialPanel, BorderLayout.NORTH);
      JPanel centerPlaceholder = new JPanel();
      main.add(centerPlaceholder, BorderLayout.CENTER);
      getContentPane().add(main);

      serialPanel.setConnectionListener(
          handle -> {
            main.remove(centerPlaceholder);
            if (ruskaCommandPanel != null) {
              ruskaCommandPanel.dispose();
            }
            ruskaCommandPanel = new RuskaCommandPanel(handle);
            main.add(ruskaCommandPanel, BorderLayout.CENTER);
            main.revalidate();
            main.repaint();
          });
    } else {
      getContentPane().add(serialPanel);
    }
  }

  @Override
  public void dispose() {
    if (ruskaCommandPanel != null) {
      ruskaCommandPanel.dispose();
    }
    super.dispose();
  }
}
