package boundary;

import java.util.Scanner;
import entity.Medicine;
import control.PharmacyControl;
import utility.Validation;
import adt.ClinicADT;

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
        String name, unit, usage, error;
        int qty;

        // Medicine Name
        do {
            System.out.print("Medicine Name (or 0 to cancel): ");
            name = sc.nextLine().trim();
            if (name.equals("0")) {
                System.out.println("Operation cancelled.");
                return;
            }
            error = Validation.validateName(name);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        // Quantity
        do {
            qty = promptForInt("Quantity (or 0 to cancel): ");
            if (qty == 0) {
                System.out.println("Operation cancelled.");
                return;
            }
            error = Validation.validateMedicineQuantity(qty);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        // Unit
        do {
            System.out.print("Unit (mg/ml/g) (or 0 to cancel): ");
            unit = sc.nextLine().trim().toLowerCase();
            if (unit.equals("0")) {
                System.out.println("Operation cancelled.");
                return;
            }
            error = Validation.validateMedicineUnit(unit);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        // Usage
        do {
            System.out.print("Usage (or 0 to cancel): ");
            usage = sc.nextLine().trim();
            if (usage.equals("0")) {
                System.out.println("Operation cancelled.");
                return;
            }
            error = Validation.validateMedicineUsage(usage);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        control.addMedicine(new Medicine(name, qty, unit, usage));
    }

    private void dispenseMedicine() {
        if (control.isEmpty()) {
            System.out.println("No medicines available.");
            return;
        }

        displaySimpleList();
        
        String id;
        Medicine med;
        while (true) {
            System.out.print("Enter Medicine ID to dispense (or 0 to cancel): ");
            id = sc.nextLine().trim().toUpperCase();

            if (id.equals("0")) {
                System.out.println("Operation cancelled.");
                return;
            }

            med = control.getMedicineById(id);
            if (med == null) {
                System.out.println("Invalid Medicine ID. Please try again.\n");
            } else {
                break;  // valid ID found, exit loop
            }
        }

        int qty;
        String error;
        do {
            qty = promptForInt("Quantity to dispense (or 0 to cancel): ");
            if (qty == 0) {
                System.out.println("Operation cancelled.");
                return;
            }
            error = Validation.validateDispenseQuantity(med.getQuantity(), qty);
            if (error != null) {
                System.out.println(error);
            }
        } while (error != null);

        control.dispenseMedicineById(id, qty);
    }

    private void restockMedicine() {
        if (control.isEmpty()) {
            System.out.println("No medicines available to restock.");
            return;
        }

        displaySimpleList();
        
        String id;
        Medicine med;
        while(true){
            System.out.print("Enter Medicine ID to restock (or 0 to cancel): ");
            id = sc.nextLine().trim().toUpperCase();
            if (id.equals("0")) {
                System.out.println("Operation cancelled.");
                return;
            }

            med = control.getMedicineById(id);
            if (med == null) {
                System.out.println("Invalid Medicine ID.\n");
            }
            else {
                break;
            }
        }
        int qty;
        String error;
        do {
            qty = promptForInt("Quantity to add (or 0 to cancel): ");
            if (qty == 0) {
                System.out.println("Operation cancelled.");
                return;
            }
            error = Validation.validateMedicineQuantity(qty);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        med.setQuantity(med.getQuantity() + qty);
        System.out.println("Restocked successfully. " + qty + " units added to " + med.getName() + ".");
    }

    private void removeMedicine() {
        if (control.isEmpty()) {
            System.out.println("No medicines to remove.");
            return;
        }

        displaySimpleList();

        String id;
        Medicine med;
        while (true) {
            System.out.print("Enter Medicine ID to dispense (or 0 to cancel): ");
            id = sc.nextLine().trim().toUpperCase();

            if (id.equals("0")) {
                System.out.println("Operation cancelled.");
                return;
            }

            med = control.getMedicineById(id);
            if (med == null) {
                System.out.println("Invalid Medicine ID. Please try again.\n");
            } else {
                break;  // valid ID found, exit loop
            }
        }
        control.removeMedicineById(id);
    }

    private void generateLowStockReport() {
        while (true) {
             int threshold = promptForInt("Enter low stock threshold (e.g., 5) or 0 to cancel: ");

             if (threshold == 0) {
                 System.out.println("Operation cancelled.");
                 return;
             }

             if (threshold < 0) {
                 System.out.println("Please enter a positive number or 0 to cancel.");
                 continue;
             }

             control.printLowStockMedicines(threshold, sc);
             break;
         }
    }

    private int promptForInt(String message) {
        while (true) {
            System.out.print(message);
            try {
                return Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.\n");
            }
        }
    }

    private void displaySimpleList() {
        ClinicADT<Medicine> list = control.getAllMedicines();

        if (list.isEmpty()) {
            System.out.println("No medicines in stock.");
            return;
        }

        String format = "| %-6s | %-20s | %-8s |\n";
        String line = "+--------+----------------------+----------+";
        System.out.println(line);
        System.out.printf(format, "ID", "Name", "Quantity");
        System.out.println(line);

        for (Medicine m : list) {
            System.out.printf(format, m.getId(), m.getName(), m.getQuantity());
        }

        System.out.println(line);
    }
}
