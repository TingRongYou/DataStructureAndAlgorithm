package control;

import adt.ClinicADT;
import adt.MyClinicADT;
import entity.Medicine;

import java.io.*;
import java.util.Iterator;
import java.util.Scanner;

public class PharmacyControl {
    private final ClinicADT<Medicine> medicineList = new MyClinicADT<>();
    private final String medicineFilePath = "src/textFile/medicine.txt";

    public PharmacyControl() {
        loadFromFile();
    }

    public void addMedicine(Medicine med) {
        medicineList.add(med);
        saveToFile();
        System.out.println("\nMedicine added successfully!\n");
        printSingleMedicine(med);
    }

    public boolean dispenseMedicineById(String id, int amount) {
        Medicine m = getMedicineById(id);
        if (m != null) {
            if (m.getQuantity() >= amount) {
                m.setQuantity(m.getQuantity() - amount);
                System.out.println(amount + " units of " + m.getName() + " dispensed.");
                saveToFile();
                return true;
            } else {
                System.out.println("Not enough stock.");
            }
        } else {
            System.out.println("Medicine not found.");
        }
        return false;
    }

    public boolean restockMedicineById(String id, int amount) {
        Medicine m = getMedicineById(id);
        if (m != null) {
            m.setQuantity(m.getQuantity() + amount);
            System.out.println("Medicine restocked: " + amount + " added to " + m.getName());
            saveToFile();
            return true;
        }
        System.out.println("Medicine not found.");
        return false;
    }

    public boolean removeMedicineById(String id) {
        Iterator<Medicine> it = medicineList.iterator();
        int index = 0;
        while (it.hasNext()) {
            if (it.next().getId().equalsIgnoreCase(id)) {
                medicineList.remove(index);
                System.out.println("Medicine removed: " + id);
                saveToFile();
                return true;
            }
            index++;
        }
        System.out.println("Medicine not found.");
        return false;
    }

    public void displayStock() {
        if (medicineList.isEmpty()) {
            System.out.println("No medicines in stock.");
            return;
        }

        printHeader();
        Iterator<Medicine> it = medicineList.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
        }
        printLine();
    }

    public void printLowStockMedicines(int threshold, Scanner sc) {
        System.out.println("\n=== Medicines Low In Stock (≤ " + threshold + ") ===");

        boolean found = false;
        printHeader();

        Iterator<Medicine> it = medicineList.iterator();
        while (it.hasNext()) {
            Medicine m = it.next();
            if (m.getQuantity() <= threshold) {
                System.out.println(m);
                found = true;
            }
        }

        if (found) {
            printLine();
            System.out.print("\nDo you want to restock any medicine? (y/n): ");
            String ans = sc.nextLine().trim().toLowerCase();
            if (ans.equals("y")) {
                System.out.print("Enter Medicine ID to restock: ");
                String id = sc.nextLine().trim().toUpperCase();

                Medicine m = getMedicineById(id);
                if (m == null) {
                    System.out.println("Invalid Medicine ID.");
                    return;
                }

                System.out.print("Enter quantity to add: ");
                try {
                    int qty = Integer.parseInt(sc.nextLine());
                    if (qty > 0) {
                        m.setQuantity(m.getQuantity() + qty);
                        saveToFile();
                        System.out.println(qty + " units added to " + m.getName() + ".");
                    } else {
                        System.out.println("Quantity must be greater than 0.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid quantity input.");
                }
            }
        } else {
            System.out.println("|       All medicines are sufficiently stocked.       |");
            printLine();
        }
    }

    public void printAllMedicinesSortedByName() {
        if (medicineList.isEmpty()) {
            System.out.println("No medicines available.");
            return;
        }

        ClinicADT<Medicine> sorted = new MyClinicADT<>();
        Iterator<Medicine> it = medicineList.iterator();
        while (it.hasNext()) {
            sorted.add(it.next());
        }

        // Bubble sort on name
        for (int i = 0; i < sorted.size() - 1; i++) {
            for (int j = 0; j < sorted.size() - 1 - i; j++) {
                Medicine m1 = sorted.get(j);
                Medicine m2 = sorted.get(j + 1);
                if (m1.getName().compareToIgnoreCase(m2.getName()) > 0) {
                    sorted.set(j, m2);
                    sorted.set(j + 1, m1);
                }
            }
        }

        System.out.println("\n=== All Medicines (Sorted by Name) ===");
        printHeader();
        Iterator<Medicine> sortedIt = sorted.iterator();
        while (sortedIt.hasNext()) {
            System.out.println(sortedIt.next());
        }
        printLine();
    }

    private void printHeader() {
        String format = "| %-5s | %-20s | %-8s | %-8s | %-30s |%n";
        String line = "+-------+----------------------+----------+----------+--------------------------------+";
        System.out.println(line);
        System.out.printf(format, "ID", "Name", "Quantity", "Unit", "Usage");
        System.out.println(line);
    }

    private void printLine() {
        System.out.println("+-------+----------------------+----------+----------+--------------------------------+");
    }

    private void printSingleMedicine(Medicine m) {
        printHeader();
        System.out.println(m);
        printLine();
    }

    private void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(medicineFilePath))) {
            Iterator<Medicine> it = medicineList.iterator();
            while (it.hasNext()) {
                Medicine m = it.next();
                writer.printf("%s,%s,%d,%s,%s%n",
                        m.getId(), m.getName(), m.getQuantity(), m.getUnit(), m.getUsage());
            }
        } catch (IOException e) {
            System.out.println("Error saving to file: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        File file = new File(medicineFilePath);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length == 5) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    int qty = Integer.parseInt(parts[2].trim());
                    String unit = parts[3].trim();
                    String usage = parts[4].trim();
                    medicineList.add(new Medicine(id, name, qty, unit, usage));
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error loading from file: " + e.getMessage());
        }
    }

    // === Accessors ===
    public Medicine getMedicineById(String id) {
        Iterator<Medicine> it = medicineList.iterator();
        while (it.hasNext()) {
            Medicine m = it.next();
            if (m.getId().equalsIgnoreCase(id)) {
                return m;
            }
        }
        return null;
    }

    public ClinicADT<Medicine> getAllMedicines() {
        return medicineList;
    }

    public Medicine getMedicineAt(int index) {
        return medicineList.get(index);
    }

    public int getSize() {
        return medicineList.size();
    }

    public boolean isEmpty() {
        return medicineList.isEmpty();
    }
}
