package ca.lajthabalazs.pressure_integity_test.config;

import ca.lajthabalazs.pressure_integity_test.io.ResourceTextFileReader;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfig;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfigReader;
import ca.lajthabalazs.pressure_integrity_test.config.SiteConfigWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SiteConfigWriterTest {

  @Test
  public void write_fullConfig_producesJsonMatchingExpectedResource() throws Exception {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(SiteConfigWriterTest.class);
    SiteConfigReader reader = new SiteConfigReader(fileReader);
    SiteConfigWriter writer = new SiteConfigWriter();

    SiteConfig config = reader.read("/site-config-sample.json");
    String written = writer.writeToString(config);
    String expected = fileReader.readAllText("/site-config-write-expected-sample.json");
    Assertions.assertEquals(expected, written);
  }

  @Test
  public void write_preservesDecimalPoints_inWrittenJson() throws Exception {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(SiteConfigWriterTest.class);
    SiteConfigReader reader = new SiteConfigReader(fileReader);
    SiteConfigWriter writer = new SiteConfigWriter();

    SiteConfig config = reader.read("/site-config-sample.json");
    String written = writer.writeToString(config);

    // Verify decimals are written in full (no truncation)
    Assertions.assertTrue(
        written.contains("0.018993"),
        "Written JSON should contain volumeWeight 0.018993; got: " + written);
    Assertions.assertTrue(
        written.contains("\"sigma\":0.01"),
        "Written JSON should contain sigma 0.01; got: " + written);
    Assertions.assertTrue(
        written.contains("\"sigma\":0.2"),
        "Written JSON should contain sigma 0.2; got: " + written);
    Assertions.assertTrue(
        written.contains("14.7"), "Written JSON should contain leakLimit 14.7; got: " + written);

    // Verify no scientific notation for these values
    Assertions.assertFalse(
        written.matches(".*0\\.01899[^3].*"), "Written JSON should not truncate 0.018993");
    Assertions.assertFalse(
        written.contains("1.47E"), "Written JSON should not use scientific notation for 14.7");
    Assertions.assertFalse(
        written.contains("1.8993E"),
        "Written JSON should not use scientific notation for 0.018993");
  }

  @Test
  public void write_emptySensorsConfig_producesJsonMatchingExpectedResource() throws Exception {
    ResourceTextFileReader fileReader = new ResourceTextFileReader(SiteConfigWriterTest.class);
    SiteConfigReader reader = new SiteConfigReader(fileReader);
    SiteConfigWriter writer = new SiteConfigWriter();

    SiteConfig config = reader.read("/site-config-empty-sensors.json");
    String written = writer.writeToString(config);
    String expected = fileReader.readAllText("/site-config-write-expected-empty-sensors.json");

    Assertions.assertEquals(expected, written);
  }
}
