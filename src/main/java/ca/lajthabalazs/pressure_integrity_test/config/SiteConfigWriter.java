package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import java.io.IOException;
import java.math.BigDecimal;

/** Writes a site configuration to a JSON string. */
public class SiteConfigWriter {

  private final ObjectMapper objectMapper;

  public SiteConfigWriter() {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(
        new SimpleModule()
            .addSerializer(
                BigDecimal.class,
                new StdScalarSerializer<BigDecimal>(BigDecimal.class) {
                  @Override
                  public void serialize(
                      BigDecimal value, JsonGenerator gen, SerializerProvider provider)
                      throws IOException {
                    gen.writeRawValue(value.toPlainString());
                  }
                }));
  }

  /**
   * Serializes the site configuration to a JSON string.
   *
   * @param config the site configuration to write
   * @return the JSON string
   * @throws SiteConfigWriteException if serialization fails
   */
  public String writeToString(SiteConfig config) throws SiteConfigWriteException {
    try {
      return objectMapper.writeValueAsString(config);
    } catch (JsonProcessingException e) {
      throw new SiteConfigWriteException("Failed to write config: " + e.getMessage(), e);
    }
  }

  /** Thrown when the configuration cannot be serialized. */
  public static class SiteConfigWriteException extends Exception {
    public SiteConfigWriteException(String message) {
      super(message);
    }

    public SiteConfigWriteException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
