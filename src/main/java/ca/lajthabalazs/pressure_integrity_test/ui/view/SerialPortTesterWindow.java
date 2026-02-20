package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.serial.PortDescriptor;
import ca.lajthabalazs.pressure_integrity_test.serial.SerialPorts;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * First-step window for serial port testing: user selects port, then clicks "Test Ruska" or "Test
 * Almemo" to open the appropriate device test window (parameters + Connect).
 */
public class SerialPortTesterWindow extends JFrame {

  private static final String NO_PORTS_PLACEHOLDER = "(No ports found)";
  private static final String NO_PORTS_MESSAGE = "No serial ports detected";
  private static final String DEMO_PORT = "DemoPort";

  private final Frame parent;
  private JComboBox<String> portCombo;
  private JButton testRuskaButton;
  private JButton testAlmemoButton;
  private JLabel noPortsLabel;

  public SerialPortTesterWindow(Frame parent) {
    super("Serial Port Tester");
    this.parent = parent;
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setSize(520, 180);
    if (parent != null) {
      setLocationRelativeTo(parent);
    } else {
      setLocationByPlatform(true);
    }

    JPanel content = new JPanel(new BorderLayout());
    content.setBackground(Color.WHITE);
    content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

    JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
    form.setBackground(Color.WHITE);

    form.add(new JLabel("Port:"));
    portCombo = new JComboBox<>();
    portCombo.setPrototypeDisplayValue("COM99 (Descriptive Port Name)");
    refreshPorts();
    form.add(portCombo);

    testRuskaButton = new JButton("Test Ruska");
    testRuskaButton.addActionListener(e -> onTestRuska());
    form.add(testRuskaButton);

    testAlmemoButton = new JButton("Test Almemo");
    testAlmemoButton.addActionListener(e -> onTestAlmemo());
    form.add(testAlmemoButton);

    content.add(form, BorderLayout.CENTER);

    noPortsLabel = new JLabel(NO_PORTS_MESSAGE);
    noPortsLabel.setForeground(Color.RED);
    JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
    messagePanel.setBackground(Color.WHITE);
    messagePanel.add(noPortsLabel);
    content.add(messagePanel, BorderLayout.SOUTH);

    getContentPane().add(content);

    updatePortsState();
  }

  private void refreshPorts() {
    String selected =
        portCombo != null && portCombo.getItemCount() > 0
            ? (String) portCombo.getSelectedItem()
            : null;
    if (portCombo != null) {
      portCombo.removeAllItems();
    } else {
      return;
    }
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
    if (testRuskaButton != null) {
      updatePortsState();
    }
  }

  private boolean hasPorts() {
    return portCombo.getItemCount() > 1
        || (portCombo.getItemCount() == 1 && !NO_PORTS_PLACEHOLDER.equals(portCombo.getItemAt(0)));
  }

  private void updatePortsState() {
    boolean enabled = hasPorts();
    testRuskaButton.setEnabled(enabled);
    testAlmemoButton.setEnabled(enabled);
    noPortsLabel.setVisible(!enabled);
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

  private void onTestRuska() {
    openDeviceWindow(true);
  }

  private void onTestAlmemo() {
    openDeviceWindow(false);
  }

  private void openDeviceWindow(boolean ruska) {
    String portName = getSelectedPortName();
    if (portName == null) {
      JOptionPane.showMessageDialog(
          this, "Please select a serial port.", "Serial Port Tester", JOptionPane.WARNING_MESSAGE);
      return;
    }
    SerialPortTestDeviceWindow deviceWindow =
        new SerialPortTestDeviceWindow(parent, portName, ruska);
    deviceWindow.setVisible(true);
  }

  /** Refreshes the list of available serial ports. Call when the window is shown if needed. */
  public void refreshPortsList() {
    refreshPorts();
  }
}
