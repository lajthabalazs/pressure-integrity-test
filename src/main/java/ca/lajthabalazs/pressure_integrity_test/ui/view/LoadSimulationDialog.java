package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.ui.UIColors;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Modal dialog for loading an old .ITV file for simulation. */
public class LoadSimulationDialog extends JDialog {

  private final FileChooserPanel fileChooserPanel;
  private File selectedFile;

  public LoadSimulationDialog(Frame owner, File rootDirectory) {
    super(owner, "Load File for Simulation", true);
    File dir =
        rootDirectory != null && rootDirectory.isDirectory()
            ? rootDirectory
            : new File(System.getProperty("user.dir"));

    setLayout(new BorderLayout(10, 10));
    setSize(500, 200);
    setLocationRelativeTo(owner);
    getContentPane().setBackground(Color.WHITE);

    fileChooserPanel =
        new FileChooserPanel(
            "Choose .ITV file",
            () -> dir,
            file -> {
              selectedFile = file;
              fileChooserPanel.setDisplayedFile(file);
            },
            new FileNameExtensionFilter("ITV files", "itv", "ITV"),
            false);
    add(fileChooserPanel, BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
    buttonPanel.setBackground(Color.WHITE);
    JButton startButton = new JButton("Start simulation");
    startButton.setBackground(UIColors.PRIMARY_BUTTON_BACKGROUND);
    startButton.setForeground(UIColors.PRIMARY_BUTTON_FOREGROUND);
    startButton.setOpaque(true);
    startButton.addActionListener(e -> dispose());
    buttonPanel.add(startButton);
    add(buttonPanel, BorderLayout.SOUTH);
  }
}
