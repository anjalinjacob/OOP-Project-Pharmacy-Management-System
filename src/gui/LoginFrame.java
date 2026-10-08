package gui;

import dao.UserDAO;
import model.User;
import session.Session;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/**
 * First screen of the application. Opens maximized with large title,
 * fields and buttons. Validates username/password against the users
 * table and routes to the correct home page based on role.
 */
public class LoginFrame extends JFrame {

    private static final Font TITLE_FONT  = new Font("Segoe UI", Font.BOLD, 40);
    private static final Font LABEL_FONT  = new Font("Segoe UI", Font.PLAIN, 18);
    private static final Font FIELD_FONT  = new Font("Segoe UI", Font.PLAIN, 15);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 18);

    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("Pharmacy Management System - Login");
        setSize(1000, 700);                           // used if the user un-maximizes
        setMinimumSize(new Dimension(700, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);      // open as a full window

        // ----- the login card -----
        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCCCCCC), 2),
                BorderFactory.createEmptyBorder(40, 60, 40, 60)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(14, 14, 14, 14);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Pharmacy Management System", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 14, 35, 14);
        card.add(titleLabel, gbc);
        gbc.insets = new Insets(14, 14, 14, 14);
        gbc.gridwidth = 1;

// ----- username / password rows in their own panel -----
        usernameField = new JTextField();
        passwordField = new JPasswordField();
        styleField(usernameField);
        styleField(passwordField);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(LABEL_FONT);
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(LABEL_FONT);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints f = new GridBagConstraints();
        f.insets = new Insets(12, 12, 12, 12);
        f.anchor = GridBagConstraints.WEST;

        f.gridx = 0; f.gridy = 0; form.add(userLabel, f);
        f.gridx = 1;              form.add(usernameField, f);
        f.gridx = 0; f.gridy = 1; form.add(passLabel, f);
        f.gridx = 1;              form.add(passwordField, f);

        // add the whole form to the card, centered
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;          // don't stretch the form
        gbc.anchor = GridBagConstraints.CENTER;      // center it in the card
        card.add(form, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;    // restore for the buttons below
        gbc.gridwidth = 1;


        JButton loginButton = new JButton("Login");
        JButton clearButton = new JButton("Clear");
        JButton exitButton = new JButton("Exit");
        for (JButton b : new JButton[]{loginButton, clearButton, exitButton}) {
            b.setFont(BUTTON_FONT);
            b.setPreferredSize(new Dimension(100, 35));
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.add(loginButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(exitButton);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(30, 14, 0, 14);
        card.add(buttonPanel, gbc);

        // ----- centre the card in the maximized window -----
        JPanel background = new JPanel(new GridBagLayout());
        background.add(card);
        add(background);

        getRootPane().setDefaultButton(loginButton);   // Enter key = Login

        loginButton.addActionListener(e -> attemptLogin());
        clearButton.addActionListener(e -> clearFields());
        exitButton.addActionListener(e->System.exit(0));
        passwordField.addActionListener(e -> attemptLogin());
    }

    private void styleField(JTextField field) {
        field.setFont(FIELD_FONT);
        field.setPreferredSize(new Dimension(300, 35));
        
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xAAAAAA)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password cannot be empty.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            UserDAO userDAO = new UserDAO();
            User user = userDAO.validateLogin(username, password);

            if (user == null) {
                JOptionPane.showMessageDialog(this, "Invalid username or password.",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Session.userId = user.getUserId();
            Session.username = user.getUsername();
            Session.role = user.getRole();

            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                new AdminHomeFrame().setVisible(true);
                this.dispose();
            }
            else if ("PHARMACIST".equalsIgnoreCase(user.getRole())) {
                new PharmacistHomeFrame().setVisible(true);
                this.dispose();
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database connection failed:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
    }
}