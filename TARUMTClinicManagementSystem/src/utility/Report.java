/*
 * Utility class for generating formatted reports
 */
package utility;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Report {  
    public static final int WIDTH = 120; // Increased width for longer header/footer
    
    // ==================== HEADER ====================
    public static void printHeader(String reportTitle) {
        String university = "TUNKU ABDUL RAHMAN UNIVERSITY OF MANAGEMENT AND TECHNOLOGY";
        String subsystem = "CLINIC MANAGEMENT SYSTEM";
        String title = reportTitle.toUpperCase();

        // Generate timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd, hh:mm a");
        String timestamp = LocalDateTime.now().format(formatter);

        // Print header with extra padding
        System.out.println("=".repeat(WIDTH));
        System.out.println(centerText("", WIDTH)); // extra top padding
        System.out.println(centerText(university, WIDTH));
        System.out.println(centerText(subsystem, WIDTH));
        System.out.println(centerText("", WIDTH)); // extra space before title
        System.out.println(centerText(title, WIDTH));
        System.out.println(centerText("", WIDTH)); // extra space after title
        System.out.println(centerText("Generated at: " + timestamp, WIDTH));
        System.out.println("*".repeat(WIDTH));
        System.out.println();
    }

    public static void printFooter() {
        System.out.println();
        System.out.println("*".repeat(WIDTH));
        System.out.println(centerText("", WIDTH)); // extra padding before footer text
        System.out.println(centerText("END OF THE REPORT", WIDTH));
        System.out.println(centerText("", WIDTH)); // extra padding after footer text
        System.out.println("=".repeat(WIDTH));
        System.out.println();
    }

    // Helper method to center text
    public static String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }

}