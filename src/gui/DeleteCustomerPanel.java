package gui;

import dao.CustomerDAO;
import model.Customer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Content panel for "Delete Customer". Lets the pharmacist search/select
 * a customer, review their details, and delete after confirmation.
 * Customers with prescription history are protected from deletion.
 *
 * Database tables used: customers, prescriptions (history check)
 */
public class DeleteCustomerPanel extends JPanel {

    private final CustomerDAO customerDAO = new CustomerDAO();

    private JTextField searchField;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private List<Customer> lastResults;
    private Customer selectedCustomer;

    private JLabel detailsLabel;
    private JButton deleteButton;

    public DeleteCustomerPanel(PharmacistHomeFrame homeFrame) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel header = new JLabel("Delete Customer");
        header.setFont(new Font("Arial", Font.BOLD, 16));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        JButton searchButton = new JButton("Search (ID / Name / Phone)");
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(header, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.SOUTH);

        tableModel = new DefaultTableModel(
                new Object[]{"Customer ID", "Name", "Phone", "Email", "Address"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultsTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(resultsTable);
        tableScroll.setPreferredSize(new Dimension(100, 220));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        detailsLabel = new JLabel("Select a customer above to see details.");
        deleteButton = new JButton("Delete Selected Customer");
        deleteButton.setEnabled(false);

        JPanel deleteButtonPanel = new JPanel();
        deleteButtonPanel.add(deleteButton);

        bottomPanel.add(detailsLabel, BorderLayout.NORTH);
        bottomPanel.add(deleteButtonPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> performSearch());
        resultsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedCustomer();
            }
        });
        deleteButton.addActionListener(e -> deleteCustomer());
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

            selectedCustomer = null;
            deleteButton.setEnabled(false);
            detailsLabel.setText("Select a customer above to see details.");

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
        selectedCustomer = lastResults.get(row);
        detailsLabel.setText("<html>Customer ID: " + selectedCustomer.getCustomerId()
                + "&nbsp;&nbsp; Name: " + selectedCustomer.getName()
                + "&nbsp;&nbsp; Phone: " + selectedCustomer.getPhone() + "</html>");
        deleteButton.setEnabled(true);
    }

    private void deleteCustomer() {
        if (selectedCustomer == null) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this customer?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            if (customerDAO.hasPrescriptionHistory(selectedCustomer.getCustomerId())) {
                JOptionPane.showMessageDialog(this,
                        "This customer has prescription history and cannot be deleted.",
                        "Deletion Blocked", JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean deleted = customerDAO.deleteCustomer(selectedCustomer.getCustomerId());
            if (deleted) {
                JOptionPane.showMessageDialog(this, "Customer deleted successfully.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Delete failed. Customer may no longer exist.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}