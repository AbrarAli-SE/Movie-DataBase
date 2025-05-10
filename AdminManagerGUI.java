import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AdminManagerGUI extends JFrame {
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

        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        menuPanel.setBackground(new Color(245, 247, 250));
        String[] labels = { "Manage Actors", "Manage Directors", "Manage Genres", "Manage Studios", "Manage Users", "Manage Movies", "Back", "Exit" };
        for (String label : labels) {
            CustomButton button = new CustomButton(label);
            menuPanel.add(button);
            button.addActionListener(this::handleMenuClick);
        }
        mainPanel.add(menuPanel, BorderLayout.CENTER);

        JLabel footerLabel = new JLabel("Admin control panel for the movie database.", SwingConstants.CENTER);
        footerLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        footerLabel.setForeground(new Color(51, 51, 51));
        mainPanel.add(footerLabel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void handleMenuClick(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Manage Actors" -> new ActorManagerGUI().setVisible(true);
            case "Manage Directors" -> new DirectorManagerGUI().setVisible(true);
            case "Manage Genres" -> new GenreManagerGUI().setVisible(true);
            case "Manage Studios" -> new StudioManagerGUI().setVisible(true);
            case "Manage Users" -> new UserManagerGUI().setVisible(true);
            case "Manage Movies" -> new MovieManagerGUI(false, userId).setVisible(true);
            case "Back" -> dispose();
            case "Exit" -> System.exit(0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminManagerGUI(0).setVisible(true));
    }
}