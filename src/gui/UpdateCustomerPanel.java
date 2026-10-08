package gui;

import dao.CustomerDAO;
import model.Customer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

import java.sql.SQLException;

import java.util.List;

/**
 * Content panel for "Update Customer". First lets the pharmacist search
 * and select a customer from a small JTable, then loads that customer's
 * details into a large editable form.
 *
 * Database table used: customers
 */
public class UpdateCustomerPanel extends JPanel {

    private static final Font LABEL_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FIELD_FONT = new Font("Segoe UI", Font.PLAIN, 16);

    private final CustomerDAO customerDAO = new CustomerDAO();

    private JTextField searchField;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private List<Customer> lastResults;

    private JTextField idField;
    private JTextField nameField;
    private JTextField phoneField;
    private JTextField emailField;
    private JTextField addressField;
    private JTextField dateRegisteredField;
    private JButton updateButton;

    public UpdateCustomerPanel(PharmacistHomeFrame homeFrame) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel header = new JLabel("Update Customer");
        header.setFont(new Font("Arial", Font.BOLD, 16));

        // --- Search panel ---
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        JButton searchButton = new JButton("Search (ID / Name / Phone)");
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        // --- Results table (small) ---
        tableModel = new DefaultTableModel(
                new Object[]{"Customer ID", "Name", "Phone", "Email", "Address"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultsTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(resultsTable);
        tableScroll.setPreferredSize(new Dimension(100, 110));

        JPanel searchArea = new JPanel(new BorderLayout());
        searchArea.add(searchPanel, BorderLayout.NORTH);
        searchArea.add(tableScroll, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new BorderLayout(0, 5));
        topPanel.add(header, BorderLayout.NORTH);
        topPanel.add(searchArea, BorderLayout.CENTER);

        // --- Edit form (large) ---
        JPanel editPanel = new JPanel(new GridBagLayout());
        editPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Edit Selected Customer"),
                BorderFactory.createEmptyBorder(15, 25, 15, 25)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 8, 10, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        idField = makeField(false);
        nameField = makeField(true);
        phoneField = makeField(true);
        emailField = makeField(true);
        addressField = makeField(true);
        dateRegisteredField = makeField(false);

        int row = 0;
        addFormRow(editPanel, gbc, row++, "Customer ID:", idField);
        addFormRow(editPanel, gbc, row++, "Name:", nameField);
        addFormRow(editPanel, gbc, row++, "Phone:", phoneField);
        addFormRow(editPanel, gbc, row++, "Email:", emailField);
        addFormRow(editPanel, gbc, row++, "Address:", addressField);
        addFormRow(editPanel, gbc, row++, "Date Registered:", dateRegisteredField);

        updateButton = new JButton("Update");
        updateButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        updateButton.setFocusPainted(false);
        updateButton.setPreferredSize(new Dimension(160, 42));
        updateButton.setEnabled(false);
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 8, 8, 8);
        editPanel.add(updateButton, gbc);

        setFieldsEnabled(false);

        // keep the form at the top of the remaining space
        JPanel editWrapper = new JPanel(new BorderLayout());
        editWrapper.add(editPanel, BorderLayout.NORTH);

        add(topPanel, BorderLayout.NORTH);
        add(editWrapper, BorderLayout.CENTER);

        searchButton.addActionListener(e -> performSearch());
        resultsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedCustomer();
            }
        });
        updateButton.addActionListener(e -> updateCustomer());
    }

    // one large text field
    private JTextField makeField(boolean editable) {
        JTextField f = new JTextField(30);
        f.setFont(FIELD_FONT);
        f.setPreferredSize(new Dimension(380, 38));
        f.setEditable(editable);
        return f;
    }

    // one label + field row in the form
    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JTextField field) {
        JLabel l = new JLabel(label);
        l.setFont(LABEL_FONT);

        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 8, 10, 8);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(l, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private void setFieldsEnabled(boolean enabled) {
        nameField.setEnabled(enabled);
        phoneField.setEnabled(enabled);
        emailField.setEnabled(enabled);
        addressField.setEnabled(enabled);
        updateButton.setEnabled(enabled);
    }

    private void performSearch() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Customer ID, name, or phone to search.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            lastResults = customerDAO.searchByIdOrNameOrPhone(text);

            tableModel.setRowCount(0);
            for (Customer c : lastResults) {
                tableModel.addRow(new Object[]{
                        c.getCustomerId(), c.getName(), c.getPhone(), c.getEmail(), c.getAddress()
                });
            }

            if (lastResults.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No matching customers found.",
                        "No Results", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSelectedCustomer() {
        int row = resultsTable.getSelectedRow();
        if (row == -1 || lastResults == null || row >= lastResults.size()) {
            return;
        }
        Customer c = lastResults.get(row);
        idField.setText(String.valueOf(c.getCustomerId()));
        nameField.setText(c.getName());
        phoneField.setText(c.getPhone());
        emailField.setText(c.getEmail());
        addressField.setText(c.getAddress());
        dateRegisteredField.setText(c.getDateRegistered() != null ? c.getDateRegistered().toString() : "");
        setFieldsEnabled(true);
    }

    private void updateCustomer() {
        if (idField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a customer first.",
                    "No Customer Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();

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
        customer.setCustomerId(Integer.parseInt(idField.getText().trim()));
        customer.setName(name);
        customer.setPhone(phone);
        customer.setEmail(emailField.getText().trim());
        customer.setAddress(addressField.getText().trim());
        try {
            boolean updated = customerDAO.updateCustomer(customer);
            if (updated) {
                JOptionPane.showMessageDialog(this, "Customer updated successfully.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                performSearch();
            } else {
                JOptionPane.showMessageDialog(this, "Update failed. Customer may no longer exist.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}