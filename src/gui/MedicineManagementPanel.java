package gui;

import model.Medicine;
import dao.MedicineDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Medicine management module for the Admin.
 * This is a JPanel (NOT a frame): AdminHomeFrame places it inside its own window.
 * Layout: header on top, navigation on the left, content swapped in place.
 * "Back to Admin Home" runs the callback supplied by AdminHomeFrame.
 */
public class MedicineManagementPanel extends JPanel {

    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 15);
    private static final Font FIELD_FONT = new Font("Segoe UI", Font.PLAIN, 15);

    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private final Runnable onBack;

    private JPanel nav;
    private JButton[] navButtons;

    public MedicineManagementPanel(Runnable onBack) {
        super(new BorderLayout());
        this.onBack = onBack;

        // keep the nav bar proportional to the window whenever it is resized
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeNav();
            }
        });
        add(buildNavPanel(), BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        showWelcomePanel();
    }

    // =====================================================
    // NAVIGATION BAR
    // =====================================================
    private JPanel buildNavPanel() {
        nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));

        JButton addMedicineBtn = new JButton("Add Medicine");
        JButton manageBtn = new JButton("Update/Delete Medicines");
        JButton searchBtn = new JButton("Search Medicine");
        JButton viewBtn = new JButton("View Medicines");
        JButton stockExpiryBtn = new JButton("Stock & Expiry");
        JButton backBtn = new JButton("Back to Admin Home");

        navButtons = new JButton[]{addMedicineBtn, manageBtn, searchBtn,
                viewBtn, stockExpiryBtn, backBtn};

        Font navFont = new Font("Segoe UI", Font.BOLD, 15);
        for (JButton btn : navButtons) {
            btn.setFont(navFont);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setMargin(new Insets(6, 14, 6, 14));
            btn.setFocusPainted(false);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        }

        // first five buttons stacked at the top
        for (int i = 0; i < navButtons.length - 1; i++) {
            nav.add(navButtons[i]);
            nav.add(Box.createRigidArea(new Dimension(0, 6)));
        }
        nav.add(Box.createVerticalGlue()); // pushes Back to the bottom
        nav.add(backBtn);

        addMedicineBtn.addActionListener(e -> showContent(buildAddMedicinePanel()));
        manageBtn.addActionListener(e -> showContent(buildManagePanel()));
        searchBtn.addActionListener(e -> showContent(buildSearchPanel()));
        viewBtn.addActionListener(e -> showContent(buildViewPanel()));
        stockExpiryBtn.addActionListener(e -> showContent(buildStockExpiryPanel()));
        backBtn.addActionListener(e -> onBack.run());

        resizeNav();
        return nav;
    }

    /** Nav width = 16% of the window, but never narrower than the longest button text. */
    private void resizeNav() {
        if (nav == null || navButtons == null) {
            return;
        }
        int widest = 0;
        for (JButton b : navButtons) {
            widest = Math.max(widest, b.getPreferredSize().width);
        }
        int needed = widest + 30 + 10;
        int proportional = (int) (getWidth() * 0.16);
        nav.setPreferredSize(new Dimension(Math.max(needed, proportional), 0));
        nav.revalidate();
    }

    // =====================================================
    // CONTENT HELPERS
    // =====================================================
    private void showWelcomePanel() {
        JPanel welcome = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Select an option from the menu to get started.",
                SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        welcome.add(label, BorderLayout.CENTER);
        showContent(welcome);
    }

    /** Replaces the central content area (nav bar stays visible). */
    private void showContent(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private JLabel makeTitle(String text) {
        JLabel title = new JLabel(text, SwingConstants.CENTER);
        title.setFont(TITLE_FONT);
        title.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        return title;
    }

    private JTextField makeField(String text) {
        JTextField f = new JTextField(text, 20);
        f.setFont(FIELD_FONT);
        f.setPreferredSize(new Dimension(260, 32));
        return f;
    }

    private JButton makeButton(String text, int width) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(width, 36));
        return b;
    }

    /** Adds a "label | field" row to a GridBagLayout form. */
    private void addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(8, 10, 8, 10);
        c.anchor = GridBagConstraints.WEST;

        JLabel l = new JLabel(label);
        l.setFont(LABEL_FONT);
        c.gridx = 0;
        form.add(l, c);

        c.gridx = 1;
        form.add(field, c);
    }

    /** Wraps a form so it sits centred under a title. */
    private JPanel wrapForm(String title, JPanel form, JPanel bottom) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));
        panel.add(makeTitle(title), BorderLayout.NORTH);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER));
        center.add(form);
        panel.add(center, BorderLayout.CENTER);

        if (bottom != null) {
            panel.add(bottom, BorderLayout.SOUTH);
        }
        return panel;
    }

    // Dates are typed/shown as DD-MM-YYYY; the Medicine model holds a java.sql.Date.
    private static final String DATE_FORMAT = "dd-MM-yyyy";

    private java.sql.Date parseDate(String text) throws java.text.ParseException {
        SimpleDateFormat f = new SimpleDateFormat(DATE_FORMAT);
        f.setLenient(false);
        return new java.sql.Date(f.parse(text).getTime());
    }

    private String formatDate(java.util.Date date) {
        return date == null ? "" : new SimpleDateFormat(DATE_FORMAT).format(date);
    }

    private JTable makeTable(String[] columns, String[][] data) {
        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        return table;
    }

    // =====================================================
    // ADD MEDICINE
    // =====================================================
    private JPanel buildAddMedicinePanel() {
        JPanel form = new JPanel(new GridBagLayout());

        JTextField idField = makeField("");
        JTextField nameField = makeField("");
        JTextField categoryField = makeField("");
        JTextField priceField = makeField("");
        JTextField quantityField = makeField("");
        JTextField expiryField = makeField("");
        expiryField.setToolTipText("Format: DD-MM-YYYY");
        JCheckBox prescriptionBox = new JCheckBox("Yes");
        prescriptionBox.setFont(LABEL_FONT);

        addRow(form, 0, "Medicine ID:", idField);
        addRow(form, 1, "Medicine Name:", nameField);
        addRow(form, 2, "Category:", categoryField);
        addRow(form, 3, "Price:", priceField);
        addRow(form, 4, "Quantity:", quantityField);
        addRow(form, 5, "Expiry Date (DD-MM-YYYY):", expiryField);
        addRow(form, 6, "Prescription Required:", prescriptionBox);

        JButton addButton = makeButton("Add Medicine", 160);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.add(addButton);

        addButton.addActionListener(e -> {
            try {
                int id = Integer.parseInt(idField.getText().trim());
                String name = nameField.getText().trim();
                String category = categoryField.getText().trim();
                double price = Double.parseDouble(priceField.getText().trim());
                int quantity = Integer.parseInt(quantityField.getText().trim());
                java.sql.Date expiryDate = parseDate(expiryField.getText().trim());

                if (name.isEmpty() || category.isEmpty()) {
                    throw new IllegalArgumentException("empty");
                }

                Medicine medicine = new Medicine(id, name, category, price,
                        quantity, expiryDate, prescriptionBox.isSelected());

                if (new MedicineDAO().addMedicine(medicine)) {
                    JOptionPane.showMessageDialog(this, "Medicine added successfully!");
                    idField.setText("");
                    nameField.setText("");
                    categoryField.setText("");
                    priceField.setText("");
                    quantityField.setText("");
                    expiryField.setText("");
                    prescriptionBox.setSelected(false);
                } else {
                    JOptionPane.showMessageDialog(this, "Medicine could not be added.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Please enter valid details (expiry date format: DD-MM-YYYY).");
            }
        });

        return wrapForm("Add Medicine", form, bottom);
    }

    // =====================================================
    // MANAGE MEDICINES (select -> Update / Delete)
    // =====================================================
    private JPanel buildManagePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));
        panel.add(makeTitle("Update/Delete Medicines"), BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JLabel selectLabel = new JLabel("Select Medicine:");
        selectLabel.setFont(LABEL_FONT);

        JComboBox<String> medicineList = new JComboBox<>();
        medicineList.setFont(FIELD_FONT);
        medicineList.setPreferredSize(new Dimension(300, 32));
        medicineList.addItem("Select Medicine");

        ArrayList<Medicine> medicines = new MedicineDAO().getAllMedicines();
        for (Medicine m : medicines) {
            medicineList.addItem(m.getMedicineId() + " - " + m.getMedicineName());
        }

        selectionPanel.add(selectLabel);
        selectionPanel.add(medicineList);

        JButton updateButton = makeButton("Update", 110);
        JButton deleteButton = makeButton("Delete", 110);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 5));
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);

        center.add(selectionPanel);
        center.add(Box.createVerticalStrut(10));
        center.add(buttonPanel);
        panel.add(center, BorderLayout.CENTER);

        updateButton.addActionListener(e -> {
            if (medicineList.getSelectedIndex() <= 0) {
                JOptionPane.showMessageDialog(this, "Please select a medicine.");
                return;
            }
            int id = Integer.parseInt(medicineList.getSelectedItem().toString().split(" - ")[0]);
            showContent(buildUpdatePanel(id));
        });

        deleteButton.addActionListener(e -> {
            if (medicineList.getSelectedIndex() <= 0) {
                JOptionPane.showMessageDialog(this, "Please select a medicine.");
                return;
            }
            String selected = medicineList.getSelectedItem().toString();
            int id = Integer.parseInt(selected.split(" - ")[0]);
            String medicineName = selected.substring(selected.indexOf(" - ") + 3);

            int choice = JOptionPane.showConfirmDialog(this, "Delete " + medicineName + "?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                if (new MedicineDAO().deleteMedicine(id)) {
                    JOptionPane.showMessageDialog(this, medicineName + " deleted successfully.");
                    showContent(buildManagePanel()); // refresh list
                } else {
                    JOptionPane.showMessageDialog(this, "Medicine could not be deleted.");
                }
            }
        });

        return panel;
    }

    // =====================================================
    // UPDATE MEDICINE
    // =====================================================
    private JPanel buildUpdatePanel(int id) {
        Medicine medicine = new MedicineDAO().searchMedicineById(id);

        if (medicine == null) {
            JOptionPane.showMessageDialog(this, "Medicine not found.");
            return buildManagePanel();
        }

        JPanel form = new JPanel(new GridBagLayout());

        JTextField idField = makeField(String.valueOf(medicine.getMedicineId()));
        idField.setEditable(false);
        JTextField nameField = makeField(medicine.getMedicineName());
        JTextField categoryField = makeField(medicine.getCategory());
        JTextField priceField = makeField(String.valueOf(medicine.getPrice()));
        JTextField quantityField = makeField(String.valueOf(medicine.getStockQuantity()));
        JTextField expiryField = makeField(formatDate(medicine.getExpiryDate()));
        JCheckBox prescriptionBox = new JCheckBox("Yes");
        prescriptionBox.setFont(LABEL_FONT);
        prescriptionBox.setSelected(medicine.isPrescriptionRequired());

        addRow(form, 0, "Medicine ID:", idField);
        addRow(form, 1, "Medicine Name:", nameField);
        addRow(form, 2, "Category:", categoryField);
        addRow(form, 3, "Price:", priceField);
        addRow(form, 4, "Quantity:", quantityField);
        addRow(form, 5, "Expiry Date (DD-MM-YYYY):", expiryField);
        addRow(form, 6, "Prescription Required:", prescriptionBox);

        JButton updateButton = makeButton("Update Medicine", 170);
        JButton cancelButton = makeButton("Cancel", 110);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 5));
        bottom.add(updateButton);
        bottom.add(cancelButton);

        cancelButton.addActionListener(e -> showContent(buildManagePanel()));

        updateButton.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                String category = categoryField.getText().trim();
                double price = Double.parseDouble(priceField.getText().trim());
                int quantity = Integer.parseInt(quantityField.getText().trim());
                java.sql.Date expiryDate = parseDate(expiryField.getText().trim());

                Medicine updated = new Medicine(id, name, category, price,
                        quantity, expiryDate, prescriptionBox.isSelected());

                if (new MedicineDAO().updateMedicine(updated)) {
                    JOptionPane.showMessageDialog(this, "Medicine updated successfully!");
                    showContent(buildManagePanel());
                } else {
                    JOptionPane.showMessageDialog(this, "Medicine could not be updated.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Please enter valid details (expiry date format: DD-MM-YYYY).");
            }
        });

        return wrapForm("Update Medicine", form, bottom);
    }

    // =====================================================
    // SEARCH MEDICINE
    // =====================================================
    private JPanel buildSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));
        panel.add(makeTitle("Search Medicine"), BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JLabel searchLabel = new JLabel("Medicine Name or ID:");
        searchLabel.setFont(LABEL_FONT);
        JTextField searchField = makeField("");
        JButton searchButton = makeButton("Search", 100);
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        resultArea.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane resultScroll = new JScrollPane(resultArea);
        resultScroll.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel center = new JPanel(new BorderLayout(0, 15));
        center.add(searchPanel, BorderLayout.NORTH);
        center.add(resultScroll, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);

        Runnable search = () -> {
            String searchText = searchField.getText().trim();
            if (searchText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a medicine name or ID.");
                return;
            }

            MedicineDAO dao = new MedicineDAO();
            resultArea.setText("");

            try {
                int id = Integer.parseInt(searchText);
                Medicine medicine = dao.searchMedicineById(id);
                if (medicine != null) {
                    resultArea.setText(formatMedicine(medicine));
                } else {
                    resultArea.setText("No medicine found with ID: " + id);
                }
            } catch (NumberFormatException ex) {
                ArrayList<Medicine> list = dao.searchMedicineByName(searchText);
                if (list.isEmpty()) {
                    resultArea.setText("No medicine found with name: " + searchText);
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (Medicine m : list) {
                        sb.append(formatMedicine(m))
                          .append("\n\n-------------------------\n\n");
                    }
                    resultArea.setText(sb.toString());
                }
            }
        };

        searchButton.addActionListener(e -> search.run());
        searchField.addActionListener(e -> search.run()); // Enter key also searches

        return panel;
    }

    private String formatMedicine(Medicine m) {
        return "Medicine ID: " + m.getMedicineId() + "\n"
                + "Name: " + m.getMedicineName() + "\n"
                + "Category: " + m.getCategory() + "\n"
                + "Price: " + m.getPrice() + "\n"
                + "Quantity: " + m.getStockQuantity() + "\n"
                + "Expiry Date: " + formatDate(m.getExpiryDate()) + "\n"
                + "Prescription Required: " + (m.isPrescriptionRequired() ? "Yes" : "No");
    }

    // =====================================================
    // VIEW MEDICINES
    // =====================================================
    private JPanel buildViewPanel() {
        ArrayList<Medicine> medicines = new MedicineDAO().getAllMedicines();

        String[] columns = {"ID", "Name", "Category", "Price", "Quantity",
                "Expiry Date", "Prescription"};
        String[][] data = new String[medicines.size()][7];

        for (int i = 0; i < medicines.size(); i++) {
            Medicine m = medicines.get(i);
            data[i][0] = String.valueOf(m.getMedicineId());
            data[i][1] = m.getMedicineName();
            data[i][2] = m.getCategory();
            data[i][3] = String.valueOf(m.getPrice());
            data[i][4] = String.valueOf(m.getStockQuantity());
            data[i][5] = formatDate(m.getExpiryDate());
            data[i][6] = m.isPrescriptionRequired() ? "Yes" : "No";
        }

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));
        panel.add(makeTitle("All Medicines"), BorderLayout.NORTH);
        panel.add(new JScrollPane(makeTable(columns, data)), BorderLayout.CENTER);
        return panel;
    }

    // =====================================================
    // STOCK & EXPIRY
    // =====================================================
    private JPanel buildStockExpiryPanel() {
        ArrayList<Medicine> medicines = new MedicineDAO().getStockAndExpiry();

        String[] columns = {"ID", "Medicine", "Quantity", "Expiry Date", "Status"};
        String[][] data = new String[medicines.size()][5];
        LocalDate today = LocalDate.now();

        for (int i = 0; i < medicines.size(); i++) {
            Medicine m = medicines.get(i);
            data[i][0] = String.valueOf(m.getMedicineId());
            data[i][1] = m.getMedicineName();
            data[i][2] = String.valueOf(m.getStockQuantity());
            data[i][3] = formatDate(m.getExpiryDate());

            String status;
            try {
                if (new java.sql.Date(m.getExpiryDate().getTime()).toLocalDate().isBefore(today)) {
                    status = "Expired";
                } else if (m.getStockQuantity() <= 10) {
                    status = "Low Stock";
                } else {
                    status = "Available";
                }
            } catch (Exception ex) {
                status = "Invalid Date";
            }
            data[i][4] = status;
        }

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));
        panel.add(makeTitle("Stock & Expiry"), BorderLayout.NORTH);
        panel.add(new JScrollPane(makeTable(columns, data)), BorderLayout.CENTER);
        return panel;
    }
}
