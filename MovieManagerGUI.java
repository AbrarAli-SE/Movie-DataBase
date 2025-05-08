import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.time.LocalDate;

public class MovieManagerGUI extends JFrame {
    private JTable movieTable;
    private DefaultTableModel movieModel;

    public MovieManagerGUI() {
        setTitle("Movie Manager");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top menu buttons
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new GridLayout(1, 6));
        String[] labels = { "Add Movie", "Edit Movie", "Delete Movie", "View All Movies", "Search Movies", "Exit" };
        for (String label : labels) {
            JButton button = new JButton(label);
            button.addActionListener(this::handleMenuClick);
            menuPanel.add(button);
        }
        add(menuPanel, BorderLayout.NORTH);

        // Table setup
        movieModel = new DefaultTableModel(new String[] { "ID", "Title", "Release Date", "Duration", "Budget" }, 0);
        movieTable = new JTable(movieModel);
        add(new JScrollPane(movieTable), BorderLayout.CENTER);

        refreshMovies();
    }

    private void handleMenuClick(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Add Movie" -> addMovie();
            case "Edit Movie" -> editMovie();
            case "Delete Movie" -> deleteMovie();
            case "View All Movies" -> refreshMovies();
            case "Search Movies" -> searchMovies();
            case "Exit" -> System.exit(0);
        }
    }

    private void addMovie() {
        String title = JOptionPane.showInputDialog(this, "Enter Movie Title:");
        if (title == null || title.trim().isEmpty())
            return;

        String releaseDateStr = JOptionPane.showInputDialog(this, "Enter Release Date (YYYY-MM-DD):");
        Date releaseDate = releaseDateStr.isEmpty() ? null : Date.valueOf(releaseDateStr);

        int duration = InputValidatorGUI.getValidInt();
        double budget = InputValidatorGUI.getValidDouble();

        if (duration <= 0 || budget < 0) {
            JOptionPane.showMessageDialog(this, "Duration must be positive and budget non-negative.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO Movie (title, releaseDate, duration, budget) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, title);
                stmt.setDate(2, releaseDate != null ? releaseDate : null);
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
        }

        refreshMovies();
    }

    private void editMovie() {
        int movieId = InputValidatorGUI.getValidInt();
        String title = JOptionPane.showInputDialog(this, "Enter New Movie Title:");
        String releaseDateStr = JOptionPane.showInputDialog(this, "Enter New Release Date (YYYY-MM-DD):");
        Date releaseDate = releaseDateStr.isEmpty() ? null : Date.valueOf(releaseDateStr);
        int duration = InputValidatorGUI.getValidInt();
        double budget = InputValidatorGUI.getValidDouble();

        if (title == null || title.trim().isEmpty() || duration <= 0 || budget < 0) {
            JOptionPane.showMessageDialog(this, "Invalid input.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE Movie SET title = ?, releaseDate = ?, duration = ?, budget = ? WHERE movieId = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, title);
                stmt.setDate(2, releaseDate != null ? releaseDate : null);
                stmt.setInt(3, duration);
                stmt.setDouble(4, budget);
                stmt.setInt(5, movieId);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Movie updated successfully!");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error editing movie: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        refreshMovies();
    }

    private void deleteMovie() {
        int movieId = InputValidatorGUI.getValidInt();

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM Movie WHERE movieId = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, movieId);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Movie deleted successfully!");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting movie: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        refreshMovies();
    }

    private void refreshMovies() {
        movieModel.setRowCount(0); // Clear table before refreshing
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT movieId, title, releaseDate, duration, budget FROM Movie")) {
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
        }
    }

    private void searchMovies() {
        String searchTerm = JOptionPane.showInputDialog(this, "Enter Search Term:");
        if (searchTerm == null || searchTerm.trim().isEmpty())
            return;

        movieModel.setRowCount(0); // Clear table before refreshing
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
        }
    }

    private void addReview(String movieTitle, int userId) {
        int movieId = getMovieIdByTitle(movieTitle);
        if (movieId == -1) {
            JOptionPane.showMessageDialog(this, "Error: Movie not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double rating = InputValidatorGUI.getValidDouble();
        String comment = JOptionPane.showInputDialog(this, "Enter Comment (optional, press Enter to skip):");
        if (comment.isEmpty()) {
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
        }
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
        }
        return -1;
    }

    private void displayReviews(int movieId) {
        String sql = "SELECT userId, rating, comment, reviewDate FROM Review WHERE movieId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();
            StringBuilder reviews = new StringBuilder("Reviews:\n");
            boolean hasReviews = false;
            while (rs.next()) {
                hasReviews = true;
                reviews.append(String.format("- User %d: Rating %.1f/10, Comment: %s, Date: %s\n",
                        rs.getInt("userId"), rs.getDouble("rating"),
                        rs.getString("comment") != null ? rs.getString("comment") : "No comment",
                        rs.getDate("reviewDate")));
            }
            if (!hasReviews) {
                reviews.append("No reviews available.");
            }
            JOptionPane.showMessageDialog(this, reviews.toString(), "Movie Reviews", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching reviews: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MovieManagerGUI().setVisible(true));
    }
}

