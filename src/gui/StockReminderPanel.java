package gui;

import dao.MedicineDAO;
import model.Medicine;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Content panel for "Stock Refill Reminder". Dynamically lists medicines
 * that need attention: low stock, already expired, or expiring within
 * the next 30 days. No separate low-stock table is used - everything is
 * computed fresh from the medicines table each time.
 *
 * Database table used: medicines
 */
public class StockReminderPanel extends JPanel {

    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final int EXPIRING_SOON_DAYS = 30;

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private DefaultTableModel tableModel;

    public StockReminderPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel header = new JLabel("Stock Refill Reminder (threshold: " + LOW_STOCK_THRESHOLD + " units)");
        header.setFont(new Font("Arial", Font.BOLD, 16));

        tableModel = new DefaultTableModel(
                new Object[]{"Medicine ID", "Medicine Name", "Category", "Stock", "Expiry Date", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);

        JButton refreshButton = new JButton("Refresh");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refreshButton);

        add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        refreshButton.addActionListener(e -> loadMedicines());

        loadMedicines();
    }

    private void loadMedicines() {
        try {
            List<Medicine> medicines = medicineDAO.getAttentionNeededMedicines(LOW_STOCK_THRESHOLD);
            tableModel.setRowCount(0);

            LocalDate today = LocalDate.now();
            for (Medicine m : medicines) {
                boolean lowStock = m.getStockQuantity() <= LOW_STOCK_THRESHOLD;
                boolean expired = m.getExpiryDate() != null && m.getExpiryDate().toLocalDate().isBefore(today);
                boolean expiringSoon = m.getExpiryDate() != null && !expired
                        && !m.getExpiryDate().toLocalDate().isAfter(today.plusDays(EXPIRING_SOON_DAYS));

                String status;
                if (expired) {
                    status = "EXPIRED";
                } else if (lowStock && expiringSoon) {
                    status = "LOW STOCK + EXPIRING SOON";
                } else if (lowStock) {
                    status = "LOW STOCK";
                } else if (expiringSoon) {
                    status = "EXPIRING SOON";
                } else {
                    status = "OK";
                }

                tableModel.addRow(new Object[]{
                        m.getMedicineId(), m.getMedicineName(), m.getCategory(),
                        m.getStockQuantity(), m.getExpiryDate(), status
                });
            }

            if (medicines.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No medicines currently require attention.",
                        "Stock Refill Reminder", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}