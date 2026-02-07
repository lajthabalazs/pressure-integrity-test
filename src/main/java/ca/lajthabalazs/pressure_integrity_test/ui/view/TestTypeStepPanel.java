package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.NewTestWizardViewModel;
import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.StageConfig;
import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.TestType;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
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
  private final Box stagesPanel;
  private TestType lastRebuiltTestType;

  TestTypeStepPanel(NewTestWizardViewModel viewModel) {
    this.viewModel = viewModel;
    setBackground(Color.WHITE);
    setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));

    JPanel typeRow = new JPanel();
    typeRow.setLayout(new javax.swing.BoxLayout(typeRow, javax.swing.BoxLayout.X_AXIS));
    typeRow.setBackground(Color.WHITE);
    typeRow.add(new JLabel("Test type:"));
    typeRow.add(Box.createHorizontalStrut(8));
    typeCombo = new JComboBox<>(TestType.values());
    typeCombo.setPrototypeDisplayValue(TestType.EITV);
    typeCombo.setMaximumSize(typeCombo.getPreferredSize());
    typeCombo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UNDERLINE_COLOR));
    typeCombo.addActionListener(e -> viewModel.setTestType((TestType) typeCombo.getSelectedItem()));
    typeRow.add(typeCombo);
    typeRow.add(Box.createHorizontalGlue());
    typeRow.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
    typeRow.setMaximumSize(typeRow.getPreferredSize());
    add(typeRow);

    add(Box.createVerticalStrut(12));

    stagesHeaderLabel = new JLabel("Stages");
    stagesHeaderLabel.setFont(stagesHeaderLabel.getFont().deriveFont(Font.BOLD, 14f));
    stagesHeaderLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
    stagesHeaderLabel.setMaximumSize(stagesHeaderLabel.getPreferredSize());
    add(stagesHeaderLabel);

    add(Box.createVerticalStrut(4));

    stagesPanel = Box.createVerticalBox();
    stagesPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
    add(stagesPanel);

    add(Box.createVerticalGlue());

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

    for (int i = 0; i < stages.size(); i++) {
      final int stageIndex = i;
      StageConfig config = stages.get(i);

      Box stageLine = Box.createHorizontalBox();
      stageLine.add(new JLabel("Stage " + (i + 1) + ":"));
      stageLine.add(Box.createHorizontalStrut(6));
      stageLine.add(new JLabel("Overpressure (bar):"));

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

      stageLine.add(overpressureField);
      stageLine.add(Box.createHorizontalStrut(6));
      stageLine.add(new JLabel("Duration:"));
      stageLine.add(hoursField);
      stageLine.add(new JLabel("h"));
      stageLine.add(minutesCombo);
      stageLine.add(new JLabel("min"));
      stageLine.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
      stageLine.setMaximumSize(stageLine.getPreferredSize());
      stagesPanel.add(stageLine);
      if (i < stages.size() - 1) {
        stagesPanel.add(Box.createVerticalStrut(6));
      }
    }

    stagesPanel.setMaximumSize(stagesPanel.getPreferredSize());

    stagesPanel.revalidate();
    stagesPanel.repaint();
  }
}
