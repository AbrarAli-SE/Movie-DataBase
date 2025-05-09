import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AdminManagerGUI extends JFrame {
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

    private int userId;

    public AdminManagerGUI(int userId) {
        this.userId = userId;
        setTitle("Admin Manager");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(245, 247, 250));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(44, 62, 80));
        JLabel titleLabel = new JLabel("Admin Manager", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(8, 1, 10, 10));
        buttonPanel.setBackground(new Color(245, 247, 250));

        String[] labels = {
                "Movie Operations", "Director Operations", "Studio Operations",
                "Actor Operations", "Genre Operations", "View Registered Users", "Back", "Logout"
        };
        for (String label : labels) {
            RoundedButton button = new RoundedButton(label);
            buttonPanel.add(button);
            button.addActionListener(this::handleMenuClick);
        }

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(new Color(245, 247, 250));
        centerWrapper.add(buttonPanel);
        mainPanel.add(centerWrapper, BorderLayout.CENTER);

        JLabel footerLabel = new JLabel("Admin panel for managing database operations.", SwingConstants.CENTER);
        footerLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        footerLabel.setForeground(new Color(51, 51, 51));
        mainPanel.add(footerLabel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
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
            case "Back" -> dispose();
            case "Logout" -> {
                JOptionPane.showMessageDialog(this, "Logged out.");
                dispose();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminManagerGUI(1).setVisible(true));
    }
}