package ca.lajthabalazs.pressure_integrity_test.ui;

import java.awt.Color;

/** Shared UI color constants for dialogs and views. */
public final class UIColors {

  private UIColors() {}

  /** Background color for primary action buttons (e.g. Finalize, Start test). */
  public static final Color PRIMARY_BUTTON_BACKGROUND = new Color(0x2196F3);

  /** Foreground (text) color for primary action buttons. */
  public static final Color PRIMARY_BUTTON_FOREGROUND = Color.WHITE;

  /** Color for validation error (e.g. overpressure exceeds site limit). */
  public static final Color ERROR_MARK_COLOR = new Color(0xdc3545);
}
