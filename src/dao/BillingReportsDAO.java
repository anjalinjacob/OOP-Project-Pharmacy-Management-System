package dao;

import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BillingReportsDAO {

    // 1. Gets bills for a specific pharmacist (or all if pid == 0)
    public List<Object[]> getBills(int pid) throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT bill_id, IFNULL(prescription_id, 'None') AS prescription_id, " +
                     "pharmacist_id, bill_date, bill_amount " +
                     "FROM bills WHERE (? = 0 OR pharmacist_id = ?)";
                     
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pid);
            ps.setInt(2, pid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{
                            rs.getInt("bill_id"),
                            rs.getString("prescription_id"),
                            rs.getInt("pharmacist_id"),
                            rs.getString("bill_date"),
                            rs.getDouble("bill_amount")
                    });
                }
            }
        }
        return list;
    }

    // 2. Gets medicines sold, grouped by medicine and pharmacist
    public List<Object[]> getMedicinesHandled(int pid) throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT u.user_id, u.username, m.medicine_name, " +
                     "SUM(bi.quantity) AS qty, SUM(bi.total_price) AS amt " +
                     "FROM bills b " +
                     "JOIN users u ON b.pharmacist_id = u.user_id " +
                     "JOIN bill_items bi ON b.bill_id = bi.bill_id " +
                     "JOIN medicines m ON bi.medicine_id = m.medicine_id " +
                     "WHERE (? = 0 OR b.pharmacist_id = ?) " +
                     "GROUP BY u.user_id, u.username, m.medicine_name";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pid);
            ps.setInt(2, pid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("medicine_name"),
                            rs.getInt("qty"),
                            rs.getDouble("amt")
                    });
                }
            }
        }
        return list;
    }

        // 4. Number of medicines currently available (in stock and not expired)
    public int getAvailableMedicines() throws SQLException {
        String sql = "SELECT COUNT(*) FROM medicines ";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
    // 3. Gets an overview summary of all pharmacists
    public List<Object[]> getSummary() throws SQLException {
        List<Object[]> list = new ArrayList<>();
        // Using LEFT JOIN so even pharmacists with 0 bills show up
        String sql = "SELECT u.user_id, u.username, " +
                     "COUNT(DISTINCT b.bill_id) AS bills_handled, " +
                     "COALESCE(SUM(bi.quantity), 0) AS med_units, " +
                     "COALESCE(SUM(bi.total_price), 0) AS total_billed " +
                     "FROM users u " +
                     "LEFT JOIN bills b ON u.user_id = b.pharmacist_id " +
                     "LEFT JOIN bill_items bi ON b.bill_id = bi.bill_id " +
                     "WHERE u.role = 'PHARMACIST' " +
                     "GROUP BY u.user_id, u.username";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getInt("bills_handled"),
                        rs.getInt("med_units"),
                        rs.getDouble("total_billed")
                });
            }
        }
        return list;
    }
}