package ca.lajthabalazs.pressure_integrity_test.ui.viewmodel;

import java.util.ArrayList;
import java.util.List;

/** View model for the data interface step (step 3) of the new test wizard. */
public class DataInterfaceStepViewModel {

  public interface Listener {
    void onStateChanged();
  }

  private final List<Listener> listeners = new ArrayList<>();

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
