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
import ca.lajthabalazs.pressure_integrity_test.measurement.processing.StackedMeasurementVectorStream;
import ca.lajthabalazs.pressure_integrity_test.measurement.streaming.MeasurementVectorPlaybackStream;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
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
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

public class MainWindow extends JFrame {

  private static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Budapest");
  private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(DISPLAY_ZONE);
  private static final int DEFAULT_COLUMN_WIDTH_PX = 120;

  /** Background for the currently selected playback speed button (green). */
  private static final Color SELECTED_SPEED_BG = new Color(100, 200, 100);

  /** Green color for blinking resume button. */
  private static final Color RESUME_BLINK_GREEN = new Color(100, 255, 100);

  /** Fixed size for speed buttons so they are uniform and "120x" fits. */
  private static final Dimension SPEED_BUTTON_SIZE = new Dimension(56, 26);

  private final File rootDirectory;
  private final DefaultTableModel dataTableModel;
  private final JTable dataTable;

  /**
   * Sensor order from site config; used for column order and ITV key mapping when appending rows.
   */
  private final AtomicReference<List<SensorConfig>> dataSensorOrder = new AtomicReference<>(null);

  private final AtomicReference<MeasurementVectorPlaybackStream> currentPlaybackStream =
      new AtomicReference<>(null);
  private final AtomicReference<StackedMeasurementVectorStream> currentStackedStream =
      new AtomicReference<>(null);
  private final JPanel simulationControlPanel;

  private JButton simulationPauseResumeButton;
  private final List<JButton> speedButtons = new ArrayList<>();
  private final List<Double> speedValues = new ArrayList<>();
  private volatile double currentPlaybackSpeed = 30.0;
  private Timer resumeBlinkTimer;

  private final DashboardPanel dashboardPanel;
  private final JPanel contentCards;
  private final CardLayout contentCardLayout;

  private static final String CARD_WELCOME = "welcome";
  private static final String CARD_MEASUREMENT = "measurement";

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

    contentCardLayout = new CardLayout();
    contentCards = new JPanel(contentCardLayout);

    // Welcome card: centered message when no measurement is running
    JPanel welcomePanel = new JPanel(new GridBagLayout());
    welcomePanel.setBackground(Color.WHITE);
    JLabel welcomeLabel = new JLabel("Use the menu to start a measurement");
    welcomePanel.add(welcomeLabel, new GridBagConstraints());

    // Measurement card: controls + tabbed pane
    JTabbedPane tabbedPane = new JTabbedPane();
    dashboardPanel = new DashboardPanel();
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

    JPanel measurementPanel = new JPanel(new BorderLayout());
    simulationControlPanel = createSimulationControlPanel();
    simulationControlPanel.setVisible(false);
    measurementPanel.add(simulationControlPanel, BorderLayout.NORTH);
    measurementPanel.add(tabbedPane, BorderLayout.CENTER);

    contentCards.add(welcomePanel, CARD_WELCOME);
    contentCards.add(measurementPanel, CARD_MEASUREMENT);
    contentCardLayout.show(contentCards, CARD_WELCOME);

    getContentPane().add(contentCards, BorderLayout.CENTER);
  }

  private JPanel createSimulationControlPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    panel.setBorder(
        javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createEtchedBorder(),
            javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8)));

    simulationPauseResumeButton = new JButton("Pause");
    simulationPauseResumeButton.setBackground(Color.WHITE);
    simulationPauseResumeButton.setOpaque(true);
    simulationPauseResumeButton.addActionListener(
        e -> {
          MeasurementVectorPlaybackStream stream = currentPlaybackStream.get();
          if (stream != null) {
            if (stream.isPaused()) {
              stream.resume();
            } else {
              stream.pause();
            }
            updateSimulationControlState(stream);
          }
        });

    panel.add(new JLabel("Simulation:"));
    panel.add(simulationPauseResumeButton);
    panel.add(new JLabel("Speed:"));
    for (double speed : new double[] {2, 5, 10, 30, 60, 120}) {
      JButton speedBtn = new JButton((long) speed + "x");
      speedBtn.setPreferredSize(SPEED_BUTTON_SIZE);
      speedBtn.setMinimumSize(SPEED_BUTTON_SIZE);
      speedBtn.setMaximumSize(SPEED_BUTTON_SIZE);
      speedBtn.setBackground(Color.WHITE);
      speedBtn.setOpaque(true);
      speedBtn.addActionListener(
          e -> {
            MeasurementVectorPlaybackStream stream = currentPlaybackStream.get();
            if (stream != null) {
              stream.setSpeed(speed);
              currentPlaybackSpeed = speed;
              updateSpeedButtonsAppearance();
              updateSimulationControlState(stream);
            }
          });
      speedValues.add(speed);
      speedButtons.add(speedBtn);
      panel.add(speedBtn);
    }

    return panel;
  }

  private void updateSpeedButtonsAppearance() {
    for (int i = 0; i < speedButtons.size(); i++) {
      JButton btn = speedButtons.get(i);
      double speed = speedValues.get(i);
      if (speed == currentPlaybackSpeed) {
        btn.setEnabled(false);
        btn.setBackground(SELECTED_SPEED_BG);
      } else {
        btn.setEnabled(true);
        btn.setBackground(Color.WHITE);
      }
    }
  }

  private void updateSimulationControlState(MeasurementVectorPlaybackStream stream) {
    boolean paused = stream.isPaused();
    simulationPauseResumeButton.setText(paused ? "Resume" : "Pause");

    // Start/stop blinking timer for resume button
    if (resumeBlinkTimer != null) {
      resumeBlinkTimer.stop();
      resumeBlinkTimer = null;
    }

    if (paused) {
      // Start blinking green
      resumeBlinkTimer =
          new Timer(
              500,
              e -> {
                Color current = simulationPauseResumeButton.getBackground();
                simulationPauseResumeButton.setBackground(
                    current.equals(Color.WHITE) ? RESUME_BLINK_GREEN : Color.WHITE);
              });
      resumeBlinkTimer.start();
    } else {
      // Not paused, ensure button is white
      simulationPauseResumeButton.setBackground(Color.WHITE);
    }

    updateSpeedButtonsAppearance();
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
    MeasurementVectorPlaybackStream previousPlayback = currentPlaybackStream.getAndSet(null);
    if (previousPlayback != null) {
      previousPlayback.stopPlayback();
    }
    StackedMeasurementVectorStream previousStacked = currentStackedStream.getAndSet(null);
    if (previousStacked != null) {
      previousStacked.stop();
    }
    // Stop any existing blink timer
    if (resumeBlinkTimer != null) {
      resumeBlinkTimer.stop();
      resumeBlinkTimer = null;
    }
    simulationControlPanel.setVisible(false);
    contentCardLayout.show(contentCards, CARD_WELCOME);

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
    currentPlaybackStream.set(playbackStream);
    StackedMeasurementVectorStream stackedStream =
        new StackedMeasurementVectorStream(siteConfig, playbackStream);
    currentStackedStream.set(stackedStream);
    dashboardPanel.clear();
    dashboardPanel.subscribe(stackedStream);
    stackedStream.subscribe(
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
                      () -> {
                        currentPlaybackStream.set(null);
                        JOptionPane.showMessageDialog(
                            this,
                            "ITV file contains no measurement vectors.",
                            "Simulation",
                            JOptionPane.ERROR_MESSAGE);
                      });
                  return;
                }
                long startTime = System.currentTimeMillis();
                SwingUtilities.invokeLater(
                    () -> {
                      currentPlaybackSpeed = 30.0;
                      playbackStream.startPlayback(vectors, startTime);
                      playbackStream.pause(); // Start paused
                      contentCardLayout.show(contentCards, CARD_MEASUREMENT);
                      simulationControlPanel.setVisible(true);
                      updateSimulationControlState(playbackStream);
                    });
              } catch (FailedToReadFileException e) {
                SwingUtilities.invokeLater(
                    () -> {
                      currentPlaybackStream.set(null);
                      JOptionPane.showMessageDialog(
                          this,
                          "Failed to read ITV file: " + e.getMessage(),
                          "Simulation",
                          JOptionPane.ERROR_MESSAGE);
                    });
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
