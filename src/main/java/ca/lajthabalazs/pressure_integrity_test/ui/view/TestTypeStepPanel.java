package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.ui.UIColors;
import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.NewTestWizardViewModel;
import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.StageConfig;
import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.TestType;
import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.TestTypeStepViewModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

class TestTypeStepPanel extends JPanel {

  private static final int[] MINUTE_OPTIONS = {0, 10, 20, 30, 40, 50};

  private final NewTestWizardViewModel wizardViewModel;
  private final JComboBox<TestType> typeCombo;
  private final JLabel stagesHeaderLabel;
  private final Box stagesPanel;
  private TestType lastRebuiltTestType;
  private final List<DecimalTextField> overpressureFields = new ArrayList<>();
  private final List<DecimalTextField> durationHourFields = new ArrayList<>();

  TestTypeStepPanel(NewTestWizardViewModel wizardViewModel) {
    this.wizardViewModel = wizardViewModel;
    setBackground(Color.WHITE);
    setLayout(new BorderLayout());

    JPanel typeRow = new JPanel();
    typeRow.setLayout(new javax.swing.BoxLayout(typeRow, javax.swing.BoxLayout.X_AXIS));
    typeRow.setBackground(Color.WHITE);
    typeRow.add(new JLabel("Test type:"));
    typeRow.add(Box.createHorizontalStrut(8));
    typeCombo = new JComboBox<>(TestType.values());
    typeCombo.setPrototypeDisplayValue(TestType.IITV);
    typeCombo.setPreferredSize(new java.awt.Dimension(180, typeCombo.getPreferredSize().height));
    typeCombo.setMaximumSize(typeCombo.getPreferredSize());
    typeCombo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UNDERLINE_COLOR));
    typeCombo.setRenderer(
        new DefaultListCellRenderer() {
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
        });
    typeCombo.addActionListener(
        e -> wizardViewModel.getTestTypeStep().setTestType((TestType) typeCombo.getSelectedItem()));
    typeRow.add(typeCombo);
    typeRow.add(Box.createHorizontalGlue());
    typeRow.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
    typeRow.setMaximumSize(typeRow.getPreferredSize());

    stagesHeaderLabel = new JLabel("Stages");
    stagesHeaderLabel.setFont(stagesHeaderLabel.getFont().deriveFont(Font.BOLD, 14f));
    stagesHeaderLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
    stagesHeaderLabel.setMaximumSize(stagesHeaderLabel.getPreferredSize());

    JPanel northPanel = new JPanel();
    northPanel.setLayout(new javax.swing.BoxLayout(northPanel, javax.swing.BoxLayout.Y_AXIS));
    northPanel.setBackground(Color.WHITE);
    northPanel.add(typeRow);
    northPanel.add(Box.createVerticalStrut(12));
    northPanel.add(stagesHeaderLabel);
    northPanel.add(Box.createVerticalStrut(12));
    add(northPanel, BorderLayout.NORTH);

    stagesPanel = Box.createVerticalBox();
    stagesPanel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
    JScrollPane scrollPane = new JScrollPane(stagesPanel);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setBackground(Color.WHITE);
    scrollPane.getViewport().setBackground(Color.WHITE);
    add(scrollPane, BorderLayout.CENTER);

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

  /** Returns true if any overpressure or duration field has an invalid value. */
  boolean hasInvalidFields() {
    for (DecimalTextField f : overpressureFields) {
      if (!f.isValidValue()) {
        return true;
      }
    }
    for (DecimalTextField f : durationHourFields) {
      if (!f.isValidValue()) {
        return true;
      }
    }
    return false;
  }

  void updateFromViewModel() {
    var step = wizardViewModel.getTestTypeStep();
    TestType type = step.getTestType();
    if (type != null) {
      typeCombo.setSelectedItem(type);
    }

    if (type != lastRebuiltTestType) {
      lastRebuiltTestType = type;
      rebuildStagesPanel();
    }
    updateOverpressureBorders();
  }

  private void updateOverpressureBorders() {
    double maxBar = getMaxOverpressureBarForValidation();
    var stages = wizardViewModel.getTestTypeStep().getStages();
    for (int i = 0; i < overpressureFields.size() && i < stages.size(); i++) {
      DecimalTextField field = overpressureFields.get(i);
      field.setValidRange(TestTypeStepViewModel.MIN_OVERPRESSURE_BAR, maxBar);
      double bar = stages.get(i).overpressureBar();
      BigDecimal siteMax = getSiteConfigMaxOverpressureBar();
      boolean exceeds = siteMax != null && bar > siteMax.doubleValue();
      field.setBorder(
          exceeds
              ? BorderFactory.createLineBorder(UIColors.ERROR_MARK_COLOR, 2)
              : BorderFactory.createMatteBorder(0, 0, 1, 0, UNDERLINE_COLOR));
    }
  }

  private double getMaxOverpressureBarForValidation() {
    BigDecimal siteMax = getSiteConfigMaxOverpressureBar();
    return siteMax != null ? siteMax.doubleValue() : TestTypeStepViewModel.MAX_OVERPRESSURE_BAR;
  }

  private BigDecimal getSiteConfigMaxOverpressureBar() {
    var config = wizardViewModel.getSiteSelectionStep().getSiteConfig();
    if (config == null || config.getDesignPressure() == null) {
      return null;
    }
    return config.getDesignPressure().getOverpressure_bar();
  }

  private static final java.awt.Color UNDERLINE_COLOR = new java.awt.Color(0xcccccc);

  private void rebuildStagesPanel() {
    stagesPanel.removeAll();
    overpressureFields.clear();
    durationHourFields.clear();
    var step = wizardViewModel.getTestTypeStep();
    if (step.getTestType() == null) {
      stagesPanel.revalidate();
      stagesPanel.repaint();
      return;
    }

    var stages = step.getStages();

    final int stageLabelWidth = 56;
    final int columnGap = 12;

    for (int i = 0; i < stages.size(); i++) {
      final int stageIndex = i;
      StageConfig config = stages.get(i);

      JLabel stageLabel = new JLabel("Stage " + (i + 1) + ":");
      stageLabel.setFont(stageLabel.getFont().deriveFont(Font.BOLD));
      stageLabel.setPreferredSize(
          new java.awt.Dimension(stageLabelWidth, stageLabel.getPreferredSize().height));
      stageLabel.setAlignmentY(java.awt.Component.TOP_ALIGNMENT);

      String overpressureDisplay =
          (config.overpressureBar() == 0 && config.durationMinutes() == 0)
              ? ""
              : String.valueOf(config.overpressureBar()).replace('.', ',');
      DecimalTextField overpressureField = new DecimalTextField(overpressureDisplay, 4);
      overpressureField.setPreferredSize(new java.awt.Dimension(80, 25));
      overpressureField.setValidRange(
          TestTypeStepViewModel.MIN_OVERPRESSURE_BAR, getMaxOverpressureBarForValidation());
      overpressureFields.add(overpressureField);

      Box pressureRow = Box.createHorizontalBox();
      pressureRow.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
      pressureRow.add(new JLabel("Overpressure (bar):"));
      pressureRow.add(Box.createHorizontalStrut(6));
      pressureRow.add(overpressureField);

      int hours = config.getDurationHours();
      int minutes = config.getDurationMinutesRemainder();
      String hoursDisplay =
          (config.overpressureBar() == 0 && config.durationMinutes() == 0)
              ? ""
              : String.valueOf(hours);
      DecimalTextField hoursField = new DecimalTextField(hoursDisplay, 3, false);
      hoursField.setPreferredSize(new java.awt.Dimension(50, 25));
      hoursField.setValidRange(0.0, 24.0);
      durationHourFields.add(hoursField);

      Integer[] minuteChoices = new Integer[MINUTE_OPTIONS.length];
      for (int j = 0; j < MINUTE_OPTIONS.length; j++) {
        minuteChoices[j] = MINUTE_OPTIONS[j];
      }
      JComboBox<Integer> minutesCombo = new JComboBox<>(minuteChoices);
      minutesCombo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UNDERLINE_COLOR));
      minutesCombo.setRenderer(
          new DefaultListCellRenderer() {
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
          });
      minutesCombo.setSelectedItem(minutes);

      Box timeRow = Box.createHorizontalBox();
      timeRow.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
      timeRow.add(new JLabel("Duration:"));
      timeRow.add(Box.createHorizontalStrut(6));
      timeRow.add(hoursField);
      timeRow.add(new JLabel("h"));
      timeRow.add(minutesCombo);
      timeRow.add(new JLabel("min"));

      Box rightColumn = Box.createVerticalBox();
      rightColumn.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
      rightColumn.setAlignmentY(java.awt.Component.TOP_ALIGNMENT);
      pressureRow.setMaximumSize(pressureRow.getPreferredSize());
      rightColumn.add(pressureRow);
      rightColumn.add(Box.createVerticalStrut(4));
      timeRow.setMaximumSize(timeRow.getPreferredSize());
      rightColumn.add(timeRow);

      Box stageRow = Box.createHorizontalBox();
      stageRow.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
      stageRow.add(stageLabel);
      stageRow.add(Box.createHorizontalStrut(columnGap));
      stageRow.add(rightColumn);
      stageRow.setMaximumSize(stageRow.getPreferredSize());

      Runnable updateStage =
          () -> {
            wizardViewModel
                .getTestTypeStep()
                .tryUpdateStageFromStrings(
                    stageIndex,
                    overpressureField.getValueForParsing(),
                    hoursField.getValueForParsing(),
                    (Integer) minutesCombo.getSelectedItem());
            updateOverpressureBorders();
            // Defer so DecimalTextField validation and any deferred setText complete first
            javax.swing.SwingUtilities.invokeLater(() -> wizardViewModel.fireChange());
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

      stagesPanel.add(stageRow);
      if (i < stages.size() - 1) {
        stagesPanel.add(Box.createVerticalStrut(10));
      }
    }

    stagesPanel.setMaximumSize(stagesPanel.getPreferredSize());

    stagesPanel.revalidate();
    stagesPanel.repaint();
  }
}
