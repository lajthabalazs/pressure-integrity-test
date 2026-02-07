package ca.lajthabalazs.pressure_integrity_test.ui.viewmodel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** View model for the test type and stages step (step 2) of the new test wizard. */
public class TestTypeStepViewModel {

  public static final double MIN_OVERPRESSURE_BAR = 0.0;

  /** Fallback max when no site config is loaded; all values are then considered correct. */
  public static final double MAX_OVERPRESSURE_BAR = 2.0;

  public interface Listener {
    void onStateChanged();
  }

  private final List<Listener> listeners = new ArrayList<>();

  private TestType testType;
  private final List<StageConfig> stages = new ArrayList<>();
  private boolean finalized;

  /** When set, stage overpressure is clamped to this max; when null, no site config is loaded. */
  private BigDecimal siteConfigMaxOverpressureBar;

  public TestTypeStepViewModel() {
    setTestType(TestType.EITV);
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

  public TestType getTestType() {
    return testType;
  }

  public void setTestType(TestType type) {
    this.testType = type;
    updateStagesForTestType();
    notifyListeners();
  }

  private void updateStagesForTestType() {
    int count = testType != null ? testType.getStageCount() : 0;
    while (stages.size() > count) {
      stages.removeLast();
    }
    while (stages.size() < count) {
      stages.add(new StageConfig(0.0, 0));
    }
  }

  public List<StageConfig> getStages() {
    return Collections.unmodifiableList(stages);
  }

  public void setStage(int index, StageConfig config) {
    if (index >= 0 && index < stages.size()) {
      stages.set(index, config);
      notifyListeners();
    }
  }

  /**
   * Sets the maximum overpressure (bar) from the loaded site config. When null, no site config is
   * loaded and all values are considered correct (clamping uses {@value #MAX_OVERPRESSURE_BAR} as
   * fallback).
   */
  public void setSiteConfigMaxOverpressureBar(BigDecimal maxBar) {
    this.siteConfigMaxOverpressureBar = maxBar;
  }

  /**
   * Tries to parse overpressure and duration from the raw input strings and update the stage. Does
   * nothing if either field is empty or unparseable. Overpressure is clamped to {@value
   * #MIN_OVERPRESSURE_BAR} and to site config max when loaded, otherwise to {@value
   * #MAX_OVERPRESSURE_BAR}. Hours clamped to 0–24.
   *
   * @param stageIndex zero-based stage index
   * @param overpressureText trimmed decimal string (comma or dot as separator)
   * @param hoursText trimmed integer string
   * @param durationMinutesRemainder minutes part (0, 10, 20, 30, 40, or 50)
   */
  public void tryUpdateStageFromStrings(
      int stageIndex, String overpressureText, String hoursText, int durationMinutesRemainder) {
    if (stageIndex < 0 || stageIndex >= stages.size()) {
      return;
    }
    if (overpressureText == null || overpressureText.isEmpty()) {
      return;
    }
    double maxBar =
        siteConfigMaxOverpressureBar != null
            ? siteConfigMaxOverpressureBar.doubleValue()
            : MAX_OVERPRESSURE_BAR;
    double overpressure;
    try {
      double val = Double.parseDouble(overpressureText.trim().replace(',', '.'));
      overpressure = Math.max(MIN_OVERPRESSURE_BAR, Math.min(maxBar, val));
    } catch (NumberFormatException e) {
      return;
    }
    if (hoursText == null || hoursText.isEmpty()) {
      return;
    }
    int h;
    try {
      int val = Integer.parseInt(hoursText.trim());
      h = Math.max(0, Math.min(24, val));
    } catch (NumberFormatException e) {
      return;
    }
    int durationMinutes = h * 60 + durationMinutesRemainder;
    setStage(stageIndex, new StageConfig(overpressure, durationMinutes));
  }

  public boolean hasData() {
    if (testType == null) {
      return false;
    }
    if (stages.size() != testType.getStageCount()) {
      return false;
    }
    for (StageConfig stage : stages) {
      if (stage.durationMinutes() <= 0) {
        return false;
      }
    }
    return true;
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
