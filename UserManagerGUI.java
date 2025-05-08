import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.time.LocalDate;

public class UserManagerGUI extends JFrame {
    private JTable userTable;
    private DefaultTableModel userModel;

    public UserManagerGUI() {
        setTitle("User Manager");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top menu buttons
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new GridLayout(1, 4));
        String[] labels = { "Login", "Register", "View Users", "Exit" };
        for (String label : labels) {
            JButton button = new JButton(label);
            button.addActionListener(this::handleMenuClick);
            menuPanel.add(button);
        }
        add(menuPanel, BorderLayout.NORTH);

        // Table setup
        userModel = new DefaultTableModel(new String[] { "ID", "Username", "Email", "Join Date" }, 0);
        userTable = new JTable(userModel);
        add(new JScrollPane(userTable), BorderLayout.CENTER);

        refreshUsers();
    }

    private void handleMenuClick(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Login" -> login();
            case "Register" -> registerUser();
            case "View Users" -> refreshUsers();
            case "Exit" -> System.exit(0);
        }
    }

    private void login() {
        String username = JOptionPane.showInputDialog(this, "Enter Username:");
        String password = JOptionPane.showInputDialog(this, "Enter Password:");
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty())
            return;

        String sql = "SELECT userId, username FROM User WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt("userId");
                String uname = rs.getString("username");
                JOptionPane.showMessageDialog(this, "Login successful!");
                if (uname.startsWith("admin_")) {
                    new AdminManagerGUI(userId).setVisible(true);
                } else {
                    new MovieManagerGUI().setVisible(true);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password!", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error during login: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void registerUser() {
        String username = JOptionPane.showInputDialog(this, "Enter Username:");
        String email = JOptionPane.showInputDialog(this, "Enter Email:");
        String password = JOptionPane.showInputDialog(this, "Enter Password:");
        if (username == null || email == null || password == null || username.trim().isEmpty() || email.trim().isEmpty()
                || password.trim().isEmpty())
            return;

        String sql = "INSERT INTO User (username, email, password, joinDate) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, password);
            stmt.setDate(4, Date.valueOf(LocalDate.now()));
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "User registered successfully!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error registering user: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshUsers() {
        userModel.setRowCount(0); // Clear table before refreshing
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT userId, username, email, joinDate FROM User")) {
            while (rs.next()) {
                userModel.addRow(new Object[] { rs.getInt("userId"), rs.getString("username"), rs.getString("email"),
                        rs.getDate("joinDate") });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching users: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UserManagerGUI().setVisible(true));
    }
}