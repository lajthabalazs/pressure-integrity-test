package ca.lajthabalazs.pressure_integrity_test.ui.view;

import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.NewTestWizardViewModel;
import ca.lajthabalazs.pressure_integrity_test.ui.viewmodel.WizardStep;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class NewTestWizardDialog extends JDialog {

  private final NewTestWizardViewModel viewModel;
  private final NewTestWizardStepIndicator stepIndicator;
  private final JPanel contentPanel;
  private final CardLayout cardLayout;
  private final JLabel stepTitleLabel;
  private final SiteSelectionStepPanel siteSelectionPanel;
  private final TestTypeStepPanel testTypePanel;
  private final DataInterfaceStepPanel dataInterfacePanel;
  private final JPanel stepContentPanel;
  private final JButton finalizeButton;
  private final JButton openForEditingButton;
  private final JButton prevButton;
  private final JButton nextButton;
  private final JButton startButton;
  private final JButton cancelButton;

  public NewTestWizardDialog(Frame owner) {
    super(owner, "New Test Setup", true);
    this.viewModel = new NewTestWizardViewModel();

    setLayout(new BorderLayout(10, 10));
    setSize(650, 550);
    setLocationRelativeTo(owner);
    getContentPane().setBackground(Color.WHITE);

    stepIndicator = new NewTestWizardStepIndicator(viewModel);
    add(stepIndicator, BorderLayout.NORTH);

    // Main section: title + content + Finalize/Open for editing
    JPanel mainSection = new JPanel(new BorderLayout(0, 10));
    mainSection.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
    mainSection.setBackground(Color.WHITE);

    stepTitleLabel = new JLabel();
    stepTitleLabel.setFont(stepTitleLabel.getFont().deriveFont(Font.BOLD, 16f));
    mainSection.add(stepTitleLabel, BorderLayout.NORTH);

    cardLayout = new CardLayout();
    contentPanel = new JPanel(cardLayout);
    contentPanel.setBackground(Color.WHITE);

    siteSelectionPanel = new SiteSelectionStepPanel(viewModel);
    testTypePanel = new TestTypeStepPanel(viewModel);
    dataInterfacePanel = new DataInterfaceStepPanel(viewModel);

    contentPanel.add(siteSelectionPanel, WizardStep.SITE_SELECTION.name());
    contentPanel.add(testTypePanel, WizardStep.TEST_TYPE.name());
    contentPanel.add(dataInterfacePanel, WizardStep.DATA_INTERFACE.name());

    stepContentPanel = new JPanel(new BorderLayout());
    stepContentPanel.setBackground(Color.WHITE);
    stepContentPanel.add(contentPanel, BorderLayout.CENTER);

    JPanel finalizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
    finalizePanel.setBackground(Color.WHITE);
    finalizeButton = new JButton("Finalize");
    finalizeButton.addActionListener(e -> onFinalize());
    openForEditingButton = new JButton("Open for editing");
    openForEditingButton.addActionListener(e -> onOpenForEditing());
    finalizePanel.add(finalizeButton);
    finalizePanel.add(openForEditingButton);
    stepContentPanel.add(finalizePanel, BorderLayout.SOUTH);

    mainSection.add(stepContentPanel, BorderLayout.CENTER);
    add(mainSection, BorderLayout.CENTER);

    // Bottom: navigation + Start + Cancel
    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
    bottomPanel.setBackground(Color.WHITE);
    prevButton = new JButton("← Prev");
    prevButton.addActionListener(e -> viewModel.goToPreviousStep());
    nextButton = new JButton("Next →");
    nextButton.addActionListener(e -> viewModel.goToNextStep());
    startButton = new JButton("Start test");
    startButton.addActionListener(e -> onStart());
    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(e -> dispose());

    bottomPanel.add(prevButton);
    bottomPanel.add(nextButton);
    bottomPanel.add(startButton);
    bottomPanel.add(cancelButton);

    add(bottomPanel, BorderLayout.SOUTH);

    viewModel.addListener(this::onViewModelChanged);
    onViewModelChanged();

    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowOpened(WindowEvent e) {
            viewModel.goToStep(0);
          }
        });
  }

  private void onViewModelChanged() {
    cardLayout.show(contentPanel, viewModel.getCurrentStep().name());
    stepIndicator.repaint();

    stepTitleLabel.setText(viewModel.getCurrentStep().getDisplayName());

    updateStepPanelEditability();

    boolean canFinalize = viewModel.canFinalizeCurrentStep();
    boolean canOpenForEditing = viewModel.canOpenForEditingCurrentStep();
    finalizeButton.setEnabled(canFinalize);
    openForEditingButton.setEnabled(canOpenForEditing);

    prevButton.setEnabled(viewModel.canGoToPreviousStep());
    nextButton.setEnabled(viewModel.canGoToNextStep());
    startButton.setEnabled(viewModel.canStart());
  }

  private void updateStepPanelEditability() {
    siteSelectionPanel.setEditable(viewModel.isStepEditable(0));
    siteSelectionPanel.updateFromViewModel();
    testTypePanel.setEditable(viewModel.isStepEditable(1));
    testTypePanel.updateFromViewModel();
    dataInterfacePanel.setEditable(viewModel.isStepEditable(2));
    dataInterfacePanel.updateFromViewModel();
  }

  private void onFinalize() {
    viewModel.finalizeCurrentStep();
  }

  private void onOpenForEditing() {
    viewModel.openForEditingCurrentStep();
  }

  private void onStart() {
    // TODO: Start the test
    dispose();
  }
}
