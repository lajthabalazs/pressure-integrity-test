package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.NewTestWizardViewModel;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

class SiteSelectionStepPanel extends JPanel {

  private final NewTestWizardViewModel viewModel;
  private final JTextField filePathField;
  private final JButton browseButton;

  SiteSelectionStepPanel(NewTestWizardViewModel viewModel) {
    this.viewModel = viewModel;
    setBackground(Color.WHITE);
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1;

    filePathField = new JTextField(40);
    filePathField.setEditable(false);
    filePathField.setBackground(Color.WHITE);
    filePathField.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xcccccc)));
    gbc.gridx = 0;
    gbc.gridy = 0;
    add(filePathField, gbc);

    browseButton = new JButton("Browse...");
    browseButton.addActionListener(e -> browseForFile());
    gbc.gridx = 1;
    gbc.weightx = 0;
    add(browseButton, gbc);

    updateFromViewModel();
  }

  void setEditable(boolean editable) {
    browseButton.setEnabled(editable);
  }

  void updateFromViewModel() {
    File file = viewModel.getSelectedFile();
    filePathField.setText(file != null ? file.getAbsolutePath() : "");
  }

  private void browseForFile() {
    JFileChooser chooser = new JFileChooser();
    if (viewModel.getSelectedFile() != null) {
      chooser.setCurrentDirectory(viewModel.getSelectedFile().getParentFile());
    }
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      viewModel.setSelectedFile(chooser.getSelectedFile());
      updateFromViewModel();
    }
  }
}
