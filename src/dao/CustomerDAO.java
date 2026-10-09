package dao;

import database.DatabaseConnection;
import model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all database access for the "customers" table:
 * add, update, delete, and search operations.
 */
public class CustomerDAO {

    /** Inserts a new customer and returns the generated customer_id. */
    public int addCustomer(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers (name, phone, email, address, date_registered) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, customer.getName());
            ps.setString(2, customer.getPhone());
            ps.setString(3, customer.getEmail());
            ps.setString(4, customer.getAddress());
            ps.setDate(5, customer.getDateRegistered());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    /** Finds a customer by ID. Returns null if not found. */
    public Customer getCustomerById(int customerId) throws SQLException {
        String sql = "SELECT * FROM customers WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Updates an existing customer's editable fields.
     * customer_id is never changed; date_of_birth is included per
     * the required UPDATE query but kept read-only in the GUI.
     */
    public boolean updateCustomer(Customer customer) throws SQLException {
        String sql = "UPDATE customers SET name = ?, phone = ?, email = ?, address = ? " +
                     "WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, customer.getName());
            ps.setString(2, customer.getPhone());
            ps.setString(3, customer.getEmail());
            ps.setString(4, customer.getAddress());
            ps.setInt(5, customer.getCustomerId());

            return ps.executeUpdate() > 0;
        }
    }

    /** Returns true if the customer has at least one prescription on record. */
    public boolean hasPrescriptionHistory(int customerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM prescriptions WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /** Deletes a customer by ID. Caller must check prescription history first. */
    public boolean deleteCustomer(int customerId) throws SQLException {
        String sql = "DELETE FROM customers WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, customerId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Searches customers by ID, name, or phone.
     * Any field left blank/null is ignored in the search.
     */
    public List<Customer> searchCustomers(String idText, String name, String phone) throws SQLException {
        List<Customer> results = new ArrayList<>();

        // If a numeric ID was given, search by exact ID only.
        if (idText != null && !idText.trim().isEmpty()) {
            String sql = "SELECT * FROM customers WHERE customer_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(idText.trim()));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        results.add(mapRow(rs));
                    }
                }
            }
            return results;
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM customers WHERE 1=1");
        if (name != null && !name.trim().isEmpty()) {
            sql.append(" AND name LIKE ?");
        }
        if (phone != null && !phone.trim().isEmpty()) {
            sql.append(" AND phone LIKE ?");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (name != null && !name.trim().isEmpty()) {
                ps.setString(idx++, "%" + name.trim() + "%");
            }
            if (phone != null && !phone.trim().isEmpty()) {
                ps.setString(idx++, "%" + phone.trim() + "%");
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }
        return results;
    }

    /**
     * Searches customers by a single combined text box: treats the text as
     * a Customer ID if it is purely numeric, otherwise matches it against
     * EITHER name OR phone. Used by the Update/Delete Customer screens,
     * which offer one search field instead of three separate ones.
     */
    public List<Customer> searchByIdOrNameOrPhone(String text) throws SQLException {
        List<Customer> results = new ArrayList<>();
        String trimmed = text.trim();

        if (trimmed.matches("\\d+")) {
            String sql = "SELECT * FROM customers WHERE customer_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(trimmed));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        results.add(mapRow(rs));
                    }
                }
            }
            return results;
        }

        String sql = "SELECT * FROM customers WHERE name LIKE ? OR phone LIKE ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + trimmed + "%");
            ps.setString(2, "%" + trimmed + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }
        return results;
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getInt("customer_id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("address"),
                rs.getDate("date_registered")
        );
    }
}