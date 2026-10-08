package gui;

import dao.CustomerDAO;
import dao.PrescriptionDAO;
import model.Customer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * Content panel for "Search Customer". Searches by ID, Name, or Phone and
 * shows results in a JTable. From a selected row the pharmacist can start
 * a new prescription (which hides the nav bar) or view prescription history.
 *
 * Database tables used: customers, prescriptions (for history)
 */
public class SearchCustomerPanel extends JPanel {

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final PrescriptionDAO prescriptionDAO = new PrescriptionDAO();
    private final PharmacistHomeFrame homeFrame;

    private JTextField idField;
    private JTextField nameField;
    private JTextField phoneField;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private List<Customer> lastResults;

    public SearchCustomerPanel(PharmacistHomeFrame homeFrame) {
        this.homeFrame = homeFrame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel header = new JLabel("Search Customer");
        header.setFont(new Font("Arial", Font.BOLD, 16));

        JPanel searchPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        searchPanel.add(new JLabel("Customer ID:"), gbc);
        idField = new JTextField(8);
        gbc.gridx = 1;
        searchPanel.add(idField, gbc);

        gbc.gridx = 2;
        searchPanel.add(new JLabel("Name:"), gbc);
        nameField = new JTextField(10);
        gbc.gridx = 3;
        searchPanel.add(nameField, gbc);

        gbc.gridx = 4;
        searchPanel.add(new JLabel("Phone:"), gbc);
        phoneField = new JTextField(10);
        gbc.gridx = 5;
        searchPanel.add(phoneField, gbc);

        JButton searchButton = new JButton("Search");
        gbc.gridx = 6;
        searchPanel.add(searchButton, gbc);

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

        JButton createPrescriptionButton = new JButton("Create New Prescription");
        JButton viewHistoryButton = new JButton("View Previous Prescriptions");
        JPanel actionPanel = new JPanel();
        actionPanel.add(createPrescriptionButton);
        actionPanel.add(viewHistoryButton);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(resultsTable), BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> performSearch());
        createPrescriptionButton.addActionListener(e -> createPrescriptionForSelected());
        viewHistoryButton.addActionListener(e -> viewHistoryForSelected());
    }

    private void performSearch() {
        String id = idField.getText().trim();
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();

        if (id.isEmpty() && name.isEmpty() && phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter at least one search value.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            lastResults = customerDAO.searchCustomers(id, name, phone);
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
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Customer ID must be numeric.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Customer getSelectedCustomer() {
        int row = resultsTable.getSelectedRow();
        if (row == -1 || lastResults == null || row >= lastResults.size()) {
            JOptionPane.showMessageDialog(this, "Please select a customer from the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return lastResults.get(row);
    }

    private void createPrescriptionForSelected() {
        Customer selected = getSelectedCustomer();
        if (selected == null) {
            return;
        }
        homeFrame.startPrescriptionWorkflow(selected);
    }

        private void viewHistoryForSelected() {
        Customer selected = getSelectedCustomer();
        if (selected == null) {
            return;
        }
        try {
            List<Object[]> rows = prescriptionDAO.getPurchaseHistoryByCustomer(selected.getCustomerId());
            if (rows.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No previous prescriptions for this customer.",
                        "Prescription History", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            DefaultTableModel model = new DefaultTableModel(
                    new Object[]{"Prescription ID", "Date", "Bill ID", "Medicine", "Quantity", "Amount"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            Set<Object> prescriptionIds = new HashSet<>();
            int units = 0;
            double total = 0;
            for (Object[] r : rows) {
                prescriptionIds.add(r[0]);
                boolean purchased = r[3] != null;
                if (purchased) {
                    units += (Integer) r[4];
                    total += (Double) r[5];
                }
                model.addRow(new Object[]{
                        r[0],
                        r[1],
                        r[2] == null ? "-" : r[2],
                        purchased ? r[3] : "No medicines purchased",
                        purchased ? r[4] : "",
                        purchased ? String.format("%.2f", (Double) r[5]) : ""
                });
            }
            model.addRow(new Object[]{"", "", "", "TOTAL", units, String.format("%.2f", total)});

            JTable table = new JTable(model);
            table.setRowHeight(30);
            table.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 15));

            JLabel summary = new JLabel("Customer: " + selected.getName()
                    + "   |   Prescriptions: " + prescriptionIds.size()
                    + "   |   Medicine units: " + units
                    + "   |   Total spent: " + String.format("%.2f", total));
            summary.setFont(new Font("Segoe UI", Font.BOLD, 16));
            summary.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

            JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                    "Prescription History - " + selected.getName(),
                    Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setLayout(new BorderLayout());
            dialog.add(summary, BorderLayout.NORTH);
            dialog.add(new JScrollPane(table), BorderLayout.CENTER);
            dialog.setSize(900, 560);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}