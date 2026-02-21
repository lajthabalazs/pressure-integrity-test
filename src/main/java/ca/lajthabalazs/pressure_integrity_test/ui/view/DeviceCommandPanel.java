package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.command.Command;
import ca.lajthabalazs.pressure_integrity_test.serial.SerialPortHandle;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Panel for sending commands and plain text over a serial port, with a scrolling log of sent and
 * received messages. Usable for any device (e.g. Ruska, Almemo) by passing the appropriate command
 * list; use an empty list when no predefined commands exist.
 */
public class DeviceCommandPanel extends JPanel {

  private static final String PROMPT = ">> ";
  private static final String CONTINUATION_INDENT = "   ";
  private static final Color SENT_COLOR = new Color(0x55_55_55);
  private static final Color RECEIVED_COLOR = Color.BLACK;
  private static final String PLAIN_TEXT_WARNING =
      "Danger zone!\n\nCommands sent in plain text are not validated. "
          + "Only proceed if you know what you're doing!";

  private final SerialPortHandle handle;
  private final List<Command> commands;
  private final JComboBox<Command> commandCombo;
  private final JPanel paramPanel;
  private final JTextField paramField;
  private final JTextField plainTextField;
  private final JTextPane logPane;
  private final StyledDocument logDoc;
  private final AttributeSet sentStyle;
  private final AttributeSet receivedStyle;
  private volatile boolean readerRunning = true;
  private volatile boolean disconnectNotified;
  private boolean dontShowPlainTextWarningThisSession;

  /**
   * @param handle the connected serial port
   * @param commands list of commands for the dropdown (e.g. {@link
   *     ca.lajthabalazs.pressure_integrity_test.command.RuskaReadCommands#ALL}); may be empty
   */
  public DeviceCommandPanel(SerialPortHandle handle, List<Command> commands) {
    this.handle = handle;
    this.commands = List.copyOf(commands);
    setLayout(new BorderLayout(0, 8));

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    top.setBackground(Color.WHITE);

    top.add(new JLabel("Command:"));
    commandCombo = new JComboBox<>();
    paramPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    paramPanel.setBackground(Color.WHITE);
    paramPanel.add(new JLabel("Param:"));
    paramField = new JTextField(6);
    paramPanel.add(paramField);
    for (Command c : this.commands) {
      commandCombo.addItem(c);
    }
    commandCombo.setRenderer(
        (list, value, index, isSelected, cellHasFocus) -> {
          JLabel l = new JLabel(value == null ? "" : value.getDisplayName());
          if (isSelected) {
            l.setBackground(list.getSelectionBackground());
            l.setForeground(list.getSelectionForeground());
            l.setOpaque(true);
          }
          return l;
        });
    Command first = this.commands.isEmpty() ? null : this.commands.get(0);
    paramPanel.setVisible(first != null && !first.getParameters().isEmpty());
    commandCombo.addItemListener(
        e -> {
          Command c = (Command) commandCombo.getSelectedItem();
          paramPanel.setVisible(c != null && !c.getParameters().isEmpty());
          if (c != null && !c.getParameters().isEmpty()) {
            paramField.setText("");
          }
        });
    top.add(commandCombo);
    top.add(paramPanel);

    JButton sendButton = new JButton("Send");
    sendButton.addActionListener(e -> onSend());
    top.add(sendButton);

    JPanel plainTextRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    plainTextRow.setBackground(Color.WHITE);
    plainTextRow.add(new JLabel("Plain text:"));
    plainTextField = new JTextField(30);
    plainTextRow.add(plainTextField);
    JButton sendPlainButton = new JButton("Send plain text");
    sendPlainButton.addActionListener(e -> onSendPlainText());
    plainTextRow.add(sendPlainButton);

    JPanel northCol = new JPanel(new BorderLayout(0, 4));
    northCol.setBackground(Color.WHITE);
    northCol.add(top, BorderLayout.NORTH);
    northCol.add(plainTextRow, BorderLayout.CENTER);
    add(northCol, BorderLayout.NORTH);

    logPane = new JTextPane();
    logPane.setEditable(false);
    logPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    logDoc = logPane.getStyledDocument();

    SimpleAttributeSet sent = new SimpleAttributeSet();
    StyleConstants.setForeground(sent, SENT_COLOR);
    StyleConstants.setItalic(sent, true);
    StyleConstants.setFontFamily(sent, Font.MONOSPACED);
    StyleConstants.setFontSize(sent, 12);
    this.sentStyle = sent;

    SimpleAttributeSet recv = new SimpleAttributeSet();
    StyleConstants.setForeground(recv, RECEIVED_COLOR);
    StyleConstants.setFontFamily(recv, Font.MONOSPACED);
    StyleConstants.setFontSize(recv, 12);
    this.receivedStyle = recv;

    add(new JScrollPane(logPane), BorderLayout.CENTER);

    startReaderThread();
  }

  private static String formatMessageForDisplay(String raw) {
    if (raw == null) return PROMPT;
    String[] lines = raw.split("\n", -1);
    StringBuilder sb = new StringBuilder();
    sb.append(PROMPT).append(lines[0]);
    for (int i = 1; i < lines.length; i++) {
      sb.append("\n").append(CONTINUATION_INDENT).append(lines[i]);
    }
    return sb.toString();
  }

  private void appendToLog(String text, AttributeSet style) {
    SwingUtilities.invokeLater(
        () -> {
          try {
            logDoc.insertString(logDoc.getLength(), text + "\n", style);
            logPane.setCaretPosition(logDoc.getLength());
          } catch (BadLocationException ignored) {
          }
        });
  }

  private void onSend() {
    Command command = (Command) commandCombo.getSelectedItem();
    if (command == null) return;

    List<String> rawParams =
        command.getParameters().isEmpty() ? List.of() : List.of(paramField.getText());
    Optional<String> messageOpt = command.buildMessage(rawParams);
    if (messageOpt.isEmpty()) {
      JOptionPane.showMessageDialog(
          this, "Invalid parameter value.", "Send", JOptionPane.WARNING_MESSAGE);
      return;
    }
    String message = messageOpt.get();
    sendMessage(message);
  }

  private void onSendPlainText() {
    String message = plainTextField.getText();
    if (dontShowPlainTextWarningThisSession) {
      sendMessage(message);
      return;
    }
    boolean[] dontShowAgainOut = new boolean[1];
    if (showPlainTextWarningDialog(dontShowAgainOut)) {
      if (dontShowAgainOut[0]) {
        dontShowPlainTextWarningThisSession = true;
      }
      sendMessage(message);
    }
  }

  private boolean showPlainTextWarningDialog(boolean[] dontShowAgainOut) {
    JCheckBox understandBox = new JCheckBox("I understand.");
    JCheckBox notShowAgainBox = new JCheckBox("Don't show this dialog box again in this session.");
    JButton dialogSendButton = new JButton("Send");
    JButton cancelButton = new JButton("Cancel");
    dialogSendButton.setEnabled(false);
    understandBox.addActionListener(e -> dialogSendButton.setEnabled(understandBox.isSelected()));
    JDialog dialog =
        new JDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this), "Plain text", true);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    JPanel content = new JPanel(new BorderLayout(12, 12));
    content.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 20, 16, 20));
    content.add(
        new JLabel("<html>" + PLAIN_TEXT_WARNING.replace("\n", "<br>") + "</html>"),
        BorderLayout.NORTH);
    JPanel checkboxes = new JPanel(new java.awt.GridLayout(2, 1, 0, 6));
    checkboxes.add(understandBox);
    checkboxes.add(notShowAgainBox);
    content.add(checkboxes, BorderLayout.CENTER);
    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    final boolean[] sendClicked = new boolean[1];
    dialogSendButton.addActionListener(
        e -> {
          sendClicked[0] = true;
          dontShowAgainOut[0] = notShowAgainBox.isSelected();
          dialog.setVisible(false);
          dialog.dispose();
        });
    cancelButton.addActionListener(
        e -> {
          dialog.setVisible(false);
          dialog.dispose();
        });
    buttons.add(cancelButton);
    buttons.add(dialogSendButton);
    content.add(buttons, BorderLayout.SOUTH);
    dialog.getContentPane().add(content);
    dialog.pack();
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);
    return sendClicked[0];
  }

  private void sendMessage(String message) {
    String toSend = message + "\r";
    appendToLog(formatMessageForDisplay(message), sentStyle);
    try {
      handle.getOutputStream().write(toSend.getBytes(StandardCharsets.UTF_8));
      handle.getOutputStream().flush();
    } catch (IOException e) {
      appendToLog(formatMessageForDisplay("Error: " + e.getMessage()), receivedStyle);
    }
  }

  private void notifyDisconnected() {
    if (disconnectNotified) return;
    disconnectNotified = true;
    SwingUtilities.invokeLater(
        () -> {
          appendToLog(formatMessageForDisplay("[Device disconnected]"), receivedStyle);
          JOptionPane.showMessageDialog(
              this,
              "The device has been disconnected.",
              "Device disconnected",
              JOptionPane.WARNING_MESSAGE);
        });
  }

  private void startReaderThread() {
    Thread t =
        new Thread(
            () -> {
              byte[] buf = new byte[4096];
              InputStream in = handle.getInputStream();
              StringBuilder line = new StringBuilder();
              while (readerRunning && in != null) {
                try {
                  int n = in.read(buf);
                  if (n <= 0) {
                    if (n < 0) {
                      if (readerRunning) notifyDisconnected();
                      break;
                    }
                    continue;
                  }
                  String chunk = new String(buf, 0, n, StandardCharsets.UTF_8);
                  line.append(chunk);
                  int cr;
                  while ((cr = line.indexOf("\r")) >= 0 || (cr = line.indexOf("\n")) >= 0) {
                    String one = line.substring(0, cr).trim();
                    line.delete(0, cr + 1);
                    if (!one.isEmpty()) {
                      appendToLog(formatMessageForDisplay(one), receivedStyle);
                    }
                  }
                } catch (IOException e) {
                  if (readerRunning) {
                    notifyDisconnected();
                  }
                  break;
                }
              }
            },
            "SerialPortReader");
    t.setDaemon(true);
    t.start();
  }

  /** Call when the panel is no longer used (e.g. window closed) to stop the reader thread. */
  public void dispose() {
    readerRunning = false;
  }
}
