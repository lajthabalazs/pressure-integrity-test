package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.NewTestWizardViewModel;
import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.StageConfig;
import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.TestType;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

class TestTypeStepPanel extends JPanel {

  private static final int[] MINUTE_OPTIONS = {0, 10, 20, 30, 40, 50};
  private static final double MIN_OVERPRESSURE = 0;
  private static final double MAX_OVERPRESSURE = 2;

  private final NewTestWizardViewModel viewModel;
  private final JComboBox<TestType> typeCombo;
  private final JLabel stagesHeaderLabel;
  private final JPanel stagesPanel;
  private TestType lastRebuiltTestType;

  TestTypeStepPanel(NewTestWizardViewModel viewModel) {
    this.viewModel = viewModel;
    setBackground(Color.WHITE);
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    JLabel typeLabel = new JLabel("Test type:");
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbc.weightx = 0;
    add(typeLabel, gbc);

    typeCombo = new JComboBox<>(TestType.values());
    typeCombo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UNDERLINE_COLOR));
    typeCombo.addActionListener(e -> viewModel.setTestType((TestType) typeCombo.getSelectedItem()));
    gbc.gridx = 1;
    gbc.weightx = 1;
    add(typeCombo, gbc);

    stagesHeaderLabel = new JLabel("Stages");
    stagesHeaderLabel.setFont(stagesHeaderLabel.getFont().deriveFont(Font.BOLD, 14f));
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 2;
    gbc.weightx = 1;
    gbc.insets = new Insets(12, 5, 4, 5);
    add(stagesHeaderLabel, gbc);

    stagesPanel = new JPanel(new GridBagLayout());
    stagesPanel.setBackground(Color.WHITE);
    stagesPanel.setBorder(null);
    gbc.insets = new Insets(0, 5, 5, 5);
    gbc.gridy = 2;
    gbc.weighty = 1;
    gbc.fill = GridBagConstraints.BOTH;
    add(stagesPanel, gbc);

    updateFromViewModel();
  }

  void setEditable(boolean editable) {
    typeCombo.setEnabled(editable);
    setChildrenEnabled(stagesPanel, editable);
  }

  private static void setChildrenEnabled(java.awt.Container container, boolean enabled) {
    for (java.awt.Component c : container.getComponents()) {
      c.setEnabled(enabled);
      if (c instanceof java.awt.Container cont) {
        setChildrenEnabled(cont, enabled);
      }
    }
  }

  void updateFromViewModel() {
    TestType type = viewModel.getTestType();
    if (type != null) {
      typeCombo.setSelectedItem(type);
    }

    if (type != lastRebuiltTestType) {
      lastRebuiltTestType = type;
      rebuildStagesPanel();
    }
  }

  private static final java.awt.Color UNDERLINE_COLOR = new java.awt.Color(0xcccccc);

  private void rebuildStagesPanel() {
    stagesPanel.removeAll();
    if (viewModel.getTestType() == null) {
      stagesPanel.revalidate();
      stagesPanel.repaint();
      return;
    }

    var stages = viewModel.getStages();
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.anchor = GridBagConstraints.WEST;

    for (int i = 0; i < stages.size(); i++) {
      final int stageIndex = i;
      StageConfig config = stages.get(i);

      JLabel stageLabel = new JLabel("Stage " + (i + 1) + ":");
      gbc.gridx = 0;
      gbc.gridy = i;
      gbc.gridwidth = 1;
      stagesPanel.add(stageLabel, gbc);

      JPanel stageRow = new JPanel();
      stageRow.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
      stageRow.setBackground(Color.WHITE);
      stageRow.add(new JLabel("Overpressure (bar):"));

      String overpressureDisplay =
          (config.overpressureBar() == 0 && config.durationMinutes() == 0)
              ? ""
              : String.valueOf(config.overpressureBar()).replace('.', ',');
      DecimalTextField overpressureField = new DecimalTextField(overpressureDisplay, 4);
      overpressureField.setPreferredSize(new java.awt.Dimension(80, 25));

      int hours = config.getDurationHours();
      int minutes = config.getDurationMinutesRemainder();
      String hoursDisplay =
          (config.overpressureBar() == 0 && config.durationMinutes() == 0)
              ? ""
              : String.valueOf(hours);
      DecimalTextField hoursField = new DecimalTextField(hoursDisplay, 3, false);
      hoursField.setPreferredSize(new java.awt.Dimension(50, 25));

      Integer[] minuteChoices = new Integer[MINUTE_OPTIONS.length];
      for (int j = 0; j < MINUTE_OPTIONS.length; j++) {
        minuteChoices[j] = MINUTE_OPTIONS[j];
      }
      JComboBox<Integer> minutesCombo = new JComboBox<>(minuteChoices);
      minutesCombo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UNDERLINE_COLOR));
      minutesCombo.setSelectedItem(minutes);

      Runnable updateStage =
          () -> {
            String overpressureText = overpressureField.getValueForParsing();
            if (overpressureText.isEmpty()) {
              return;
            }
            double overpressure;
            try {
              double val = Double.parseDouble(overpressureText);
              overpressure = Math.max(MIN_OVERPRESSURE, Math.min(MAX_OVERPRESSURE, val));
            } catch (NumberFormatException e) {
              return;
            }
            String hoursText = hoursField.getValueForParsing();
            if (hoursText.isEmpty()) {
              return;
            }
            int h;
            try {
              int val = Integer.parseInt(hoursText);
              h = Math.max(0, Math.min(24, val));
            } catch (NumberFormatException e) {
              return;
            }
            int m = (Integer) minutesCombo.getSelectedItem();
            viewModel.setStage(stageIndex, new StageConfig(overpressure, h * 60 + m));
          };

      javax.swing.event.DocumentListener docListener =
          new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
              updateStage.run();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
              updateStage.run();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
              updateStage.run();
            }
          };
      overpressureField.getDocument().addDocumentListener(docListener);
      hoursField.getDocument().addDocumentListener(docListener);
      minutesCombo.addActionListener(e -> updateStage.run());

      stageRow.add(overpressureField);
      stageRow.add(new JLabel("Duration:"));
      stageRow.add(hoursField);
      stageRow.add(new JLabel("h"));
      stageRow.add(minutesCombo);
      stageRow.add(new JLabel("min"));

      gbc.gridx = 1;
      stagesPanel.add(stageRow, gbc);
    }

    stagesPanel.revalidate();
    stagesPanel.repaint();
  }
}
