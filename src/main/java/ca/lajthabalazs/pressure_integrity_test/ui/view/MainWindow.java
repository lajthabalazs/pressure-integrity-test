package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.config.SensorConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfigReader;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfigReader.SiteConfigParseException;
import ca.lajthabalazs.pressure_integrity_test.io.FileSystemTextFileReader;
import ca.lajthabalazs.pressure_integrity_test.io.ItvFileReader;
import ca.lajthabalazs.pressure_integrity_test.io.TextFileReader.FailedToReadFileException;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVectorPlaybackStream;
import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class MainWindow extends JFrame {

  private static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Budapest");
  private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(DISPLAY_ZONE);
  private static final int DEFAULT_COLUMN_WIDTH_PX = 120;

  private final File rootDirectory;
  private final DefaultTableModel dataTableModel;
  private final JTable dataTable;

  /**
   * Sensor order from site config; used for column order and ITV key mapping when appending rows.
   */
  private final AtomicReference<List<SensorConfig>> dataSensorOrder = new AtomicReference<>(null);

  public MainWindow(File rootDirectory) {
    this.rootDirectory =
        rootDirectory != null ? rootDirectory : new File(System.getProperty("user.dir"));
    setTitle("Pressure Integrity Test");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(800, 600);
    setLocationRelativeTo(null);

    JMenuBar menuBar = new JMenuBar();
    JMenu testMenu = new JMenu("Test");
    JMenuItem startNewTestItem = new JMenuItem("Start new test");
    startNewTestItem.addActionListener(e -> onStartNewTest());
    testMenu.add(startNewTestItem);
    menuBar.add(testMenu);

    JMenu simulationMenu = new JMenu("Simulation");
    JMenuItem loadOldFileItem = new JMenuItem("Load ITV file");
    loadOldFileItem.addActionListener(e -> onLoadOldFileForSimulation());
    simulationMenu.add(loadOldFileItem);
    menuBar.add(simulationMenu);

    setJMenuBar(menuBar);

    // Tabbed content: Dashboard | Data
    JTabbedPane tabbedPane = new JTabbedPane();

    JPanel dashboardPanel = new JPanel();
    dashboardPanel.setBackground(Color.WHITE);
    dashboardPanel.add(new JLabel("Dashboard"));
    tabbedPane.addTab("Dashboard", dashboardPanel);

    dataTableModel = new DefaultTableModel();
    dataTable = new JTable(dataTableModel);
    dataTable.setAutoCreateRowSorter(false);
    dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    JScrollPane dataScroll = new JScrollPane(dataTable);
    dataScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    JPanel dataPanel = new JPanel(new BorderLayout());
    dataPanel.add(dataScroll, BorderLayout.CENTER);
    tabbedPane.addTab("Data", dataPanel);

    getContentPane().add(tabbedPane, BorderLayout.CENTER);
  }

  private void onStartNewTest() {
    NewTestWizardDialog dialog = new NewTestWizardDialog(this, rootDirectory);
    dialog.setVisible(true);
  }

  private void onLoadOldFileForSimulation() {
    LoadSimulationDialog dialog = new LoadSimulationDialog(this, rootDirectory);
    dialog.setVisible(true);
    File itvFile = dialog.getSelectedItvFile();
    File siteConfigFile = dialog.getSelectedSiteConfigFile();
    if (itvFile == null || siteConfigFile == null) {
      return;
    }
    SiteConfig siteConfig;
    try {
      SiteConfigReader reader = new SiteConfigReader(new FileSystemTextFileReader());
      siteConfig = reader.read(siteConfigFile.toPath().toAbsolutePath().normalize().toString());
    } catch (FailedToReadFileException | SiteConfigParseException e) {
      JOptionPane.showMessageDialog(
          this,
          "Failed to load site config: " + e.getMessage(),
          "Simulation",
          JOptionPane.ERROR_MESSAGE);
      return;
    }
    startSimulationStream(itvFile, siteConfig);
  }

  private void startSimulationStream(File itvFile, SiteConfig siteConfig) {
    dataSensorOrder.set(null);
    dataTableModel.setRowCount(0);

    List<SensorConfig> sensors = siteConfig.getSensors();
    dataSensorOrder.set(sensors);
    List<String> columnNames = new ArrayList<>();
    columnNames.add("Time");
    for (SensorConfig s : sensors) {
      if (s.getId() != null) {
        columnNames.add(s.getId());
      }
    }
    dataTableModel.setColumnIdentifiers(columnNames.toArray());
    for (int i = 0; i < dataTable.getColumnCount(); i++) {
      dataTable.getColumnModel().getColumn(i).setPreferredWidth(DEFAULT_COLUMN_WIDTH_PX);
    }

    String path = itvFile.toPath().toAbsolutePath().normalize().toString();
    Charset charset = charsetForItv();

    MeasurementVectorPlaybackStream playbackStream = new MeasurementVectorPlaybackStream();
    playbackStream.setSpeed(30.0);
    playbackStream.subscribe(
        vector -> {
          SwingUtilities.invokeLater(
              () -> {
                appendVectorToTable(vector);
                scrollDataTableToBottom();
              });
        });

    Thread worker =
        new Thread(
            () -> {
              try {
                List<MeasurementVector> vectors =
                    new ItvFileReader(new FileSystemTextFileReader(charset)).read(path);
                if (vectors == null || vectors.isEmpty()) {
                  SwingUtilities.invokeLater(
                      () ->
                          JOptionPane.showMessageDialog(
                              this,
                              "ITV file contains no measurement vectors.",
                              "Simulation",
                              JOptionPane.ERROR_MESSAGE));
                  return;
                }
                long startTime = System.currentTimeMillis();
                SwingUtilities.invokeLater(() -> playbackStream.startPlayback(vectors, startTime));
              } catch (FailedToReadFileException e) {
                SwingUtilities.invokeLater(
                    () ->
                        JOptionPane.showMessageDialog(
                            this,
                            "Failed to read ITV file: " + e.getMessage(),
                            "Simulation",
                            JOptionPane.ERROR_MESSAGE));
              }
            });
    worker.start();
  }

  private void appendVectorToTable(MeasurementVector vector) {
    List<SensorConfig> sensorOrder = dataSensorOrder.get();
    if (sensorOrder == null) {
      return;
    }
    String timeStr = TIME_FORMAT.format(Instant.ofEpochMilli(vector.getTimeUtc()));
    Object[] row = new Object[sensorOrder.size() + 1];
    row[0] = timeStr;
    for (int i = 0; i < sensorOrder.size(); i++) {
      String itvKey = siteSensorIdToItvKey(sensorOrder.get(i));
      Measurement m = vector.getMeasurementsMap().get(itvKey);
      row[i + 1] = m != null ? m.getValueInDefaultUnit().toPlainString() : "";
    }
    dataTableModel.addRow(row);
  }

  /** Maps site config sensor ID to the ITV measurement key (p1, p2, T1..T61, fi1..fi10). */
  private static String siteSensorIdToItvKey(SensorConfig sensor) {
    String type = sensor.getType();
    String id = sensor.getId();
    if (id == null) return "";
    if ("pressure".equals(type)) return id.toLowerCase();
    if ("humidity".equals(type)) {
      String u = id.toUpperCase();
      if (u.startsWith("RH")) {
        try {
          return "fi" + id.substring(2).trim();
        } catch (Exception ignored) {
          // fall through
        }
      }
      return "fi" + id;
    }
    return id; // temperature or other
  }

  private void scrollDataTableToBottom() {
    int last = dataTableModel.getRowCount() - 1;
    if (last >= 0) {
      dataTable.scrollRectToVisible(dataTable.getCellRect(last, 0, true));
    }
  }

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
}
