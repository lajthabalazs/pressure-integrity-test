package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.NewTestWizardViewModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

class CalibrationStepPanel extends JPanel {

  private final NewTestWizardViewModel wizardViewModel;
  private final JCheckBox requiresCalibrationCheckBox;
  private final FileChooserPanel fileChooserPanel;
  private final JPanel contentAreaPanel;
  private final JTextArea contentTextArea;

  CalibrationStepPanel(NewTestWizardViewModel wizardViewModel) {
    this.wizardViewModel = wizardViewModel;
    setBackground(Color.WHITE);
    setLayout(new BorderLayout(0, 10));

    var step = wizardViewModel.getCalibrationStep();

    JPanel topPanel = new JPanel(new BorderLayout(5, 5));
    topPanel.setBackground(Color.WHITE);

    requiresCalibrationCheckBox =
        new JCheckBox("Requires calibration file", step.isRequiresCalibrationFile());
    requiresCalibrationCheckBox.setBackground(Color.WHITE);
    requiresCalibrationCheckBox.addActionListener(
        e -> {
          step.setRequiresCalibrationFile(requiresCalibrationCheckBox.isSelected());
          updateFromViewModel();
        });
    topPanel.add(requiresCalibrationCheckBox, BorderLayout.NORTH);

    fileChooserPanel =
        new FileChooserPanel(
            "Choose calibration file",
            step::getRootDirectory,
            file -> {
              step.setSelectedFile(file);
              updateFromViewModel();
            },
            new FileNameExtensionFilter("JSON files", "json"),
            false);
    topPanel.add(fileChooserPanel, BorderLayout.CENTER);
    add(topPanel, BorderLayout.NORTH);

    JLabel contentLabel = new JLabel("Calibration file content:");
    contentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    contentAreaPanel = new JPanel(new BorderLayout(5, 5));
    contentAreaPanel.setBackground(Color.WHITE);
    contentAreaPanel.add(contentLabel, BorderLayout.NORTH);

    contentTextArea = new JTextArea(12, 40);
    contentTextArea.setEditable(false);
    contentTextArea.setBackground(Color.WHITE);
    contentTextArea.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xcccccc)));
    contentTextArea.setFont(contentTextArea.getFont().deriveFont(12f));
    JScrollPane contentScrollPane = new JScrollPane(contentTextArea);
    contentScrollPane.setBorder(null);
    contentScrollPane.getViewport().setBackground(Color.WHITE);
    contentAreaPanel.add(contentScrollPane, BorderLayout.CENTER);
    add(contentAreaPanel, BorderLayout.CENTER);

    updateFromViewModel();
  }

  void setEditable(boolean editable) {
    requiresCalibrationCheckBox.setEnabled(editable);
    fileChooserPanel.setEditable(editable);
  }

  void updateFromViewModel() {
    var step = wizardViewModel.getCalibrationStep();
    boolean updating = requiresCalibrationCheckBox.isSelected() != step.isRequiresCalibrationFile();
    if (updating) {
      requiresCalibrationCheckBox.setSelected(step.isRequiresCalibrationFile());
    }
    boolean showFileAndContent = step.isRequiresCalibrationFile();
    fileChooserPanel.setVisible(showFileAndContent);
    contentAreaPanel.setVisible(showFileAndContent);
    if (showFileAndContent) {
      fileChooserPanel.setDisplayedFile(step.getSelectedFile());
      String content;
      if (step.getLoadError() != null) {
        content = "Error: " + step.getLoadError();
      } else {
        content = step.getCalibrationContentSummary();
      }
      contentTextArea.setText(content);
      contentTextArea.setCaretPosition(0);
    }
  }
}
