package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calibration configuration: a mapping from sensor id to linear calibration (e.g. for
 * thermometers).
 *
 * <p>JSON format is a single object whose keys are sensor ids and values are {@link
 * LinearCalibration} objects (with A and B).
 */
public class CalibrationConfig {

  private final Map<String, LinearCalibration> sensorCalibrations = new LinkedHashMap<>();

  /** Returns the mapping from sensor id to linear calibration. */
  @JsonAnyGetter
  public Map<String, LinearCalibration> getSensorCalibrations() {
    return sensorCalibrations;
  }

  /** Sets the mapping from sensor id to linear calibration (e.g. when deserializing). */
  @JsonAnySetter
  public void setSensorCalibration(String sensorId, LinearCalibration calibration) {
    if (sensorId != null) {
      sensorCalibrations.put(sensorId, calibration);
    }
  }

  /** Returns the linear calibration for the given sensor id, or null if not present. */
  public LinearCalibration getCalibrationForSensor(String sensorId) {
    return sensorCalibrations.get(sensorId);
  }
}
