package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.config.Containment;
import ca.lajthabalazs.pressure_integrity_test.config.DesignPressure;
import ca.lajthabalazs.pressure_integrity_test.config.HumiditySensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.PressureSensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfig;
import ca.lajthabalazs.pressure_integrity_test.config.TemperatureSensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.ValidRange;
import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.NewTestWizardViewModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

class SiteSelectionStepPanel extends JPanel {

  private final NewTestWizardViewModel viewModel;
  private final JLabel filePathLabel;
  private final JButton chooseButton;
  private final JScrollPane configScrollPane;
  private final JPanel configContentPanel;

  SiteSelectionStepPanel(NewTestWizardViewModel viewModel) {
    this.viewModel = viewModel;
    this.setPreferredSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    this.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    setBackground(Color.WHITE);
    setLayout(new BorderLayout());

    JPanel fileChooserPanel = new JPanel();
    fileChooserPanel.setLayout(new BoxLayout(fileChooserPanel, BoxLayout.Y_AXIS));

    chooseButton = new JButton("Choose site config");
    chooseButton.addActionListener(e -> chooseSiteConfig());
    chooseButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    fileChooserPanel.add(chooseButton);

    fileChooserPanel.add(Box.createVerticalStrut(5));

    filePathLabel = new JLabel();
    filePathLabel.setForeground(new Color(0x666666));
    filePathLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    fileChooserPanel.add(filePathLabel);

    fileChooserPanel.add(Box.createVerticalStrut(10));
    fileChooserPanel.setBackground(Color.WHITE);
    fileChooserPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

    this.add(fileChooserPanel, BorderLayout.NORTH);

    configContentPanel = new JPanel();
    configContentPanel.setLayout(new BoxLayout(configContentPanel, BoxLayout.Y_AXIS));
    configContentPanel.setBackground(Color.WHITE);
    configContentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

    configScrollPane = new JScrollPane(configContentPanel);
    configScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
    configScrollPane.setBorder(null);
    configScrollPane.getViewport().setBackground(Color.WHITE);
    configScrollPane.setPreferredSize(new java.awt.Dimension(500, 280));
    configScrollPane.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));
    configScrollPane.setVisible(false);
    this.add(configScrollPane, BorderLayout.CENTER);
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

    SiteConfig config = viewModel.getSiteConfig();
    String loadError = viewModel.getSiteConfigLoadError();
    configScrollPane.setVisible(config != null || (loadError != null && file != null));
    rebuildConfigContent(config, loadError);
  }

  private void rebuildConfigContent(SiteConfig config, String loadError) {
    configContentPanel.removeAll();
    configContentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

    if (loadError != null && config == null) {
      Box errRow = Box.createHorizontalBox();
      errRow.add(new JLabel("Failed to load config: " + loadError));
      errRow.setAlignmentX(Component.LEFT_ALIGNMENT);
      configContentPanel.add(errRow);
      configContentPanel.revalidate();
      configContentPanel.repaint();
      return;
    }

    if (config == null) {
      configContentPanel.revalidate();
      configContentPanel.repaint();
      return;
    }

    // Header: site name
    JLabel headerLabel = new JLabel(config.getId() != null ? config.getId() : "Site");
    headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
    headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    configContentPanel.add(headerLabel);
    configContentPanel.add(Box.createVerticalStrut(4));

    // Description
    if (config.getDescription() != null && !config.getDescription().isEmpty()) {
      Box descRow = Box.createHorizontalBox();
      JLabel desc =
          new JLabel("<html>" + config.getDescription().replace("\n", "<br>") + "</html>");
      desc.setAlignmentX(Component.LEFT_ALIGNMENT);
      descRow.add(desc);
      descRow.setAlignmentX(Component.LEFT_ALIGNMENT);
      configContentPanel.add(descRow);
      configContentPanel.add(Box.createVerticalStrut(6));
    }

    // Net containment volume
    Containment containment = config.getContainment();
    if (containment != null && containment.getNetVolume_m3() != null) {
      Box row = Box.createHorizontalBox();
      row.add(
          new JLabel(
              "Net containment volume: " + formatDecimal(containment.getNetVolume_m3()) + " m³"));
      row.setAlignmentX(Component.LEFT_ALIGNMENT);
      configContentPanel.add(row);
      configContentPanel.add(Box.createVerticalStrut(2));
    }

    // Maximum over pressure, Allowed leakage
    DesignPressure design = config.getDesignPressure();
    if (design != null) {
      if (design.getOverpressure_Pa() != null) {
        Box row = Box.createHorizontalBox();
        row.add(
            new JLabel(
                "Maximum over pressure: " + formatDecimal(design.getOverpressure_Pa()) + " Pa"));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        configContentPanel.add(row);
        configContentPanel.add(Box.createVerticalStrut(2));
      }
      if (design.getLeakLimit_percent_per_day() != null) {
        Box row = Box.createHorizontalBox();
        row.add(
            new JLabel(
                "Allowed leakage: " + formatDecimal(design.getLeakLimit_percent_per_day()) + " %"));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        configContentPanel.add(row);
        configContentPanel.add(Box.createVerticalStrut(4));
      }
    }

    List<SensorConfig> sensors = config.getSensors() != null ? config.getSensors() : List.of();
    List<PressureSensorConfig> pressureSensors = new ArrayList<>();
    List<TemperatureSensorConfig> temperatureSensors = new ArrayList<>();
    List<HumiditySensorConfig> humiditySensors = new ArrayList<>();
    for (SensorConfig s : sensors) {
      if (s instanceof PressureSensorConfig) pressureSensors.add((PressureSensorConfig) s);
      else if (s instanceof TemperatureSensorConfig)
        temperatureSensors.add((TemperatureSensorConfig) s);
      else if (s instanceof HumiditySensorConfig) humiditySensors.add((HumiditySensorConfig) s);
    }

    // Pressure sensors
    if (!pressureSensors.isEmpty()) {
      JLabel sectionLabel = new JLabel("Pressure sensors (total: " + pressureSensors.size() + ")");
      sectionLabel.setFont(sectionLabel.getFont().deriveFont(Font.BOLD));
      sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
      configContentPanel.add(sectionLabel);
      configContentPanel.add(Box.createVerticalStrut(2));
      for (PressureSensorConfig p : pressureSensors) {
        Box row = Box.createHorizontalBox();
        row.add(
            new JLabel("  " + formatSensorLine(p.getId(), p.getDescription(), p.getSigma(), null)));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        configContentPanel.add(row);
      }
      configContentPanel.add(Box.createVerticalStrut(4));
    }

    // Temperature sensors (with attached humidity if any)
    if (!temperatureSensors.isEmpty()) {
      int withHumidity = 0;
      for (TemperatureSensorConfig t : temperatureSensors) {
        for (HumiditySensorConfig h : humiditySensors) {
          if (t.getId() != null && t.getId().equals(h.getPairedTemperatureSensor())) {
            withHumidity++;
            break;
          }
        }
      }
      String tempHeader =
          "Temperature sensors (total: "
              + temperatureSensors.size()
              + ", "
              + withHumidity
              + " with humidity sensors)";
      JLabel sectionLabel = new JLabel(tempHeader);
      sectionLabel.setFont(sectionLabel.getFont().deriveFont(Font.BOLD));
      sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
      configContentPanel.add(sectionLabel);
      configContentPanel.add(Box.createVerticalStrut(2));
      for (TemperatureSensorConfig t : temperatureSensors) {
        Box tempRow = Box.createHorizontalBox();
        tempRow.add(new JLabel("  " + formatTempLine(t)));
        tempRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        configContentPanel.add(tempRow);
        for (HumiditySensorConfig h : humiditySensors) {
          if (t.getId() != null && t.getId().equals(h.getPairedTemperatureSensor())) {
            Box humRow = Box.createHorizontalBox();
            humRow.add(new JLabel("    Humidity: " + formatHumidityLine(h)));
            humRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            configContentPanel.add(humRow);
          }
        }
      }
      configContentPanel.add(Box.createVerticalStrut(4));
    }

    configContentPanel.revalidate();
    configContentPanel.repaint();
  }

  private static String formatDecimal(BigDecimal value) {
    return value != null ? value.toPlainString() : "—";
  }

  private static String formatSensorLine(
      String id, String desc, BigDecimal sigma, BigDecimal extra) {
    StringBuilder sb = new StringBuilder();
    sb.append(id != null ? id : "—");
    sb.append(", ");
    sb.append(desc != null ? desc : "—");
    sb.append(", sigma ");
    sb.append(formatDecimal(sigma));
    if (extra != null) {
      sb.append(", ");
      sb.append(extra.toPlainString());
    }
    return sb.toString();
  }

  private static String formatTempLine(TemperatureSensorConfig t) {
    StringBuilder sb = new StringBuilder();
    sb.append(t.getId() != null ? t.getId() : "—");
    sb.append(", ");
    sb.append(t.getDescription() != null ? t.getDescription() : "—");
    sb.append(", volume coeff ");
    sb.append(formatDecimal(t.getVolumeWeight()));
    sb.append(", valid range ");
    ValidRange vr = t.getValidRange();
    if (vr != null && (vr.getMin() != null || vr.getMax() != null)) {
      sb.append(formatDecimal(vr.getMin())).append("–").append(formatDecimal(vr.getMax()));
    } else {
      sb.append("—");
    }
    sb.append(", sigma ");
    sb.append(formatDecimal(t.getSigma()));
    return sb.toString();
  }

  private static String formatHumidityLine(HumiditySensorConfig h) {
    StringBuilder sb = new StringBuilder();
    sb.append(h.getId() != null ? h.getId() : "—");
    sb.append(", ");
    sb.append(h.getDescription() != null ? h.getDescription() : "—");
    ValidRange vr = h.getValidRange();
    sb.append(", valid range ");
    if (vr != null && (vr.getMin() != null || vr.getMax() != null)) {
      sb.append(formatDecimal(vr.getMin())).append("–").append(formatDecimal(vr.getMax()));
    } else {
      sb.append("—");
    }
    sb.append(", sigma ");
    sb.append(formatDecimal(h.getSigma()));
    return sb.toString();
  }

  private void chooseSiteConfig() {
    JFileChooser chooser = new JFileChooser();
    File current = viewModel.getSelectedFile();
    if (current != null && current.getParentFile() != null) {
      chooser.setCurrentDirectory(current.getParentFile());
    } else {
      File root = viewModel.getRootDirectory();
      if (root != null && root.isDirectory()) {
        chooser.setCurrentDirectory(root);
      }
    }
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      viewModel.setSelectedFile(chooser.getSelectedFile());
      updateFromViewModel();
    }
  }
}
