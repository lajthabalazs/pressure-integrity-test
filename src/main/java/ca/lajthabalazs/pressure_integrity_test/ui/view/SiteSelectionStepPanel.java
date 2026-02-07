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
import java.awt.Dimension;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

class SiteSelectionStepPanel extends JPanel {

  private static final String HTML_BODY_START =
      "<html><body style='font-family:sans-serif;font-size:10px;margin:0'>";
  private static final String HTML_BODY_END = "</body></html>";

  private final NewTestWizardViewModel wizardViewModel;
  private final FileChooserPanel fileChooserPanel;
  private final JScrollPane configScrollPane;
  private final JEditorPane configEditor;

  SiteSelectionStepPanel(NewTestWizardViewModel wizardViewModel) {
    this.wizardViewModel = wizardViewModel;
    this.setPreferredSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    this.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    setBackground(Color.WHITE);
    setLayout(new BorderLayout());

    fileChooserPanel =
        new FileChooserPanel(
            "Choose site config",
            () -> {
              var step = wizardViewModel.getSiteSelectionStep();
              File current = step.getSelectedFile();
              if (current != null && current.getParentFile() != null) {
                return current.getParentFile();
              }
              File root = step.getRootDirectory();
              return (root != null && root.isDirectory()) ? root : null;
            },
            file -> {
              wizardViewModel.getSiteSelectionStep().setSelectedFile(file);
              updateFromViewModel();
            },
            new FileNameExtensionFilter("JSON files", "json"),
            false);
    this.add(fileChooserPanel, BorderLayout.NORTH);

    configEditor = new JEditorPane();
    configEditor.setContentType("text/html");
    configEditor.setEditable(false);
    configEditor.setBackground(Color.WHITE);
    configEditor.setBorder(new EmptyBorder(0, 0, 0, 0));

    configScrollPane = new JScrollPane(configEditor);
    configScrollPane.setBorder(null);
    configScrollPane.getViewport().setBackground(Color.WHITE);
    configScrollPane.setPreferredSize(new java.awt.Dimension(500, 280));
    configScrollPane.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));
    configScrollPane.setVisible(false);
    this.add(configScrollPane, BorderLayout.CENTER);
    updateFromViewModel();
  }

  void setEditable(boolean editable) {
    fileChooserPanel.setEditable(editable);
  }

  void updateFromViewModel() {
    var step = wizardViewModel.getSiteSelectionStep();
    fileChooserPanel.setDisplayedFile(step.getSelectedFile());

    SiteConfig config = step.getSiteConfig();
    String loadError = step.getSiteConfigLoadError();
    configScrollPane.setVisible(
        config != null || (loadError != null && step.getSelectedFile() != null));
    rebuildConfigContent(config, loadError);
  }

  private void rebuildConfigContent(SiteConfig config, String loadError) {
    if (loadError != null && config == null) {
      configEditor.setText(
          HTML_BODY_START + htmlEscape("Failed to load config: " + loadError) + HTML_BODY_END);
      return;
    }

    if (config == null) {
      configEditor.setText(HTML_BODY_START + HTML_BODY_END);
      return;
    }

    StringBuilder sb = new StringBuilder();
    sb.append(HTML_BODY_START);

    // Header: site name (bold, slightly larger)
    String siteName = config.getId() != null ? config.getId() : "Site";
    sb.append("<p style='margin:0 0 6px 0'><b style='font-size:12px'>")
        .append(htmlEscape(siteName))
        .append("</b></p>");

    // Description
    if (config.getDescription() != null && !config.getDescription().isEmpty()) {
      sb.append("<p style='margin:0 0 6px 0'>")
          .append(htmlEscape(config.getDescription()).replace("\n", "<br>"))
          .append("</p>");
    }

    // Net containment volume
    Containment containment = config.getContainment();
    if (containment != null && containment.getNetVolume_m3() != null) {
      sb.append("<p style='margin:0 0 2px 0'>Net containment volume: ")
          .append(htmlEscape(formatDecimal(containment.getNetVolume_m3())))
          .append(" m³</p>");
    }

    // Maximum over pressure, Allowed leakage
    DesignPressure design = config.getDesignPressure();
    if (design != null) {
      if (design.getOverpressure_bar() != null) {
        sb.append("<p style='margin:0 0 2px 0'>Maximum over pressure: ")
            .append(htmlEscape(formatDecimal(design.getOverpressure_bar())))
            .append(" bar</p>");
      }
      if (design.getLeakLimit_percent_per_day() != null) {
        sb.append("<p style='margin:0 0 2px 0'>Allowed leakage: ")
            .append(htmlEscape(formatDecimal(design.getLeakLimit_percent_per_day())))
            .append(" %</p>");
      }
    }
    sb.append("<p style='margin:4px 0 0 0'></p>");

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

    // Pressure sensors (space between each sensor)
    if (!pressureSensors.isEmpty()) {
      sb.append("<p style='margin:0 0 2px 0'><b>Pressure sensors (total: ")
          .append(pressureSensors.size())
          .append(")</b></p>");
      for (PressureSensorConfig p : pressureSensors) {
        sb.append("<p style='margin:0 0 6px 12px'>")
            .append(htmlEscape(formatSensorLine(p.getId(), p.getDescription(), p.getSigma(), null)))
            .append("</p>");
      }
      sb.append("<p style='margin:4px 0 0 0'></p>");
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
      sb.append("<p style='margin:0 0 2px 0'><b>Temperature sensors (total: ")
          .append(temperatureSensors.size())
          .append(", ")
          .append(withHumidity)
          .append(" with humidity sensors)</b></p>");
      for (TemperatureSensorConfig t : temperatureSensors) {
        // Space above each temp+humidity group; no extra space before attached humidity
        sb.append("<p style='margin:6px 0 0 12px'>")
            .append(htmlEscape(formatTempLine(t)))
            .append("</p>");
        for (HumiditySensorConfig h : humiditySensors) {
          if (t.getId() != null && t.getId().equals(h.getPairedTemperatureSensor())) {
            sb.append("<p style='margin:0 0 0 24px'>Humidity: ")
                .append(htmlEscape(formatHumidityLine(h)))
                .append("</p>");
          }
        }
      }
    }

    sb.append(HTML_BODY_END);
    configEditor.setText(sb.toString());
    configEditor.setCaretPosition(0);
  }

  private static String htmlEscape(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
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
}
