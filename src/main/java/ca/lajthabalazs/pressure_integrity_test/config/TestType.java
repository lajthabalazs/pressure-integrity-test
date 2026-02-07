package ca.lajthabalazs.pressure_integrity_test.config;

/** Test type; the number indicates the required number of stages. */
public enum TestType {
  EITV(1),
  IITV(2),
  TITV(3);

  private final int expectedStageCount;

  TestType(int expectedStageCount) {
    this.expectedStageCount = expectedStageCount;
  }

  public int getExpectedStageCount() {
    return expectedStageCount;
  }
}
