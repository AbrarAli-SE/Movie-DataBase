import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AdminManagerGUI extends JFrame {
    static class CustomButton extends JButton {
        public CustomButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setFont(new Font("Segoe UI", Font.PLAIN, 12)); // Reduced font size to 12
            setForeground(Color.WHITE);
            setBackground(new Color(0, 123, 255));
            // Set a preferred size to make the button thinner while ensuring text fits
            setPreferredSize(new Dimension(120, 30)); // Thinner width (120px), reasonable height (30px)
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

        // Use GridBagLayout for more control over button sizing
        JPanel menuPanel = new JPanel(new GridBagLayout());
        menuPanel.setBackground(new Color(245, 247, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // 5px padding between buttons
        gbc.fill = GridBagConstraints.NONE; // Buttons should not stretch
        gbc.anchor = GridBagConstraints.CENTER;

        String[] labels = { "Manage Actors", "Manage Directors", "Manage Genres", "Manage Studios", 
                            "Manage Users", "Manage Movies", "Back", "Exit" };
        for (int i = 0; i < labels.length; i++) {
            CustomButton button = new CustomButton(labels[i]);
            gbc.gridx = i % 2; // 2 columns
            gbc.gridy = i / 2; // 4 rows
            menuPanel.add(button, gbc);
            button.addActionListener(this::handleMenuClick);
            System.out.println("Added button: " + labels[i] + " - Visible: " + button.isVisible() + 
                              ", Size: " + button.getSize() + ", Preferred Size: " + button.getPreferredSize());
        }
        mainPanel.add(menuPanel, BorderLayout.CENTER);

        JLabel footerLabel = new JLabel("Admin control panel for the movie database.", SwingConstants.CENTER);
        footerLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        footerLabel.setForeground(new Color(51, 51, 51));
        mainPanel.add(footerLabel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        // Force revalidation and repaint to ensure UI updates
        menuPanel.revalidate();
        menuPanel.repaint();
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void handleMenuClick(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Manage Actors" -> new ActorManagerGUI().setVisible(true);
            case "Manage Directors" -> new DirectorManagerGUI().setVisible(true);
            case "Manage Genres" -> new GenreManagerGUI().setVisible(true);
            case "Manage Studios" -> new StudioManagerGUI().setVisible(true);
            case "Manage Users" -> new UserManagerGUI().setVisible(true);
            case "Manage Movies" -> new MovieManagerGUI(false, userId, true).setVisible(true);
            case "Back" -> dispose();
            case "Exit" -> System.exit(0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminManagerGUI(0).setVisible(true));
    }
}