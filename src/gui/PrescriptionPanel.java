package gui;

import dao.MedicineDAO;
import dao.PrescriptionDAO;
import model.Customer;
import model.Medicine;
import model.PrescriptionMedicine;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Prescription workflow panel. Shown full-screen inside PharmacistHomeFrame
 * with the normal header/navigation bar hidden, as required whenever the
 * pharmacist is building a prescription for a specific customer.
 *
 * Database tables used: medicines (search/stock, read-only here),
 * prescriptions + prescription_medicines (written on Save Prescription)
 * IMPORTANT: Stock is NOT reduced here - only later, when the bill is finalized.
 */
public class PrescriptionPanel extends JPanel {

    private final PharmacistHomeFrame homeFrame;
    private final Customer customer;
    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final PrescriptionDAO prescriptionDAO = new PrescriptionDAO();

    private JTextField medicineSearchField;
    private JTable medicineResultsTable;
    private DefaultTableModel medicineResultsModel;
    private List<Medicine> lastSearchResults;

    private JTable cartTable;
    private DefaultTableModel cartModel;
    private final List<PrescriptionMedicine> cartItems = new ArrayList<>();

    private JLabel grandTotalLabel;

    public PrescriptionPanel(PharmacistHomeFrame homeFrame, Customer customer) {
        this.homeFrame = homeFrame;
        this.customer = customer;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- Title + customer info ---
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("New Prescription", SwingConstants.LEFT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        JButton cancelButton = new JButton("Cancel / Back to Home");
        titlePanel.add(titleLabel, BorderLayout.WEST);
        titlePanel.add(cancelButton, BorderLayout.EAST);

        JPanel customerPanel = new JPanel(new GridLayout(1, 3));
        customerPanel.setBorder(BorderFactory.createTitledBorder("Customer"));
        customerPanel.add(new JLabel("Customer ID: " + customer.getCustomerId()));
        customerPanel.add(new JLabel("Name: " + customer.getName()));
        customerPanel.add(new JLabel("Phone: " + customer.getPhone()));

        // --- Medicine search panel ---
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Medicine"));
        medicineSearchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        JButton addToCartButton = new JButton("Add Medicine");
        searchPanel.add(new JLabel("Medicine Name:"));
        searchPanel.add(medicineSearchField);
        searchPanel.add(searchButton);
        searchPanel.add(addToCartButton);

        medicineResultsModel = new DefaultTableModel(
                new Object[]{"Medicine ID", "Medicine Name", "Category", "Price", "Available Stock", "Prescription Required"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        medicineResultsTable = new JTable(medicineResultsModel);
        JScrollPane searchResultsScroll = new JScrollPane(medicineResultsTable);
        searchResultsScroll.setPreferredSize(new Dimension(900, 150));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(titlePanel);
        topPanel.add(customerPanel);
        topPanel.add(searchPanel);
        topPanel.add(searchResultsScroll);

        add(topPanel, BorderLayout.NORTH);

        // --- Current prescription (cart) ---
        cartModel = new DefaultTableModel(
                new Object[]{"Medicine ID", "Medicine Name", "Quantity", "Unit Price", "Total"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cartTable = new JTable(cartModel);
        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartScroll.setBorder(BorderFactory.createTitledBorder("Current Prescription"));
        add(cartScroll, BorderLayout.CENTER);

        // --- Bottom: grand total + actions ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        grandTotalLabel = new JLabel("Grand Total: Rs. 0.00", SwingConstants.RIGHT);
        grandTotalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        grandTotalLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 15));

        JButton removeButton = new JButton("Remove Medicine");
        JButton clearButton = new JButton("Clear Prescription");
        JButton saveButton = new JButton("Save Prescription");
        JPanel actionPanel = new JPanel();
        actionPanel.add(removeButton);
        actionPanel.add(clearButton);
        actionPanel.add(saveButton);

        bottomPanel.add(grandTotalLabel, BorderLayout.NORTH);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> searchMedicines());
        addToCartButton.addActionListener(e -> addSelectedMedicineToCart());
        removeButton.addActionListener(e -> removeSelectedFromCart());
        clearButton.addActionListener(e -> clearCart());
        saveButton.addActionListener(e -> savePrescription());
        cancelButton.addActionListener(e -> homeFrame.returnToHome());
    }

    private void searchMedicines() {
        String name = medicineSearchField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a medicine name to search.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            lastSearchResults = medicineDAO.searchByName(name);
            medicineResultsModel.setRowCount(0);

            for (Medicine m : lastSearchResults) {
                medicineResultsModel.addRow(new Object[]{
                        m.getMedicineId(), m.getMedicineName(), m.getCategory(), m.getPrice(),
                        m.getStockQuantity(), m.isPrescriptionRequired() ? "Yes" : "No"
                });
            }

            if (lastSearchResults.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No medicines found matching \"" + name + "\".",
                        "No Results", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addSelectedMedicineToCart() {
        int row = medicineResultsTable.getSelectedRow();
        if (row == -1 || lastSearchResults == null || row >= lastSearchResults.size()) {
            JOptionPane.showMessageDialog(this, "Please select a medicine from the search results.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Medicine medicine = lastSearchResults.get(row);

        try {
            Medicine fresh = medicineDAO.getMedicineById(medicine.getMedicineId());
            if (fresh == null) {
                JOptionPane.showMessageDialog(this, "Medicine no longer exists.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (fresh.getStockQuantity() == 0) {
                JOptionPane.showMessageDialog(this, "Medicine is currently out of stock.",
                        "Out of Stock", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String input = JOptionPane.showInputDialog(this,
                    "Available stock: " + fresh.getStockQuantity() + "\nEnter quantity:",
                    "Enter Quantity", JOptionPane.QUESTION_MESSAGE);
            if (input == null) {
                return;
            }

            int quantity;
            try {
                quantity = Integer.parseInt(input.trim());
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Quantity must be a valid number.",
                        "Invalid Quantity", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (quantity <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be greater than 0.",
                        "Invalid Quantity", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (quantity > fresh.getStockQuantity()) {
                JOptionPane.showMessageDialog(this, "Insufficient stock available.",
                        "Insufficient Stock", JOptionPane.WARNING_MESSAGE);
                return;
            }

            cartItems.add(new PrescriptionMedicine(
                    fresh.getMedicineId(), fresh.getMedicineName(), quantity, fresh.getPrice()));
            refreshCartTable();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeSelectedFromCart() {
        int row = cartTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a medicine from the current prescription.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        cartItems.remove(row);
        refreshCartTable();
    }

    private void clearCart() {
        cartItems.clear();
        refreshCartTable();
    }

    private void refreshCartTable() {
        cartModel.setRowCount(0);
        double grandTotal = 0;
        for (PrescriptionMedicine item : cartItems) {
            cartModel.addRow(new Object[]{
                    item.getMedicineId(), item.getMedicineName(), item.getQuantity(),
                    item.getUnitPrice(), item.getTotalPrice()
            });
            grandTotal += item.getTotalPrice();
        }
        grandTotalLabel.setText(String.format("Grand Total: Rs. %.2f", grandTotal));
    }

    private void savePrescription() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add at least one medicine before saving.",
                    "Empty Prescription", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int prescriptionId = prescriptionDAO.savePrescription(customer.getCustomerId(), cartItems);
            JOptionPane.showMessageDialog(this, "Prescription saved successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            homeFrame.showBillingForPrescription(prescriptionId, customer);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to save prescription:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}