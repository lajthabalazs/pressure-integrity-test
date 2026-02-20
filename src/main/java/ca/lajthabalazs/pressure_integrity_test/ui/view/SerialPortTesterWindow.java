package ca.lajthabalazs.pressure_integrity_test.ui.view;

import com.fazecast.jSerialComm.SerialPort;
import java.awt.BorderLayout;
import java.awt.Frame;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

/** Window for serial port testing with tabs for Ruska and Ahlborn Almemo devices. */
public class SerialPortTesterWindow extends JFrame {

  private static final String TAB_RUSKA = "Ruska";
  private static final String TAB_AHLBORN_ALMEMO = "Ahlborn Almemo";

  /**
   * Ruska: baud 1200, 2400, 9600, 19200; parity none/even/odd; 7 or 8 data bits; 1 or 2 stop bits.
   */
  private static final int[] RUSKA_BAUD = {1200, 2400, 9600, 19200};

  private static final int[] RUSKA_DATA_BITS = {7, 8};
  private static final int[] RUSKA_STOP_BITS = {SerialPort.ONE_STOP_BIT, SerialPort.TWO_STOP_BITS};
  private static final int[] RUSKA_PARITY = {
    SerialPort.NO_PARITY, SerialPort.EVEN_PARITY, SerialPort.ODD_PARITY
  };

  /** ALMEMO: baud 300–230400; no parity; 8 data bits; 1 stop bit. */
  private static final int[] ALMEMO_BAUD = {300, 600, 1200, 2400, 9600, 57600, 115200, 230400};

  private static final int[] ALMEMO_DATA_BITS = {8};
  private static final int[] ALMEMO_STOP_BITS = {SerialPort.ONE_STOP_BIT};
  private static final int[] ALMEMO_PARITY = {SerialPort.NO_PARITY};

  public SerialPortTesterWindow(Frame parent) {
    setTitle("Serial Port Tester");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setSize(500, 500);
    if (parent != null) {
      setLocationRelativeTo(parent);
    } else {
      setLocationByPlatform(true);
    }

    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.addTab(
        TAB_RUSKA, new SerialPortPanel(RUSKA_BAUD, RUSKA_DATA_BITS, RUSKA_STOP_BITS, RUSKA_PARITY));
    tabbedPane.addTab(
        TAB_AHLBORN_ALMEMO,
        new SerialPortPanel(ALMEMO_BAUD, ALMEMO_DATA_BITS, ALMEMO_STOP_BITS, ALMEMO_PARITY));

    getContentPane().add(tabbedPane, BorderLayout.CENTER);
  }
}
