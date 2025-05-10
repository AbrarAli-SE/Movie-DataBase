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

    static class BudgetRenderer extends DefaultTableCellRenderer {
        private final DecimalFormat formatter = new DecimalFormat("$#,##0.###M");

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof Double) {
                double budget = (Double) value;
                System.out.println("Rendering budget for row " + row + ": " + budget);
                value = formatter.format(budget / 1_000_000);
            } else {
                System.out.println("Unexpected budget value for row " + row + ": " + value);
                value = "$0M";
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

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

   private boolean isGuest;
    private int userId;
    private boolean isAdmin;
    private JTextField searchField;
    private JScrollPane scrollPane;
    private DefaultTableModel movieModel;
    private JTable movieTable;

    public MovieManagerGUI(boolean isGuest, int userId, boolean isAdmin) {
        this.isGuest = isGuest;
        this.userId = userId;
        this.isAdmin = isAdmin;
        setTitle(isGuest ? "Guest Movie Search" : (isAdmin ? "Admin Movie Manager" : "Movie Manager"));
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(245, 247, 250));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(44, 62, 80));
        JLabel titleLabel = new JLabel(isGuest ? "Guest Movie Search" : (isAdmin ? "Admin Movie Manager" : "Movie Manager"), SwingConstants.CENTER);
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
    // Include "Reviews" column in guest mode
    movieModel = new DefaultTableModel(new String[] { "Movie ID", "Title", "Release Date", "Duration", "Budget", "Genre", "Directors", "Actors", "Studios", "Reviews" }, 0);
    movieTable = new JTable(movieModel);
    movieTable.setRowHeight(30);
    movieTable.getColumnModel().getColumn(0).setPreferredWidth(80); // Movie ID
    movieTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Title
    movieTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Budget
    movieTable.getColumnModel().getColumn(5).setPreferredWidth(150); // Genre
    movieTable.getColumnModel().getColumn(6).setPreferredWidth(150); // Directors
    movieTable.getColumnModel().getColumn(7).setPreferredWidth(150); // Actors
    movieTable.getColumnModel().getColumn(8).setPreferredWidth(150); // Studios
    movieTable.getColumnModel().getColumn(9).setPreferredWidth(100); // Reviews
    movieTable.getColumnModel().getColumn(4).setCellRenderer(new BudgetRenderer());
    movieTable.getColumnModel().getColumn(9).setCellRenderer(new ButtonRenderer());
    movieTable.getColumnModel().getColumn(9).setCellEditor(new ButtonEditor(new JCheckBox()));

    scrollPane = new JScrollPane(movieTable);
    mainPanel.add(scrollPane, BorderLayout.CENTER);
}


    private void setupUserUI(JPanel mainPanel) {
    // Use GridLayout to ensure all buttons are visible in a single row
    JPanel searchPanel = new JPanel(new GridLayout(1, 0, 10, 0)); // 1 row, variable columns, 10px horizontal gap
    searchPanel.setBackground(new Color(245, 247, 250));

    searchField = new JTextField(20);
    searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    CustomButton searchButton = new CustomButton("Search");
    searchButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    CustomButton addMovieButton = new CustomButton("Add Movie");
    addMovieButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    CustomButton addReviewButton = new CustomButton("Add Review");
    addReviewButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    CustomButton editMovieButton = new CustomButton("Edit Movie");
    editMovieButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    CustomButton backButton = new CustomButton("Back");
    backButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));

    searchPanel.add(new JLabel("Search Movies:"));
    searchPanel.add(searchField);
    searchPanel.add(searchButton);
    searchPanel.add(addMovieButton);
    searchPanel.add(addReviewButton);
    searchPanel.add(editMovieButton);
    searchPanel.add(backButton);

    // Add Delete Movie button only for admin
    if (isAdmin) {
        System.out.println("Adding Delete Movie button for admin mode");
        CustomButton deleteMovieButton = new CustomButton("Delete Movie");
        deleteMovieButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        deleteMovieButton.addActionListener(e -> deleteMovie());
        searchPanel.add(deleteMovieButton);
        // Debug button properties
        System.out.println("Delete Movie button - Visible: " + deleteMovieButton.isVisible() + 
                          ", Size: " + deleteMovieButton.getSize() + 
                          ", Enabled: " + deleteMovieButton.isEnabled());
    } else {
        System.out.println("Skipping Delete Movie button for non-admin mode");
    }

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
    editMovieButton.addActionListener(e -> editMovie());
    backButton.addActionListener(e -> dispose());

    // Force revalidation and repaint to ensure UI updates
    searchPanel.revalidate();
    searchPanel.repaint();
    mainPanel.add(searchPanel, BorderLayout.NORTH);

    System.out.println("Initializing DefaultTableModel for user mode");
    movieModel = new DefaultTableModel(new String[] { "Movie ID", "Title", "Release Date", "Duration", "Budget", "Genre", "Directors", "Actors", "Studios", "Reviews" }, 0);
    movieTable = new JTable(movieModel);
    movieTable.setRowHeight(30);
    movieTable.getColumnModel().getColumn(0).setPreferredWidth(80); // Movie ID
    movieTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Title
    movieTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Budget
    movieTable.getColumnModel().getColumn(5).setPreferredWidth(150); // Genre
    movieTable.getColumnModel().getColumn(6).setPreferredWidth(150); // Directors
    movieTable.getColumnModel().getColumn(7).setPreferredWidth(150); // Actors
    movieTable.getColumnModel().getColumn(8).setPreferredWidth(150); // Studios
    movieTable.getColumnModel().getColumn(4).setCellRenderer(new BudgetRenderer());
    movieTable.getColumnModel().getColumn(9).setCellRenderer(new ButtonRenderer());
    movieTable.getColumnModel().getColumn(9).setCellEditor(new ButtonEditor(new JCheckBox()));
    scrollPane = new JScrollPane(movieTable);
    mainPanel.add(scrollPane, BorderLayout.CENTER);

    // Revalidate and repaint mainPanel to ensure layout updates
    mainPanel.revalidate();
    mainPanel.repaint();
}
    private void deleteMovie() {
        if (!isAdmin) {
            JOptionPane.showMessageDialog(this, "Only admins can delete movies.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int row = movieTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a movie to delete.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int movieId = (Integer) movieTable.getValueAt(row, 0);
        String title = (String) movieTable.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete movie: " + title + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction
            try {
                // Delete related records from dependent tables
                String[] deleteQueries = {
                    "DELETE FROM Movie_Genre WHERE movieId = ?",
                    "DELETE FROM Movie_Directors WHERE movieId = ?",
                    "DELETE FROM Movie_Actor WHERE movieId = ?",
                    "DELETE FROM Movie_Studios WHERE movieId = ?",
                    "DELETE FROM Review WHERE movieId = ?"
                };
                for (String query : deleteQueries) {
                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setInt(1, movieId);
                        int rowsAffected = stmt.executeUpdate();
                        System.out.println("Deleted " + rowsAffected + " rows from " + query);
                    }
                }

                // Delete the movie
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM Movie WHERE movieId = ?")) {
                    stmt.setInt(1, movieId);
                    int rowsAffected = stmt.executeUpdate();
                    System.out.println("Deleted " + rowsAffected + " rows from Movie table");
                    if (rowsAffected == 0) {
                        throw new SQLException("No movie found with ID: " + movieId);
                    }
                }

                conn.commit();
                JOptionPane.showMessageDialog(this, "Movie deleted successfully!");
            } catch (SQLException e) {
                conn.rollback();
                System.out.println("Error deleting movie: " + e.getMessage() + ", SQL State: " + e.getSQLState() + ", Error Code: " + e.getErrorCode());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error deleting movie: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println("Error connecting to database: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error connecting to database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        refreshMovies();
    }
    
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

    private String formatBudget(double budget) {
        DecimalFormat formatter = new DecimalFormat("$#,##0.###M");
        return formatter.format(budget / 1_000_000);
    }

    private String truncateString(String text) {
        if (text == null || text.isEmpty()) return "";
        if (text.length() <= 50) return text;
        return text.substring(0, 47) + "...";
    }

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

    private List<String> getAvailableDirectors() {
        List<String> directors = new ArrayList<>();
        String sql = "SELECT name FROM Director ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                directors.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching directors: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error fetching directors: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return directors;
    }

    private List<String> getAvailableActors() {
        List<String> actors = new ArrayList<>();
        String sql = "SELECT name FROM Actor ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                actors.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching actors: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error fetching actors: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return actors;
    }

    private List<String> getAvailableStudios() {
        List<String> studios = new ArrayList<>();
        String sql = "SELECT name FROM Studio ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                studios.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching studios: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error fetching studios: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return studios;
    }

    private int addNewGenre(String genreName) {
        String sql = "INSERT INTO Genre (name) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, genreName);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Error adding genre: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error adding genre: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return -1;
    }

    private int addNewDirector(String directorName) {
        String sql = "INSERT INTO Director (name) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, directorName);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Error adding director: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error adding director: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return -1;
    }

    private int addNewActor(String actorName) {
        String sql = "INSERT INTO Actor (name) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, actorName);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Error adding actor: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error adding actor: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return -1;
    }

    private int addNewStudio(String studioName) {
        String sql = "INSERT INTO Studio (name) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, studioName);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Error adding studio: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error adding studio: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return -1;
    }


    private void editMovie() {
    int row = movieTable.getSelectedRow();
    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Please select a movie to edit.", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }
    int movieId = (Integer) movieTable.getValueAt(row, 0);

    String sql = "SELECT m.title, m.releaseDate, m.duration, m.budget, " +
                    "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', ') AS genres, " +
                    "GROUP_CONCAT(DISTINCT d.name ORDER BY d.name SEPARATOR ', ') AS directors, " +
                    "GROUP_CONCAT(DISTINCT a.name ORDER BY a.name SEPARATOR ', ') AS actors, " +
                    "GROUP_CONCAT(DISTINCT s.name ORDER BY s.name SEPARATOR ', ') AS studios " +
                    "FROM Movie m " +
                    "LEFT JOIN Movie_Genre mg ON m.movieId = mg.movieId " +
                    "LEFT JOIN Genre g ON mg.genreId = g.genreId " +
                    "LEFT JOIN Movie_Directors md ON m.movieId = md.movieId " +
                    "LEFT JOIN Director d ON md.directorId = d.directorId " +
                    "LEFT JOIN Movie_Actor ma ON m.movieId = ma.movieId " +
                    "LEFT JOIN Actor a ON ma.actorId = a.actorId " +
                    "LEFT JOIN Movie_Studios ms ON m.movieId = ms.movieId " +
                    "LEFT JOIN Studio s ON ms.studioId = s.studioId " +
                    "WHERE m.movieId = ? GROUP BY m.movieId";
    String title = "";
    String releaseDateStr = "";
    int duration = 0;
    double budget = 0;
    String[] currentGenres = new String[0];
    String[] currentDirectors = new String[0];
    String[] currentActors = new String[0];
    String[] currentStudios = new String[0];
    try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setInt(1, movieId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            title = rs.getString("title") != null ? rs.getString("title") : "";
            releaseDateStr = rs.getDate("releaseDate") != null ? rs.getDate("releaseDate").toString() : "";
            duration = rs.getInt("duration");
            budget = rs.getDouble("budget");
            currentGenres = rs.getString("genres") != null ? rs.getString("genres").split(", ") : new String[0];
            currentDirectors = rs.getString("directors") != null ? rs.getString("directors").split(", ") : new String[0];
            currentActors = rs.getString("actors") != null ? rs.getString("actors").split(", ") : new String[0];
            currentStudios = rs.getString("studios") != null ? rs.getString("studios").split(", ") : new String[0];
        }
    } catch (SQLException e) {
        System.out.println("Error fetching movie details: " + e.getMessage());
        JOptionPane.showMessageDialog(this, "Error fetching movie details: " + e.getMessage(), "Error",
                JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
        return;
    }

    JTextField titleField = new JTextField(title, 15);
    JTextField releaseDateField = new JTextField(releaseDateStr, 10);
    JTextField durationField = new JTextField(String.valueOf(duration), 5);
    JTextField budgetField = new JTextField(String.valueOf(budget), 10);

    List<String> availableGenres = getAvailableGenres();
    JList<String> genreList = new JList<>(availableGenres.toArray(new String[0]));
    genreList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    for (String genre : currentGenres) {
        int index = availableGenres.indexOf(genre);
        if (index != -1) genreList.addSelectionInterval(index, index);
    }
    JPanel genrePanel = new JPanel(new BorderLayout());
    genrePanel.add(new JLabel("Select Genres (Ctrl for multiple)"), BorderLayout.NORTH);
    JScrollPane genreScrollPane = new JScrollPane(genreList);
    genreScrollPane.setPreferredSize(new Dimension(150, 80));
    genrePanel.add(genreScrollPane, BorderLayout.CENTER);
    CustomButton addGenreButton = new CustomButton("Add New Genre");
    genrePanel.add(addGenreButton, BorderLayout.SOUTH);

    List<String> availableDirectors = getAvailableDirectors();
    JList<String> directorList = new JList<>(availableDirectors.toArray(new String[0]));
    directorList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    for (String director : currentDirectors) {
        int index = availableDirectors.indexOf(director);
        if (index != -1) directorList.addSelectionInterval(index, index);
    }
    JPanel directorPanel = new JPanel(new BorderLayout());
    directorPanel.add(new JLabel("Select Directors (Ctrl for multiple)"), BorderLayout.NORTH);
    JScrollPane directorScrollPane = new JScrollPane(directorList);
    directorScrollPane.setPreferredSize(new Dimension(150, 80));
    directorPanel.add(directorScrollPane, BorderLayout.CENTER);
    CustomButton addDirectorButton = new CustomButton("Add New Director");
    directorPanel.add(addDirectorButton, BorderLayout.SOUTH);

    List<String> availableActors = getAvailableActors();
    JList<String> actorList = new JList<>(availableActors.toArray(new String[0]));
    actorList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    for (String actor : currentActors) {
        int index = availableActors.indexOf(actor);
        if (index != -1) actorList.addSelectionInterval(index, index);
    }
    JPanel actorPanel = new JPanel(new BorderLayout());
    actorPanel.add(new JLabel("Select Actors (Ctrl for multiple)"), BorderLayout.NORTH);
    JScrollPane actorScrollPane = new JScrollPane(actorList);
    actorScrollPane.setPreferredSize(new Dimension(150, 80));
    actorPanel.add(actorScrollPane, BorderLayout.CENTER);
    CustomButton addActorButton = new CustomButton("Add New Actor");
    actorPanel.add(addActorButton, BorderLayout.SOUTH);

    List<String> availableStudios = getAvailableStudios();
    JList<String> studioList = new JList<>(availableStudios.toArray(new String[0]));
    studioList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    for (String studio : currentStudios) {
        int index = availableStudios.indexOf(studio);
        if (index != -1) studioList.addSelectionInterval(index, index);
    }
    JPanel studioPanel = new JPanel(new BorderLayout());
    studioPanel.add(new JLabel("Select Studios (Ctrl for multiple)"), BorderLayout.NORTH);
    JScrollPane studioScrollPane = new JScrollPane(studioList);
    studioScrollPane.setPreferredSize(new Dimension(150, 80));
    studioPanel.add(studioScrollPane, BorderLayout.CENTER);
    CustomButton addStudioButton = new CustomButton("Add New Studio");
    studioPanel.add(addStudioButton, BorderLayout.SOUTH);

    addGenreButton.addActionListener(e -> {
        String newGenre = JOptionPane.showInputDialog(this, "Enter new genre name:");
        if (newGenre != null && !newGenre.trim().isEmpty()) {
            int genreId = addNewGenre(newGenre.trim());
            if (genreId != -1) {
                availableGenres.add(newGenre.trim());
                genreList.setListData(availableGenres.toArray(new String[0]));
            }
        }
    });

    addDirectorButton.addActionListener(e -> {
        String newDirector = JOptionPane.showInputDialog(this, "Enter new director name:");
        if (newDirector != null && !newDirector.trim().isEmpty()) {
            int directorId = addNewDirector(newDirector.trim());
            if (directorId != -1) {
                availableDirectors.add(newDirector.trim());
                directorList.setListData(availableDirectors.toArray(new String[0]));
            }
        }
    });

    addActorButton.addActionListener(e -> {
        String newActor = JOptionPane.showInputDialog(this, "Enter new actor name:");
        if (newActor != null && !newActor.trim().isEmpty()) {
            int actorId = addNewActor(newActor.trim());
            if (actorId != -1) {
                availableActors.add(newActor.trim());
                actorList.setListData(availableActors.toArray(new String[0]));
            }
        }
    });

    addStudioButton.addActionListener(e -> {
        String newStudio = JOptionPane.showInputDialog(this, "Enter new studio name:");
        if (newStudio != null && !newStudio.trim().isEmpty()) {
            int studioId = addNewStudio(newStudio.trim());
            if (studioId != -1) {
                availableStudios.add(newStudio.trim());
                studioList.setListData(availableStudios.toArray(new String[0]));
            }
        }
    });

    // Header panel for input fields (2x2 grid)
    JPanel headerPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbcHeader = new GridBagConstraints();
    gbcHeader.insets = new Insets(5, 5, 5, 5);
    gbcHeader.anchor = GridBagConstraints.WEST;

    gbcHeader.gridx = 0;
    gbcHeader.gridy = 0;
    headerPanel.add(new JLabel("Title:"), gbcHeader);
    gbcHeader.gridx = 1;
    headerPanel.add(titleField, gbcHeader);

    gbcHeader.gridx = 2;
    gbcHeader.gridy = 0;
    headerPanel.add(new JLabel("Release Date (YYYY-MM-DD):"), gbcHeader);
    gbcHeader.gridx = 3;
    headerPanel.add(releaseDateField, gbcHeader);

    gbcHeader.gridx = 0;
    gbcHeader.gridy = 1;
    headerPanel.add(new JLabel("Duration (minutes):"), gbcHeader);
    gbcHeader.gridx = 1;
    headerPanel.add(durationField, gbcHeader);

    gbcHeader.gridx = 2;
    gbcHeader.gridy = 1;
    headerPanel.add(new JLabel("Budget (dollars):"), gbcHeader);
    gbcHeader.gridx = 3;
    headerPanel.add(budgetField, gbcHeader);

    // Grid panel for dropdowns (2x2 grid)
    JPanel gridPanel = new JPanel(new GridLayout(2, 2, 10, 10));
    gridPanel.add(genrePanel);
    gridPanel.add(directorPanel);
    gridPanel.add(actorPanel);
    gridPanel.add(studioPanel);

    // Main panel combining header and grid
    JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    mainPanel.add(headerPanel, BorderLayout.NORTH);
    mainPanel.add(gridPanel, BorderLayout.CENTER);

    // Wrap the main panel in a scroll pane and set a preferred size for the dialog
    mainPanel.setPreferredSize(new Dimension(600, 400));
    JScrollPane scrollPane = new JScrollPane(mainPanel);
    scrollPane.setPreferredSize(new Dimension(650, 450));

    int result = JOptionPane.showConfirmDialog(this, scrollPane, "Edit Movie", JOptionPane.OK_CANCEL_OPTION);
    if (result != JOptionPane.OK_OPTION) {
        return;
    }

    String newTitle = titleField.getText().trim();
    if (newTitle.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Error: Title is required.", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }
    Date releaseDate = null;
    if (!releaseDateField.getText().trim().isEmpty()) {
        try {
            releaseDate = Date.valueOf(releaseDateField.getText().trim());
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, "Error: Invalid date format. Use YYYY-MM-DD.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
    }
    int newDuration;
    try {
        newDuration = Integer.parseInt(durationField.getText().trim());
        if (newDuration <= 0) {
            JOptionPane.showMessageDialog(this, "Error: Duration must be positive.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, "Error: Invalid duration format.", "Error",
                JOptionPane.ERROR_MESSAGE);
        return;
    }
    double newBudget;
    try {
        newBudget = Double.parseDouble(budgetField.getText().trim());
        if (newBudget < 0) {
            JOptionPane.showMessageDialog(this, "Error: Budget must be non-negative.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, "Error: Invalid budget format.", "Error",
                JOptionPane.ERROR_MESSAGE);
        return;
    }

    List<String> selectedGenres = genreList.getSelectedValuesList();
    List<String> selectedDirectors = directorList.getSelectedValuesList();
    List<String> selectedActors = actorList.getSelectedValuesList();
    List<String> selectedStudios = studioList.getSelectedValuesList();

    try (Connection conn = DatabaseConnection.getConnection()) {
        conn.setAutoCommit(false);
        try {
            String sqlUpdate = "UPDATE Movie SET title = ?, releaseDate = ?, duration = ?, budget = ? WHERE movieId = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                stmt.setString(1, newTitle);
                stmt.setDate(2, releaseDate);
                stmt.setInt(3, newDuration);
                stmt.setDouble(4, newBudget);
                stmt.setInt(5, movieId);
                stmt.executeUpdate();
            }

            conn.createStatement().executeUpdate("DELETE FROM Movie_Genre WHERE movieId = " + movieId);
            conn.createStatement().executeUpdate("DELETE FROM Movie_Directors WHERE movieId = " + movieId);
            conn.createStatement().executeUpdate("DELETE FROM Movie_Actor WHERE movieId = " + movieId);
            conn.createStatement().executeUpdate("DELETE FROM Movie_Studios WHERE movieId = " + movieId);

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

            if (!selectedDirectors.isEmpty()) {
                String sqlDirector = "INSERT INTO Movie_Directors (movieId, directorId) SELECT ?, directorId FROM Director WHERE name = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sqlDirector)) {
                    for (String director : selectedDirectors) {
                        stmt.setInt(1, movieId);
                        stmt.setString(2, director);
                        stmt.executeUpdate();
                    }
                }
            }

            if (!selectedActors.isEmpty()) {
                String sqlActor = "INSERT INTO Movie_Actor (movieId, actorId) SELECT ?, actorId FROM Actor WHERE name = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sqlActor)) {
                    for (String actor : selectedActors) {
                        stmt.setInt(1, movieId);
                        stmt.setString(2, actor);
                        stmt.executeUpdate();
                    }
                }
            }

            if (!selectedStudios.isEmpty()) {
                String sqlStudio = "INSERT INTO Movie_Studios (movieId, studioId) SELECT ?, studioId FROM Studio WHERE name = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sqlStudio)) {
                    for (String studio : selectedStudios) {
                        stmt.setInt(1, movieId);
                        stmt.setString(2, studio);
                        stmt.executeUpdate();
                    }
                }
            }

            conn.commit();
            JOptionPane.showMessageDialog(this, "Movie updated successfully!");
        } catch (SQLException e) {
            conn.rollback();
            System.out.println("Error updating movie: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error updating movie: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            conn.setAutoCommit(true);
        }
    } catch (SQLException e) {
        System.out.println("Error connecting to database: " + e.getMessage());
        JOptionPane.showMessageDialog(this, "Error connecting to database: " + e.getMessage(), "Error",
                JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }

    refreshMovies();
}


    private void refreshMovies() {
        System.out.println("Refreshing movies for " + (isGuest ? "guest" : "user") + " mode");
        movieModel.setRowCount(0);
        String sql = "SELECT m.movieId, m.title, m.releaseDate, m.duration, m.budget, " +
                     "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', ') AS genres, " +
                     "GROUP_CONCAT(DISTINCT d.name ORDER BY d.name SEPARATOR ', ') AS directors, " +
                     "GROUP_CONCAT(DISTINCT a.name ORDER BY a.name SEPARATOR ', ') AS actors, " +
                     "GROUP_CONCAT(DISTINCT s.name ORDER BY s.name SEPARATOR ', ') AS studios " +
                     "FROM Movie m " +
                     "LEFT JOIN Movie_Genre mg ON m.movieId = mg.movieId " +
                     "LEFT JOIN Genre g ON mg.genreId = g.genreId " +
                     "LEFT JOIN Movie_Directors md ON m.movieId = md.movieId " +
                     "LEFT JOIN Director d ON md.directorId = d.directorId " +
                     "LEFT JOIN Movie_Actor ma ON m.movieId = ma.movieId " +
                     "LEFT JOIN Actor a ON ma.actorId = a.actorId " +
                     "LEFT JOIN Movie_Studios ms ON m.movieId = ms.movieId " +
                     "LEFT JOIN Studio s ON ms.studioId = s.studioId " +
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
                        truncateString(rs.getString("genres")),
                        truncateString(rs.getString("directors")),
                        truncateString(rs.getString("actors")),
                        truncateString(rs.getString("studios"))
                } : new Object[] {
                        rs.getInt("movieId"),
                        rs.getString("title") != null ? rs.getString("title") : "",
                        rs.getDate("releaseDate") != null ? rs.getDate("releaseDate") : "",
                        formatDuration(rs.getInt("duration")),
                        rs.getDouble("budget"),
                        truncateString(rs.getString("genres")),
                        truncateString(rs.getString("directors")),
                        truncateString(rs.getString("actors")),
                        truncateString(rs.getString("studios")),
                        "View Reviews"
                };
                movieModel.addRow(row);
            }
            if (!hasMovies) {
                System.out.println("No movies found in database");
                movieModel.addRow(new Object[] { "", "No movies found", "", "", "", "", "", "", "" });
            }
            movieTable.revalidate();
            movieTable.repaint();
        } catch (SQLException e) {
            System.out.println("Error fetching movies: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error fetching movies: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            movieModel.addRow(new Object[] { "", "Error loading movies", "", "", "", "", "", "", "" });
        }
    }

    private void searchMovies() {
        String searchTerm = searchField.getText().trim();
        System.out.println("Searching movies with term: " + searchTerm);
        movieModel.setRowCount(0);
        String sql = "SELECT m.movieId, m.title, m.releaseDate, m.duration, m.budget, " +
                     "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', ') AS genres, " +
                     "GROUP_CONCAT(DISTINCT d.name ORDER BY d.name SEPARATOR ', ') AS directors, " +
                     "GROUP_CONCAT(DISTINCT a.name ORDER BY a.name SEPARATOR ', ') AS actors, " +
                     "GROUP_CONCAT(DISTINCT s.name ORDER BY s.name SEPARATOR ', ') AS studios " +
                     "FROM Movie m " +
                     "LEFT JOIN Movie_Genre mg ON m.movieId = mg.movieId " +
                     "LEFT JOIN Genre g ON mg.genreId = g.genreId " +
                     "LEFT JOIN Movie_Directors md ON m.movieId = md.movieId " +
                     "LEFT JOIN Director d ON md.directorId = d.directorId " +
                     "LEFT JOIN Movie_Actor ma ON m.movieId = ma.movieId " +
                     "LEFT JOIN Actor a ON ma.actorId = a.actorId " +
                     "LEFT JOIN Movie_Studios ms ON m.movieId = ms.movieId " +
                     "LEFT JOIN Studio s ON ms.studioId = s.studioId " +
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
                        truncateString(rs.getString("genres")),
                        truncateString(rs.getString("directors")),
                        truncateString(rs.getString("actors")),
                        truncateString(rs.getString("studios"))
                } : new Object[] {
                        rs.getInt("movieId"),
                        rs.getString("title") != null ? rs.getString("title") : "",
                        rs.getDate("releaseDate") != null ? rs.getDate("releaseDate") : "",
                        formatDuration(rs.getInt("duration")),
                        rs.getDouble("budget"),
                        truncateString(rs.getString("genres")),
                        truncateString(rs.getString("directors")),
                        truncateString(rs.getString("actors")),
                        truncateString(rs.getString("studios")),
                        "View Reviews"
                };
                movieModel.addRow(row);
            }
            if (!hasMovies) {
                System.out.println("No movies found for search term: " + searchTerm);
                movieModel.addRow(new Object[] { "", "No movies found", "", "", "", "", "", "", "" });
            }
            movieTable.revalidate();
            movieTable.repaint();
        } catch (SQLException e) {
            System.out.println("Error searching movies: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error searching movies: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            movieModel.addRow(new Object[] { "", "Error searching movies", "", "", "", "", "", "", "" });
        }
    }

    private void showReviews(int movieId) {
        System.out.println("Showing reviews for movieId: " + movieId);
        JDialog reviewDialog = new JDialog(this, "Movie Details and Reviews for Movie ID: " + movieId, true);
        reviewDialog.setSize(600, 500);
        reviewDialog.setLocationRelativeTo(this);
        reviewDialog.setLayout(new BorderLayout(10, 10));

        JPanel detailsPanel = new JPanel(new GridBagLayout());
        detailsPanel.setBackground(new Color(245, 247, 250));
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Movie Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        String sqlMovie = "SELECT m.title, m.releaseDate, m.duration, m.budget, " +
                         "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', ') AS genres, " +
                         "GROUP_CONCAT(DISTINCT d.name ORDER BY d.name SEPARATOR ', ') AS directors, " +
                         "GROUP_CONCAT(DISTINCT a.name ORDER BY d.name SEPARATOR ', ') AS actors, " +
                         "GROUP_CONCAT(DISTINCT s.name ORDER BY d.name SEPARATOR ', ') AS studios " +
                         "FROM Movie m " +
                         "LEFT JOIN Movie_Genre mg ON m.movieId = mg.movieId " +
                         "LEFT JOIN Genre g ON mg.genreId = g.genreId " +
                         "LEFT JOIN Movie_Directors md ON m.movieId = md.movieId " +
                         "LEFT JOIN Director d ON md.directorId = d.directorId " +
                         "LEFT JOIN Movie_Actor ma ON m.movieId = ma.movieId " +
                         "LEFT JOIN Actor a ON ma.actorId = a.actorId " +
                         "LEFT JOIN Movie_Studios ms ON m.movieId = ms.movieId " +
                         "LEFT JOIN Studio s ON ms.studioId = s.studioId " +
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
                gbc.gridy++;
                detailsPanel.add(new JLabel("<html><b>Directors:</b> " + (rs.getString("directors") != null ? rs.getString("directors") : "N/A") + "</html>"), gbc);
                gbc.gridy++;
                detailsPanel.add(new JLabel("<html><b>Actors:</b> " + (rs.getString("actors") != null ? rs.getString("actors") : "N/A") + "</html>"), gbc);
                gbc.gridy++;
                detailsPanel.add(new JLabel("<html><b>Studios:</b> " + (rs.getString("studios") != null ? rs.getString("studios") : "N/A") + "</html>"), gbc);
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


    private void showGuestReviews(int movieId) {
        JDialog reviewDialog = new JDialog(this, "Reviews for Movie ID: " + movieId, true);
        reviewDialog.setSize(600, 400);
        reviewDialog.setLocationRelativeTo(this);
        reviewDialog.setLayout(new BorderLayout(10, 10));

        JPanel detailsPanel = new JPanel(new GridLayout(8, 1));
        detailsPanel.setBackground(new Color(245, 247, 250));
        String sqlMovie = "SELECT m.title, m.releaseDate, m.duration, m.budget, " +
                         "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', ') AS genres, " +
                         "GROUP_CONCAT(DISTINCT d.name ORDER BY d.name SEPARATOR ', ') AS directors, " +
                         "GROUP_CONCAT(DISTINCT a.name ORDER BY a.name SEPARATOR ', ') AS actors, " +
                         "GROUP_CONCAT(DISTINCT s.name ORDER BY s.name SEPARATOR ', ') AS studios " +
                         "FROM Movie m " +
                         "LEFT JOIN Movie_Genre mg ON m.movieId = mg.movieId " +
                         "LEFT JOIN Genre g ON mg.genreId = g.genreId " +
                         "LEFT JOIN Movie_Directors md ON m.movieId = md.movieId " +
                         "LEFT JOIN Director d ON md.directorId = d.directorId " +
                         "LEFT JOIN Movie_Actor ma ON m.movieId = ma.movieId " +
                         "LEFT JOIN Actor a ON ma.actorId = a.actorId " +
                         "LEFT JOIN Movie_Studios ms ON m.movieId = ms.movieId " +
                         "LEFT JOIN Studio s ON ms.studioId = s.studioId " +
                         "WHERE m.movieId = ? GROUP BY m.movieId";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlMovie)) {
            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                detailsPanel.add(new JLabel("Title: " + (rs.getString("title") != null ? rs.getString("title") : "N/A")));
                detailsPanel.add(new JLabel("Release Date: " + (rs.getDate("releaseDate") != null ? rs.getDate("releaseDate") : "N/A")));
                detailsPanel.add(new JLabel("Duration: " + formatDuration(rs.getInt("duration"))));
                detailsPanel.add(new JLabel("Budget: " + formatBudget(rs.getDouble("budget"))));
                detailsPanel.add(new JLabel("Genres: " + (rs.getString("genres") != null ? rs.getString("genres") : "N/A")));
                detailsPanel.add(new JLabel("Directors: " + (rs.getString("directors") != null ? rs.getString("directors") : "N/A")));
                detailsPanel.add(new JLabel("Actors: " + (rs.getString("actors") != null ? rs.getString("actors") : "N/A")));
                detailsPanel.add(new JLabel("Studios: " + (rs.getString("studios") != null ? rs.getString("studios") : "N/A")));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching movie details: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error fetching movie details: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        DefaultTableModel reviewModel = new DefaultTableModel(new String[] { "Email", "Rating", "Comment", "Review Date" }, 0);
        JTable reviewTable = new JTable(reviewModel);
        String sqlReviews = "SELECT r.rating, r.comment, r.reviewDate, u.email FROM Review r LEFT JOIN User u ON r.userId = u.userId WHERE r.movieId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlReviews)) {
            stmt.setInt(1, movieId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reviewModel.addRow(new Object[] {
                    rs.getString("email") != null ? rs.getString("email") : "Unknown",
                    rs.getDouble("rating"),
                    rs.getString("comment") != null ? rs.getString("comment") : "No comment",
                    rs.getDate("reviewDate") != null ? rs.getDate("reviewDate") : "N/A"
                });
            }
        } catch (SQLException e) {
            System.out.println("Error fetching reviews: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error fetching reviews: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        reviewDialog.add(detailsPanel, BorderLayout.NORTH);
        reviewDialog.add(new JScrollPane(reviewTable), BorderLayout.CENTER);
        CustomButton closeButton = new CustomButton("Close");
        closeButton.addActionListener(e -> reviewDialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(245, 247, 250));
        buttonPanel.add(closeButton);
        reviewDialog.add(buttonPanel, BorderLayout.SOUTH);
        reviewDialog.setVisible(true);
    }

    private void addMovie() {
        JTextField titleField = new JTextField(15);
        JTextField releaseDateField = new JTextField(10);
        JTextField durationField = new JTextField(5);
        JTextField budgetField = new JTextField(10);

        List<String> availableGenres = getAvailableGenres();
        JPanel genrePanel = new JPanel(new BorderLayout());
        JList<String> genreList;
        if (availableGenres.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "No genres available. Proceed without genres?",
                    "No Genres Available",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            genrePanel.add(new JLabel("No genres available"), BorderLayout.CENTER);
            genreList = new JList<>();
        } else {
            genreList = new JList<>(availableGenres.toArray(new String[0]));
            genreList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            genreList.setVisibleRowCount(3);
            JScrollPane genreScrollPane = new JScrollPane(genreList);
            genreScrollPane.setPreferredSize(new Dimension(150, 80));
            genrePanel.add(new JLabel("Select Genres (Ctrl for multiple)"), BorderLayout.NORTH);
            genrePanel.add(genreScrollPane, BorderLayout.CENTER);
        }
        CustomButton addGenreButton = new CustomButton("Add New Genre");
        genrePanel.add(addGenreButton, BorderLayout.SOUTH);

        List<String> availableDirectors = getAvailableDirectors();
        JPanel directorPanel = new JPanel(new BorderLayout());
        JList<String> directorList;
        if (availableDirectors.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "No directors available. Proceed without directors?",
                    "No Directors Available",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            directorPanel.add(new JLabel("No directors available"), BorderLayout.CENTER);
            directorList = new JList<>();
        } else {
            directorList = new JList<>(availableDirectors.toArray(new String[0]));
            directorList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            directorList.setVisibleRowCount(3);
            JScrollPane directorScrollPane = new JScrollPane(directorList);
            directorScrollPane.setPreferredSize(new Dimension(150, 80));
            directorPanel.add(new JLabel("Select Directors (Ctrl for multiple)"), BorderLayout.NORTH);
            directorPanel.add(directorScrollPane, BorderLayout.CENTER);
        }
        CustomButton addDirectorButton = new CustomButton("Add New Director");
        directorPanel.add(addDirectorButton, BorderLayout.SOUTH);

        List<String> availableActors = getAvailableActors();
        JPanel actorPanel = new JPanel(new BorderLayout());
        JList<String> actorList;
        if (availableActors.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "No actors available. Proceed without actors?",
                    "No Actors Available",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            actorPanel.add(new JLabel("No actors available"), BorderLayout.CENTER);
            actorList = new JList<>();
        } else {
            actorList = new JList<>(availableActors.toArray(new String[0]));
            actorList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            actorList.setVisibleRowCount(3);
            JScrollPane actorScrollPane = new JScrollPane(actorList);
            actorScrollPane.setPreferredSize(new Dimension(150, 80));
            actorPanel.add(new JLabel("Select Actors (Ctrl for multiple)"), BorderLayout.NORTH);
            actorPanel.add(actorScrollPane, BorderLayout.CENTER);
        }
        CustomButton addActorButton = new CustomButton("Add New Actor");
        actorPanel.add(addActorButton, BorderLayout.SOUTH);

        List<String> availableStudios = getAvailableStudios();
        JPanel studioPanel = new JPanel(new BorderLayout());
        JList<String> studioList;
        if (availableStudios.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "No studios available. Proceed without studios?",
                    "No Studios Available",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            studioPanel.add(new JLabel("No studios available"), BorderLayout.CENTER);
            studioList = new JList<>();
        } else {
            studioList = new JList<>(availableStudios.toArray(new String[0]));
            studioList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            studioList.setVisibleRowCount(3);
            JScrollPane studioScrollPane = new JScrollPane(studioList);
            studioScrollPane.setPreferredSize(new Dimension(150, 80));
            studioPanel.add(new JLabel("Select Studios (Ctrl for multiple)"), BorderLayout.NORTH);
            studioPanel.add(studioScrollPane, BorderLayout.CENTER);
        }
        CustomButton addStudioButton = new CustomButton("Add New Studio");
        studioPanel.add(addStudioButton, BorderLayout.SOUTH);

        addGenreButton.addActionListener(e -> {
            String newGenre = JOptionPane.showInputDialog(this, "Enter new genre name:");
            if (newGenre != null && !newGenre.trim().isEmpty()) {
                int genreId = addNewGenre(newGenre.trim());
                if (genreId != -1) {
                    availableGenres.add(newGenre.trim());
                    genreList.setListData(availableGenres.toArray(new String[0]));
                }
            }
        });

        addDirectorButton.addActionListener(e -> {
            String newDirector = JOptionPane.showInputDialog(this, "Enter new director name:");
            if (newDirector != null && !newDirector.trim().isEmpty()) {
                int directorId = addNewDirector(newDirector.trim());
                if (directorId != -1) {
                    availableDirectors.add(newDirector.trim());
                    directorList.setListData(availableDirectors.toArray(new String[0]));
                }
            }
        });

        addActorButton.addActionListener(e -> {
            String newActor = JOptionPane.showInputDialog(this, "Enter new actor name:");
            if (newActor != null && !newActor.trim().isEmpty()) {
                int actorId = addNewActor(newActor.trim());
                if (actorId != -1) {
                    availableActors.add(newActor.trim());
                    actorList.setListData(availableActors.toArray(new String[0]));
                }
            }
        });

        addStudioButton.addActionListener(e -> {
            String newStudio = JOptionPane.showInputDialog(this, "Enter new studio name:");
            if (newStudio != null && !newStudio.trim().isEmpty()) {
                int studioId = addNewStudio(newStudio.trim());
                if (studioId != -1) {
                    availableStudios.add(newStudio.trim());
                    studioList.setListData(availableStudios.toArray(new String[0]));
                }
            }
        });

        // Header panel for input fields (2x2 grid)
        JPanel headerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcHeader = new GridBagConstraints();
        gbcHeader.insets = new Insets(5, 5, 5, 5);
        gbcHeader.anchor = GridBagConstraints.WEST;

        gbcHeader.gridx = 0;
        gbcHeader.gridy = 0;
        headerPanel.add(new JLabel("Title:"), gbcHeader);
        gbcHeader.gridx = 1;
        headerPanel.add(titleField, gbcHeader);

        gbcHeader.gridx = 2;
        gbcHeader.gridy = 0;
        headerPanel.add(new JLabel("Release Date (YYYY-MM-DD):"), gbcHeader);
        gbcHeader.gridx = 3;
        headerPanel.add(releaseDateField, gbcHeader);

        gbcHeader.gridx = 0;
        gbcHeader.gridy = 1;
        headerPanel.add(new JLabel("Duration (minutes):"), gbcHeader);
        gbcHeader.gridx = 1;
        headerPanel.add(durationField, gbcHeader);

        gbcHeader.gridx = 2;
        gbcHeader.gridy = 1;
        headerPanel.add(new JLabel("Budget (dollars):"), gbcHeader);
        gbcHeader.gridx = 3;
        headerPanel.add(budgetField, gbcHeader);

        // Grid panel for dropdowns (2x2 grid)
        JPanel gridPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        gridPanel.add(genrePanel);
        gridPanel.add(directorPanel);
        gridPanel.add(actorPanel);
        gridPanel.add(studioPanel);

        // Main panel combining header and grid
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(gridPanel, BorderLayout.CENTER);

        // Wrap the main panel in a scroll pane and set a preferred size for the dialog
        mainPanel.setPreferredSize(new Dimension(600, 400));
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setPreferredSize(new Dimension(650, 450));

        int result = JOptionPane.showConfirmDialog(this, scrollPane, "Add Movie", JOptionPane.OK_CANCEL_OPTION);
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

        List<String> selectedGenres = availableGenres.isEmpty() ? new ArrayList<>() : genreList.getSelectedValuesList();
        List<String> selectedDirectors = availableDirectors.isEmpty() ? new ArrayList<>() : directorList.getSelectedValuesList();
        List<String> selectedActors = availableActors.isEmpty() ? new ArrayList<>() : actorList.getSelectedValuesList();
        List<String> selectedStudios = availableStudios.isEmpty() ? new ArrayList<>() : studioList.getSelectedValuesList();

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
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

                if (!selectedDirectors.isEmpty()) {
                    String sqlDirector = "INSERT INTO Movie_Directors (movieId, directorId) SELECT ?, directorId FROM Director WHERE name = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlDirector)) {
                        for (String director : selectedDirectors) {
                            stmt.setInt(1, movieId);
                            stmt.setString(2, director);
                            stmt.executeUpdate();
                        }
                    }
                }

                if (!selectedActors.isEmpty()) {
                    String sqlActor = "INSERT INTO Movie_Actor (movieId, actorId) SELECT ?, actorId FROM Actor WHERE name = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlActor)) {
                        for (String actor : selectedActors) {
                            stmt.setInt(1, movieId);
                            stmt.setString(2, actor);
                            stmt.executeUpdate();
                        }
                    }
                }

                if (!selectedStudios.isEmpty()) {
                    String sqlStudio = "INSERT INTO Movie_Studios (movieId, studioId) SELECT ?, studioId FROM Studio WHERE name = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlStudio)) {
                        for (String studio : selectedStudios) {
                            stmt.setInt(1, movieId);
                            stmt.setString(2, studio);
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
        SwingUtilities.invokeLater(() -> new MovieManagerGUI(false, 1, false).setVisible(true));
    }
}