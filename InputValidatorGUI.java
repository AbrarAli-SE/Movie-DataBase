import javax.swing.*;

public class InputValidatorGUI {
    public static int getValidInt() {
        while (true) {
            String input = JOptionPane.showInputDialog(null, "Please enter a valid number:", "Input Required",
                    JOptionPane.QUESTION_MESSAGE);
            if (input != null && input.matches("\\d+")) {
                return Integer.parseInt(input);
            }
            JOptionPane.showMessageDialog(null, "Invalid input! Please enter a valid number.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static double getValidDouble() {
        while (true) {
            String input = JOptionPane.showInputDialog(null, "Please enter a valid decimal number:", "Input Required",
                    JOptionPane.QUESTION_MESSAGE);
            if (input != null && input.matches("\\d+(\\.\\d+)?")) {
                return Double.parseDouble(input);
            }
            JOptionPane.showMessageDialog(null, "Invalid input! Please enter a valid decimal number.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static String maskPassword(String password) {
        if (password == null || password.length() < 4) {
            return "****";
        }
        return password.substring(0, 2) + "*".repeat(password.length() - 4) + password.substring(password.length() - 2);
    }

    public static void main(String[] args) {
        int number = getValidInt();
        JOptionPane.showMessageDialog(null, "You entered: " + number, "Confirmation", JOptionPane.INFORMATION_MESSAGE);

        double decimal = getValidDouble();
        JOptionPane.showMessageDialog(null, "You entered: " + decimal, "Confirmation", JOptionPane.INFORMATION_MESSAGE);

        String password = JOptionPane.showInputDialog(null, "Enter your password:", "Password Input",
                JOptionPane.QUESTION_MESSAGE);
        JOptionPane.showMessageDialog(null, "Masked Password: " + maskPassword(password), "Confirmation",
                JOptionPane.INFORMATION_MESSAGE);
    }
}   