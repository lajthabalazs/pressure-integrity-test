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
  public static final Command PA =
      new Command(
          "PA",
          "Pressure (absolute)",
          "Returns 'PA,x' where x is the current pressure in current units in the same format and "
              + "precision as displayed on the front panel (including decimal point). This value "
              + "will not be affected by tare mode.",
          NO_PARAMS);

  /** Pressure: current pressure and elapsed time in tenths of seconds. */
  public static final Command PB =
      new Command(
          "PB",
          "Pressure and elapsed time",
          "Returns 'PB,x,y' where x is the current pressure as with PA and y is the elapsed time "
              + "in tenths of seconds (see ET for a description of elapsed time).",
          NO_PARAMS);

  /** Pressure: pressure, elapsed time, transducer frequency, diode voltage. */
  public static final Command PF =
      new Command(
          "PF",
          "Pressure and transducer data",
          "Returns 'PF,x,y,z,v' where x and y are as defined for PB and z is the transducer "
              + "frequency and v is the transducer diode voltage.",
          NO_PARAMS);

  /** Pressure: current pressure less tare (PT,? if no tare). */
  public static final Command PT =
      new Command(
          "PT",
          "Pressure (tare)",
          "Returns 'PT,x' where x is the current pressure less the tare pressure in current units. "
              + "Independent of display mode. If the PPG has not been placed in tare mode since "
              + "the last time it was powered up, then 'PT,?' will be returned.",
          NO_PARAMS);

  /** Pressure: current pressure; less tare if upper display in tare mode. */
  public static final Command PS =
      new Command(
          "PS",
          "Pressure (display)",
          "Returns 'PS,x' where x is the current pressure in current units in the same format and "
              + "precision as displayed on the front panel. Will return the pressure less the tare "
              + "pressure if the upper display is in tare mode.",
          NO_PARAMS);

  /** Pressure: continuous transmission state (PC,1 or PC,0). */
  public static final Command PC =
      new Command(
          "PC",
          "Continuous pressure state",
          "Returns 'PC,1' if the continuous pressure transmission is enabled or 'PC,0' if disabled.",
          NO_PARAMS);

  /** Pressure: current continuous pressure interval in tenths of seconds. */
  public static final Command PI =
      new Command(
          "PI",
          "Pressure interval",
          "Returns 'PI,x' where x is the current value of PI (continuous pressure transmission "
              + "interval in tenths of seconds).",
          NO_PARAMS);

  /** Rate: latest rate (RS,x per second or RM,x per minute). */
  public static final Command RS =
      new Command(
          "RS",
          "Rate",
          "Returns 'RS,x' or 'RM,x' where x is the latest rate value in the current units for the "
              + "selected rate period (/min or /sec). Returns 'RS,x' if rate period is per/second "
              + "and 'RM,x' if per/minute. Will not affect display mode.",
          NO_PARAMS);

  /** Rate: continuous rate transmission state. */
  public static final Command RC =
      new Command(
          "RC",
          "Continuous rate state",
          "Returns 'RC,1' if the continuous rate transmission is enabled or 'RC,0' if disabled.",
          NO_PARAMS);

  /** Rate: continuous rate interval in tenths of seconds. */
  public static final Command RI =
      new Command(
          "RI",
          "Rate interval",
          "Returns 'RI,x' where x is the current value of RI (continuous rate transmission interval "
              + "in tenths of seconds).",
          NO_PARAMS);

  /** Rate: rate period – seconds (0) or minutes (1). */
  public static final Command RP =
      new Command(
          "RP",
          "Rate period",
          "Returns 'RP,x' where x is the current value of RP (0 = seconds, 1 = minutes).",
          NO_PARAMS);

  /** Rate: rate display on/off. */
  public static final Command RO =
      new Command(
          "RO",
          "Rate display",
          "Returns 'RO,x' where x is the current value of RO (1 = rate display on, 0 = off).",
          NO_PARAMS);

  /** Units: current units code (0–14, see manual). */
  public static final Command UN =
      new Command(
          "UN",
          "Units",
          "Returns 'UN,x' where x is the current units code (0=inHg, 1=psi, 2=mbar, 3=kPa, etc.; "
              + "see manual for full table).",
          NO_PARAMS);

  /** Units: user-defined unit x (1–4); returns conversion constant and abbreviation. */
  public static final Command UD =
      new Command(
          "UD",
          "User-defined unit",
          "Returns UD,x,y,z where x is 1 to 4 for the user defined unit, y is the conversion "
              + "constant (multiplied by kPa), and z is the 4 character units code abbreviation "
              + "for the front panel display.",
          List.of(new IntParameter(1, 4)));

  /** Misc: elapsed time in tenths of seconds. */
  public static final Command ET =
      new Command(
          "ET",
          "Elapsed time",
          "Returns 'ET,x' where x is the elapsed time in tenths of seconds since the elapsed time "
              + "clock was started. Timer is set to 0 at power on; after 24 hours (ET=864000) it "
              + "resets to 0.",
          NO_PARAMS);

  /** Misc: tare mode (0=off, 1=upper tare, 2=lower tare). */
  public static final Command TM =
      new Command(
          "TM",
          "Tare mode",
          "Returns 'TM,x' where x is the tare mode (0=tare display off, 1=upper display in tare mode, "
              + "2=lower display in tare mode).",
          NO_PARAMS);

  /** Misc: error code from error buffer. */
  public static final Command ER =
      new Command(
          "ER",
          "Error code",
          "Returns 'ER,x' where x is the error code from the error code buffer. See Section 5.3.6 "
              + "for list of error codes.",
          NO_PARAMS);

  /** Misc: self-test; returns first result (use ER for more). */
  public static final Command ST =
      new Command(
          "ST",
          "Self-test",
          "Puts the upper display in test mode and performs a self test. Clears error buffer and "
              + "inserts test result(s). Returns 'ST,x' where x is the first test result as defined "
              + "for error codes. Use ER message to read additional results if any.",
          NO_PARAMS);

  /** Misc: battery voltage. */
  public static final Command XB =
      new Command(
          "XB", "Battery voltage", "Returns XB,x where x is the battery voltage.", NO_PARAMS);

  /** Misc: main board software version. */
  public static final Command V1 =
      new Command(
          "V1",
          "Main board version",
          "Returns V1,x where x is the version of main board software.",
          NO_PARAMS);

  /** Misc: front panel software version. */
  public static final Command V2 =
      new Command(
          "V2",
          "Front panel version",
          "Returns V2,x where x is the version of front panel software. Returns V2,0.00 if there "
              + "is no front panel.",
          NO_PARAMS);

  /** Misc: echo mode on/off. */
  public static final Command ECHO =
      new Command(
          "ECHO",
          "Echo mode",
          "Returns ECHO,x where x is 1 if echo mode is on and 0 if echo mode is off.",
          NO_PARAMS);

  /** Misc: pressure medium (N₂=1, Air=0). */
  public static final Command MD =
      new Command(
          "MD",
          "Pressure medium",
          "Returns the value for MD (pressure medium: N₂=1, Air=0).",
          NO_PARAMS);

  /** All Ruska read commands in manual order (pressure, rate, units, misc). */
  public static final List<Command> ALL =
      List.of(
          PA, PB, PF, PT, PS, PC, PI, RS, RC, RI, RP, RO, UN, UD, ET, TM, ER, ST, XB, V1, V2, ECHO,
          MD);

  private RuskaReadCommands() {}
}
