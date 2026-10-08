package gui;

import session.Session;
import javax.swing.*;
import java.awt.*;


/**
 * Admin home.
 *  - Header (same as the pharmacist home) is always visible.
 *  - MENU screen: Manage Users / Manage Medicines / Billing Reports / Logout, centred.
 *  - Choosing one of the first three hides the menu and opens that panel,
 *    with a "Back to Admin" button on the left side.
 *  - Logout returns to the login page.
 */
public class AdminHomeFrame extends JFrame {

    private static final String CARD_MENU = "MENU";
    private static final String CARD_WORKSPACE = "WORKSPACE";

    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final JPanel workspaceContent = new JPanel(new BorderLayout());

    public AdminHomeFrame() {
        // access control: only ADMIN can open this page
        if (!"ADMIN".equals(Session.role)) {
            JOptionPane.showMessageDialog(null, "Access denied. Please login as Admin.");
            new LoginFrame().setVisible(true);
            dispose();
            return;
        }

        setTitle("Pharmacy Management System - Admin Home");
        setSize(1280, 800);
        setMinimumSize(new Dimension(1000, 650));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        cardPanel.add(buildMenuPanel(), CARD_MENU);
        cardPanel.add(buildWorkspacePanel(), CARD_WORKSPACE);

        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);

        cards.show(cardPanel, CARD_MENU);
    }

    // =====================================================
    // HEADER (same as pharmacist home)
    // =====================================================
    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Pharmacy Management System");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));

        JLabel welcomeLabel = new JLabel("Welcome, " + Session.username + " (Admin)");
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));

        header.add(titleLabel);
        header.add(welcomeLabel);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        return header;
    }

    // =====================================================
    // MENU SCREEN (buttons in the centre)
    // =====================================================
    private JPanel buildMenuPanel() {
        JPanel menu = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 0, 10, 0);
        c.gridx = 0;

        JButton usersBtn = makeMenuButton("Manage Users");
        JButton medBtn = makeMenuButton("Manage Medicines");
        JButton reportBtn = makeMenuButton("Billing Reports");
        JButton logoutBtn = makeMenuButton("Logout");

        c.gridy = 0; menu.add(usersBtn, c);
        c.gridy = 1; menu.add(medBtn, c);
        c.gridy = 2; menu.add(reportBtn, c);
        c.gridy = 3; menu.add(logoutBtn, c);

        usersBtn.addActionListener(e -> openWorkspace(new ManageUsersPanel()));
        medBtn.addActionListener(e -> openWorkspace(new MedicineManagementPanel(() -> backToMenu())));
        reportBtn.addActionListener(e -> openWorkspace(new BillingReportsPanel()));

        logoutBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?",
                    "Confirm Logout", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                Session.clear();
                new LoginFrame().setVisible(true);
                dispose();
            }
        });

        return menu;
    }

    private JButton makeMenuButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 16));
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(300, 52));
        return b;
    }

    // =====================================================
    // WORKSPACE SCREEN (menu hidden, Back to Admin on the side)
    // =====================================================
    private JPanel buildWorkspacePanel() {
    JPanel workspace = new JPanel(new BorderLayout());

    // Add workspaceContent to CENTER — without WEST, it takes 100% of the panel space
    workspace.add(workspaceContent, BorderLayout.CENTER);

    return workspace;
}

    private void openWorkspace(JPanel panel) {
        workspaceContent.removeAll();
        workspaceContent.add(panel, BorderLayout.CENTER);
        workspaceContent.revalidate();
        workspaceContent.repaint();
        cards.show(cardPanel, CARD_WORKSPACE);
    }

    public void backToMenu() {
        workspaceContent.removeAll();
        cards.show(cardPanel, CARD_MENU);
    }
}