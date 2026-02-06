package ca.lajthabalazs.pressure_integrity_test.main;

import ca.lajthabalazs.pressure_integrity_test.ui.view.MainWindow;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.EventQueue;

public class Main {
  public static void main(String[] args) {
    FlatLightLaf.setup();

    EventQueue.invokeLater(
        () -> {
          MainWindow window = new MainWindow();
          window.setVisible(true);
        });
  }
}
