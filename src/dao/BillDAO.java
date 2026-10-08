package dao;

import database.DatabaseConnection;
import model.PrescriptionMedicine;

import java.sql.*;
import java.util.List;

/**
 * Handles all database access for "bills" and "bill_items".
 * This is the ONLY place that reduces medicine stock, and it only
 * does so when a bill is finalized - never while searching, selecting,
 * or building a draft prescription/cart.
 */
public class BillDAO {

    private final MedicineDAO medicineDAO = new MedicineDAO();

    /**
     * Finalizes a bill for a PRESCRIPTION sale.
     * Inserts into bills (prescription_id = prescriptionId), inserts the
     * bill_items, and reduces medicine stock - all in one transaction.
     *
     * @return the generated bill_id
     */
    public int finalizePrescriptionBill(int prescriptionId, int pharmacistId,
                                         List<PrescriptionMedicine> items) throws SQLException {
        return finalizeBill(prescriptionId, pharmacistId, items);
    }

    /**
     * Finalizes a bill for a WALK-IN sale with no prescription.
     * prescription_id is stored as NULL and no row is ever written to
     * the prescriptions table for this sale.
     *
     * @return the generated bill_id
     */
    public int finalizeDirectSale(int pharmacistId, List<PrescriptionMedicine> items) throws SQLException {
        return finalizeBill(null, pharmacistId, items);
    }

    private int finalizeBill(Integer prescriptionId, int pharmacistId,
                              List<PrescriptionMedicine> items) throws SQLException {

        if (items == null || items.isEmpty()) {
            throw new SQLException("Cannot finalize a bill with no items.");
        }

        double billAmount = 0;
        for (PrescriptionMedicine item : items) {
            billAmount += item.getTotalPrice();
        }

        String insertBill =
                "INSERT INTO bills (prescription_id, pharmacist_id, bill_amount) VALUES (?, ?, ?)";
        String insertItem =
                "INSERT INTO bill_items (bill_id, medicine_id, quantity, unit_price, total_price) " +
                "VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int billId;
            try (PreparedStatement ps = conn.prepareStatement(insertBill, Statement.RETURN_GENERATED_KEYS)) {
                if (prescriptionId == null) {
                    ps.setNull(1, Types.INTEGER);
                } else {
                    ps.setInt(1, prescriptionId);
                }
                ps.setInt(2, pharmacistId);
                ps.setDouble(3, billAmount);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    billId = keys.getInt(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(insertItem)) {
                for (PrescriptionMedicine item : items) {
                    ps.setInt(1, billId);
                    ps.setInt(2, item.getMedicineId());
                    ps.setInt(3, item.getQuantity());
                    ps.setDouble(4, item.getUnitPrice());
                    ps.setDouble(5, item.getTotalPrice());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // Reduce stock for every item - this is the only place stock changes.
            for (PrescriptionMedicine item : items) {
                boolean reduced = medicineDAO.reduceStock(conn, item.getMedicineId(), item.getQuantity());
                if (!reduced) {
                    throw new SQLException("Insufficient stock for medicine ID " + item.getMedicineId()
                            + " while finalizing the bill.");
                }
            }

            conn.commit();
            return billId;

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }
}