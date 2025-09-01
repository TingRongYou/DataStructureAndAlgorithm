package entity;

public class Medicine {
    private static int counter = 001; // Auto-generated ID 

    private final String id;
    private final String name;
    private int quantity;
    private final String unit;
    private final String usage;
    private final String expiration;
    private double pricePerUnit;        
    private String intakeMethod;        
    private String intakeMeasurePerDay; 

    // Constructor for adding a new medicine (auto ID generation)
    public Medicine(String name, int quantity, String unit, String usage, String expiration,
                    double pricePerUnit, String intakeMethod, String intakeMeasurePerDay) {
        this.id = "M" + counter++;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.usage = usage;
        this.expiration = expiration;
        this.pricePerUnit = Math.max(0.0, pricePerUnit);
        this.intakeMethod = (intakeMethod == null || intakeMethod.isBlank())
                ? "ORAL_AFTER_MEAL" : intakeMethod.trim();
        this.intakeMeasurePerDay = normalizeIntakeMeasure(intakeMeasurePerDay);
    }

    // Constructor for loading medicine from file (manual ID)
    public Medicine(String id, String name, int quantity, String unit, String usage, String expiration,
                    double pricePerUnit, String intakeMethod, String intakeMeasurePerDay) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.usage = usage;
        this.expiration = expiration;
        this.pricePerUnit = Math.max(0.0, pricePerUnit);
        this.intakeMethod = (intakeMethod == null || intakeMethod.isBlank())
                ? "ORAL_AFTER_MEAL" : intakeMethod.trim();
        this.intakeMeasurePerDay = normalizeIntakeMeasure(intakeMeasurePerDay);

        // Ensure the counter is always ahead to prevent duplicate IDs
        try {
            int numericPart = Integer.parseInt(id.substring(1)); // Remove 'M' prefix
            if (numericPart >= counter) counter = numericPart + 1;
        } catch (NumberFormatException ignored) {}
    }

    private String normalizeIntakeMeasure(String s) {
        if (s == null) return "1 unit/day";
        String t = s.trim();
        if (t.isEmpty()) return "1 unit/day";
        // If only a number was entered, add a sensible suffix
        if (t.matches("\\d+")) return t + " unit/day";
        return t;
    }

    // === Static Utilities ===
    public static void resetCounter(int newStart) { counter = newStart; }

    // === Getters ===
    public String getId() { return id; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public String getUsage() { return usage; }
    public String getExpiration() { return expiration; }
    public double getPricePerUnit() { return pricePerUnit; }
    public String getIntakeMethod() { return intakeMethod; }
    public String getIntakeMeasurePerDay() { return intakeMeasurePerDay; }

    // === Setters ===
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setPricePerUnit(double pricePerUnit) { this.pricePerUnit = Math.max(0.0, pricePerUnit); }
    public void setIntakeMethod(String intakeMethod) {
        if (intakeMethod != null && !intakeMethod.isBlank()) this.intakeMethod = intakeMethod.trim();
    }
    public void setIntakeMeasurePerDay(String intakeMeasurePerDay) {
        this.intakeMeasurePerDay = normalizeIntakeMeasure(intakeMeasurePerDay);
    }

    // === Display Formatting (keep in sync with PharmacyControl header widths) ===
    private static String fit(String s, int width) {
        if (s == null) s = "";
        if (s.length() <= width) return String.format("%-" + width + "s", s);
        return s.substring(0, Math.max(0, width - 1)) + "…";
    }

    @Override
    public String toString() {
        // | ID(5) | Name(20) | Qty(8) | Unit(8) | Price(10) | Usage(16) | Intake/Day(14) | Expiry(10) |
        return String.format("| %s | %s | %8d | %s | %10.2f | %s | %s | %s |",
                fit(id, 5),
                fit(name, 20),
                quantity,
                fit(unit, 8),
                pricePerUnit,
                fit(usage, 16),
                fit(intakeMeasurePerDay, 14),
                fit(expiration, 10)
        );
    }
}
