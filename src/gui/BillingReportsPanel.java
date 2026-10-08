package gui;

import dao.BillingReportsDAO;
import dao.UserDAO;
import model.User;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Billing Reports - panel version of the old BillingReportsFrame.
 * Tabs: Total Summary, Bills, Medicines Handled, Pharmacist Summary.
 * The pharmacist filter is shown only on the Bills and Medicines Handled tabs.
 * AdminHomeFrame shows it in its content area.
 */
public class BillingReportsPanel extends JPanel {
    BillingReportsDAO dao = new BillingReportsDAO();

    JComboBox<String> pharmaBox = new JComboBox<>();
    ArrayList<Integer> pharmaIds = new ArrayList<>();   // pharmacist id for each combo item (0 = all)

    DefaultTableModel billModel = makeModel("Bill ID", "Prescription ID", "Pharmacist ID", "Bill Date", "Bill Amount");
    DefaultTableModel medModel = makeModel("Pharmacist ID", "Pharmacist", "Medicine", "Quantity Sold", "Amount");
    DefaultTableModel sumModel = makeModel("Pharmacist ID", "Pharmacist", "Bills Handled", "Medicine Units", "Total Billed");

    JLabel pharmacistsValue = new JLabel("0");
    JLabel billsValue = new JLabel("0");
    JLabel medicinesValue = new JLabel("0");
    JLabel incomeValue = new JLabel("0.00");

    public BillingReportsPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));

        // fill the pharmacist drop-down
        pharmaBox.addItem("All Pharmacists");
        pharmaIds.add(0);
        try {
            for (User u : new UserDAO().getAllUsers()) {
                if (u.getRole().equals("PHARMACIST")) {
                    pharmaBox.addItem(u.getUsername() + " (ID " + u.getUserId() + ")");
                    pharmaIds.add(u.getUserId());
                }
            }
        } catch (SQLException ex) {
            msg("Cannot load pharmacists. Please check the database connection.");
        }

        JLabel title = new JLabel("Billing Reports");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JLabel pharmaLabel = new JLabel("Pharmacist:");
        pharmaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        pharmaBox.setFont(new Font("Segoe UI", Font.PLAIN, 15));

        JButton clearBtn = new JButton("Clear Filter");
        clearBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        clearBtn.setFocusPainted(false);
        clearBtn.setPreferredSize(new Dimension(130, 36));

        JButton backBtn = new JButton("Back to Admin");
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backBtn.setFocusPainted(false);
        backBtn.setPreferredSize(new Dimension(150, 36));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        top.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        top.add(title);
        top.add(pharmaLabel);
        top.add(pharmaBox);
        top.add(clearBtn);
        top.add(backBtn);

        JPanel cards = new JPanel(new GridLayout(1, 4, 20, 0));
        cards.add(makeCard("Pharmacists", pharmacistsValue));
        cards.add(makeCard("Bills", billsValue));
        cards.add(makeCard("Available Medicines", medicinesValue));
        cards.add(makeCard("Total Income", incomeValue));

        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setBorder(BorderFactory.createEmptyBorder(30, 20, 20, 20));
        totalPanel.add(cards, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabs.addTab("Total Summary", totalPanel);
        tabs.addTab("Bills", new JScrollPane(makeTable(billModel)));
        tabs.addTab("Medicines Handled", new JScrollPane(makeTable(medModel)));
        tabs.addTab("Pharmacist Summary", new JScrollPane(makeTable(sumModel)));

        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        // show the pharmacist filter only on the tabs that use it (Bills, Medicines Handled)
        Runnable updateFilterVisibility = () -> {
            String tab = tabs.getTitleAt(tabs.getSelectedIndex());
            boolean showFilter = tab.equals("Bills") || tab.equals("Medicines Handled");
            pharmaLabel.setVisible(showFilter);
            pharmaBox.setVisible(showFilter);
            clearBtn.setVisible(showFilter);
            top.revalidate();
            top.repaint();
        };
        tabs.addChangeListener(e -> updateFilterVisibility.run());
        updateFilterVisibility.run();   // set the correct state for the first tab

        // selecting a pharmacist loads the data for that pharmacist
        pharmaBox.addActionListener(e -> load());

        // clear the filter: go back to "All Pharmacists" (this triggers load())
        clearBtn.addActionListener(e -> pharmaBox.setSelectedIndex(0));

        backBtn.addActionListener(e -> {
            // go back to the Admin menu
            Container parent = getParent();
            while (parent != null && !(parent instanceof AdminHomeFrame)) {
                parent = parent.getParent();
            }
            if (parent instanceof AdminHomeFrame adminFrame) {
                adminFrame.backToMenu();
            }
        });

        load();
    }

    JPanel makeCard(String caption, JLabel valueLabel) {
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 44));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel captionLabel = new JLabel(caption);
        captionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        captionLabel.setForeground(Color.GRAY);
        captionLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                BorderFactory.createEmptyBorder(25, 15, 25, 15)));
        card.setPreferredSize(new Dimension(200, 170));
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(captionLabel, BorderLayout.SOUTH);
        return card;
    }

    JTable makeTable(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(30);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        return t;
    }

    // loads all the tables for the selected pharmacist
    void load() {
        int selected = pharmaBox.getSelectedIndex();
        if (selected < 0) return;
        int pid = pharmaIds.get(selected);
        try {
            // 1. bills (the last row shows the total)
            billModel.setRowCount(0);
            double billTotal = 0;
            for (Object[] r : dao.getBills(pid)) {
                billTotal += (Double) r[4];
                billModel.addRow(new Object[]{r[0], r[1], r[2], r[3], money((Double) r[4])});
            }
            billModel.addRow(new Object[]{"", "", "", "TOTAL", money(billTotal)});

            // 2. medicines handled
            medModel.setRowCount(0);
            int units = 0;
            double medTotal = 0;
            for (Object[] r : dao.getMedicinesHandled(pid)) {
                units += (Integer) r[3];
                medTotal += (Double) r[4];
                medModel.addRow(new Object[]{r[0], r[1], r[2], r[3], money((Double) r[4])});
            }
            medModel.addRow(new Object[]{"", "", "TOTAL", units, money(medTotal)});

            // 3. summary of every pharmacist
            sumModel.setRowCount(0);
            int bills = 0, sumUnits = 0;
            double grand = 0;
            for (Object[] r : dao.getSummary()) {
                bills += (Integer) r[2];
                sumUnits += (Integer) r[3];
                grand += (Double) r[4];
                sumModel.addRow(new Object[]{r[0], r[1], r[2], r[3], money((Double) r[4])});
            }
            sumModel.addRow(new Object[]{"", "TOTAL", bills, sumUnits, money(grand)});

            // 4. total summary cards (overall, not affected by the dropdown)
            pharmacistsValue.setText(String.valueOf(pharmaIds.size() - 1));
            billsValue.setText(String.valueOf(bills));
            medicinesValue.setText(String.valueOf(dao.getAvailableMedicines()));
            incomeValue.setText(money(grand));

        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1146) {
                msg("Billing tables not found. Please run billing_reports.sql in MySQL first.");
            } else {
                msg("Cannot load the billing report. Please check the database connection.");
            }
        }
    }

    String money(double d) {
        return String.format("%.2f", d);
    }

    static DefaultTableModel makeModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
    }

    void msg(String text) {
        JOptionPane.showMessageDialog(this, text);
    }
}