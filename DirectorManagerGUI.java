import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

public class DirectorManagerGUI extends JFrame {
    private JTable directorTable;
    private DefaultTableModel directorModel;

    public DirectorManagerGUI() {
        setTitle("Director Manager");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top menu buttons
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new GridLayout(1, 5));
        String[] labels = { "Add Director", "Edit Director", "Delete Director", "View Directors", "Exit" };
        for (String label : labels) {
            JButton button = new JButton(label);
            button.addActionListener(this::handleMenuClick);
            menuPanel.add(button);
        }
        add(menuPanel, BorderLayout.NORTH);

        // Table setup
        directorModel = new DefaultTableModel(new String[] { "ID", "Name" }, 0);
        directorTable = new JTable(directorModel);
        add(new JScrollPane(directorTable), BorderLayout.CENTER);

        refreshDirectors();
    }

    private void handleMenuClick(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Add Director" -> addDirector();
            case "Edit Director" -> editDirector();
            case "Delete Director" -> deleteDirector();
            case "View Directors" -> refreshDirectors();
            case "Exit" -> System.exit(0);
        }
    }

    private void addDirector() {
        String name = JOptionPane.showInputDialog(this, "Enter Director Name:");
        if (name != null && !name.trim().isEmpty()) {
            executeUpdate("INSERT INTO Director (name) VALUES (?)", name);
            refreshDirectors();
        }
    }

    private void editDirector() {
        String idStr = JOptionPane.showInputDialog(this, "Enter Director ID:");
        if (idStr != null && idStr.matches("\\d+")) {
            int directorId = Integer.parseInt(idStr);
            String newName = JOptionPane.showInputDialog(this, "Enter New Director Name:");
            if (newName != null && !newName.trim().isEmpty()) {
                executeUpdate("UPDATE Director SET name = ? WHERE directorId = ?", newName, directorId);
                refreshDirectors();
            }
        }
    }

    private void deleteDirector() {
        String idStr = JOptionPane.showInputDialog(this, "Enter Director ID:");
        if (idStr != null && idStr.matches("\\d+")) {
            int directorId = Integer.parseInt(idStr);
            executeUpdate("DELETE FROM Director WHERE directorId = ?", directorId);
            refreshDirectors();
        }
    }

    private void refreshDirectors() {
        directorModel.setRowCount(0); // Clear table before refreshing
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT directorId, name FROM Director")) {
            while (rs.next()) {
                directorModel.addRow(new Object[] { rs.getInt("directorId"), rs.getString("name") });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching directors: " + e.getMessage());
        }
    }

    private void executeUpdate(String sql, Object... params) {
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DirectorManagerGUI().setVisible(true));
    }
}