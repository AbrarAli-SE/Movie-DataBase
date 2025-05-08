import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

public class ActorManagerGUI extends JFrame {
    private JTable actorTable;
    private DefaultTableModel actorModel;

    public ActorManagerGUI() {
        setTitle("Actor Manager");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top menu buttons
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new GridLayout(1, 5));
        String[] labels = { "Add Actor", "Edit Actor", "Delete Actor", "View Actors", "Exit" };
        for (String label : labels) {
            JButton button = new JButton(label);
            button.addActionListener(this::handleMenuClick);
            menuPanel.add(button);
        }
        add(menuPanel, BorderLayout.NORTH);

        // Table setup
        actorModel = new DefaultTableModel(new String[] { "ID", "Name" }, 0);
        actorTable = new JTable(actorModel);
        add(new JScrollPane(actorTable), BorderLayout.CENTER);

        refreshActors();
    }

    private void handleMenuClick(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Add Actor" -> addActor();
            case "Edit Actor" -> editActor();
            case "Delete Actor" -> deleteActor();
            case "View Actors" -> refreshActors();
            case "Exit" -> System.exit(0);
        }
    }

    private void addActor() {
        String name = JOptionPane.showInputDialog(this, "Enter Actor Name:");
        if (name != null && !name.trim().isEmpty()) {
            executeUpdate("INSERT INTO Actor (name) VALUES (?)", name);
            refreshActors();
        }
    }

    private void editActor() {
        String idStr = JOptionPane.showInputDialog(this, "Enter Actor ID:");
        if (idStr != null && idStr.matches("\\d+")) {
            int actorId = Integer.parseInt(idStr);
            String newName = JOptionPane.showInputDialog(this, "Enter New Actor Name:");
            if (newName != null && !newName.trim().isEmpty()) {
                executeUpdate("UPDATE Actor SET name = ? WHERE actorId = ?", newName, actorId);
                refreshActors();
            }
        }
    }

    private void deleteActor() {
        String idStr = JOptionPane.showInputDialog(this, "Enter Actor ID:");
        if (idStr != null && idStr.matches("\\d+")) {
            int actorId = Integer.parseInt(idStr);
            executeUpdate("DELETE FROM Actor WHERE actorId = ?", actorId);
            refreshActors();
        }
    }

    private void refreshActors() {
        actorModel.setRowCount(0); // Clear table before refreshing
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT actorId, name FROM Actor")) {
            while (rs.next()) {
                actorModel.addRow(new Object[] { rs.getInt("actorId"), rs.getString("name") });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching actors: " + e.getMessage());
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
        SwingUtilities.invokeLater(() -> new ActorManagerGUI().setVisible(true));
    }
}