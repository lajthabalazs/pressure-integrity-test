package ca.lajthabalazs.pressure_integrity_test.main;

import ca.lajthabalazs.pressure_integrity_test.ui.view.MainWindow;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.EventQueue;
import java.io.File;

public class Main {
  /** Root directory for config files etc.; by default the main resources folder. */
  private static final File ROOT_DIRECTORY = defaultRootDirectory();

  private static File defaultRootDirectory() {
    File resources = new File("src/main/resources");
    if (resources.isDirectory()) {
      return resources.getAbsoluteFile();
    }
    return new File(System.getProperty("user.dir"));
  }

  public static void main(String[] args) {
    FlatLightLaf.setup();
    EventQueue.invokeLater(
        () -> {
          MainWindow window = new MainWindow(ROOT_DIRECTORY);
          window.setVisible(true);
        });
  }
}
