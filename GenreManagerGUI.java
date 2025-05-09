import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class GenreManagerGUI extends JFrame {
    static class RoundedButton extends JButton {
        public RoundedButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setForeground(Color.WHITE);
            setBackground(new Color(0, 123, 255));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setBackground(new Color(0, 86, 179));
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    setBackground(new Color(0, 123, 255));
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    private JTable genreTable;
    private DefaultTableModel genreModel;

    public GenreManagerGUI() {
        setTitle("Genre Manager");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(245, 247, 250));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(44, 62, 80));
        JLabel titleLabel = new JLabel("Genre Manager", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        menuPanel.setBackground(new Color(245, 247, 250));
        String[] labels = { "Add Genre", "Edit Genre", "Delete Genre", "Back", "Exit" };
        for (String label : labels) {
            RoundedButton button = new RoundedButton(label);
            menuPanel.add(button);
            button.addActionListener(this::handleMenuClick);
        }
        mainPanel.add(menuPanel, BorderLayout.NORTH);

        genreModel = new DefaultTableModel(new String[] { "ID", "Name" }, 0);
        genreTable = new JTable(genreModel);
        mainPanel.add(new JScrollPane(genreTable), BorderLayout.CENTER);

        JLabel footerLabel = new JLabel("Manage genres in the movie database.", SwingConstants.CENTER);
        footerLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        footerLabel.setForeground(new Color(51, 51, 51));
        mainPanel.add(footerLabel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        refreshGenres();
    }

    private void handleMenuClick(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Add Genre" -> addGenre();
            case "Edit Genre" -> editGenre();
            case "Delete Genre" -> deleteGenre();
            case "Back" -> dispose();
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
        genreModel.setRowCount(0);
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