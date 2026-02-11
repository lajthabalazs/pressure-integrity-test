package ca.lajthabalazs.pressure_integity_test.measurement;

import ca.lajthabalazs.pressure_integrity_test.measurement.ErrorSeverity;
import ca.lajthabalazs.pressure_integrity_test.measurement.Measurement;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementError;
import ca.lajthabalazs.pressure_integrity_test.measurement.MeasurementVector;
import ca.lajthabalazs.pressure_integrity_test.measurement.Pressure;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Tests for {@link MeasurementVector} error handling. */
public class MeasurementVectorTest {

  @Test
  public void defaultConstructors_haveEmptyErrorList_andNoSevereError() {
    Measurement p = new Pressure(1000L, "P1", new BigDecimal("101325"));
    MeasurementVector v1 = new MeasurementVector(1000L, Map.of("P1", p));
    MeasurementVector v2 = new MeasurementVector(1000L, List.of(p));

    Assertions.assertTrue(v1.getErrors().isEmpty());
    Assertions.assertFalse(v1.hasSevereError());
    Assertions.assertTrue(v2.getErrors().isEmpty());
    Assertions.assertFalse(v2.hasSevereError());
  }

  @Test
  public void vectorWithWarnings_only_hasSevereErrorFalse_andErrorsExposed() {
    Measurement p = new Pressure(1000L, "P1", new BigDecimal("101325"));
    MeasurementError e1 =
        new MeasurementError("P1", ErrorSeverity.WARNING, "Out of recommended range");

    MeasurementVector v = new MeasurementVector(1000L, Map.of("P1", p), List.of(e1));

    Assertions.assertEquals(1, v.getErrors().size());
    Assertions.assertEquals(e1, v.getErrors().getFirst());
    Assertions.assertFalse(v.hasSevereError());
  }

  @Test
  public void vectorWithSevereError_hasSevereErrorTrue() {
    Measurement p = new Pressure(1000L, "P1", new BigDecimal("101325"));
    MeasurementError warning = new MeasurementError("P1", ErrorSeverity.WARNING, "Minor issue");
    MeasurementError severe = new MeasurementError("P1", ErrorSeverity.SEVERE, "Critical failure");

    MeasurementVector v = new MeasurementVector(1000L, List.of(p), List.of(warning, severe));

    Assertions.assertEquals(2, v.getErrors().size());
    Assertions.assertTrue(v.hasSevereError());
  }

  @Test
  public void nullErrorsInConstructors_treatedAsEmptyList() {
    Measurement p = new Pressure(1000L, "P1", new BigDecimal("101325"));

    MeasurementVector v1 = new MeasurementVector(1000L, Map.of("P1", p), null);
    MeasurementVector v2 = new MeasurementVector(1000L, List.of(p), null);

    Assertions.assertTrue(v1.getErrors().isEmpty());
    Assertions.assertFalse(v1.hasSevereError());
    Assertions.assertTrue(v2.getErrors().isEmpty());
    Assertions.assertFalse(v2.hasSevereError());
  }
}
