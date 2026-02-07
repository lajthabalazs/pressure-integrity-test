package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.NewTestWizardViewModel;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

class DataInterfaceStepPanel extends JPanel {

  private final NewTestWizardViewModel wizardViewModel;
  private final JTextArea textArea;
  private boolean isUpdatingFromViewModel;

  DataInterfaceStepPanel(NewTestWizardViewModel wizardViewModel) {
    this.wizardViewModel = wizardViewModel;
    setBackground(Color.WHITE);
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1;
    gbc.weighty = 1;

    add(new JLabel("Data interface configuration:"), gbc);

    textArea = new JTextArea(10, 40);
    textArea.setBackground(Color.WHITE);
    textArea.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xcccccc)));
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea
        .getDocument()
        .addDocumentListener(
            new javax.swing.event.DocumentListener() {
              @Override
              public void insertUpdate(javax.swing.event.DocumentEvent e) {
                syncToViewModel();
              }

              @Override
              public void removeUpdate(javax.swing.event.DocumentEvent e) {
                syncToViewModel();
              }

              @Override
              public void changedUpdate(javax.swing.event.DocumentEvent e) {
                syncToViewModel();
              }
            });
    gbc.gridy = 1;
    add(new JScrollPane(textArea), gbc);

    updateFromViewModel();
  }

  private void syncToViewModel() {
    if (!isUpdatingFromViewModel) {
      wizardViewModel.getDataInterfaceStep().setDataInterfaceText(textArea.getText());
    }
  }

  void setEditable(boolean editable) {
    textArea.setEditable(editable);
  }

  void updateFromViewModel() {
    String text = wizardViewModel.getDataInterfaceStep().getDataInterfaceText();
    javax.swing.SwingUtilities.invokeLater(
        () -> {
          if (text.equals(textArea.getText())) {
            return;
          }
          isUpdatingFromViewModel = true;
          try {
            textArea.setText(text);
          } finally {
            isUpdatingFromViewModel = false;
          }
        });
  }
}
