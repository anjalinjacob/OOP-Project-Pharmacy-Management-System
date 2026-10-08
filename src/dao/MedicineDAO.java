package dao;

import database.DatabaseConnection;
import model.Medicine;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Single DAO for the "medicines" table, shared by:
 *  - the Pharmacist module (search, stock reminder, billing / reduceStock)
 *  - the Admin module (add, list, search, update, delete, stock & expiry)
 *
 * Table: medicines
 * Columns: medicine_id, medicine_name, category, price, stock_quantity,
 *          expiry_date (DATE), prescription_required
 *
 * Pharmacist methods keep throwing SQLException; admin methods keep catching
 * errors and returning false / empty results - so neither GUI needs to change.
 */
public class MedicineDAO {

    private static final String COLUMNS =
            "medicine_id, medicine_name, category, price, stock_quantity, " +
            "expiry_date, prescription_required";

    // =====================================================
    // PHARMACIST METHODS (unchanged behaviour)
    // =====================================================

    /** Searches ALL medicines whose name contains the given text (used by the prescription workflow). */
    public List<Medicine> searchByName(String name) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM medicines WHERE medicine_name LIKE ?";
        return runNameSearch(sql, name);
    }

    /**
     * Searches only medicines that do NOT require a prescription
     * (used by the "Issue Medicine Without Prescription" counter-sale screen).
     */
    public List<Medicine> searchAvailableWithoutPrescription(String name) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM medicines " +
                     "WHERE medicine_name LIKE ? AND prescription_required = FALSE";
        return runNameSearch(sql, name);
    }

    private List<Medicine> runNameSearch(String sql, String name) throws SQLException {
        List<Medicine> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + name.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }
        return results;
    }

    /** Retrieves one medicine by ID. Returns null if not found. */
    public Medicine getMedicineById(int medicineId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM medicines WHERE medicine_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, medicineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Returns medicines that need attention: low stock, already expired,
     * or expiring within the next 30 days.
     */
    public List<Medicine> getAttentionNeededMedicines(int lowStockThreshold) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM medicines " +
                     "WHERE stock_quantity <= ? " +
                     "OR expiry_date < CURDATE() " +
                     "OR expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)";

        List<Medicine> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, lowStockThreshold);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }
        return results;
    }

    /**
     * Reduces stock_quantity for a medicine by the given quantity.
     * Meant to be called only during Finalize Bill, within an existing
     * transaction (pass the same Connection used there).
     */
    public boolean reduceStock(Connection conn, int medicineId, int quantity) throws SQLException {
        String sql = "UPDATE medicines SET stock_quantity = stock_quantity - ? " +
                     "WHERE medicine_id = ? AND stock_quantity >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, medicineId);
            ps.setInt(3, quantity);
            return ps.executeUpdate() > 0;
        }
    }

    // =====================================================
    // ADMIN METHODS (unchanged behaviour, now on "medicines")
    // =====================================================

    public boolean addMedicine(Medicine medicine) {
        String sql = "INSERT INTO medicines (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, medicine.getMedicineId());
            ps.setString(2, medicine.getMedicineName());
            ps.setString(3, medicine.getCategory());
            ps.setDouble(4, medicine.getPrice());
            ps.setInt(5, medicine.getStockQuantity());
            ps.setDate(6, toSqlDate(medicine.getExpiryDate()));
            ps.setBoolean(7, medicine.isPrescriptionRequired());

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error adding medicine.");
            e.printStackTrace();
            return false;
        }
    }

    public ArrayList<Medicine> getAllMedicines() {
        return runListQuery("SELECT " + COLUMNS + " FROM medicines",
                "Error viewing medicines.");
    }

    /** Admin-side lookup by ID (catches errors, returns null if not found). */
    public Medicine searchMedicineById(int id) {
        try {
            return getMedicineById(id);
        } catch (Exception e) {
            System.out.println("Error searching medicine by ID.");
            e.printStackTrace();
            return null;
        }
    }

    /** Admin-side search by name (catches errors, returns an empty list on failure). */
    public ArrayList<Medicine> searchMedicineByName(String name) {
        try {
            return new ArrayList<>(searchByName(name));
        } catch (Exception e) {
            System.out.println("Error searching medicine by name.");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public boolean updateMedicine(Medicine medicine) {
        String sql = "UPDATE medicines SET " +
                     "medicine_name = ?, category = ?, price = ?, stock_quantity = ?, " +
                     "expiry_date = ?, prescription_required = ? " +
                     "WHERE medicine_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, medicine.getMedicineName());
            ps.setString(2, medicine.getCategory());
            ps.setDouble(3, medicine.getPrice());
            ps.setInt(4, medicine.getStockQuantity());
            ps.setDate(5, toSqlDate(medicine.getExpiryDate()));
            ps.setBoolean(6, medicine.isPrescriptionRequired());
            ps.setInt(7, medicine.getMedicineId());

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error updating medicine.");
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteMedicine(int id) {
        String sql = "DELETE FROM medicines WHERE medicine_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error deleting medicine.");
            e.printStackTrace();
            return false;
        }
    }

    public ArrayList<Medicine> getStockAndExpiry() {
        return runListQuery("SELECT " + COLUMNS + " FROM medicines ORDER BY expiry_date ASC",
                "Error checking stock and expiry.");
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private ArrayList<Medicine> runListQuery(String sql, String errorMessage) {
        ArrayList<Medicine> medicines = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                medicines.add(mapRow(rs));
            }
        } catch (Exception e) {
            System.out.println(errorMessage);
            e.printStackTrace();
        }
        return medicines;
    }

    /** Works whether the model stores java.sql.Date or java.util.Date. */
    private java.sql.Date toSqlDate(java.util.Date date) {
        return new java.sql.Date(date.getTime());
    }

    private Medicine mapRow(ResultSet rs) throws SQLException {
        return new Medicine(
                rs.getInt("medicine_id"),
                rs.getString("medicine_name"),
                rs.getString("category"),
                rs.getDouble("price"),
                rs.getInt("stock_quantity"),
                rs.getDate("expiry_date"),
                rs.getBoolean("prescription_required")
        );
    }
}