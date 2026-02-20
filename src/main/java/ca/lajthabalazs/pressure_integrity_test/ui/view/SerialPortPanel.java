package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.serial.PortDescriptor;
import ca.lajthabalazs.pressure_integrity_test.serial.SerialPortHandle;
import ca.lajthabalazs.pressure_integrity_test.serial.SerialPorts;
import com.fazecast.jSerialComm.SerialPort;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Panel for serial port selection and connection with configurable baud rate, data bits, stop bits,
 * and parity. Each array parameter is either shown as a dropdown (multiple values) or a fixed label
 * (single value). Empty arrays are rejected in the constructor. Uses underline-style controls
 * similar to {@link TestTypeStepPanel}.
 */
public class SerialPortPanel extends JPanel {

  private static final String NO_PORTS_PLACEHOLDER = "(No ports found)";
  private static final Color UNDERLINE_COLOR = new Color(0xcccccc);

  private final int[] supportedBaudRates;
  private final int[] dataBitsPerWord;
  private final int[] supportedNumberOfStopBits;
  private final int[] supportedParity;

  private JComboBox<String> portCombo;
  private JComboBox<String> baudCombo;
  private int[] baudValues;
  private JComboBox<String> dataBitsCombo;
  private int[] dataBitsValues;
  private JComboBox<String> stopBitsCombo;
  private int[] stopBitsValues;
  private JComboBox<String> parityCombo;
  private int[] parityValues;

  private SerialPortHandle openPort;

  /**
   * Creates a serial port panel with the given parameter sets. Each array must have at least one
   * element; otherwise {@link IllegalArgumentException} is thrown. Parity values must be {@link
   * SerialPort#NO_PARITY}, {@link SerialPort#EVEN_PARITY}, {@link SerialPort#ODD_PARITY}, {@link
   * SerialPort#MARK_PARITY}, or {@link SerialPort#SPACE_PARITY}. Stop bit values must be {@link
   * SerialPort#ONE_STOP_BIT}, {@link SerialPort#ONE_POINT_FIVE_STOP_BITS}, or {@link
   * SerialPort#TWO_STOP_BITS}.
   */
  public SerialPortPanel(
      int[] supportedBaudRates,
      int[] dataBitsPerWord,
      int[] supportedNumberOfStopBits,
      int[] supportedParity) {
    if (supportedBaudRates == null || supportedBaudRates.length == 0) {
      throw new IllegalArgumentException("supportedBaudRates must not be null or empty");
    }
    if (dataBitsPerWord == null || dataBitsPerWord.length == 0) {
      throw new IllegalArgumentException("dataBitsPerWord must not be null or empty");
    }
    if (supportedNumberOfStopBits == null || supportedNumberOfStopBits.length == 0) {
      throw new IllegalArgumentException("supportedNumberOfStopBits must not be null or empty");
    }
    if (supportedParity == null || supportedParity.length == 0) {
      throw new IllegalArgumentException("supportedParity must not be null or empty");
    }
    this.supportedBaudRates = supportedBaudRates.clone();
    this.dataBitsPerWord = dataBitsPerWord.clone();
    this.supportedNumberOfStopBits = supportedNumberOfStopBits.clone();
    this.supportedParity = supportedParity.clone();

    setBackground(Color.WHITE);
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

    JPanel row1 = createHeader();
    add(row1, BorderLayout.CENTER);
  }

  private JPanel createHeader() {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
    row.setBackground(Color.WHITE);
    row.add(new JLabel("Port:"));
    row.add(Box.createHorizontalStrut(6));
    portCombo = new JComboBox<>();
    portCombo.setPrototypeDisplayValue("COM99 (Descriptive Port Name)");
    portCombo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UNDERLINE_COLOR));
    portCombo.setRenderer(createCenterRenderer());
    int portHeight = portCombo.getPreferredSize().height;
    portCombo.setPreferredSize(new Dimension(100, portHeight));
    portCombo.setMaximumSize(new Dimension(400, portHeight));
    refreshPorts();
    row.add(portCombo);
    row.add(Box.createHorizontalStrut(14));
    if (this.supportedBaudRates.length > 1) {
      row.add(new JLabel("Baud:"));
      row.add(Box.createHorizontalStrut(6));
      baudCombo = new JComboBox<>();
      baudValues = this.supportedBaudRates.clone();
      for (int baud : baudValues) {
        baudCombo.addItem(String.valueOf(baud));
      }
      baudCombo.setSelectedIndex(0);
      addUnderlineCombo(row, baudCombo, 72);
    } else {
      baudCombo = null;
      baudValues = null;
      row.add(new JLabel("Baud:"));
      row.add(Box.createHorizontalStrut(6));
      addUnderlineLabel(row, String.valueOf(this.supportedBaudRates[0]), 48);
    }
    row.add(Box.createHorizontalStrut(14));
    row.add(new JLabel("Data bits:"));
    row.add(Box.createHorizontalStrut(6));
    if (this.dataBitsPerWord.length > 1) {
      dataBitsCombo = new JComboBox<>();
      dataBitsValues = this.dataBitsPerWord.clone();
      for (int bits : dataBitsValues) {
        dataBitsCombo.addItem(String.valueOf(bits));
      }
      dataBitsCombo.setSelectedIndex(0);
      addUnderlineCombo(row, dataBitsCombo, 50);
    } else {
      dataBitsCombo = null;
      dataBitsValues = null;
      addUnderlineLabel(row, String.valueOf(this.dataBitsPerWord[0]), 24);
    }
    row.add(Box.createHorizontalStrut(14));
    row.add(new JLabel("Stop:"));
    row.add(Box.createHorizontalStrut(6));
    if (this.supportedNumberOfStopBits.length > 1) {
      stopBitsCombo = new JComboBox<>();
      stopBitsValues = this.supportedNumberOfStopBits.clone();
      for (int sb : stopBitsValues) {
        stopBitsCombo.addItem(stopBitsToDisplay(sb));
      }
      stopBitsCombo.setSelectedIndex(0);
      addUnderlineCombo(row, stopBitsCombo, 44);
    } else {
      stopBitsCombo = null;
      stopBitsValues = null;
      addUnderlineLabel(row, stopBitsToDisplay(this.supportedNumberOfStopBits[0]), 24);
    }
    row.add(Box.createHorizontalStrut(14));
    row.add(new JLabel("Parity:"));
    row.add(Box.createHorizontalStrut(6));
    if (this.supportedParity.length > 1) {
      parityCombo = new JComboBox<>();
      parityValues = this.supportedParity.clone();
      for (int p : parityValues) {
        parityCombo.addItem(parityToDisplay(p));
      }
      parityCombo.setSelectedIndex(0);
      addUnderlineCombo(row, parityCombo, 88);
    } else {
      parityCombo = null;
      parityValues = null;
      addUnderlineLabel(row, parityToDisplay(this.supportedParity[0]), 64);
    }
    row.add(Box.createHorizontalStrut(14));
    JButton connectButton = new JButton("Connect");
    connectButton.addActionListener(e -> onConnect());
    row.add(connectButton);

    return row;
  }

  private static DefaultListCellRenderer createCenterRenderer() {
    return new DefaultListCellRenderer() {
      @Override
      public java.awt.Component getListCellRendererComponent(
          javax.swing.JList<?> list,
          Object value,
          int index,
          boolean isSelected,
          boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setHorizontalAlignment(SwingConstants.CENTER);
        return this;
      }
    };
  }

  private void addUnderlineCombo(JPanel row, JComboBox<String> combo, int width) {
    combo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UNDERLINE_COLOR));
    combo.setRenderer(createCenterRenderer());
    int h = combo.getPreferredSize().height;
    combo.setPreferredSize(new Dimension(width, h));
    combo.setMaximumSize(new Dimension(width, h));
    row.add(combo);
  }

  private void addUnderlineLabel(JPanel row, String text, int width) {
    JLabel label = new JLabel(text);
    label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UNDERLINE_COLOR));
    label.setPreferredSize(new Dimension(width, 22));
    label.setMaximumSize(new Dimension(width, 22));
    row.add(label);
  }

  private static String parityToDisplay(int parity) {
    return switch (parity) {
      case SerialPort.NO_PARITY -> "No Parity";
      case SerialPort.EVEN_PARITY -> "Even";
      case SerialPort.ODD_PARITY -> "Odd";
      case SerialPort.MARK_PARITY -> "Mark";
      case SerialPort.SPACE_PARITY -> "Space";
      default -> String.valueOf(parity);
    };
  }

  private static String stopBitsToDisplay(int stopBits) {
    return switch (stopBits) {
      case SerialPort.ONE_STOP_BIT -> "1";
      case SerialPort.ONE_POINT_FIVE_STOP_BITS -> "1.5";
      case SerialPort.TWO_STOP_BITS -> "2";
      default -> String.valueOf(stopBits);
    };
  }

  /**
   * Sets the selected port by system port name (e.g. "COM3"). Call after {@link #refreshPorts()} if
   * needed. No-op if the port is not in the list.
   */
  public void setSelectedPort(String portName) {
    if (portName == null) return;
    for (int i = 0; i < portCombo.getItemCount(); i++) {
      String item = portCombo.getItemAt(i);
      if (item != null && (item.equals(portName) || item.startsWith(portName + " "))) {
        portCombo.setSelectedIndex(i);
        return;
      }
    }
  }

  /** Refreshes the list of available serial ports in the dropdown. */
  public void refreshPorts() {
    String selected = portCombo.getItemCount() > 0 ? (String) portCombo.getSelectedItem() : null;
    portCombo.removeAllItems();
    for (PortDescriptor desc : SerialPorts.getAvailablePorts()) {
      portCombo.addItem(desc.toDisplayString());
    }
    if (portCombo.getItemCount() == 0) {
      portCombo.addItem(NO_PORTS_PLACEHOLDER);
    }
    portCombo.addItem(DEMO_PORT);
    if (selected != null) {
      for (int i = 0; i < portCombo.getItemCount(); i++) {
        if (selected.equals(portCombo.getItemAt(i))) {
          portCombo.setSelectedIndex(i);
          break;
        }
      }
    }
  }

  private String getSelectedPortName() {
    String item = (String) portCombo.getSelectedItem();
    if (item == null || NO_PORTS_PLACEHOLDER.equals(item)) {
      return null;
    }
    if (DEMO_PORT.equals(item)) {
      return DEMO_PORT;
    }
    int paren = item.indexOf(' ');
    return paren > 0 ? item.substring(0, paren) : item;
  }

  private int getSelectedBaudRate() {
    if (supportedBaudRates.length == 1) {
      return supportedBaudRates[0];
    }
    return baudValues[baudCombo.getSelectedIndex()];
  }

  private int getSelectedDataBits() {
    if (dataBitsPerWord.length == 1) {
      return dataBitsPerWord[0];
    }
    return dataBitsValues[dataBitsCombo.getSelectedIndex()];
  }

  private int getSelectedStopBits() {
    if (supportedNumberOfStopBits.length == 1) {
      return supportedNumberOfStopBits[0];
    }
    return stopBitsValues[stopBitsCombo.getSelectedIndex()];
  }

  private int getSelectedParity() {
    if (supportedParity.length == 1) {
      return supportedParity[0];
    }
    return parityValues[parityCombo.getSelectedIndex()];
  }

  private static final String DEMO_PORT = "DemoPort";

  private void onConnect() {
    String portName = getSelectedPortName();
    if (portName == null) {
      JOptionPane.showMessageDialog(
          this, "Please select a serial port.", "Connect", JOptionPane.WARNING_MESSAGE);
      return;
    }
    if (openPort != null && openPort.isOpen()) {
      JOptionPane.showMessageDialog(
          this,
          "Port is already open. Close it before reconnecting.",
          "Connect",
          JOptionPane.WARNING_MESSAGE);
      return;
    }

    SerialPortHandle handle = SerialPorts.getHandle(portName);
    boolean paramsOk =
        handle.setComPortParameters(
            getSelectedBaudRate(),
            getSelectedDataBits(),
            getSelectedStopBits(),
            getSelectedParity());
    if (!paramsOk) {
      JOptionPane.showMessageDialog(
          this, "Failed to set port parameters.", "Connect", JOptionPane.ERROR_MESSAGE);
      return;
    }
    boolean opened = handle.openPort();
    if (opened) {
      openPort = handle;
      JOptionPane.showMessageDialog(
          this, "Connected to " + portName + ".", "Connect", JOptionPane.INFORMATION_MESSAGE);
    } else {
      JOptionPane.showMessageDialog(
          this, "Failed to open port " + portName + ".", "Connect", JOptionPane.ERROR_MESSAGE);
    }
  }
}
