import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;

public class MovieManagerGUI extends JFrame {
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

    private JPanel moviesPanel;
    private boolean isGuest;
    private int userId;
    private JTextField searchField;
    private JScrollPane scrollPane;

    public MovieManagerGUI(boolean isGuest, int userId) {
        this.isGuest = isGuest;
        this.userId = userId;
        setTitle(isGuest ? "Guest Movie Search" : "Movie Manager");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(245, 247, 250));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(44, 62, 80));
        JLabel titleLabel = new JLabel(isGuest ? "Guest Movie Search" : "Movie Manager", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        if (isGuest) {
            setupGuestUI(mainPanel);
        } else {
            setupUserUI(mainPanel);
        }

        JLabel footerLabel = new JLabel(isGuest ? "Search movies as a guest." : "Manage movies and reviews.", SwingConstants.CENTER);
        footerLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        footerLabel.setForeground(new Color(51, 51, 51));
        mainPanel.add(footerLabel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        refreshMovies();
    }

    private void setupGuestUI(JPanel mainPanel) {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        searchPanel.setBackground(new Color(245, 247, 250));

        searchField = new JTextField(20);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        RoundedButton searchButton = new RoundedButton("Search");
        RoundedButton backButton = new RoundedButton("Back");

        searchPanel.add(new JLabel("Search Movies:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(backButton);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { searchMovies(); }
            @Override
            public void removeUpdate(DocumentEvent e) { searchMovies(); }
            @Override
            public void changedUpdate(DocumentEvent e) { searchMovies(); }
        });

        searchButton.addActionListener(e -> searchMovies());
        backButton.addActionListener(e -> dispose());

        mainPanel.add(searchPanel, BorderLayout.NORTH);

        new DefaultTableModel(new String[] { "Movie ID", "Title", "Release Date", "Duration", "Budget" }, 0);
        JTable movieTable = new JTable();
        scrollPane = new JScrollPane(movieTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void setupUserUI(JPanel mainPanel) {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        searchPanel.setBackground(new Color(245, 247, 250));

        searchField = new JTextField(20);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        RoundedButton searchButton = new RoundedButton("Search");
        RoundedButton addMovieButton = new RoundedButton("Add Movie");
        RoundedButton addReviewButton = new RoundedButton("Add Review");
        RoundedButton backButton = new RoundedButton("Back");

        searchPanel.add(new JLabel("Search Movies:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(addMovieButton);
        searchPanel.add(addReviewButton);
        searchPanel.add(backButton);

        searchPanel.add(new JLabel("Search Movies:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(addMovieButton);
        searchPanel.add(addReviewButton);
        searchPanel.add(backButton);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { searchMovies(); }
            @Override
            public void removeUpdate(DocumentEvent e) { searchMovies(); }
            @Override
            public void changedUpdate(DocumentEvent e) { searchMovies(); }
        });

        searchButton.addActionListener(e -> searchMovies());
        addMovieButton.addActionListener(e -> addMovie());
        addReviewButton.addActionListener(e -> addReview());
        backButton.addActionListener(e -> dispose());

        mainPanel.add(searchPanel, BorderLayout.NORTH);

        moviesPanel = new JPanel();
        moviesPanel.setLayout(new BoxLayout(moviesPanel, BoxLayout.Y_AXIS));
        moviesPanel.setBackground(new Color(245, 247, 250));
        scrollPane = new JScrollPane(moviesPanel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void refreshMovies() {
        if (isGuest) {
            JTable table = (JTable) scrollPane.getViewport().getView();
            DefaultTableModel movieModel = (DefaultTableModel) table.getModel();
            movieModel.setRowCount(0);
            String sql = "SELECT movieId, title, releaseDate, duration, budget FROM Movie";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    movieModel.addRow(new Object[] {
                            rs.getInt("movieId"),
                            rs.getString("title"),
                            rs.getDate("releaseDate"),
                            rs.getInt("duration"),
                            rs.getDouble("budget")
                    });
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error fetching movies: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        } else {
            moviesPanel.removeAll();
            String sql = "SELECT movieId, title, releaseDate, duration, budget FROM Movie";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int movieId = rs.getInt("movieId");
                    JPanel moviePanel = new JPanel(new GridBagLayout());
                    moviePanel.setBackground(Color.WHITE);
                    moviePanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
                    moviePanel.setMaximumSize(new Dimension(900, Integer.MAX_VALUE));

                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.insets = new Insets(0, 5, 0, 15);
                    gbc.anchor = GridBagConstraints.WEST;
                    gbc.gridx = 0;
                    gbc.gridy = 0;
                    moviePanel.add(new JLabel(String.valueOf(movieId)), gbc);
                    gbc.gridx++;
                    moviePanel.add(new JLabel(rs.getString("title") != null ? rs.getString("title") : ""), gbc);
                    gbc.gridx++;
                    moviePanel.add(new JLabel(rs.getDate("releaseDate") != null ? rs.getDate("releaseDate").toString() : ""), gbc);
                    gbc.gridx++;
                    moviePanel.add(new JLabel(rs.getInt("duration") + " min"), gbc);
                    gbc.gridx++;
                    moviePanel.add(new JLabel("$" + rs.getDouble("budget")), gbc);
                    gbc.gridx++;
                    RoundedButton viewReviewsButton = new RoundedButton("View Reviews");
                    viewReviewsButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    moviePanel.add(viewReviewsButton, gbc);

                    viewReviewsButton.addActionListener(e -> showReviews(movieId));

                    moviesPanel.add(moviePanel);
                    moviesPanel.add(Box.createVerticalStrut(10));
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error fetching movies: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
            moviesPanel.revalidate();
            moviesPanel.repaint();
        }
    }

    private void searchMovies() {
        String searchTerm = searchField.getText().trim();
        if (isGuest) {
            JTable table = (JTable) scrollPane.getViewport().getView();
            DefaultTableModel movieModel = (DefaultTableModel) table.getModel();
            movieModel.setRowCount(0);
            String sql = "SELECT movieId, title, releaseDate, duration, budget FROM Movie WHERE LOWER(title) LIKE ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "%" + searchTerm.toLowerCase() + "%");
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    movieModel.addRow(new Object[] {
                            rs.getInt("movieId"),
                            rs.getString("title"),
                            rs.getDate("releaseDate"),
                            rs.getInt("duration"),
                            rs.getDouble("budget")
                    });
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error searching movies: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        } else {
            moviesPanel.removeAll();
            String sql = "SELECT movieId, title, releaseDate, duration, budget FROM Movie WHERE LOWER(title) LIKE ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "%" + searchTerm.toLowerCase() + "%");
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    int movieId = rs.getInt("movieId");
                    JPanel moviePanel = new JPanel(new GridBagLayout());
                    moviePanel.setBackground(Color.WHITE);
                    moviePanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
                    moviePanel.setMaximumSize(new Dimension(900, Integer.MAX_VALUE));

                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.insets = new Insets(0, 5, 0, 15);
                    gbc.anchor = GridBagConstraints.WEST;
                    gbc.gridx = 0;
                    gbc.gridy = 0;
                    moviePanel.add(new JLabel(String.valueOf(movieId)), gbc);
                    gbc.gridx++;
                    moviePanel.add(new JLabel(rs.getString("title") != null ? rs.getString("title") : ""), gbc);
                    gbc.gridx++;
                    moviePanel.add(new JLabel(rs.getDate("releaseDate") != null ? rs.getDate("releaseDate").toString() : ""), gbc);
                    gbc.gridx++;
                    moviePanel.add(new JLabel(rs.getInt("duration") + " min"), gbc);
                    gbc.gridx++;
                    moviePanel.add(new JLabel("$" + rs.getDouble("budget")), gbc);
                    gbc.gridx++;
                    RoundedButton viewReviewsButton = new RoundedButton("View Reviews");
                    viewReviewsButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    moviePanel.add(viewReviewsButton, gbc);

                    viewReviewsButton.addActionListener(e -> showReviews(movieId));

                    moviesPanel.add(moviePanel);
                    moviesPanel.add(Box.createVerticalStrut(10));
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error searching movies: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
            moviesPanel.revalidate();
            moviesPanel.repaint();
        }
    }

    private void showReviews(int movieId) {
        JDialog reviewDialog = new JDialog(this, "Reviews for Movie ID: " + movieId, true);
        reviewDialog.setSize(600, 400);
        reviewDialog.setLocationRelativeTo(this);
        reviewDialog.setLayout(new BorderLayout(10, 10));

        JPanel reviewsPanel = new JPanel();
        reviewsPanel.setLayout(new BoxLayout(reviewsPanel, BoxLayout.Y_AXIS));
        reviewsPanel.setBackground(new Color(245, 247, 250));
        reviewsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String sql = "SELECT r.userId, r.rating, r.comment, r.reviewDate, u.email " +
                     "FROM Review r LEFT JOIN User u ON r.userId = u.userId " +
                     "WHERE r.movieId = ?";
        boolean hasReviews = false;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                hasReviews = true;
                JPanel reviewPanel = new JPanel(new GridBagLayout());
                reviewPanel.setBackground(new Color(245, 245, 245));
                reviewPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(3, 5, 3, 5);
                gbc.anchor = GridBagConstraints.WEST;
                gbc.gridx = 0;
                gbc.gridy = 0;
                reviewPanel.add(new JLabel("<html><b>Email:</b> " + (rs.getString("email") != null ? rs.getString("email") : "Unknown") + "</html>"), gbc);
                gbc.gridy++;
                reviewPanel.add(new JLabel("<html><b>Rating:</b> " + rs.getDouble("rating") + "</html>"), gbc);
                gbc.gridy++;
                reviewPanel.add(new JLabel("<html><b>Comment:</b> " + (rs.getString("comment") != null ? rs.getString("comment") : "No comment") + "</html>"), gbc);
                gbc.gridy++;
                reviewPanel.add(new JLabel("<html><b>Review Date:</b> " + (rs.getDate("reviewDate") != null ? rs.getDate("reviewDate").toString() : "N/A") + "</html>"), gbc);
                reviewsPanel.add(reviewPanel);
                reviewsPanel.add(Box.createVerticalStrut(5));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching reviews: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return;
        }

        if (!hasReviews) {
            reviewsPanel.add(new JLabel("No reviews available for this movie."));
        }

        JScrollPane reviewsScrollPane = new JScrollPane(reviewsPanel);
        reviewDialog.add(reviewsScrollPane, BorderLayout.CENTER);

        RoundedButton closeButton = new RoundedButton("Close");
        closeButton.addActionListener(e -> reviewDialog.dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(new Color(245, 247, 250));
        buttonPanel.add(closeButton);
        reviewDialog.add(buttonPanel, BorderLayout.SOUTH);

        reviewDialog.setVisible(true);
    }

    private void addMovie() {
        String title = JOptionPane.showInputDialog(this, "Enter Movie Title:");
        if (title == null || title.trim().isEmpty()) {
            return;
        }

        String releaseDateStr = JOptionPane.showInputDialog(this, "Enter Release Date (YYYY-MM-DD):");
        Date releaseDate = null;
        if (releaseDateStr != null && !releaseDateStr.trim().isEmpty()) {
            try {
                releaseDate = Date.valueOf(releaseDateStr);
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        String durationStr = JOptionPane.showInputDialog(this, "Enter Duration (minutes):");
        int duration;
        try {
            duration = Integer.parseInt(durationStr);
            if (duration <= 0) {
                JOptionPane.showMessageDialog(this, "Duration must be positive.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid duration format.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String budgetStr = JOptionPane.showInputDialog(this, "Enter Budget (in dollars):");
        double budget;
        try {
            budget = Double.parseDouble(budgetStr);
            if (budget < 0) {
                JOptionPane.showMessageDialog(this, "Budget must be non-negative.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid budget format.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO Movie (title, releaseDate, duration, budget) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, title);
                stmt.setDate(2, releaseDate);
                stmt.setInt(3, duration);
                stmt.setDouble(4, budget);
                stmt.executeUpdate();
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "Movie added successfully with ID: " + rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding movie: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        refreshMovies();
    }

    private void addReview() {
        String movieTitle = JOptionPane.showInputDialog(this, "Enter Movie Title:");
        if (movieTitle == null || movieTitle.trim().isEmpty()) {
            return;
        }

        int movieId = getMovieIdByTitle(movieTitle);
        if (movieId == -1) {
            JOptionPane.showMessageDialog(this, "Error: Movie not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String ratingStr = JOptionPane.showInputDialog(this, "Enter Rating (0-10):");
        double rating;
        try {
            rating = Double.parseDouble(ratingStr);
            if (rating < 0 || rating > 10) {
                JOptionPane.showMessageDialog(this, "Rating must be between 0 and 10.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid rating format.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String comment = JOptionPane.showInputDialog(this, "Enter Comment (optional, press Enter to skip):");
        if (comment != null && comment.trim().isEmpty()) {
            comment = null;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO Review (movieId, userId, rating, comment, reviewDate) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, movieId);
                stmt.setInt(2, userId);
                stmt.setDouble(3, rating);
                stmt.setString(4, comment);
                stmt.setDate(5, Date.valueOf(LocalDate.now()));
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Review added successfully!");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding review: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        refreshMovies();
    }

    private int getMovieIdByTitle(String movieTitle) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT movieId FROM Movie WHERE title = ?")) {
            stmt.setString(1, movieTitle);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("movieId");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error retrieving movie ID: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return -1;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MovieManagerGUI(false, 1).setVisible(true));
    }
}