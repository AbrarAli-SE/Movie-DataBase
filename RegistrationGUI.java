import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

public class RegistrationGUI extends JFrame {
    public RegistrationGUI() {
        setTitle("User Registration");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(4, 2));

        JLabel usernameLabel = new JLabel("Username:");
        JTextField usernameField = new JTextField();
        JLabel emailLabel = new JLabel("Email:");
        JTextField emailField = new JTextField();
        JLabel passwordLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField();
        JButton registerButton = new JButton("Register");

        add(usernameLabel);
        add(usernameField);
        add(emailLabel);
        add(emailField);
        add(passwordLabel);
        add(passwordField);
        add(new JLabel()); // Empty space for alignment
        add(registerButton);

        registerButton.addActionListener(e -> registerUser(usernameField.getText(), emailField.getText(),
                new String(passwordField.getPassword())));

        setVisible(true);
    }

    private void registerUser(String username, String email, String password) {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String sql = "INSERT INTO User (username, email, password, joinDate) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, password);
            stmt.setDate(4, Date.valueOf(LocalDate.now()));
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "User registered successfully!");
            dispose(); // Close window after successful registration
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error registering user: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RegistrationGUI::new);
    }
}