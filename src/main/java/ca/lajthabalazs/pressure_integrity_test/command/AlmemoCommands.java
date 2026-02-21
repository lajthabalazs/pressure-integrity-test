package ca.lajthabalazs.pressure_integrity_test.command;

import java.util.List;

/**
 * Commands for Ahlborn Almemo devices. Parameters for device (G) and measuring point (M) are
 * two-digit strings sent without any separating character (e.g. G00, M01). See almemo_manual.txt
 * for details.
 */
public final class AlmemoCommands {

  private static final List<Parameter> NO_PARAMS = List.of();
  private static final TwoDigitStringParameter TWO_DIGIT = new TwoDigitStringParameter();

  /** Select device. Parameter: two numeric digits (e.g. 00, 01, 15). Sent as G00, G99, etc. */
  public static final Command G =
      new AlmemoCommand(
          "G",
          "Select device",
          "Selection of a device. Parameter: two numeric digits (00–99). Sent without separator, e.g. G00 or G99.",
          List.of(TWO_DIGIT));

  /** Fetch an overview of the entire setting of the device and the connected sensors. */
  public static final Command P15 =
      new Command(
          "P15",
          "Device and sensors overview",
          "Fetch an overview of the entire setting of the device and the connected sensors.",
          NO_PARAMS);

  /** Fetch an overview of the current device configuration, settings and output modules. */
  public static final Command P19 =
      new Command(
          "P19",
          "Device configuration overview",
          "Fetch an overview of the current device configuration, settings and output modules.",
          NO_PARAMS);

  /** Fetch date. */
  public static final Command P13 = new Command("P13", "Fetch date", "Fetch date.", NO_PARAMS);

  /** Fetch time. */
  public static final Command P10 = new Command("P10", "Fetch time", "Fetch time.", NO_PARAMS);

  /** Fetch measurement time. */
  public static final Command P46 =
      new Command("P46", "Fetch measurement time", "Fetch measurement time.", NO_PARAMS);

  /**
   * Select measuring point. Parameter: two numeric digits (e.g. 00, 01, 95). Sent as M00, M01, etc.
   */
  public static final Command M =
      new AlmemoCommand(
          "M",
          "Select measuring point",
          "Select measuring point. Parameter: two numeric digits (00–99). Sent without separator, e.g. M00 or M95.",
          List.of(TWO_DIGIT));

  /** Outputs measured value for the selected measuring point. */
  public static final Command P01 =
      new Command(
          "P01",
          "Measured value",
          "Outputs measured value for the selected measuring point.",
          NO_PARAMS);

  /** Outputs max value for the selected measuring point. */
  public static final Command P02 =
      new Command(
          "P02", "Max value", "Outputs max value for the selected measuring point.", NO_PARAMS);

  /** Outputs min value for the selected measuring point. */
  public static final Command P03 =
      new Command(
          "P03", "Min value", "Outputs min value for the selected measuring point.", NO_PARAMS);

  /** Outputs measured value list (last, min, max, average, ...). */
  public static final Command P18 =
      new Command(
          "P18",
          "Measured value list",
          "Outputs measured value list (last, min, max, average, ...).",
          NO_PARAMS);

  /** Outputs all measurements in a cycle. */
  public static final Command S1 =
      new Command(
          "S1",
          "Output all measurements in cycle",
          "Outputs all measurements in a cycle.",
          NO_PARAMS);

  /** Start cycle without output of the header. */
  public static final Command S2 =
      new Command(
          "S2", "Start cycle (no header)", "Start cycle without output of the header.", NO_PARAMS);

  /** Start cycle with output of the header. */
  public static final Command S3 =
      new Command(
          "S3", "Start cycle (with header)", "Start cycle with output of the header.", NO_PARAMS);

  /** Stop cycle. */
  public static final Command X = new Command("X", "Stop cycle", "Stop cycle.", NO_PARAMS);

  /**
   * All Almemo commands in logical order (device, overview, date/time, measuring point, values,
   * cycle).
   */
  public static final List<Command> ALL =
      List.of(G, P15, P19, P13, P10, P46, M, P01, P02, P03, P18, S1, S2, S3, X);

  private AlmemoCommands() {}
}
