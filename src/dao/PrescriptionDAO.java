package dao;

import database.DatabaseConnection;
import model.Prescription;
import model.PrescriptionMedicine;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all database access for "prescriptions" and "prescription_medicines".
 *
 * NOTE: Billing (bills / bill_items / stock update) now lives entirely in
 * BillDAO. This class only records WHAT was prescribed, not who billed it
 * or for how much.
 */
public class PrescriptionDAO {

    /**
     * Inserts a prescription header and all its medicine line items inside
     * a single transaction. Does NOT touch medicine stock and does NOT
     * record a pharmacist or a total - those belong to the bill created later.
     *
     * @return the generated prescription_id
     */
    public int savePrescription(int customerId, List<PrescriptionMedicine> items) throws SQLException {

        String insertPrescription =
                "INSERT INTO prescriptions (customer_id, prescription_date) VALUES (?, NOW())";
        String insertItem =
                "INSERT INTO prescription_medicines (prescription_id, medicine_id, quantity, unit_price, total_price) " +
                "VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int prescriptionId;
            try (PreparedStatement ps = conn.prepareStatement(insertPrescription, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, customerId);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    prescriptionId = keys.getInt(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(insertItem)) {
                for (PrescriptionMedicine item : items) {
                    ps.setInt(1, prescriptionId);
                    ps.setInt(2, item.getMedicineId());
                    ps.setInt(3, item.getQuantity());
                    ps.setDouble(4, item.getUnitPrice());
                    ps.setDouble(5, item.getTotalPrice());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            return prescriptionId;

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

    /** Retrieves a prescription header by ID. */
    public Prescription getPrescriptionById(int prescriptionId) throws SQLException {
        String sql = "SELECT * FROM prescriptions WHERE prescription_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, prescriptionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Prescription(
                            rs.getInt("prescription_id"),
                            rs.getInt("customer_id"),
                            rs.getTimestamp("prescription_date")
                    );
                }
            }
        }
        return null;
    }

    /** Retrieves all medicine line items noted on a prescription, joined with medicine names. */
    public List<PrescriptionMedicine> getPrescriptionMedicines(int prescriptionId) throws SQLException {
        List<PrescriptionMedicine> items = new ArrayList<>();
        String sql = "SELECT pm.*, m.medicine_name FROM prescription_medicines pm " +
                     "JOIN medicines m ON pm.medicine_id = m.medicine_id " +
                     "WHERE pm.prescription_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, prescriptionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PrescriptionMedicine pm = new PrescriptionMedicine(
                            rs.getInt("medicine_id"),
                            rs.getString("medicine_name"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price")
                    );
                    pm.setLineId(rs.getInt("prescription_medicine_id"));
                    pm.setTotalPrice(rs.getDouble("total_price"));
                    items.add(pm);
                }
            }
        }
        return items;
    }

    /** Retrieves all prescriptions belonging to one customer, most recent first. */
        // History: one row per prescribed medicine, with its bill (if any) and a Billed / Not billed status.
    public List<Object[]> getPurchaseHistoryByCustomer(int customerId) throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT p.prescription_id, p.prescription_date, " +
                     "m.medicine_name, pm.quantity, pm.total_price, " +
                     "(SELECT MIN(b.bill_id) FROM bills b WHERE b.prescription_id = p.prescription_id) AS bill_id " +
                     "FROM prescriptions p " +
                     "LEFT JOIN prescription_medicines pm ON pm.prescription_id = p.prescription_id " +
                     "LEFT JOIN medicines m ON m.medicine_id = pm.medicine_id " +
                     "WHERE p.customer_id = ? " +
                     "ORDER BY p.prescription_date DESC, p.prescription_id DESC, pm.prescription_medicine_id";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer qty = rs.getInt("quantity");
                    if (rs.wasNull()) qty = null;
                    Double amt = rs.getDouble("total_price");
                    if (rs.wasNull()) amt = null;

                    list.add(new Object[]{
                            rs.getInt("prescription_id"),
                            rs.getString("prescription_date"),
                            rs.getObject("bill_id"),
                            rs.getString("medicine_name"),
                            qty,
                            amt
                    });
                }
            }
        }
        return list;
    }
}