package ca.lajthabalazs.pressure_integrity_test.ui.view;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A JTextField for numeric input. When {@code allowDecimals} is true, accepts digits and decimal
 * separator (comma or dot). Dot is displayed as comma. When {@code allowDecimals} is false, accepts
 * only digits (integer mode). Shows a red triangle in the top-right corner when the value cannot be
 * parsed.
 */
public class DecimalTextField extends JTextField {

  private static final Color UNDERLINE_COLOR = new Color(0xcccccc);
  private static final Color ERROR_TRIANGLE_COLOR = new Color(0xdc3545);
  private static final int TRIANGLE_SIZE = 8;

  private final boolean allowDecimals;
  private boolean invalid;

  public DecimalTextField(String text, int columns, boolean allowDecimals) {
    super(
        allowDecimals && text != null ? text.replace('.', ',') : (text != null ? text : ""),
        columns);
    this.allowDecimals = allowDecimals;
    setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UNDERLINE_COLOR));
    setBackground(Color.WHITE);
    getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(DocumentEvent e) {
                if (allowDecimals) {
                  normalizeAndValidate();
                } else {
                  validateValue();
                }
              }

              @Override
              public void removeUpdate(DocumentEvent e) {
                validateValue();
              }

              @Override
              public void changedUpdate(DocumentEvent e) {
                validateValue();
              }
            });
    validateValue();
  }

  public DecimalTextField(String text, int columns) {
    this(text, columns, true);
  }

  public DecimalTextField(int columns) {
    this("", columns, true);
  }

  private void normalizeAndValidate() {
    String text = getText();
    String normalized = text.replace('.', ',');
    if (!text.equals(normalized)) {
      javax.swing.SwingUtilities.invokeLater(() -> setText(normalized));
      return;
    }
    validateValue();
  }

  private void validateValue() {
    boolean wasInvalid = invalid;
    invalid = !isEmpty() && !canParse();
    if (invalid != wasInvalid) {
      repaint();
    }
  }

  private boolean isEmpty() {
    return getText().trim().isEmpty();
  }

  private boolean canParse() {
    try {
      if (allowDecimals) {
        Double.parseDouble(getValueForParsing());
      } else {
        Integer.parseInt(getText().trim());
      }
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Returns the text formatted for parsing. For decimal mode: comma converted to dot. For integer
   * mode: trimmed text.
   */
  public String getValueForParsing() {
    if (allowDecimals) {
      return getText().trim().replace(',', '.');
    }
    return getText().trim();
  }

  public boolean isValidValue() {
    return !invalid;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (invalid) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int x = getWidth() - TRIANGLE_SIZE - 2;
      int y = 2;
      Polygon triangle =
          new Polygon(
              new int[] {x, x + TRIANGLE_SIZE, x + TRIANGLE_SIZE},
              new int[] {y, y, y + TRIANGLE_SIZE},
              3);
      g2.setColor(ERROR_TRIANGLE_COLOR);
      g2.fill(triangle);
      g2.dispose();
    }
  }
}
