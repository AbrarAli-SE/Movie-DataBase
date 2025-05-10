import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class StudioManagerGUI extends JFrame {
    static class CustomButton extends JButton {
        public CustomButton(String text) {
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
            g2.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    private JTable studioTable;
    private DefaultTableModel studioModel;

    public StudioManagerGUI() {
        setTitle("Studio Manager");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(245, 247, 250));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(44, 62, 80));
        JLabel titleLabel = new JLabel("Studio Manager", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        menuPanel.setBackground(new Color(245, 247, 250));
        String[] labels = { "Add Studio", "Edit Studio", "Delete Studio", "Back", "Exit" };
        for (String label : labels) {
            CustomButton button = new CustomButton(label);
            menuPanel.add(button);
            button.addActionListener(this::handleMenuClick);
        }
        mainPanel.add(menuPanel, BorderLayout.NORTH);

        studioModel = new DefaultTableModel(new String[] { "ID", "Name" }, 0);
        studioTable = new JTable(studioModel);
        mainPanel.add(new JScrollPane(studioTable), BorderLayout.CENTER);

        JLabel footerLabel = new JLabel("Manage studios in the movie database.", SwingConstants.CENTER);
        footerLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        footerLabel.setForeground(new Color(51, 51, 51));
        mainPanel.add(footerLabel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        refreshStudios();
    }

    private void handleMenuClick(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Add Studio" -> addStudio();
            case "Edit Studio" -> editStudio();
            case "Delete Studio" -> deleteStudio();
            case "Back" -> dispose();

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
        studioModel.setRowCount(0);
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