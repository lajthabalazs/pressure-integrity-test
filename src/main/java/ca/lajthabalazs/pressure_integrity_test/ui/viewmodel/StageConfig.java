package ca.lajthabalazs.pressure_integrity_test.ui.viewmodel;

/** Configuration for a single test stage. Duration in 10-minute increments. */
public record StageConfig(double overpressureBar, int durationMinutes) {

  private static final int MIN_INCREMENT = 10;

  public StageConfig {
    if (overpressureBar < 0 || overpressureBar > 2) {
      throw new IllegalArgumentException("Overpressure must be between 0 and 2 bar");
    }
    if (durationMinutes < 0 || durationMinutes % MIN_INCREMENT != 0) {
      throw new IllegalArgumentException(
          "Duration must be non-negative and in 10-minute increments");
    }
  }

  public int getDurationHours() {
    return durationMinutes / 60;
  }

  public int getDurationMinutesRemainder() {
    return durationMinutes % 60;
  }

  public static StageConfig of(double overpressureBar, int hours, int minutes) {
    if (minutes % MIN_INCREMENT != 0) {
      throw new IllegalArgumentException("Minutes must be in 10-minute increments");
    }
    return new StageConfig(overpressureBar, hours * 60 + minutes);
  }
}
