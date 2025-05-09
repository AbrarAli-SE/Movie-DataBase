import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class DBGUI extends JFrame {
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

    public DBGUI() {
        setTitle("Movie Database");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(245, 247, 250));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(44, 62, 80));
        JLabel titleLabel = new JLabel("Movie Database", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(5, 1, 10, 10));
        buttonPanel.setBackground(new Color(245, 247, 250));

        String[] labels = { "Login", "Register (User)", "Search Movies (Guest)", "Back", "Exit" };
        for (String label : labels) {
            RoundedButton button = new RoundedButton(label);
            buttonPanel.add(button);
            button.addActionListener(this::handleMenuClick);
        }

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(new Color(245, 247, 250));
        centerWrapper.add(buttonPanel);
        mainPanel.add(centerWrapper, BorderLayout.CENTER);

        JLabel welcomeLabel = new JLabel("Explore and manage your movie collection.", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        welcomeLabel.setForeground(new Color(51, 51, 51));
        mainPanel.add(welcomeLabel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void handleMenuClick(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Login" -> new UserManagerGUI().setVisible(true);
            case "Register (User)" -> new RegistrationGUI().setVisible(true);
            case "Search Movies (Guest)" -> new MovieManagerGUI().setVisible(true);
            case "Back" -> {
                JOptionPane.showMessageDialog(this, "Closing application.");
                dispose();
            }
            case "Exit" -> System.exit(0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DBGUI().setVisible(true));
    }
}