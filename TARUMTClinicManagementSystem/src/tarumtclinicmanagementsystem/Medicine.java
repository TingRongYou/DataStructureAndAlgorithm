package tarumtclinicmanagementsystem;

public class Medicine {
    private static int counter = 1000; // Starts at M1000
    private final String id;
    private final String name;
    private int quantity;
    private final String unit;
    private final String usage;

    // Constructor for new medicine (auto ID)
    public Medicine(String name, int quantity, String unit, String usage) {
        this.id = "M" + counter++;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.usage = usage;
    }

    // Constructor for loading from file (manual ID)
    public Medicine(String id, String name, int quantity, String unit, String usage) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.usage = usage;

        // Ensure counter doesn't generate duplicate IDs
        try {
            int numeric = Integer.parseInt(id.substring(1)); // Extract numeric part after 'M'
            if (numeric >= counter) {
                counter = numeric + 1;
            }
        } catch (NumberFormatException e) {
            // Ignore invalid ID formats
        }
    }

    // Static method to reset ID counter (useful for testing)
    public static void resetCounter(int newStart) {
        counter = newStart;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public String getUsage() { return usage; }

    // Setters
    public void setQuantity(int quantity) { this.quantity = quantity; }

    // Formatted display for table rows
    @Override
    public String toString() {
        return String.format("| %-5s | %-20s | %-8d | %-8s | %-30s |", id, name, quantity, unit, usage);
    }
}
