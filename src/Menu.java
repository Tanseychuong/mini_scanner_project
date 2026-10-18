import java.util.Scanner;

// Input helpers that don't crash on bad input
public class Menu {

    // Reads a whole line and parses it as a number; returns -1 for anything invalid
    public static int readInt(Scanner input) {
        try {
            return Integer.parseInt(input.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Prints the prompt and returns the trimmed line, or the default if the line is empty
    public static String prompt(Scanner input, String message, String defaultValue) {
        System.out.print(message);
        String line = input.nextLine().trim();
        return line.isEmpty() ? defaultValue : line;
    }

    public static boolean confirm(Scanner input, String message) {
        String answer = prompt(input, message + " [y/N]: ", "n");
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes");
    }

    // Asks whether to save a report; returns "json", "csv" or null
    public static String askReportFormat(Scanner input) {
        String answer = prompt(input, "Save report? (json/csv/N): ", "n").toLowerCase();
        return answer.equals("json") || answer.equals("csv") ? answer : null;
    }
}
