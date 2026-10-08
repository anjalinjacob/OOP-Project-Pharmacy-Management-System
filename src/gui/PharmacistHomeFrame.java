package gui;

import model.Customer;
import session.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * The ONE main window for the pharmacist. It never opens a new JFrame
 * for Add/Update/Delete/Search Customer, Issue Medicine, or Stock Reminder -
 * those just swap the central content panel in place (CardLayout).
 *
 * The only time the whole window changes "mode" is during the
 * Prescription -> Billing workflow, where the header + navigation bar
 * are hidden and a full-width workflow panel takes over, as required.
 */
public class PharmacistHomeFrame extends JFrame {

    private static final String CARD_HOME = "HOME";
    private static final String CARD_WORKFLOW = "WORKFLOW";

    private final CardLayout outerLayout = new CardLayout();
    private final JPanel outerPanel = new JPanel(outerLayout);

    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private final JPanel workflowPanel = new JPanel(new BorderLayout());

    private JPanel nav;
    private JButton[] navButtons;

    public PharmacistHomeFrame() {
        setTitle("Pharmacy Management System - Pharmacist Home");
        setSize(1280, 800);
        setMinimumSize(new Dimension(1000, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // keep the nav bar proportional to the window whenever it is resized
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeNav();
            }
        });
        // ----- HOME card: header + nav + dynamic content -----
        JPanel homeCard = new JPanel(new BorderLayout());
        homeCard.add(buildHeaderPanel(), BorderLayout.NORTH);
        homeCard.add(buildNavPanel(), BorderLayout.WEST);
        homeCard.add(contentPanel, BorderLayout.CENTER);

        // ----- WORKFLOW card: just the dynamic workflow panel, no nav -----
        outerPanel.add(homeCard, CARD_HOME);
        outerPanel.add(workflowPanel, CARD_WORKFLOW);

        add(outerPanel);

        showWelcomePanel();
        outerLayout.show(outerPanel, CARD_HOME);
    }

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel("Pharmacy Management System");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));

        JLabel welcomeLabel = new JLabel("Welcome, " + Session.username);
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));

        header.add(titleLabel);
        header.add(welcomeLabel);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        return header;
    }

    private JPanel buildNavPanel() {
    nav = new JPanel();
    nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
    nav.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));

    JButton addCustomerBtn = new JButton("Add Customer");
    JButton updateCustomerBtn = new JButton("Update Customer");
    JButton deleteCustomerBtn = new JButton("Delete Customer");
    JButton searchCustomerBtn = new JButton("Search Customer");
    JButton issueMedicineBtn = new JButton("Issue Medicine Without Prescription");
    JButton stockReminderBtn = new JButton("Stock Refill Reminder");
    JButton logoutBtn = new JButton("Logout");

    navButtons = new JButton[]{addCustomerBtn, updateCustomerBtn, deleteCustomerBtn,
            searchCustomerBtn, issueMedicineBtn, stockReminderBtn, logoutBtn};

    Font navFont = new Font("Segoe UI", Font.BOLD, 15);
    for (JButton btn : navButtons) {
        btn.setFont(navFont);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMargin(new Insets(6, 14, 6, 14));
        btn.setFocusPainted(false);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        // full width of the nav bar, never cuts the text
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
    }

    // first six buttons stacked at the top
    for (int i = 0; i < navButtons.length - 1; i++) {
        nav.add(navButtons[i]);
        nav.add(Box.createRigidArea(new Dimension(0, 6)));
    }
    nav.add(Box.createVerticalGlue());          // pushes Logout to the bottom
    nav.add(logoutBtn);

    addCustomerBtn.addActionListener(e -> showHomeContent(new AddCustomerPanel(this)));
    updateCustomerBtn.addActionListener(e -> showHomeContent(new UpdateCustomerPanel(this)));
    deleteCustomerBtn.addActionListener(e -> showHomeContent(new DeleteCustomerPanel(this)));
    searchCustomerBtn.addActionListener(e -> showHomeContent(new SearchCustomerPanel(this)));
    issueMedicineBtn.addActionListener(e -> showHomeContent(new IssueMedicinePanel(this)));
    stockReminderBtn.addActionListener(e -> showHomeContent(new StockReminderPanel()));
    logoutBtn.addActionListener(e -> logout());

    resizeNav();
    return nav;
}

/** Nav width = 22% of the window, but never narrower than the longest button text. */
    private void resizeNav() {
        if (nav == null || navButtons == null) {
            return;
        }
        int widest = 0;
        for (JButton b : navButtons) {
            widest = Math.max(widest, b.getPreferredSize().width);
        }
        int needed = widest + 30 + 10;                  // + nav side borders + a little spare
        int proportional = (int) (getWidth() * 0.16);
        nav.setPreferredSize(new Dimension(Math.max(needed, proportional), 0));
        nav.revalidate();
    }

        private void showWelcomePanel() {
            JPanel welcome = new JPanel(new BorderLayout());
            JLabel label = new JLabel("Select an option from the menu to get started.", SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 22));
            welcome.add(label, BorderLayout.CENTER);
            showHomeContent(welcome);
        }

        /** Replaces the central content area while staying on the HOME card (nav bar stays visible). */
        public void showHomeContent(JPanel panel) {
            contentPanel.removeAll();
            contentPanel.add(panel, BorderLayout.CENTER);
            contentPanel.revalidate();
            contentPanel.repaint();
            outerLayout.show(outerPanel, CARD_HOME);
        }

    /**
     * Starts the Prescription workflow for a given customer.
     * Hides the header/navigation bar (switches to the WORKFLOW card).
     */
    public void startPrescriptionWorkflow(Customer customer) {
        showWorkflowPanel(new PrescriptionPanel(this, customer));
    }

    /** Moves from the Prescription panel to the Billing panel for the given prescription. */
    public void showBillingForPrescription(int prescriptionId, Customer customer) {
        showWorkflowPanel(new BillingPanel(this, prescriptionId, customer));
    }

    private void showWorkflowPanel(JPanel panel) {
        workflowPanel.removeAll();
        workflowPanel.add(panel, BorderLayout.CENTER);
        workflowPanel.revalidate();
        workflowPanel.repaint();
        outerLayout.show(outerPanel, CARD_WORKFLOW);
    }

    /** Called when the prescription/billing workflow finishes (or is cancelled). Restores the nav bar. */
    public void returnToHome() {
        showWelcomePanel();
        outerLayout.show(outerPanel, CARD_HOME);
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            Session.clear();
            new LoginFrame().setVisible(true);
            this.dispose();
        }
    }
}