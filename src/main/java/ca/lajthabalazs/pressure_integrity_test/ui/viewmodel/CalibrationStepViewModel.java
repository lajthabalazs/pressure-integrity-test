package ca.lajthabalazs.pressure_integrity_test.ui.viewmodel;

import ca.lajthabalazs.pressure_integrity_test.config.CalibrationConfig;
import ca.lajthabalazs.pressure_integrity_test.config.CalibrationConfigReader;
import ca.lajthabalazs.pressure_integrity_test.config.LinearCalibration;
import ca.lajthabalazs.pressure_integrity_test.io.FileSystemTextFileReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** View model for the calibration step of the new test wizard. */
public class CalibrationStepViewModel {

  public interface Listener {
    void onStateChanged();
  }

  private final List<Listener> listeners = new ArrayList<>();

  private boolean requiresCalibrationFile;
  private File selectedFile;
  private CalibrationConfig calibrationConfig;
  private String loadError;
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

  public boolean isRequiresCalibrationFile() {
    return requiresCalibrationFile;
  }

  public void setRequiresCalibrationFile(boolean requires) {
    this.requiresCalibrationFile = requires;
    notifyListeners();
  }

  public File getSelectedFile() {
    return selectedFile;
  }

  public void setSelectedFile(File file) {
    this.selectedFile = file;
    this.calibrationConfig = null;
    this.loadError = null;
    if (file != null && file.exists()) {
      try {
        CalibrationConfigReader reader =
            new CalibrationConfigReader(new FileSystemTextFileReader());
        this.calibrationConfig = reader.read(file.getAbsolutePath());
      } catch (Exception e) {
        this.loadError = e.getMessage();
      }
    }
    notifyListeners();
  }

  public CalibrationConfig getCalibrationConfig() {
    return calibrationConfig;
  }

  public String getLoadError() {
    return loadError;
  }

  public boolean hasCalibrationLoaded() {
    return calibrationConfig != null;
  }

  public File getRootDirectory() {
    if (selectedFile != null && selectedFile.getParentFile() != null) {
      return selectedFile.getParentFile();
    }
    return new File(System.getProperty("user.dir"));
  }

  /** Human-readable summary of calibration file content for display. */
  public String getCalibrationContentSummary() {
    if (calibrationConfig == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    Map<String, LinearCalibration> map = calibrationConfig.getSensorCalibrations();
    if (map.isEmpty()) {
      sb.append("(No sensor calibrations)");
      return sb.toString();
    }
    for (Map.Entry<String, LinearCalibration> e : map.entrySet()) {
      LinearCalibration cal = e.getValue();
      if (cal != null) {
        sb.append(e.getKey())
            .append(": A = ")
            .append(cal.getA() != null ? cal.getA().toPlainString() : "—")
            .append(", B = ")
            .append(cal.getB() != null ? cal.getB().toPlainString() : "—")
            .append("\n");
      }
    }
    return sb.toString();
  }

  public boolean canFinalize() {
    if (finalized) {
      return false;
    }
    if (requiresCalibrationFile) {
      return hasCalibrationLoaded();
    }
    return true;
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
