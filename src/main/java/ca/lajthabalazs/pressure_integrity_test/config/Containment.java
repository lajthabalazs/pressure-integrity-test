package ca.lajthabalazs.pressure_integrity_test.config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;

/** Containment vessel parameters. */
@JsonPropertyOrder({"netVolume_m3"})
public class Containment {

  /** Net volume in cubic metres. */
  private BigDecimal netVolume_m3;

  public Containment() {}

  public Containment(BigDecimal netVolume_m3) {
    this.netVolume_m3 = netVolume_m3;
  }

  public BigDecimal getNetVolume_m3() {
    return netVolume_m3;
  }

  public void setNetVolume_m3(BigDecimal netVolume_m3) {
    this.netVolume_m3 = netVolume_m3;
  }
}
