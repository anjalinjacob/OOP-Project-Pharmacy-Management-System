package gui;

import dao.CustomerDAO;
import model.Customer;

import javax.swing.*;
import java.awt.*;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Content panel for "Add Customer". Shown inside PharmacistHomeFrame's
 * central content area (no separate JFrame is opened).
 *
 * Database table used: customers
 */
public class AddCustomerPanel extends JPanel {

    private final PharmacistHomeFrame homeFrame;
    private final CustomerDAO customerDAO = new CustomerDAO();
    private JLabel customerIdLabel;
    private JTextField nameField;
    private JTextField phoneField;
    private JTextField emailField;
    private JTextField addressField;
    private JTextField dateRegisteredField;

/** One size controls label, field and button text so everything scales together. */
private static final float FORM_FONT_SIZE = 16f;

public AddCustomerPanel(PharmacistHomeFrame homeFrame) {
    this.homeFrame = homeFrame;
    // No weights anywhere: GridBagLayout centers the whole form
    // horizontally and vertically inside the right-hand content area.
    setLayout(new GridBagLayout());
    setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(8, 12, 8, 12);
    gbc.ipady = 10;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.WEST;

    int row = 0;

    // ----- large heading, centered above the form -----
    JLabel header = new JLabel("Add Customer", SwingConstants.CENTER);
    header.setFont(new Font("Arial", Font.BOLD, 40));
    gbc.gridx = 0;
    gbc.gridy = row++;
    gbc.gridwidth = 2;
    gbc.insets = new Insets(0, 12, 30, 12);
    add(header, gbc);
    gbc.insets = new Insets(10, 14, 10, 14);
    gbc.gridwidth = 1;

    gbc.gridx = 0;
    gbc.gridy = row;
    add(makeLabel("Customer ID:"), gbc);
    gbc.gridx = 1;
    customerIdLabel = makeLabel("(auto-generated)");
    add(customerIdLabel, gbc);
    row++;

    gbc.gridx = 0;
    gbc.gridy = row;
    add(makeLabel("Name:"), gbc);
    nameField = makeField();
    gbc.gridx = 1;
    add(nameField, gbc);
    row++;

    gbc.gridx = 0;
    gbc.gridy = row;
    add(makeLabel("Phone:"), gbc);
    phoneField = makeField();
    gbc.gridx = 1;
    add(phoneField, gbc);
    row++;

    gbc.gridx = 0;
    gbc.gridy = row;
    add(makeLabel("Email:"), gbc);
    emailField = makeField();
    gbc.gridx = 1;
    add(emailField, gbc);
    row++;

    gbc.gridx = 0;
    gbc.gridy = row;
    add(makeLabel("Address:"), gbc);
    addressField = makeField();
    gbc.gridx = 1;
    add(addressField, gbc);
    row++;

    gbc.gridx = 0;
    gbc.gridy = row;
    add(makeLabel("Date Registered:"), gbc);
    dateRegisteredField = makeField();
    dateRegisteredField.setText(LocalDate.now().toString());
    dateRegisteredField.setEditable(false);
    gbc.gridx = 1;
    add(dateRegisteredField, gbc);
    row++;

    // ----- buttons, centered under the form -----
    JButton saveButton = new JButton("Save");
    JButton clearButton = new JButton("Clear");
    for (JButton b : new JButton[]{saveButton, clearButton}) {
        b.setFont(b.getFont().deriveFont(FORM_FONT_SIZE));
        b.setPreferredSize(new Dimension(150, 45));
    }

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 0));
    buttonPanel.add(saveButton);
    buttonPanel.add(clearButton);

    gbc.gridx = 0;
    gbc.gridy = row;
    gbc.gridwidth = 2;
    gbc.ipady = 0;
    gbc.insets = new Insets(30, 12, 0, 12);
    add(buttonPanel, gbc);

    saveButton.addActionListener(e -> saveCustomer());
    clearButton.addActionListener(e -> clearFields());
}

private JLabel makeLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(FORM_FONT_SIZE));
    return label;
}

private JTextField makeField() {
    JTextField field = new JTextField(18);
    field.setFont(field.getFont().deriveFont(FORM_FONT_SIZE));
    return field;
}

    private void saveCustomer() {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String address = addressField.getText().trim();
        LocalDate today = LocalDate.now();
        dateRegisteredField.setText(today.toString());

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name cannot be empty.", "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (phone.isEmpty() || !phone.matches("\\d{7,15}")) {
            JOptionPane.showMessageDialog(this, "Phone must contain 7-15 valid digits.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }



        Customer customer = new Customer();
        customer.setName(name);
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setAddress(address);
        customer.setDateRegistered(Date.valueOf(today));

        try {
            int newId = customerDAO.addCustomer(customer);
            if (newId == -1) {
                JOptionPane.showMessageDialog(this, "Failed to add customer.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            customer.setCustomerId(newId);

            int choice = JOptionPane.showConfirmDialog(this,
                    "Customer added successfully.\nCustomer ID: " + newId
                    + "\nDo you want to create a new prescription?",
                    "Success", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                homeFrame.startPrescriptionWorkflow(customer);
            } else {
                clearFields();
                JOptionPane.showMessageDialog(this,
                        "Customer added successfully.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        nameField.setText("");
        phoneField.setText("");
        emailField.setText("");
        addressField.setText("");
    }
}