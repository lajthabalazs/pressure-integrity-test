package ca.lajthabalazs.pressure_integrity_test.command;

import java.util.List;

/**
 * Read-only commands for the Ruska PPG (Pressure Pressure Gauge) as defined in the Ruska manual.
 * Each command is sent to the device and returns a response; parameters are only used where the
 * protocol requires them (e.g. UD,x).
 */
public final class RuskaReadCommands {

  private static final List<Parameter> NO_PARAMS = List.of();

  /** Pressure: current pressure in current units (same format as front panel). */
  public static final Command PA = new Command("PA", "Pressure (absolute)", NO_PARAMS);

  /** Pressure: current pressure and elapsed time in tenths of seconds. */
  public static final Command PB = new Command("PB", "Pressure and elapsed time", NO_PARAMS);

  /** Pressure: pressure, elapsed time, transducer frequency, diode voltage. */
  public static final Command PF = new Command("PF", "Pressure and transducer data", NO_PARAMS);

  /** Pressure: current pressure less tare (PT,? if no tare). */
  public static final Command PT = new Command("PT", "Pressure (tare)", NO_PARAMS);

  /** Pressure: current pressure; less tare if upper display in tare mode. */
  public static final Command PS = new Command("PS", "Pressure (display)", NO_PARAMS);

  /** Pressure: continuous transmission state (PC,1 or PC,0). */
  public static final Command PC = new Command("PC", "Continuous pressure state", NO_PARAMS);

  /** Pressure: current continuous pressure interval in tenths of seconds. */
  public static final Command PI = new Command("PI", "Pressure interval", NO_PARAMS);

  /** Rate: latest rate (RS,x per second or RM,x per minute). */
  public static final Command RS = new Command("RS", "Rate", NO_PARAMS);

  /** Rate: continuous rate transmission state. */
  public static final Command RC = new Command("RC", "Continuous rate state", NO_PARAMS);

  /** Rate: continuous rate interval in tenths of seconds. */
  public static final Command RI = new Command("RI", "Rate interval", NO_PARAMS);

  /** Rate: rate period – seconds (0) or minutes (1). */
  public static final Command RP = new Command("RP", "Rate period", NO_PARAMS);

  /** Rate: rate display on/off. */
  public static final Command RO = new Command("RO", "Rate display", NO_PARAMS);

  /** Units: current units code (0–14, see manual). */
  public static final Command UN = new Command("UN", "Units", NO_PARAMS);

  /** Units: user-defined unit x (1–4); returns conversion constant and abbreviation. */
  public static final Command UD =
      new Command("UD", "User-defined unit", List.of(new IntParameter(1, 4)));

  /** Misc: elapsed time in tenths of seconds. */
  public static final Command ET = new Command("ET", "Elapsed time", NO_PARAMS);

  /** Misc: tare mode (0=off, 1=upper tare, 2=lower tare). */
  public static final Command TM = new Command("TM", "Tare mode", NO_PARAMS);

  /** Misc: error code from error buffer. */
  public static final Command ER = new Command("ER", "Error code", NO_PARAMS);

  /** Misc: self-test; returns first result (use ER for more). */
  public static final Command ST = new Command("ST", "Self-test", NO_PARAMS);

  /** Misc: battery voltage. */
  public static final Command XB = new Command("XB", "Battery voltage", NO_PARAMS);

  /** Misc: main board software version. */
  public static final Command V1 = new Command("V1", "Main board version", NO_PARAMS);

  /** Misc: front panel software version. */
  public static final Command V2 = new Command("V2", "Front panel version", NO_PARAMS);

  /** Misc: echo mode on/off. */
  public static final Command ECHO = new Command("ECHO", "Echo mode", NO_PARAMS);

  /** Misc: pressure medium (N₂=1, Air=0). */
  public static final Command MD = new Command("MD", "Pressure medium", NO_PARAMS);

  /** All Ruska read commands in manual order (pressure, rate, units, misc). */
  public static final List<Command> ALL =
      List.of(
          PA, PB, PF, PT, PS, PC, PI, RS, RC, RI, RP, RO, UN, UD, ET, TM, ER, ST, XB, V1, V2, ECHO,
          MD);

  private RuskaReadCommands() {}
}
