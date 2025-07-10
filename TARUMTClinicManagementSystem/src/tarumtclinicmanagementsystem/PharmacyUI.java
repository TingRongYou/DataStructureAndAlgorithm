/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtclinicmanagementsystem;

/**
 *
 * @author User
 */

import java.util.Scanner;

public class PharmacyUI {
    private PharmacyControl control = new PharmacyControl();
    private Scanner sc = new Scanner(System.in);

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
                System.out.println("❌ Invalid input. Please enter a number.");
                choice = -1;
            }

        } while (choice != 0);
    }

    private void addMedicine() {
        System.out.print("Medicine Name: ");
        String name = sc.nextLine();

        System.out.print("Quantity: ");
        int qty = Integer.parseInt(sc.nextLine());

        System.out.print("Unit (e.g. mg, ml): ");
        String unit = sc.nextLine();

        System.out.print("Usage: ");
        String usage = sc.nextLine();

        Medicine med = new Medicine(name, qty, unit, usage);
        control.addMedicine(med);
    }

    private void dispenseMedicine() {
        System.out.print("Medicine Name: ");
        String name = sc.nextLine();

        System.out.print("Quantity to dispense: ");
        int qty = Integer.parseInt(sc.nextLine());

        control.dispenseMedicine(name, qty);
    }

    private void restockMedicine() {
        System.out.print("Medicine Name: ");
        String name = sc.nextLine();

        System.out.print("Quantity to add: ");
        int qty = Integer.parseInt(sc.nextLine());

        control.restockMedicine(name, qty);
    }

    private void removeMedicine() {
        System.out.print("Medicine Name: ");
        String name = sc.nextLine();

        control.removeMedicine(name);
    }

    private void generateLowStockReport() {
        System.out.print("Enter low stock threshold (e.g., 5): ");
        try {
            int threshold = Integer.parseInt(sc.nextLine());
            control.printLowStockMedicines(threshold);
        } catch (NumberFormatException e) {
            System.out.println("❌ Please enter a valid number.");
        }
    }
}

