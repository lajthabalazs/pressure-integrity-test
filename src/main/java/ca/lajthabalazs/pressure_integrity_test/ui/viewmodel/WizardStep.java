package ca.lajthabalazs.pressure_integrity_test.ui.viewmodel;

public enum WizardStep {
  SITE_SELECTION("Site Selection"),
  TEST_TYPE("Test Type"),
  DATA_INTERFACE("Data Interface");

  private final String displayName;

  WizardStep(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
