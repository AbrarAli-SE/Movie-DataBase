import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MovieManagerGUI extends JFrame {
    static class CustomButton extends JButton {
        public CustomButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setFont(new Font("Segoe UI", Font.PLAIN, 12));
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

    // Custom renderer for budget column
    static class BudgetRenderer extends DefaultTableCellRenderer {
        private final DecimalFormat formatter = new DecimalFormat("$#,##0.###M");

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof Double) {
                double budget = (Double) value;
                System.out.println("Rendering budget for row " + row + ": " + budget);
                value = formatter.format(budget / 1_000_000); // Convert to millions
            } else {
                System.out.println("Unexpected budget value for row " + row + ": " + value);
                value = "$0M";
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    private boolean isGuest;
    private int userId;
    private JTextField searchField;
    private JScrollPane scrollPane;
    private DefaultTableModel movieModel;
    private JTable movieTable;

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

        // Set group_concat_max_len to handle long genre lists
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SET SESSION group_concat_max_len = 10000");
        } catch (SQLException e) {
            System.out.println("Error setting group_concat_max_len: " + e.getMessage());
            e.printStackTrace();
        }

        refreshMovies();
    }

    private void setupGuestUI(JPanel mainPanel) {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        searchPanel.setBackground(new Color(245, 247, 250));

        searchField = new JTextField(20);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        CustomButton searchButton = new CustomButton("Search");
        searchButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        CustomButton backButton = new CustomButton("Back");
        backButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));

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

        System.out.println("Initializing DefaultTableModel for guest mode");
        movieModel = new DefaultTableModel(new String[] { "Movie ID", "Title", "Release Date", "Duration", "Budget", "Genre" }, 0);
        movieTable = new JTable(movieModel);
        movieTable.setRowHeight(30);
        movieTable.getColumnModel().getColumn(0).setPreferredWidth(80); // Movie ID
        movieTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Title
        movieTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Budget
        movieTable.getColumnModel().getColumn(5).setPreferredWidth(150); // Genre
        movieTable.getColumnModel().getColumn(4).setCellRenderer(new BudgetRenderer()); // Apply BudgetRenderer
        scrollPane = new JScrollPane(movieTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void setupUserUI(JPanel mainPanel) {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        searchPanel.setBackground(new Color(245, 247, 250));

        searchField = new JTextField(20);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        CustomButton searchButton = new CustomButton("Search");
        searchButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        CustomButton addMovieButton = new CustomButton("Add Movie");
        addMovieButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        CustomButton addReviewButton = new CustomButton("Add Review");
        addReviewButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        CustomButton backButton = new CustomButton("Back");
        backButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));

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

        System.out.println("Initializing DefaultTableModel for user mode");
        movieModel = new DefaultTableModel(new String[] { "Movie ID", "Title", "Release Date", "Duration", "Budget", "Genre", "Reviews" }, 0);
        movieTable = new JTable(movieModel);
        movieTable.setRowHeight(30);
        movieTable.getColumnModel().getColumn(0).setPreferredWidth(80); // Movie ID
        movieTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Title
        movieTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Budget
        movieTable.getColumnModel().getColumn(5).setPreferredWidth(150); // Genre
        movieTable.getColumnModel().getColumn(4).setCellRenderer(new BudgetRenderer()); // Apply BudgetRenderer
        movieTable.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer());
        movieTable.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox()));
        scrollPane = new JScrollPane(movieTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    // Custom renderer for the View Reviews button
    static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            super("View Reviews");
            setOpaque(true);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setFont(new Font("Segoe UI", Font.PLAIN, 12));
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

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    // Custom editor for the View Reviews button
    class ButtonEditor extends DefaultCellEditor {
        private CustomButton button;
        private String label;
        private boolean isPushed;
        private int movieId;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new CustomButton("View Reviews");
            button.setOpaque(true);
            button.addActionListener(e -> {
                isPushed = true;
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
           movieId = (Integer) table.getValueAt(row, 0);
            label = "View Reviews";
            button.setText(label);
            isPushed = false;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                showReviews(movieId);
            }
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }

        @Override
        protected void fireEditingStopped() {
            super.fireEditingStopped();
        }
    }

    // Utility method to format duration
    private String formatDuration(int minutes) {
        if (minutes <= 0) return "0 min";
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        StringBuilder sb = new StringBuilder();
        sb.append(minutes).append(" min");
        if (hours > 0 || remainingMinutes > 0) {
            sb.append(" (");
            if (hours > 0) {
                sb.append(hours).append("h");
                if (remainingMinutes > 0) sb.append(" ");
            }
            if (remainingMinutes > 0) {
                sb.append(remainingMinutes).append("m");
            }
            sb.append(")");
        }
        return sb.toString();
    }

    // Utility method to format budget for reviews modal
    private String formatBudget(double budget) {
        DecimalFormat formatter = new DecimalFormat("$#,##0.###M");
        return formatter.format(budget / 1_000_000);
    }

    // Utility method to truncate long genre strings
    private String truncateGenres(String genres) {
        if (genres == null || genres.isEmpty()) return "";
        if (genres.length() <= 50) return genres;
        return genres.substring(0, 47) + "...";
    }

    // Fetch available genres from the Genre table
    private List<String> getAvailableGenres() {
        List<String> genres = new ArrayList<>();
        String sql = "SELECT name FROM Genre ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                genres.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching genres: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error fetching genres: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return genres;
    }

    private void refreshMovies() {
        System.out.println("Refreshing movies for " + (isGuest ? "guest" : "user") + " mode");
        movieModel.setRowCount(0);
        String sql = "SELECT m.movieId, m.title, m.releaseDate, m.duration, m.budget, " +
                     "GROUP_CONCAT(g.name ORDER BY g.name SEPARATOR ', ') AS genres " +
                     "FROM Movie m " +
                     "LEFT JOIN Movie_Genre mg ON m.movieId = mg.movieId " +
                     "LEFT JOIN Genre g ON mg.genreId = g.genreId " +
                     "GROUP BY m.movieId";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            boolean hasMovies = false;
            while (rs.next()) {
                hasMovies = true;
                Object[] row = isGuest ? new Object[] {
                        rs.getInt("movieId"),
                        rs.getString("title") != null ? rs.getString("title") : "",
                        rs.getDate("releaseDate") != null ? rs.getDate("releaseDate") : "",
                        formatDuration(rs.getInt("duration")),
                        rs.getDouble("budget"),
                        truncateGenres(rs.getString("genres"))
                } : new Object[] {
                        rs.getInt("movieId"),
                        rs.getString("title") != null ? rs.getString("title") : "",
                        rs.getDate("releaseDate") != null ? rs.getDate("releaseDate") : "",
                        formatDuration(rs.getInt("duration")),
                        rs.getDouble("budget"),
                        truncateGenres(rs.getString("genres")),
                        "View Reviews" // Placeholder for button
                };
                movieModel.addRow(row);
            }
            if (!hasMovies) {
                System.out.println("No movies found in database");
                movieModel.addRow(new Object[] { "", "No movies found", "", "", "", "" });
            }
            movieTable.revalidate();
            movieTable.repaint();
        } catch (SQLException e) {
            System.out.println("Error fetching movies: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error fetching movies: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            movieModel.addRow(new Object[] { "", "Error loading movies", "", "", "", "" });
        }
    }

    private void searchMovies() {
        String searchTerm = searchField.getText().trim();
        System.out.println("Searching movies with term: " + searchTerm);
        movieModel.setRowCount(0);
        String sql = "SELECT m.movieId, m.title, m.releaseDate, m.duration, m.budget, " +
                     "GROUP_CONCAT(g.name ORDER BY g.name SEPARATOR ', ') AS genres " +
                     "FROM Movie m " +
                     "LEFT JOIN Movie_Genre mg ON m.movieId = mg.movieId " +
                     "LEFT JOIN Genre g ON mg.genreId = g.genreId " +
                     "WHERE LOWER(m.title) LIKE ? " +
                     "GROUP BY m.movieId";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + searchTerm.toLowerCase() + "%");
            ResultSet rs = stmt.executeQuery();
            boolean hasMovies = false;
            while (rs.next()) {
                hasMovies = true;
                Object[] row = isGuest ? new Object[] {
                        rs.getInt("movieId"),
                        rs.getString("title") != null ? rs.getString("title") : "",
                        rs.getDate("releaseDate") != null ? rs.getDate("releaseDate") : "",
                        formatDuration(rs.getInt("duration")),
                        rs.getDouble("budget"),
                        truncateGenres(rs.getString("genres"))
                } : new Object[] {
                        rs.getInt("movieId"),
                        rs.getString("title") != null ? rs.getString("title") : "",
                        rs.getDate("releaseDate") != null ? rs.getDate("releaseDate") : "",
                        formatDuration(rs.getInt("duration")),
                        rs.getDouble("budget"),
                        truncateGenres(rs.getString("genres")),
                        "View Reviews" // Placeholder for button
                };
                movieModel.addRow(row);
            }
            if (!hasMovies) {
                System.out.println("No movies found for search term: " + searchTerm);
                movieModel.addRow(new Object[] { "", "No movies found", "", "", "", "" });
            }
            movieTable.revalidate();
            movieTable.repaint();
        } catch (SQLException e) {
            System.out.println("Error searching movies: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error searching movies: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            movieModel.addRow(new Object[] { "", "Error searching movies", "", "", "", "" });
        }
    }

    private void showReviews(int movieId) {
        System.out.println("Showing reviews for movieId: " + movieId);
        JDialog reviewDialog = new JDialog(this, "Movie Details and Reviews for Movie ID: " + movieId, true);
        reviewDialog.setSize(600, 500);
        reviewDialog.setLocationRelativeTo(this);
        reviewDialog.setLayout(new BorderLayout(10, 10));

        // Movie Details Panel
        JPanel detailsPanel = new JPanel(new GridBagLayout());
        detailsPanel.setBackground(new Color(245, 247, 250));
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Movie Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        String sqlMovie = "SELECT m.title, m.releaseDate, m.duration, m.budget, " +
                         "GROUP_CONCAT(g.name ORDER BY g.name SEPARATOR ', ') AS genres " +
                         "FROM Movie m " +
                         "LEFT JOIN Movie_Genre mg ON m.movieId = mg.movieId " +
                         "LEFT JOIN Genre g ON mg.genreId = g.genreId " +
                         "WHERE m.movieId = ? " +
                         "GROUP BY m.movieId";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlMovie)) {
            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                gbc.gridx = 0;
                gbc.gridy = 0;
                detailsPanel.add(new JLabel("<html><b>Title:</b> " + (rs.getString("title") != null ? rs.getString("title") : "N/A") + "</html>"), gbc);
                gbc.gridy++;
                detailsPanel.add(new JLabel("<html><b>Release Date:</b> " + (rs.getDate("releaseDate") != null ? rs.getDate("releaseDate") : "N/A") + "</html>"), gbc);
                gbc.gridy++;
                detailsPanel.add(new JLabel("<html><b>Duration:</b> " + formatDuration(rs.getInt("duration")) + "</html>"), gbc);
                gbc.gridy++;
                detailsPanel.add(new JLabel("<html><b>Budget:</b> " + formatBudget(rs.getDouble("budget")) + "</html>"), gbc);
                gbc.gridy++;
                detailsPanel.add(new JLabel("<html><b>Genres:</b> " + (rs.getString("genres") != null ? rs.getString("genres") : "N/A") + "</html>"), gbc);
            } else {
                gbc.gridx = 0;
                gbc.gridy = 0;
                detailsPanel.add(new JLabel("Movie not found"), gbc);
            }
        } catch (SQLException e) {
            System.out.println("Error fetching movie details: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error fetching movie details: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            gbc.gridx = 0;
            gbc.gridy = 0;
            detailsPanel.add(new JLabel("Error loading movie details"), gbc);
        }

        // Reviews Panel
        JPanel reviewsPanel = new JPanel();
        reviewsPanel.setLayout(new BoxLayout(reviewsPanel, BoxLayout.Y_AXIS));
        reviewsPanel.setBackground(new Color(245, 247, 250));
        reviewsPanel.setBorder(BorderFactory.createTitledBorder("Reviews"));

        String sqlReviews = "SELECT r.userId, r.rating, r.comment, r.reviewDate, u.email " +
                           "FROM Review r LEFT JOIN User u ON r.userId = u.userId " +
                           "WHERE r.movieId = ?";
        boolean hasReviews = false;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlReviews)) {
            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                hasReviews = true;
                JPanel reviewPanel = new JPanel(new GridBagLayout());
                reviewPanel.setBackground(new Color(245, 245, 245));
                reviewPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                GridBagConstraints reviewGbc = new GridBagConstraints();
                reviewGbc.insets = new Insets(3, 5, 3, 5);
                reviewGbc.anchor = GridBagConstraints.WEST;
                reviewGbc.gridx = 0;
                reviewGbc.gridy = 0;
                reviewPanel.add(new JLabel("<html><b>Email:</b> " + (rs.getString("email") != null ? rs.getString("email") : "Unknown") + "</html>"), reviewGbc);
                reviewGbc.gridy++;
                reviewPanel.add(new JLabel("<html><b>Rating:</b> " + rs.getDouble("rating") + "</html>"), reviewGbc);
                reviewGbc.gridy++;
                reviewPanel.add(new JLabel("<html><b>Comment:</b> " + (rs.getString("comment") != null ? rs.getString("comment") : "No comment") + "</html>"), reviewGbc);
                reviewGbc.gridy++;
                reviewPanel.add(new JLabel("<html><b>Review Date:</b> " + (rs.getDate("reviewDate") != null ? rs.getDate("reviewDate").toString() : "N/A") + "</html>"), reviewGbc);
                reviewsPanel.add(reviewPanel);
                reviewsPanel.add(Box.createVerticalStrut(5));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching reviews: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error fetching reviews: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            reviewsPanel.add(new JLabel("Error loading reviews"));
            return;
        }

        if (!hasReviews) {
            reviewsPanel.add(new JLabel("No reviews available for this movie."));
        }

        // Combine panels
        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.setBackground(new Color(245, 247, 250));
        contentPanel.add(detailsPanel, BorderLayout.NORTH);
        contentPanel.add(new JScrollPane(reviewsPanel), BorderLayout.CENTER);
        reviewDialog.add(contentPanel, BorderLayout.CENTER);

        CustomButton closeButton = new CustomButton("Close");
        closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        closeButton.addActionListener(e -> reviewDialog.dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(new Color(245, 247, 250));
        buttonPanel.add(closeButton);
        reviewDialog.add(buttonPanel, BorderLayout.SOUTH);

        reviewDialog.setVisible(true);
    }

    private void addMovie() {
        // Input for movie details
        JTextField titleField = new JTextField(20);
        JTextField releaseDateField = new JTextField(10);
        JTextField durationField = new JTextField(5);
        JTextField budgetField = new JTextField(10);

        // Genre selection
        List<String> availableGenres = getAvailableGenres();
        JPanel genrePanel = new JPanel(new BorderLayout());
        if (availableGenres.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "No genres available in the database. Proceed without genres?",
                    "No Genres Available",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            genrePanel.add(new JLabel("No genres available"), BorderLayout.CENTER);
        } else {
            JList<String> genreList = new JList<>(availableGenres.toArray(new String[0]));
            genreList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            genreList.setVisibleRowCount(5);
            JScrollPane genreScrollPane = new JScrollPane(genreList);
            genreScrollPane.setPreferredSize(new Dimension(200, 100));
            genrePanel.add(new JLabel("Hold Ctrl to select multiple genres"), BorderLayout.NORTH);
            genrePanel.add(genreScrollPane, BorderLayout.CENTER);
        }

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(titleField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Release Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        inputPanel.add(releaseDateField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Duration (minutes):"), gbc);
        gbc.gridx = 1;
        inputPanel.add(durationField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Budget (dollars):"), gbc);
        gbc.gridx = 1;
        inputPanel.add(budgetField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Genres:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(genrePanel, gbc);

        int result = JOptionPane.showConfirmDialog(this, inputPanel, "Add Movie", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Error: Title is required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Date releaseDate = null;
        String releaseDateStr = releaseDateField.getText().trim();
        if (!releaseDateStr.isEmpty()) {
            try {
                releaseDate = Date.valueOf(releaseDateStr);
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, "Error: Invalid date format. Use YYYY-MM-DD.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        int duration;
        try {
            duration = Integer.parseInt(durationField.getText().trim());
            if (duration <= 0) {
                JOptionPane.showMessageDialog(this, "Error: Duration must be positive.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Error: Invalid duration format.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        double budget;
        try {
            budget = Double.parseDouble(budgetField.getText().trim());
            if (budget < 0) {
                JOptionPane.showMessageDialog(this, "Error: Budget must be non-negative.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Error: Invalid budget format.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> selectedGenres = availableGenres.isEmpty() ? new ArrayList<>() : ((JList<String>) ((JScrollPane) genrePanel.getComponent(1)).getViewport().getView()).getSelectedValuesList();

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Insert movie
                String sqlMovie = "INSERT INTO Movie (title, releaseDate, duration, budget) VALUES (?, ?, ?, ?)";
                int movieId;
                try (PreparedStatement stmt = conn.prepareStatement(sqlMovie, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, title);
                    stmt.setDate(2, releaseDate);
                    stmt.setInt(3, duration);
                    stmt.setDouble(4, budget);
                    stmt.executeUpdate();
                    ResultSet rs = stmt.getGeneratedKeys();
                    if (!rs.next()) {
                        throw new SQLException("Failed to retrieve movie ID");
                    }
                    movieId = rs.getInt(1);
                }

                // Insert genres
                if (!selectedGenres.isEmpty()) {
                    String sqlGenre = "INSERT INTO Movie_Genre (movieId, genreId) SELECT ?, genreId FROM Genre WHERE name = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlGenre)) {
                        for (String genre : selectedGenres) {
                            stmt.setInt(1, movieId);
                            stmt.setString(2, genre);
                            stmt.executeUpdate();
                        }
                    }
                }

                conn.commit();
                JOptionPane.showMessageDialog(this, "Movie added successfully with ID: " + movieId);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println("Error adding movie: " + e.getMessage());
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
                JOptionPane.showMessageDialog(this, "Error: Rating must be between 0 and 10.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Error: Invalid rating format.", "Error",
                    JOptionPane.ERROR_MESSAGE);
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
            System.out.println("Error adding review: " + e.getMessage());
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
            System.out.println("Error retrieving movie ID: " + e.getMessage());
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