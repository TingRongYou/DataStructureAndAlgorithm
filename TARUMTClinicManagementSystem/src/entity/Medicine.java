package entity;

public class Medicine {
    private static int counter = 1000; // Auto-generated ID starts at M1000

    private final String id;
    private final String name;
    private int quantity;
    private final String unit;
    private final String usage;

    // Constructor for adding a new medicine (auto ID generation)
    public Medicine(String name, int quantity, String unit, String usage) {
        this.id = "M" + counter++;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.usage = usage;
    }

    // Constructor for loading medicine from file (manual ID)
    public Medicine(String id, String name, int quantity, String unit, String usage) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.usage = usage;

        // Ensure the counter is always ahead to prevent duplicate IDs
        try {
            int numericPart = Integer.parseInt(id.substring(1)); // Remove 'M' prefix
            if (numericPart >= counter) {
                counter = numericPart + 1;
            }
        } catch (NumberFormatException e) {
            // Invalid ID format: skip counter adjustment
        }
    }

    // === Static Utilities ===
    public static void resetCounter(int newStart) {
        counter = newStart;
    }

    // === Getters ===
    public String getId() { return id; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public String getUsage() { return usage; }

    // === Setters ===
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    // === Display Formatting ===
    @Override
    public String toString() {
        return String.format("| %-5s | %-20s | %-8d | %-8s | %-30s |", id, name, quantity, unit, usage);
    }
}
