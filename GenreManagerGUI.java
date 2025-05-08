import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

public class GenreManagerGUI extends JFrame {
    private JTable genreTable;
    private DefaultTableModel genreModel;

    public GenreManagerGUI() {
        setTitle("Genre Manager");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top menu buttons
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new GridLayout(1, 5));
        String[] labels = { "Add Genre", "Edit Genre", "Delete Genre", "View Genres", "Exit" };
        for (String label : labels) {
            JButton button = new JButton(label);
            button.addActionListener(this::handleMenuClick);
            menuPanel.add(button);
        }
        add(menuPanel, BorderLayout.NORTH);

        // Table setup
        genreModel = new DefaultTableModel(new String[] { "ID", "Name" }, 0);
        genreTable = new JTable(genreModel);
        add(new JScrollPane(genreTable), BorderLayout.CENTER);

        refreshGenres();
    }

    private void handleMenuClick(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Add Genre" -> addGenre();
            case "Edit Genre" -> editGenre();
            case "Delete Genre" -> deleteGenre();
            case "View Genres" -> refreshGenres();
            case "Exit" -> System.exit(0);
        }
    }

    private void addGenre() {
        String name = JOptionPane.showInputDialog(this, "Enter Genre Name:");
        if (name != null && !name.trim().isEmpty()) {
            executeUpdate("INSERT INTO Genre (name) VALUES (?)", name);
            refreshGenres();
        }
    }

    private void editGenre() {
        String idStr = JOptionPane.showInputDialog(this, "Enter Genre ID:");
        if (idStr != null && idStr.matches("\\d+")) {
            int genreId = Integer.parseInt(idStr);
            String newName = JOptionPane.showInputDialog(this, "Enter New Genre Name:");
            if (newName != null && !newName.trim().isEmpty()) {
                executeUpdate("UPDATE Genre SET name = ? WHERE genreId = ?", newName, genreId);
                refreshGenres();
            }
        }
    }

    private void deleteGenre() {
        String idStr = JOptionPane.showInputDialog(this, "Enter Genre ID:");
        if (idStr != null && idStr.matches("\\d+")) {
            int genreId = Integer.parseInt(idStr);
            executeUpdate("DELETE FROM Genre WHERE genreId = ?", genreId);
            refreshGenres();
        }
    }

    private void refreshGenres() {
        genreModel.setRowCount(0); // Clear table before refreshing
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT genreId, name FROM Genre")) {
            while (rs.next()) {
                genreModel.addRow(new Object[] { rs.getInt("genreId"), rs.getString("name") });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching genres: " + e.getMessage());
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
        SwingUtilities.invokeLater(() -> new GenreManagerGUI().setVisible(true));
    }
}