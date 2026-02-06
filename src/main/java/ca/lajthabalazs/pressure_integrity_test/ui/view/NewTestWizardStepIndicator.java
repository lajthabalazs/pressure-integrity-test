package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.NewTestWizardViewModel;
import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.WizardStep;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;

class NewTestWizardStepIndicator extends JPanel {

  private final NewTestWizardViewModel viewModel;
  private static final int STEP_RADIUS = 18;
  private static final int RING_OFFSET = 4;
  private static final int CONNECTOR_HEIGHT = 3;
  private static final float LABEL_FONT_SIZE = 14f;
  private static final Color LIGHT_GRAY_RING = new Color(180, 180, 180);

  NewTestWizardStepIndicator(NewTestWizardViewModel viewModel) {
    this.viewModel = viewModel;
    setPreferredSize(new Dimension(400, 95));
    setOpaque(true);
    setBackground(Color.WHITE);
    addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            WizardStep[] steps = WizardStep.values();
            int totalWidth = getWidth();
            int stepWidth = totalWidth / steps.length;
            int centerY = getHeight() / 2;
            for (int i = 0; i < steps.length; i++) {
              int centerX = (i * stepWidth) + (stepWidth / 2);
              double dist = Math.hypot(e.getX() - centerX, e.getY() - centerY);
              if (dist <= STEP_RADIUS * 2 && viewModel.isStepAccessible(i)) {
                viewModel.goToStep(i);
                break;
              }
            }
          }
        });
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    java.awt.Font origFont = g2.getFont();
    java.awt.Font labelFont = origFont.deriveFont(LABEL_FONT_SIZE);

    WizardStep[] steps = WizardStep.values();
    int totalWidth = getWidth();
    int stepWidth = totalWidth / steps.length;
    int centerY = getHeight() / 2;

    for (int i = 0; i < steps.length; i++) {
      int centerX = (i * stepWidth) + (stepWidth / 2);
      boolean isSelected = viewModel.getCurrentStepIndex() == i;
      boolean isFinalized = viewModel.isStepFinalized(i);

      // Connector line to next step
      if (i < steps.length - 1) {
        int nextCenterX = ((i + 1) * stepWidth) + (stepWidth / 2);
        int lineY = centerY;
        Color fg = getForeground();
        g2.setColor(
            isFinalized ? fg.brighter() : new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 80));
        g2.fillRoundRect(
            centerX + STEP_RADIUS,
            lineY - CONNECTOR_HEIGHT / 2,
            nextCenterX - centerX - 2 * STEP_RADIUS,
            CONNECTOR_HEIGHT,
            2,
            2);
      }

      // Step circle
      if (isFinalized && isSelected) {
        // Selected finalized step: gray outer ring + white inner ring + green circle with checkmark
        g2.setColor(Color.WHITE);
        g2.fillOval(
            centerX - STEP_RADIUS - RING_OFFSET,
            centerY - STEP_RADIUS - RING_OFFSET,
            (STEP_RADIUS + RING_OFFSET) * 2,
            (STEP_RADIUS + RING_OFFSET) * 2);
        g2.setColor(new Color(76, 175, 80)); // Green for completed
        g2.fillOval(centerX - STEP_RADIUS, centerY - STEP_RADIUS, STEP_RADIUS * 2, STEP_RADIUS * 2);
        g2.setColor(LIGHT_GRAY_RING);
        g2.setStroke(new java.awt.BasicStroke(3));
        g2.drawOval(
            centerX - STEP_RADIUS - RING_OFFSET,
            centerY - STEP_RADIUS - RING_OFFSET,
            (STEP_RADIUS + RING_OFFSET) * 2,
            (STEP_RADIUS + RING_OFFSET) * 2);
        g2.setStroke(new java.awt.BasicStroke(1));
        g2.setColor(Color.WHITE);
        g2.drawString("✓", centerX - 4, centerY + 5);
      } else if (isFinalized) {
        g2.setColor(new Color(76, 175, 80)); // Green for completed
        g2.fillOval(centerX - STEP_RADIUS, centerY - STEP_RADIUS, STEP_RADIUS * 2, STEP_RADIUS * 2);
        g2.setColor(Color.WHITE);
        g2.drawString("✓", centerX - 4, centerY + 5);
      } else if (isSelected) {
        // Selected non-finalized: white ring + gray outer ring + inner circle + step number
        g2.setColor(Color.WHITE);
        g2.fillOval(
            centerX - STEP_RADIUS - RING_OFFSET,
            centerY - STEP_RADIUS - RING_OFFSET,
            (STEP_RADIUS + RING_OFFSET) * 2,
            (STEP_RADIUS + RING_OFFSET) * 2);
        Color fg = getForeground();
        g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 128));
        g2.drawOval(centerX - STEP_RADIUS, centerY - STEP_RADIUS, STEP_RADIUS * 2, STEP_RADIUS * 2);
        g2.setColor(LIGHT_GRAY_RING);
        g2.setStroke(new java.awt.BasicStroke(3));
        g2.drawOval(
            centerX - STEP_RADIUS - RING_OFFSET,
            centerY - STEP_RADIUS - RING_OFFSET,
            (STEP_RADIUS + RING_OFFSET) * 2,
            (STEP_RADIUS + RING_OFFSET) * 2);
        g2.setStroke(new java.awt.BasicStroke(1));
        g2.setColor(getForeground());
        g2.drawString(String.valueOf(i + 1), centerX - 4, centerY + 5);
      } else {
        Color fg = getForeground();
        g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 128));
        g2.drawOval(centerX - STEP_RADIUS, centerY - STEP_RADIUS, STEP_RADIUS * 2, STEP_RADIUS * 2);
        g2.setColor(getForeground());
        g2.drawString(String.valueOf(i + 1), centerX - 4, centerY + 5);
      }

      // Label
      g2.setFont(labelFont);
      String label = steps[i].getDisplayName();
      int labelWidth = g2.getFontMetrics().stringWidth(label);
      g2.setColor(getForeground());
      g2.drawString(label, centerX - labelWidth / 2, centerY + STEP_RADIUS + 22);
    }

    g2.dispose();
  }
}
