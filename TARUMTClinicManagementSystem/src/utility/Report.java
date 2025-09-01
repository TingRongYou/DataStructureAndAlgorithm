/*
 * Utility class for generating formatted reports
 */
package utility;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Report {  
    public static final int WIDTH = 100; // Increased width for longer header/footer
    
    // ==================== HEADER ====================
    public static void printHeader(String reportTitle) {
        String university = "TUNKU ABDUL RAHMAN UNIVERSITY OF MANAGEMENT AND TECHNOLOGY";
        String subsystem = "CLINIC MANAGEMENT SYSTEM";
        String title = reportTitle.toUpperCase();

        // Generate timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd, hh:mm a");
        String timestamp = LocalDateTime.now().format(formatter);

        // Print header with extra padding
        System.out.println();
        System.out.println("=".repeat(WIDTH));
        System.out.println(centerText("", WIDTH)); // extra top padding
        System.out.println(centerText(university, WIDTH));
        System.out.println(centerText(subsystem, WIDTH));
        System.out.println(centerText("", WIDTH)); // extra space before title
        System.out.println(centerText(title, WIDTH));
        System.out.println(centerText("", WIDTH)); // extra space after title
        System.out.println(centerText("Generated at: " + timestamp, WIDTH));
        System.out.println();
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
    }

    // Helper method to center text
    public static String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }
    
    // Render a single labeled dot row
    public void printDotRow(String label, int value, int max, int width) {
        System.out.printf("%-10s: ", label);
        printDots(value, max, width);
            System.out.println(" " + value);
        }

    // Render the dots scaled to width
    public void printDots(int value, int max, int width) {
        // scale value → [0..width], but show at least 1 dot when value>0
        int dots = (int)Math.round((value * 1.0 / max) * width);
        if (value > 0 && dots == 0) dots = 1;
        System.out.print(".".repeat(Math.max(0, dots)));
    }

    // Safe cutter used elsewhere
    public static String cut(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, Math.max(0, n-1)) + "…";
    }
    
    // === Centering utils for reports ===
    public  static String center(String s) {
        if (s == null) s = "";
        int pad = Math.max(0, (WIDTH - s.length()) / 2);
        String out = " ".repeat(pad) + s;
        if (out.length() < WIDTH) out += " ".repeat(WIDTH - out.length());
        return out;
    }
    public static void cprintln(String s) { System.out.println(center(s)); }
    public static void cprintf(String fmt, Object... args) { System.out.println(center(String.format(fmt, args))); }


}