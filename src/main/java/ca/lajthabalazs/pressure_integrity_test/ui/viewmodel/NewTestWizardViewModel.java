package ca.lajthabalazs.pressure_integrity_test.ui.viewmodel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NewTestWizardViewModel {

  public interface Listener {
    void onStateChanged();
  }

  private final List<Listener> listeners = new ArrayList<>();

  private int currentStepIndex = 0;
  private File selectedFile;
  private boolean step1Finalized;
  private TestType testType;
  private final List<StageConfig> stages = new ArrayList<>();
  private boolean step2Finalized;
  private String dataInterfaceText = "";
  private boolean step3Finalized;

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  private void notifyListeners() {
    for (Listener listener : listeners) {
      listener.onStateChanged();
    }
  }

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
      case 0 -> step1Finalized;
      case 1 -> step2Finalized;
      case 2 -> step3Finalized;
      default -> false;
    };
  }

  public boolean isStepEditable(int stepIndex) {
    return !isStepFinalized(stepIndex);
  }

  // Step 1: Site selection
  public File getSelectedFile() {
    return selectedFile;
  }

  public void setSelectedFile(File file) {
    this.selectedFile = file;
    notifyListeners();
  }

  public boolean hasStep1Data() {
    return selectedFile != null && selectedFile.exists();
  }

  public boolean canFinalizeStep1() {
    return hasStep1Data() && !step1Finalized;
  }

  public void finalizeStep1() {
    if (canFinalizeStep1()) {
      step1Finalized = true;
      notifyListeners();
    }
  }

  // Step 2: Test type
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
      stages.remove(stages.size() - 1);
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

  public boolean hasStep2Data() {
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

  public boolean canFinalizeStep2() {
    return hasStep2Data() && !step2Finalized;
  }

  public void finalizeStep2() {
    if (canFinalizeStep2()) {
      step2Finalized = true;
      notifyListeners();
    }
  }

  // Step 3: Data interface
  public String getDataInterfaceText() {
    return dataInterfaceText;
  }

  public void setDataInterfaceText(String text) {
    this.dataInterfaceText = text != null ? text : "";
    notifyListeners();
  }

  public boolean hasStep3Data() {
    return !dataInterfaceText.isBlank();
  }

  public boolean canFinalizeStep3() {
    return hasStep3Data() && !step3Finalized;
  }

  public void finalizeStep3() {
    if (canFinalizeStep3()) {
      step3Finalized = true;
      notifyListeners();
    }
  }

  public boolean canStart() {
    return step1Finalized && step2Finalized && step3Finalized;
  }

  public boolean canFinalizeCurrentStep() {
    return switch (currentStepIndex) {
      case 0 -> canFinalizeStep1();
      case 1 -> canFinalizeStep2();
      case 2 -> canFinalizeStep3();
      default -> false;
    };
  }

  public void finalizeCurrentStep() {
    switch (currentStepIndex) {
      case 0 -> finalizeStep1();
      case 1 -> finalizeStep2();
      case 2 -> finalizeStep3();
      default -> {}
    }
  }

  public void openForEditingStep1() {
    step1Finalized = false;
    notifyListeners();
  }

  public void openForEditingStep2() {
    step2Finalized = false;
    notifyListeners();
  }

  public void openForEditingStep3() {
    step3Finalized = false;
    notifyListeners();
  }

  public void openForEditingCurrentStep() {
    switch (currentStepIndex) {
      case 0 -> openForEditingStep1();
      case 1 -> openForEditingStep2();
      case 2 -> openForEditingStep3();
      default -> {}
    }
  }

  public boolean canOpenForEditingCurrentStep() {
    return isStepFinalized(currentStepIndex);
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
    return currentStepIndex < WizardStep.values().length - 1;
  }
}
