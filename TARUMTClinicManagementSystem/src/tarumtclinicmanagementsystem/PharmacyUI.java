package tarumtclinicmanagementsystem;

import java.util.Scanner;

public class PharmacyUI {
    private final PharmacyControl control = new PharmacyControl();
    private final Scanner sc = new Scanner(System.in);

    public void run() {
        int choice;
        do {
            System.out.println("\n=== Pharmacy Management ===");
            System.out.println("1. Add Medicine");
            System.out.println("2. Dispense Medicine");
            System.out.println("3. View Stock");
            System.out.println("4. Restock Medicine");
            System.out.println("5. Remove Medicine");
            System.out.println("6. Report: Low Stock Medicines");
            System.out.println("7. Report: Medicines Sorted by Name");
            System.out.println("0. Exit");
            System.out.print("Choice: ");

            try {
                choice = Integer.parseInt(sc.nextLine());
                switch (choice) {
                    case 1 -> addMedicine();
                    case 2 -> dispenseMedicine();
                    case 3 -> control.displayStock();
                    case 4 -> restockMedicine();
                    case 5 -> removeMedicine();
                    case 6 -> generateLowStockReport();
                    case 7 -> control.printAllMedicinesSortedByName();
                    case 0 -> System.out.println("Exiting Pharmacy Module...");
                    default -> System.out.println("Invalid choice. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                choice = -1;
            }
        } while (choice != 0);
    }

    private void addMedicine() {
        System.out.print("Medicine Name: ");
        String name = sc.nextLine();

        int qty = promptForInt("Quantity: ");
        String unit;

        while (true) {
            System.out.print("Unit (mg/ml/g): ");
            unit = sc.nextLine().trim().toLowerCase();
            if (unit.equals("mg") || unit.equals("ml") || unit.equals("g")) {
                break;
            } else {
                System.out.println("Invalid unit. Please enter 'mg', 'ml', or 'g'.");
            }
        }

        System.out.print("Usage: ");
        String usage = sc.nextLine();

        Medicine med = new Medicine(name, qty, unit, usage);
        control.addMedicine(med);  // this already prints confirmation and table
    }

   private void dispenseMedicine() {
        if (control.isEmpty()) {
            System.out.println("No medicines available.");
            return;
        }

        displaySimpleList();

        System.out.print("Enter Medicine ID to dispense: ");
        String id = sc.nextLine().trim().toUpperCase();

        Medicine med = control.getMedicineById(id);
        if (med == null) {
            System.out.println("Invalid Medicine ID.");
            return;
        }

        int qty = promptForInt("Quantity to dispense: ");
        control.dispenseMedicineById(id, qty);  // ✅ Use ID-based method here
    }

    private void restockMedicine() {
        if (control.isEmpty()) {
            System.out.println("No medicines available to restock.");
            return;
        }

        System.out.println("\n=== Select Medicine to Restock ===");
        displaySimpleList();

        System.out.print("Enter Medicine ID to restock: ");
        String id = sc.nextLine().trim().toUpperCase();

        Medicine selected = control.getMedicineById(id);
        if (selected == null) {
            System.out.println("Invalid Medicine ID.");
            return;
        }

        int qty = promptForInt("Quantity to add: ");
        if (qty <= 0) {
            System.out.println("Quantity must be positive.");
            return;
        }

        selected.setQuantity(selected.getQuantity() + qty);
        System.out.println("Medicine restocked: " + qty + " added to " + selected.getName());
    }

    private void removeMedicine() {
        if (control.isEmpty()) {
            System.out.println("No medicines to remove.");
            return;
        }

        displaySimpleList();
        System.out.print("Enter Medicine ID to remove: ");
        String id = sc.nextLine().trim().toUpperCase();

        Medicine med = control.getMedicineById(id);
        if (med == null) {
            System.out.println("Invalid Medicine ID.");
            return;
        }

        control.removeMedicineById(med.getId());
    }

    private void generateLowStockReport() {
        int threshold = promptForInt("Enter low stock threshold (e.g., 5): ");
        control.printLowStockMedicines(threshold, sc);
    }

    private int promptForInt(String message) {
        while (true) {
            System.out.print(message);
            try {
                return Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private void displaySimpleList() {
        String format = "| %-5s | %-20s | %-8s |\n";
        String line = "+-------+----------------------+----------+";

        System.out.println(line);
        System.out.printf(format, "ID", "Name", "Quantity");
        System.out.println(line);

        for (int i = 0; i < control.getSize(); i++) {
            Medicine m = control.getMedicineAt(i);
            System.out.printf(format, m.getId(), m.getName(), m.getQuantity());
        }

        System.out.println(line);
    }
}
