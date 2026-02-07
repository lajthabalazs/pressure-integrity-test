package ca.lajthabalazs.pressure_integrity_test.ui.view;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/** FileFilter that accepts only empty directories. */
public class EmptyFolderFilter extends FileFilter {

  @Override
  public boolean accept(File f) {
    if (f == null || !f.isDirectory()) {
      return false;
    }
    String[] listing = f.list();
    return listing == null || listing.length == 0;
  }

  @Override
  public String getDescription() {
    return "Empty folders";
  }
}
