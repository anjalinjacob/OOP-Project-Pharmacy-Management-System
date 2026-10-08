package gui;

import dao.BillDAO;
import dao.MedicineDAO;
import model.Medicine;
import model.PrescriptionMedicine;
import session.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Content panel for "Issue Medicine Without Prescription" - a walk-in
 * counter sale with no customer or prescription record. Only medicines
 * with prescription_required = false can be searched/sold here.
 *
 * Unlike the Prescription workflow, this stays within the normal
 * Pharmacist Home screen (nav bar remains visible) and finalizes the
 * bill directly, with no separate review/billing page.
 *
 * Database tables used: medicines (search/stock), bills + bill_items (on finalize)
 */
public class IssueMedicinePanel extends JPanel {

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final BillDAO billDAO = new BillDAO();

    private JTextField searchField;
    private JTable searchResultsTable;
    private DefaultTableModel searchResultsModel;
    private List<Medicine> lastSearchResults;

    private JTable cartTable;
    private DefaultTableModel cartModel;
    private final List<PrescriptionMedicine> cartItems = new ArrayList<>();

    private JLabel grandTotalLabel;

    public IssueMedicinePanel(PharmacistHomeFrame homeFrame) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel header = new JLabel("Issue Medicine Without Prescription");
        header.setFont(new Font("Arial", Font.BOLD, 16));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        JButton addToCartButton = new JButton("Add Medicine");
        searchPanel.add(new JLabel("Medicine Name (prescription not required):"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(addToCartButton);

        searchResultsModel = new DefaultTableModel(
                new Object[]{"Medicine ID", "Medicine Name", "Category", "Price", "Available Stock"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        searchResultsTable = new JTable(searchResultsModel);
        JScrollPane searchScroll = new JScrollPane(searchResultsTable);
        searchScroll.setPreferredSize(new Dimension(900, 150));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(header, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.CENTER);
        topPanel.add(searchScroll, BorderLayout.SOUTH);

        cartModel = new DefaultTableModel(
                new Object[]{"Medicine ID", "Medicine Name", "Quantity", "Unit Price", "Total"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cartTable = new JTable(cartModel);
        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartScroll.setBorder(BorderFactory.createTitledBorder("Items to Bill"));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        grandTotalLabel = new JLabel("Grand Total: Rs. 0.00", SwingConstants.RIGHT);
        grandTotalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        grandTotalLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 15));

        JButton removeButton = new JButton("Remove Medicine");
        JButton clearButton = new JButton("Clear");
        JButton finalizeButton = new JButton("Finalize Bill");
        JPanel actionPanel = new JPanel();
        actionPanel.add(removeButton);
        actionPanel.add(clearButton);
        actionPanel.add(finalizeButton);

        bottomPanel.add(grandTotalLabel, BorderLayout.NORTH);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(cartScroll, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> searchMedicines());
        addToCartButton.addActionListener(e -> addSelectedToCart());
        removeButton.addActionListener(e -> removeSelectedFromCart());
        clearButton.addActionListener(e -> clearCart());
        finalizeButton.addActionListener(e -> finalizeBill());
    }

    private void searchMedicines() {
        String name = searchField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a medicine name to search.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            lastSearchResults = medicineDAO.searchAvailableWithoutPrescription(name);
            searchResultsModel.setRowCount(0);

            for (Medicine m : lastSearchResults) {
                searchResultsModel.addRow(new Object[]{
                        m.getMedicineId(), m.getMedicineName(), m.getCategory(),
                        m.getPrice(), m.getStockQuantity()
                });
            }

            if (lastSearchResults.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No non-prescription medicines found matching \"" + name + "\".",
                        "No Results", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addSelectedToCart() {
        int row = searchResultsTable.getSelectedRow();
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
            JOptionPane.showMessageDialog(this, "Please select an item to remove.",
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

    private void finalizeBill() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add at least one medicine before finalizing.",
                    "Empty Bill", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Confirm this walk-in sale? This will update medicine stock.",
                "Confirm Bill", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            int billId = billDAO.finalizeDirectSale(Session.userId, cartItems);
            JOptionPane.showMessageDialog(this,
                    "Bill #" + billId + " finalized and stock updated successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            clearCart();
            searchResultsModel.setRowCount(0);
            searchField.setText("");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to finalize bill (transaction rolled back):\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}