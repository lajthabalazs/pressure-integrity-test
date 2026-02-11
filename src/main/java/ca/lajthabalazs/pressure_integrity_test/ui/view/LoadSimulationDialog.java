package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfigReader;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfigReader.SiteConfigParseException;
import ca.lajthabalazs.pressure_integrity_test.io.FileSystemTextFileReader;
import ca.lajthabalazs.pressure_integrity_test.io.ItvFileReader;
import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader.FailedToReadFileException;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.ui.UIColors;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Modal dialog for loading an .ITV file and site config for simulation. */
public class LoadSimulationDialog extends JDialog {

  private static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Budapest");
  private static final DateTimeFormatter DATE_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(DISPLAY_ZONE);

  private static final int DETAIL_WIDTH_CHARS = 55;

  private final FileChooserPanel siteConfigChooserPanel;
  private final FileChooserPanel itvFileChooserPanel;
  private final JTextArea siteConfigSummaryLabel;
  private final JTextArea itvSummaryLabel;
  private final JTextArea sensorMismatchWarningLabel;
  private final JButton startButton;
  private final JButton cancelButton;

  private File selectedItvFile;
  private File selectedSiteConfigFile;
  private SiteConfig loadedSiteConfig;
  private List<MeasurementVector> loadedItvVectors;
  private boolean sensorsMatch = true;

  public LoadSimulationDialog(Frame owner, File rootDirectory) {
    super(owner, "Load File for Simulation", true);
    File baseDir =
        rootDirectory != null && rootDirectory.isDirectory()
            ? rootDirectory
            : new File(System.getProperty("user.dir"));
    File resourcesCandidate = new File(baseDir, "resources");
    File srcMainResources = new File(baseDir, "src/main/resources");
    final File resourcesDir =
        resourcesCandidate.isDirectory()
            ? resourcesCandidate
            : (srcMainResources.isDirectory() ? srcMainResources : baseDir);

    setLayout(new BorderLayout(10, 10));
    setSize(500, 380);
    setLocationRelativeTo(owner);
    getContentPane().setBackground(Color.WHITE);

    // Center: box with the two file choosers and their detail text
    JPanel centerPanel = new JPanel();
    centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
    centerPanel.setBackground(Color.WHITE);

    siteConfigChooserPanel =
        new FileChooserPanel(
            "Choose site config file",
            () -> resourcesDir,
            this::onSiteConfigSelected,
            new FileNameExtensionFilter("JSON files", "json"),
            false);
    centerPanel.add(siteConfigChooserPanel);
    siteConfigSummaryLabel = detailTextArea();
    centerPanel.add(wrapInScrollPane(siteConfigSummaryLabel));
    centerPanel.add(Box.createVerticalStrut(12));

    itvFileChooserPanel =
        new FileChooserPanel(
            "Choose .ITV file",
            () -> baseDir,
            this::onItvFileSelected,
            new FileNameExtensionFilter("ITV files", "itv", "ITV"),
            false);
    centerPanel.add(itvFileChooserPanel);
    itvSummaryLabel = detailTextArea();
    centerPanel.add(wrapInScrollPane(itvSummaryLabel));
    sensorMismatchWarningLabel = detailTextArea();
    sensorMismatchWarningLabel.setForeground(new Color(0xCC6600));
    centerPanel.add(wrapInScrollPane(sensorMismatchWarningLabel));
    add(centerPanel, BorderLayout.CENTER);
    // South: buttons
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
    buttonPanel.setBackground(Color.WHITE);
    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(e -> dispose());
    buttonPanel.add(cancelButton);
    startButton = new JButton("Start simulation");
    startButton.setEnabled(false);
    startButton.setBackground(UIColors.PRIMARY_BUTTON_BACKGROUND);
    startButton.setForeground(UIColors.PRIMARY_BUTTON_FOREGROUND);
    startButton.setOpaque(true);
    startButton.addActionListener(e -> dispose());
    buttonPanel.add(startButton);
    add(buttonPanel, BorderLayout.SOUTH);
  }

  private static JScrollPane wrapInScrollPane(JTextArea area) {
    JScrollPane scroll = new JScrollPane(area);
    scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
    scroll.getViewport().setBackground(Color.WHITE);
    scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.setPreferredSize(new Dimension(0, 60));
    scroll.setMinimumSize(new Dimension(0, 40));
    return scroll;
  }

  private static JTextArea detailTextArea() {
    JTextArea area = new JTextArea(2, DETAIL_WIDTH_CHARS);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    area.setEditable(false);
    area.setBackground(Color.WHITE);
    area.setForeground(new Color(0x333333));
    area.setBorder(new EmptyBorder(2, 0, 4, 0));
    area.setFocusable(false);
    area.setMinimumSize(new Dimension(0, 0));
    return area;
  }

  private void onSiteConfigSelected(File file) {
    selectedSiteConfigFile = file;
    loadedSiteConfig = null;
    siteConfigSummaryLabel.setText(" ");
    if (file == null) {
      updateStartButtonState();
      revalidateSensorsIfItvLoaded();
      return;
    }
    try {
      SiteConfigReader reader = new SiteConfigReader(new FileSystemTextFileReader());
      String path = file.toPath().toAbsolutePath().normalize().toString();
      loadedSiteConfig = reader.read(path);
      String title = loadedSiteConfig.getId() != null ? loadedSiteConfig.getId() : "";
      String desc =
          loadedSiteConfig.getDescription() != null ? loadedSiteConfig.getDescription() : "";
      siteConfigSummaryLabel.setText((title.isEmpty() ? "" : title + "\n") + desc);
      siteConfigSummaryLabel.setForeground(new Color(0x333333));
    } catch (FailedToReadFileException | SiteConfigParseException e) {
      siteConfigSummaryLabel.setText("Error: " + e.getMessage());
      siteConfigSummaryLabel.setForeground(new Color(0xCC0000));
    }
    updateStartButtonState();
    revalidateSensorsIfItvLoaded();
  }

  private void onItvFileSelected(File file) {
    selectedItvFile = file;
    loadedItvVectors = null;
    itvSummaryLabel.setText(" ");
    sensorMismatchWarningLabel.setText(" ");
    sensorsMatch = true;
    if (file == null) {
      updateStartButtonState();
      return;
    }
    try {
      Charset itvCharset = charsetForItv();
      ItvFileReader reader = new ItvFileReader(new FileSystemTextFileReader(itvCharset));
      String path = file.toPath().toAbsolutePath().normalize().toString();
      loadedItvVectors = reader.read(path);
      if (loadedItvVectors == null || loadedItvVectors.isEmpty()) {
        itvSummaryLabel.setText("No measurement vectors in file.");
        sensorsMatch = false;
      } else {
        long startUtc = loadedItvVectors.get(0).getTimeUtc();
        String startStr = DATE_TIME_FORMAT.format(Instant.ofEpochMilli(startUtc));
        long delayMs =
            loadedItvVectors.size() > 1 ? loadedItvVectors.get(1).getTimeUtc() - startUtc : 0;
        String delayStr = formatDelay(delayMs);
        int cycles = loadedItvVectors.size();
        itvSummaryLabel.setText(
            "Start: " + startStr + "\nDelay: " + delayStr + "   Cycles: " + cycles);
        itvSummaryLabel.setForeground(new Color(0x333333));

        if (loadedSiteConfig != null) {
          sensorsMatch = checkSensorsMatch(loadedSiteConfig, loadedItvVectors.get(0));
          if (!sensorsMatch) {
            sensorMismatchWarningLabel.setText(
                "Warning: ITV sensors do not match site config. Cannot start.");
          }
        }
      }
    } catch (FailedToReadFileException e) {
      itvSummaryLabel.setText("Error: " + e.getMessage());
      itvSummaryLabel.setForeground(new Color(0xCC0000));
      sensorsMatch = false;
    }
    updateStartButtonState();
  }

  private void revalidateSensorsIfItvLoaded() {
    if (loadedItvVectors != null && !loadedItvVectors.isEmpty() && loadedSiteConfig != null) {
      sensorsMatch = checkSensorsMatch(loadedSiteConfig, loadedItvVectors.get(0));
      if (!sensorsMatch) {
        sensorMismatchWarningLabel.setText(
            "Warning: ITV sensors do not match site config. Cannot start.");
      } else {
        sensorMismatchWarningLabel.setText(" ");
      }
      updateStartButtonState();
    }
  }

  private boolean checkSensorsMatch(SiteConfig siteConfig, MeasurementVector firstVector) {
    Set<String> expected =
        siteConfig.getSensors().stream()
            .map(SensorConfig::getId)
            .filter(id -> id != null && !id.isEmpty())
            .collect(Collectors.toSet());
    Set<String> actual = new HashSet<>(firstVector.getMeasurementsMap().keySet());
    actual.remove("p"); // main pressure is computed, not a sensor
    return expected.equals(actual);
  }

  private static String formatDelay(long delayMs) {
    if (delayMs <= 0) return "—";
    if (delayMs % 60_000 == 0 && delayMs >= 60_000) {
      return (delayMs / 60_000) + " min";
    }
    if (delayMs % 1000 == 0) return (delayMs / 1000) + " s";
    return delayMs + " ms";
  }

  /** Charset for ITV files (Central European; often Windows-1250). */
  private static Charset charsetForItv() {
    try {
      return Charset.forName("Windows-1250");
    } catch (Exception e) {
      try {
        return Charset.forName("ISO-8859-2");
      } catch (Exception e2) {
        return StandardCharsets.UTF_8;
      }
    }
  }

  private void updateStartButtonState() {
    boolean bothSelected = selectedItvFile != null && selectedSiteConfigFile != null;
    startButton.setEnabled(bothSelected && sensorsMatch);
  }

  public File getSelectedItvFile() {
    return selectedItvFile;
  }

  public File getSelectedSiteConfigFile() {
    return selectedSiteConfigFile;
  }
}
