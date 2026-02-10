package ca.lajthabalazs.pressure_integrity_test.ui.viewmodel;

import ca.lajthabalazs.pressure_integrity_test.config.SiteConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * View model for the new test wizard. Composes view models for each step and handles step
 * navigation and cross-step logic.
 */
public class NewTestWizardViewModel {

  public interface Listener {
    void onStateChanged();
  }

  private final List<Listener> listeners = new ArrayList<>();
  private final SiteSelectionStepViewModel siteSelectionStep;
  private final TestTypeStepViewModel testTypeStep;
  private final CalibrationStepViewModel calibrationStep;
  private final DataInterfaceStepViewModel dataInterfaceStep;

  private int currentStepIndex = 0;

  public NewTestWizardViewModel(File rootDirectory) {
    this.siteSelectionStep = new SiteSelectionStepViewModel(rootDirectory);
    this.testTypeStep = new TestTypeStepViewModel();
    this.calibrationStep = new CalibrationStepViewModel();
    this.dataInterfaceStep = new DataInterfaceStepViewModel();

    siteSelectionStep.addListener(this::onSiteSelectionChanged);
    testTypeStep.addListener(this::notifyListeners);
    calibrationStep.addListener(this::notifyListeners);
    dataInterfaceStep.addListener(this::notifyListeners);
  }

  private void onSiteSelectionChanged() {
    updateTestTypeStepSiteConfigMax();
    if (siteSelectionStep.getSiteConfig() != null
        && testTypeStep.isFinalized()
        && stagesExceedSiteOverpressure()) {
      testTypeStep.openForEditing();
    }
    notifyListeners();
  }

  private void updateTestTypeStepSiteConfigMax() {
    var config = siteSelectionStep.getSiteConfig();
    testTypeStep.setSiteConfigMaxOverpressureBar(
        config != null && config.getDesignPressure() != null
            ? config.getDesignPressure().getOverpressure_bar()
            : null);
  }

  private boolean stagesExceedSiteOverpressure() {
    SiteConfig config = siteSelectionStep.getSiteConfig();
    if (config == null || config.getDesignPressure() == null) {
      return false;
    }
    var maxBar = config.getDesignPressure().getOverpressure_bar();
    if (maxBar == null) {
      return false;
    }
    double maxBarDouble = maxBar.doubleValue();
    for (StageConfig stage : testTypeStep.getStages()) {
      if (stage.overpressureBar() > maxBarDouble) {
        return true;
      }
    }
    return false;
  }

  public SiteSelectionStepViewModel getSiteSelectionStep() {
    return siteSelectionStep;
  }

  public TestTypeStepViewModel getTestTypeStep() {
    return testTypeStep;
  }

  public CalibrationStepViewModel getCalibrationStep() {
    return calibrationStep;
  }

  public DataInterfaceStepViewModel getDataInterfaceStep() {
    return dataInterfaceStep;
  }

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  /** Notifies listeners so the UI can refresh (e.g. finalize button state). */
  public void fireChange() {
    notifyListeners();
  }

  private void notifyListeners() {
    for (Listener listener : listeners) {
      listener.onStateChanged();
    }
  }

  // --- Wizard navigation ---

  public int getCurrentStepIndex() {
    return currentStepIndex;
  }

  public WizardStep getCurrentStep() {
    return WizardStep.values()[currentStepIndex];
  }

  public void setCurrentStepIndex(int index) {
    if (index >= 0 && index < WizardStep.values().length) {
      this.currentStepIndex = index;
      notifyListeners();
    }
  }

  public void goToStep(int index) {
    setCurrentStepIndex(index);
  }

  public boolean isStepAccessible(int stepIndex) {
    if (stepIndex == 0) {
      return true;
    }
    return isStepFinalized(stepIndex - 1);
  }

  public boolean isStepFinalized(int stepIndex) {
    return switch (stepIndex) {
      case 0 -> siteSelectionStep.isFinalized();
      case 1 -> calibrationStep.isFinalized();
      case 2 -> testTypeStep.isFinalized();
      case 3 -> dataInterfaceStep.isFinalized();
      default -> false;
    };
  }

  public boolean isStepEditable(int stepIndex) {
    return !isStepFinalized(stepIndex);
  }

  public boolean canFinalizeCurrentStep() {
    return switch (currentStepIndex) {
      case 0 -> siteSelectionStep.canFinalize();
      case 1 -> calibrationStep.canFinalize();
      case 2 -> testTypeStep.canFinalize();
      case 3 -> dataInterfaceStep.canFinalize();
      default -> false;
    };
  }

  public void finalizeCurrentStep() {
    switch (currentStepIndex) {
      case 0 -> siteSelectionStep.finalizeStep();
      case 1 -> calibrationStep.finalizeStep();
      case 2 -> testTypeStep.finalizeStep();
      case 3 -> dataInterfaceStep.finalizeStep();
      default -> {}
    }
  }

  public void openForEditingCurrentStep() {
    switch (currentStepIndex) {
      case 0 -> siteSelectionStep.openForEditing();
      case 1 -> calibrationStep.openForEditing();
      case 2 -> testTypeStep.openForEditing();
      case 3 -> dataInterfaceStep.openForEditing();
      default -> {}
    }
  }

  public boolean canOpenForEditingCurrentStep() {
    return isStepFinalized(currentStepIndex);
  }

  public boolean canStart() {
    return siteSelectionStep.isFinalized()
        && testTypeStep.isFinalized()
        && calibrationStep.isFinalized()
        && dataInterfaceStep.isFinalized();
  }

  public void goToPreviousStep() {
    if (currentStepIndex > 0) {
      setCurrentStepIndex(currentStepIndex - 1);
    }
  }

  public void goToNextStep() {
    if (currentStepIndex < WizardStep.values().length - 1) {
      setCurrentStepIndex(currentStepIndex + 1);
    }
  }

  public boolean canGoToPreviousStep() {
    return currentStepIndex > 0;
  }

  public boolean canGoToNextStep() {
    return currentStepIndex < WizardStep.values().length - 1 && isStepFinalized(currentStepIndex);
  }

  // --- Delegators for backward compatibility (views can use step VMs directly) ---

  public File getRootDirectory() {
    return siteSelectionStep.getRootDirectory();
  }

  public File getSelectedFile() {
    return siteSelectionStep.getSelectedFile();
  }

  public void setSelectedFile(File file) {
    siteSelectionStep.setSelectedFile(file);
  }

  public SiteConfig getSiteConfig() {
    return siteSelectionStep.getSiteConfig();
  }

  public String getSiteConfigLoadError() {
    return siteSelectionStep.getSiteConfigLoadError();
  }

  public TestType getTestType() {
    return testTypeStep.getTestType();
  }

  public void setTestType(TestType type) {
    testTypeStep.setTestType(type);
  }

  public List<StageConfig> getStages() {
    return testTypeStep.getStages();
  }

  public void setStage(int index, StageConfig config) {
    testTypeStep.setStage(index, config);
  }

  public String getDataInterfaceText() {
    return dataInterfaceStep.getDataInterfaceText();
  }

  public void setDataInterfaceText(String text) {
    dataInterfaceStep.setDataInterfaceText(text);
  }
}
