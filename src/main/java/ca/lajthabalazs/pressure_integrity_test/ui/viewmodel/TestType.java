package ca.lajthabalazs.pressure_integrity_test.ui.viewmodel;

public enum TestType {
  EITV(1),
  TITV(2),
  IITV(3);

  private final int stageCount;

  TestType(int stageCount) {
    this.stageCount = stageCount;
  }

  public int getStageCount() {
    return stageCount;
  }

  @Override
  public String toString() {
    return name() + " (" + stageCount + " stage" + (stageCount > 1 ? "s" : "") + ")";
  }
}
