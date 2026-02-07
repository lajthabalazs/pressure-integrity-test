package ca.lajthabalazs.pressure_integrity_test.ui.viewmodel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** View model for the Local settings step (step 3) of the new test wizard. */
public class DataInterfaceStepViewModel {

  public interface Listener {
    void onStateChanged();
  }

  private final List<Listener> listeners = new ArrayList<>();

  private File selectedFile;
  private String dataInterfaceText = "";
  private boolean finalized;

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
    if (selectedFile != null && selectedFile.getParentFile() != null) {
      return selectedFile.getParentFile();
    }
    return new File(System.getProperty("user.dir"));
  }

  public File getSelectedFile() {
    return selectedFile;
  }

  public void setSelectedFile(File file) {
    this.selectedFile = file;
    notifyListeners();
  }

  public String getDataInterfaceText() {
    return dataInterfaceText;
  }

  public void setDataInterfaceText(String text) {
    this.dataInterfaceText = text != null ? text : "";
    notifyListeners();
  }

  public boolean hasData() {
    return !dataInterfaceText.isBlank();
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
