import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

public class StudioManagerGUI extends JFrame {
    private JTable studioTable;
    private DefaultTableModel studioModel;

    public StudioManagerGUI() {
        setTitle("Studio Manager");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top menu buttons
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new GridLayout(1, 5));
        String[] labels = { "Add Studio", "Edit Studio", "Delete Studio", "View Studios", "Exit" };
        for (String label : labels) {
            JButton button = new JButton(label);
            button.addActionListener(this::handleMenuClick);
            menuPanel.add(button);
        }
        add(menuPanel, BorderLayout.NORTH);

        // Table setup
        studioModel = new DefaultTableModel(new String[] { "ID", "Name" }, 0);
        studioTable = new JTable(studioModel);
        add(new JScrollPane(studioTable), BorderLayout.CENTER);

        refreshStudios();
    }

    private void handleMenuClick(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Add Studio" -> addStudio();
            case "Edit Studio" -> editStudio();
            case "Delete Studio" -> deleteStudio();
            case "View Studios" -> refreshStudios();
            case "Exit" -> System.exit(0);
        }
    }

    private void addStudio() {
        String name = JOptionPane.showInputDialog(this, "Enter Studio Name:");
        if (name != null && !name.trim().isEmpty()) {
            executeUpdate("INSERT INTO Studio (name) VALUES (?)", name);
            refreshStudios();
        }
    }

    private void editStudio() {
        String idStr = JOptionPane.showInputDialog(this, "Enter Studio ID:");
        if (idStr != null && idStr.matches("\\d+")) {
            int studioId = Integer.parseInt(idStr);
            String newName = JOptionPane.showInputDialog(this, "Enter New Studio Name:");
            if (newName != null && !newName.trim().isEmpty()) {
                executeUpdate("UPDATE Studio SET name = ? WHERE studioId = ?", newName, studioId);
                refreshStudios();
            }
        }
    }

    private void deleteStudio() {
        String idStr = JOptionPane.showInputDialog(this, "Enter Studio ID:");
        if (idStr != null && idStr.matches("\\d+")) {
            int studioId = Integer.parseInt(idStr);
            executeUpdate("DELETE FROM Studio WHERE studioId = ?", studioId);
            refreshStudios();
        }
    }

    private void refreshStudios() {
        studioModel.setRowCount(0); // Clear table before refreshing
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT studioId, name FROM Studio")) {
            while (rs.next()) {
                studioModel.addRow(new Object[] { rs.getInt("studioId"), rs.getString("name") });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching studios: " + e.getMessage());
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
        SwingUtilities.invokeLater(() -> new StudioManagerGUI().setVisible(true));
    }
}