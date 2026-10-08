package gui;

import dao.UserDAO;
import model.User;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;

/**
 * Manage Users - panel version of the old ManageUsersFrame.
 * Same functions (list, add, delete, clear); AdminHomeFrame shows it in its
 * content area, so the Back button is replaced by "Back to Admin" on the side.
 */
public class ManageUsersPanel extends JPanel {
    UserDAO dao = new UserDAO();

    DefaultTableModel model = new DefaultTableModel(new String[]{"User ID", "Username", "Role"}, 0) {
        public boolean isCellEditable(int row, int col) { return false; }
    };
    JTable table = new JTable(model);

    JTextField userField = new JTextField(12);
    JPasswordField passField = new JPasswordField(12);
    JComboBox<String> roleBox = new JComboBox<>(new String[]{"PHARMACIST", "ADMIN"});

    public ManageUsersPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));

        JLabel title = new JLabel("Manage Users", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        // table styling (same look as the pharmacist screens)
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));

        // form row
        Font labelFont = new Font("Segoe UI", Font.PLAIN, 15);
        userField.setFont(labelFont);
        passField.setFont(labelFont);
        roleBox.setFont(labelFont);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        form.add(makeLabel("Username:"));
        form.add(userField);
        form.add(makeLabel("Password:"));
        form.add(passField);
        form.add(makeLabel("Role:"));
        form.add(roleBox);

        // button row
        JButton addBtn = makeButton("Add User", 130);
        JButton clearBtn = makeButton("Clear", 110);
        JButton backBtn = makeButton("Back to Admin", 150);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        buttons.add(addBtn);
        buttons.add(clearBtn);
        buttons.add(backBtn);

        JPanel bottom = new JPanel(new GridLayout(2, 1));
        bottom.add(form);
        bottom.add(buttons);

        add(title, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> addUser());
        clearBtn.addActionListener(e -> clearFields());
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

        loadUsers();
    }

    JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        return l;
    }

    JButton makeButton(String text, int width) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(width, 36));
        return b;
    }

    // fills the table with all users from the database
    void loadUsers() {
        model.setRowCount(0);
        try {
            for (User u : dao.getAllUsers()) {
                model.addRow(new Object[]{u.getUserId(), u.getUsername(), u.getRole()});
            }
        } catch (SQLException ex) {
            msg("Cannot load users. Please check the database connection.");
        }
    }

    void addUser() {
        String u = userField.getText().trim();
        String p = new String(passField.getPassword());
        String role = (String) roleBox.getSelectedItem();

        if (u.isEmpty()) { msg("Username cannot be empty."); return; }
        if (p.isEmpty()) { msg("Password cannot be empty."); return; }
        if (p.length() < 4) { msg("Password must be at least 4 characters."); return; }

        try {
            if (dao.usernameExists(u)) {
                msg("Username already exists. Please choose another.");
                return;
            }
            dao.addUser(u, p, role);
            msg("User added successfully.");
            clearFields();
            loadUsers();
        } catch (SQLException ex) {
            msg("Database error. Could not add the user.");
        }
    }

    void clearFields() {
        userField.setText("");
        passField.setText("");
        roleBox.setSelectedIndex(0);
        userField.requestFocus();
    }

    void msg(String text) {
        JOptionPane.showMessageDialog(this, text);
    }
}
