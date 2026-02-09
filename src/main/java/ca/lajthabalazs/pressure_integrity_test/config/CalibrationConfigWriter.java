package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import java.io.IOException;
import java.math.BigDecimal;

/** Writes a calibration configuration to a JSON string. */
public class CalibrationConfigWriter {

  private final ObjectMapper objectMapper;

  public CalibrationConfigWriter() {
    this(createDefaultObjectMapper());
  }

  /**
   * Constructor for tests or when using a custom {@link ObjectMapper}.
   *
   * @param objectMapper mapper used to serialize
   */
  public CalibrationConfigWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Returns the JSON fragment for a BigDecimal (plain string or "null"). Used by the default
   * serializer and by tests to cover the null branch.
   */
  public static String formatBigDecimalForJson(BigDecimal value) {
    return value != null ? value.toPlainString() : "null";
  }

  private static ObjectMapper createDefaultObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(
        new SimpleModule()
            .addSerializer(
                BigDecimal.class,
                new StdScalarSerializer<BigDecimal>(BigDecimal.class) {
                  @Override
                  public void serialize(
                      BigDecimal value, JsonGenerator gen, SerializerProvider provider)
                      throws IOException {
                    gen.writeRawValue(formatBigDecimalForJson(value));
                  }
                }));
    return mapper;
  }

  /**
   * Serializes the calibration configuration to a JSON string.
   *
   * @param config the calibration config to write
   * @return the JSON string
   * @throws CalibrationConfigWriteException if serialization fails
   */
  public String writeToString(CalibrationConfig config) throws CalibrationConfigWriteException {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
    } catch (JsonProcessingException e) {
      throw new CalibrationConfigWriteException(
          "Failed to write calibration config: " + e.getMessage(), e);
    }
  }

  /** Thrown when the configuration cannot be serialized. */
  public static class CalibrationConfigWriteException extends Exception {
    public CalibrationConfigWriteException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
