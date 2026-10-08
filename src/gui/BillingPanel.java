package gui;

import dao.BillDAO;
import dao.PrescriptionDAO;
import model.Customer;
import model.PrescriptionMedicine;
import session.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class BillingPanel extends JPanel {

    private final PharmacistHomeFrame homeFrame;
    private final int prescriptionId;
    private final PrescriptionDAO prescriptionDAO = new PrescriptionDAO();
    private final BillDAO billDAO = new BillDAO();

    private List<PrescriptionMedicine> items;
    private double grandTotal;

    public BillingPanel(PharmacistHomeFrame homeFrame, int prescriptionId, Customer customer) {
        this.homeFrame = homeFrame;
        this.prescriptionId = prescriptionId;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel billTitle = new JLabel("BILL", SwingConstants.CENTER);
        billTitle.setFont(new Font("Arial", Font.BOLD, 20));

        JPanel headerPanel = new JPanel(new GridLayout(4, 1));
        DefaultTableModel itemsModel = new DefaultTableModel(
                new Object[]{"Medicine", "Quantity", "Price", "Total"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JLabel grandTotalLabel = new JLabel("Grand Total: Rs. 0.00", SwingConstants.RIGHT);
        grandTotalLabel.setFont(new Font("Arial", Font.BOLD, 14));

        try {
            String pharmacistName = Session.username;

            headerPanel.add(new JLabel("Prescription ID: " + prescriptionId));
            headerPanel.add(new JLabel("Customer ID: " + customer.getCustomerId()
                    + "    Customer Name: " + customer.getName()));
            headerPanel.add(new JLabel("Pharmacist: " + pharmacistName));
            headerPanel.add(new JLabel("Date: " + java.time.LocalDate.now()));

            items = prescriptionDAO.getPrescriptionMedicines(prescriptionId);
            grandTotal = 0;
            for (PrescriptionMedicine item : items) {
                itemsModel.addRow(new Object[]{
                        item.getMedicineName(), item.getQuantity(), item.getUnitPrice(), item.getTotalPrice()
                });
                grandTotal += item.getTotalPrice();
            }

            grandTotalLabel.setText(String.format("Grand Total: Rs. %.2f", grandTotal));

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        JTable itemsTable = new JTable(itemsModel);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(billTitle, BorderLayout.NORTH);
        topPanel.add(headerPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(itemsTable), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(grandTotalLabel, BorderLayout.NORTH);

        JButton finalizeButton = new JButton("Finalize Bill");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(finalizeButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        finalizeButton.addActionListener(e -> finalizeBill());
    }

    private void finalizeBill() {
        if (items == null || items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items to bill.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Confirm this bill? This will update medicine stock and cannot be undone.",
                "Confirm Bill", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            int billId = billDAO.finalizePrescriptionBill(prescriptionId, Session.userId, items);
            JOptionPane.showMessageDialog(this,
                    "Bill #" + billId + " finalized and stock updated successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            homeFrame.returnToHome();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to finalize bill (transaction rolled back):\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}