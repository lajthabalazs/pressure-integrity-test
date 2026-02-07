package ca.lajthabalazs.pressure_integrity_test.ui.view;

import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

/** Reusable panel with a "choose file" button and a label below showing the chosen file path. */
public class FileChooserPanel extends JPanel {

  private final JButton chooseButton;
  private final JLabel filePathLabel;
  private final Supplier<File> currentDirectorySupplier;
  private final FileFilter fileFilter;
  private final boolean directoriesOnly;

  /**
   * @param buttonText label for the button (e.g. "Choose site config")
   * @param currentDirectorySupplier supplies the directory to open the chooser in (can return null)
   * @param onFileChosen called when the user selects a file or directory (may be null)
   * @param fileFilter optional filter for the file chooser (e.g. by extension or empty folders)
   * @param directoriesOnly if true, chooser allows only directory selection
   */
  public FileChooserPanel(
      String buttonText,
      Supplier<File> currentDirectorySupplier,
      Consumer<File> onFileChosen,
      FileFilter fileFilter,
      boolean directoriesOnly) {
    this.currentDirectorySupplier =
        currentDirectorySupplier != null ? currentDirectorySupplier : () -> null;
    this.fileFilter = fileFilter;
    this.directoriesOnly = directoriesOnly;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBackground(Color.WHITE);

    chooseButton = new JButton(buttonText);
    chooseButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    chooseButton.addActionListener(
        e -> {
          JFileChooser chooser = new JFileChooser();
          if (directoriesOnly) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
          }
          if (this.fileFilter != null) {
            chooser.setFileFilter(this.fileFilter);
          }
          File dir = this.currentDirectorySupplier.get();
          if (dir != null && dir.isDirectory()) {
            chooser.setCurrentDirectory(dir);
          }
          if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && onFileChosen != null) {
            onFileChosen.accept(chooser.getSelectedFile());
          }
        });
    add(chooseButton);

    add(Box.createVerticalStrut(5));

    filePathLabel = new JLabel();
    filePathLabel.setForeground(new Color(0x666666));
    filePathLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(filePathLabel);

    add(Box.createVerticalStrut(10));
    setAlignmentX(Component.LEFT_ALIGNMENT);
  }

  /**
   * Updates the file path label. Call when the selected file changes (e.g. from view model).
   *
   * @param file the currently chosen file, or null to clear/hide the label
   */
  public void setDisplayedFile(File file) {
    if (file != null) {
      filePathLabel.setText(file.getAbsolutePath());
      filePathLabel.setVisible(true);
    } else {
      filePathLabel.setText("");
      filePathLabel.setVisible(false);
    }
  }

  /** Enables or disables the choose button. */
  public void setEditable(boolean editable) {
    chooseButton.setEnabled(editable);
  }
}
