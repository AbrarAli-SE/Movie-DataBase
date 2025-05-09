import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;

public class UserManagerGUI extends JFrame {
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

    private JTable userTable;
    private DefaultTableModel userModel;

    public UserManagerGUI() {
        setTitle("User Manager");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(245, 247, 250));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(44, 62, 80));
        JLabel titleLabel = new JLabel("User Manager", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        menuPanel.setBackground(new Color(245, 247, 250));
        String[] labels = { "Register", "Back", "Exit" };
        for (String label : labels) {
            RoundedButton button = new RoundedButton(label);
            menuPanel.add(button);
            button.addActionListener(this::handleMenuClick);
        }
        mainPanel.add(menuPanel, BorderLayout.NORTH);

        userModel = new DefaultTableModel(new String[] { "ID", "Username", "Email", "Join Date" }, 0);
        userTable = new JTable(userModel);
        mainPanel.add(new JScrollPane(userTable), BorderLayout.CENTER);

        JLabel footerLabel = new JLabel("Manage user accounts in the movie database.", SwingConstants.CENTER);
        footerLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        footerLabel.setForeground(new Color(51, 51, 51));
        mainPanel.add(footerLabel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        refreshUsers();
    }

    private void handleMenuClick(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Register" -> registerUser();
            case "Back" -> dispose();
            case "Exit" -> System.exit(0);
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
            refreshUsers();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error registering user: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshUsers() {
        userModel.setRowCount(0);
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