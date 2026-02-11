package ca.lajthabalazs.pressure_integrity_test.measurement;

/**
 * Error attached to a {@link MeasurementVector}, associated with a sensor (or derived value) and a
 * severity.
 *
 * @param sensorId ID of the sensor or derived quantity this error refers to (never null)
 * @param severity error severity ({@link ErrorSeverity#WARNING} or {@link ErrorSeverity#SEVERE})
 * @param message human-readable message describing the issue (never null)
 */
public record MeasurementError(String sensorId, ErrorSeverity severity, String message) {}
