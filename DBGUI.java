import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class DBGUI extends JFrame {
    public DBGUI() {
        setTitle("Movie Database Login");
        setSize(500, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Creating a panel to hold buttons at the top
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new GridLayout(1, 4)); // Buttons in a single row
        String[] labels = { "Login", "Register (User)", "Search Movies (Guest)", "Exit" };
        for (String label : labels) {
            JButton button = new JButton(label);
            button.addActionListener(this::handleMenuClick);
            menuPanel.add(button);
        }

        // Adding the panel to the top of the layout
        add(menuPanel, BorderLayout.NORTH);

        // Adding an empty space in the center for better structure
        add(new JLabel("Welcome to the Movie Database", SwingConstants.CENTER), BorderLayout.CENTER);
    }

    private void handleMenuClick(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Login" -> new UserManagerGUI().setVisible(true);
            case "Register (User)" -> new RegistrationGUI().setVisible(true);
            case "Search Movies (Guest)" -> new MovieManagerGUI().setVisible(true);
            case "Exit" -> System.exit(0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DBGUI().setVisible(true));
    }
}