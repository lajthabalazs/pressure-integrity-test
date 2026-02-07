package ca.lajthabalazs.pressure_integrity_test.ui.viewmodel;

import ca.lajthabalazs.pressure_integrity_test.config.SiteConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfigReader;
import ca.lajthabalazs.pressure_integrity_test.io.FileSystemTextFileReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** View model for the site selection step (step 1) of the new test wizard. */
public class SiteSelectionStepViewModel {

  public interface Listener {
    void onStateChanged();
  }

  private final List<Listener> listeners = new ArrayList<>();
  private final File rootDirectory;

  private File selectedFile;
  private SiteConfig siteConfig;
  private String siteConfigLoadError;
  private boolean finalized;

  public SiteSelectionStepViewModel(File rootDirectory) {
    this.rootDirectory =
        rootDirectory != null ? rootDirectory : new File(System.getProperty("user.dir"));
  }

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  void notifyListeners() {
    for (Listener listener : listeners) {
      listener.onStateChanged();
    }
  }

  public File getRootDirectory() {
    return rootDirectory;
  }

  public File getSelectedFile() {
    return selectedFile;
  }

  public void setSelectedFile(File file) {
    this.selectedFile = file;
    this.siteConfig = null;
    this.siteConfigLoadError = null;
    if (file != null && file.exists()) {
      try {
        SiteConfigReader reader = new SiteConfigReader(new FileSystemTextFileReader());
        this.siteConfig = reader.read(file.getAbsolutePath());
      } catch (Exception e) {
        this.siteConfigLoadError = e.getMessage();
      }
    }
    notifyListeners();
  }

  public SiteConfig getSiteConfig() {
    return siteConfig;
  }

  public String getSiteConfigLoadError() {
    return siteConfigLoadError;
  }

  public boolean hasData() {
    return selectedFile != null && selectedFile.exists();
  }

  public boolean canFinalize() {
    return hasData() && !finalized;
  }

  public void finalizeStep() {
    if (canFinalize()) {
      finalized = true;
      notifyListeners();
    }
  }

  public boolean isFinalized() {
    return finalized;
  }

  public void openForEditing() {
    if (finalized) {
      finalized = false;
      notifyListeners();
    }
  }
}
