package ca.lajthabalazs.pressure_integrity_test.ui.view;

import java.io.File;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class MainWindow extends JFrame {

  private final File rootDirectory;

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
  }

  private void onStartNewTest() {
    NewTestWizardDialog dialog = new NewTestWizardDialog(this, rootDirectory);
    dialog.setVisible(true);
  }

  private void onLoadOldFileForSimulation() {
    LoadSimulationDialog dialog = new LoadSimulationDialog(this, rootDirectory);
    dialog.setVisible(true);
  }
}
