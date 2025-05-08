import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class DBGUI extends JFrame {
    public DBGUI() {
        setTitle("Movie Database Login");
        setSize(500, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(4, 1));

        String[] labels = { "Login", "Register (User)", "Search Movies (Guest)", "Exit" };
        for (String label : labels) {
            JButton button = new JButton(label);
            button.addActionListener(this::handleMenuClick);
            add(button);
        }
    }

    private void handleMenuClick(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Login" -> new UserManagerGUI().setVisible(true);
            case "Register (User)" -> new RegistrationGUI().setVisible(true);
            case "Search Movies (Guest)" -> new MovieManagerGUI().setVisible(true); // Pass userId=0 for guest
            case "Exit" -> System.exit(0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DBGUI().setVisible(true));
    }
}