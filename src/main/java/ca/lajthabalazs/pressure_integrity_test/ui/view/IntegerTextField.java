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
 * A JTextField for integer input. Shows a red triangle in the top-right corner when the value
 * cannot be parsed.
 */
public class IntegerTextField extends JTextField {

  private static final Color UNDERLINE_COLOR = new Color(0xcccccc);
  private static final Color ERROR_TRIANGLE_COLOR = new Color(0xdc3545);
  private static final int TRIANGLE_SIZE = 8;

  private boolean invalid;

  public IntegerTextField(String text, int columns) {
    super(text, columns);
    setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UNDERLINE_COLOR));
    setBackground(Color.WHITE);
    getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(DocumentEvent e) {
                validateValue();
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

  public IntegerTextField(int columns) {
    this("", columns);
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
      Integer.parseInt(getText().trim());
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
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
