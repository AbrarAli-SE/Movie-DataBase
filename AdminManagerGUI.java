import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class AdminManagerGUI extends JFrame {
    private int userId;

    public AdminManagerGUI(int userId) {
        this.userId = userId;
        setTitle("Admin Manager");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(7, 1));

        String[] labels = {
                "Movie Operations", "Director Operations", "Studio Operations",
                "Actor Operations", "Genre Operations", "View Registered Users", "Logout"
        };

        for (String label : labels) {
            JButton button = new JButton(label);
            button.addActionListener(this::handleMenuClick);
            add(button);
        }
    }

    private void handleMenuClick(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Movie Operations" -> new MovieManagerGUI().setVisible(true);
            case "Director Operations" -> new DirectorManagerGUI().setVisible(true);
            case "Studio Operations" -> new StudioManagerGUI().setVisible(true);
            case "Actor Operations" -> new ActorManagerGUI().setVisible(true);
            case "Genre Operations" -> new GenreManagerGUI().setVisible(true);
            case "View Registered Users" -> new UserManagerGUI().setVisible(true);
            case "Logout" -> {
                JOptionPane.showMessageDialog(this, "Logged out.");
                dispose();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminManagerGUI(1).setVisible(true)); // Placeholder user ID
    }
}