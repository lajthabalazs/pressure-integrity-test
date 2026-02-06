package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.NewTestWizardViewModel;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

class SiteSelectionStepPanel extends JPanel {

  private final NewTestWizardViewModel viewModel;
  private final JLabel filePathLabel;
  private final JButton chooseButton;

  SiteSelectionStepPanel(NewTestWizardViewModel viewModel) {
    this.viewModel = viewModel;
    setBackground(Color.WHITE);
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    chooseButton = new JButton("Choose site config");
    chooseButton.addActionListener(e -> chooseSiteConfig());
    chooseButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(chooseButton);

    add(Box.createVerticalStrut(10));

    filePathLabel = new JLabel();
    filePathLabel.setForeground(new Color(0x666666));
    filePathLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(filePathLabel);

    updateFromViewModel();
  }

  void setEditable(boolean editable) {
    chooseButton.setEnabled(editable);
  }

  void updateFromViewModel() {
    File file = viewModel.getSelectedFile();
    if (file != null) {
      filePathLabel.setText(file.getAbsolutePath());
      filePathLabel.setVisible(true);
    } else {
      filePathLabel.setText("");
      filePathLabel.setVisible(false);
    }
  }

  private void chooseSiteConfig() {
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
