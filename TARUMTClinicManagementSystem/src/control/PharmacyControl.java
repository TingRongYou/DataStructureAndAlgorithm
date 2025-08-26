package control;

import adt.ClinicADT;
import adt.MyClinicADT;
import entity.Medicine;
import java.io.*;
import java.time.LocalDate;
import java.util.Scanner;
import utility.Report;
import utility.Validation;

public class PharmacyControl {
    private final ClinicADT<Medicine> medicineList = new MyClinicADT<>();
    private final String medicineFilePath = "src/textFile/medicine.txt";

    public PharmacyControl() {
        loadFromFile();
    }

    // --- Add Medicine ---
    public void addMedicine(Medicine med) {
        // Validation
        String nameError = Validation.validateMedicineName(med.getName());
        String qtyError = Validation.validateMedicineQuantity(med.getQuantity());
        String unitError = Validation.validateMedicineUnit(med.getUnit());
        String usageError = Validation.validateMedicineUsage(med.getUsage());
        String expirationError = Validation.validateMedicineExpiry(med.getExpiration());

        if (nameError != null || qtyError != null || unitError != null || usageError != null || expirationError != null) {
            System.out.println("Failed to add medicine due to validation errors:");
            if (nameError != null) System.out.println(" - " + nameError);
            if (qtyError != null) System.out.println(" - " + qtyError);
            if (unitError != null) System.out.println(" - " + unitError);
            if (usageError != null) System.out.println(" - " + usageError);
            if (expirationError != null) System.out.println(" - " + expirationError);
            return;
        }

        medicineList.add(med);
        saveToFile();
        System.out.println("\nMedicine added successfully!\n");
        printSingleMedicine(med);
    }

    // --- Dispense Medicine ---
    public boolean dispenseMedicineById(String id, int amount) {
        Medicine m = getMedicineById(id);
        if (m != null) {
            String error = Validation.validateDispenseQuantity(m.getQuantity(), amount);
            if (error != null) {
                System.out.println(error);
                return false;
            }
            m.setQuantity(m.getQuantity() - amount);
            System.out.println(amount + " units of " + m.getName() + " dispensed.");
            saveToFile();
            return true;
        } else {
            System.out.println("Medicine not found.");
        }
        return false;
    }

    // --- Restock Medicine ---
    public boolean restockMedicineById(String id, int amount) {
        Medicine m = getMedicineById(id);
        if (m != null) {
            String qtyError = Validation.validateMedicineQuantity(amount);
            if (qtyError != null) {
                System.out.println(qtyError);
                return false;
            }
            m.setQuantity(m.getQuantity() + amount);
            System.out.println("Medicine restocked: " + amount + " added to " + m.getName());
            saveToFile();
            return true;
        }
        System.out.println("Medicine not found.");
        return false;
    }

    // --- Remove Medicine ---
    public boolean removeMedicineById(String id) {
        ClinicADT.MyIterator<Medicine> it = medicineList.iterator();
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

    // --- Display All Stock ---
    public void displayStock() {
        if (medicineList.isEmpty()) {
            System.out.println("No medicines in stock.");
            return;
        }
        printHeader();
        ClinicADT.MyIterator<Medicine> it = medicineList.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
        }
        printLine();
    }

    // --- Print Low Stock and Restock Prompt ---
    public void printLowStockMedicines(int threshold, Scanner sc) {
        System.out.println("\n=== Medicines Low In Stock (≤ " + threshold + ") ===");
        boolean found = false;
        printHeader();

        ClinicADT.MyIterator<Medicine> it = medicineList.iterator();
        while (it.hasNext()) {
            Medicine m = it.next();
            if (m.getQuantity() <= threshold) {
                System.out.println(m);
                found = true;
            }
        }

        if (!found) {
            System.out.println("|       All medicines are sufficiently stocked.       |");
            printLine();
            return;
        }

        printLine();
        System.out.print("\nDo you want to restock any medicine? (y/n): ");
        String ans = sc.nextLine().trim().toLowerCase();
        if (!ans.equals("y")) return;

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
            String qtyError = Validation.validateMedicineQuantity(qty);
            if (qtyError == null) {
                m.setQuantity(m.getQuantity() + qty);
                saveToFile();
                System.out.println(qty + " units added to " + m.getName() + ".");
            } else {
                System.out.println(qtyError);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid quantity input.");
        }
    }

    // --- Print All Medicines Sorted by Name ---
    public void printAllMedicinesSortedByName() {
        if (medicineList.isEmpty()) {
            System.out.println("No medicines available.");
            return;
        }

        ClinicADT<Medicine> sorted = new MyClinicADT<>();
        ClinicADT.MyIterator<Medicine> it = medicineList.iterator();
        while (it.hasNext()) {
            sorted.add(it.next());
        }

        // Bubble sort by name
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
        ClinicADT.MyIterator<Medicine> sortedIt = sorted.iterator();
        while (sortedIt.hasNext()) {
            System.out.println(sortedIt.next());
        }
        printLine();
    }

    // --- Helper Methods ---
    private void printHeader() {
        String format = "| %-5s | %-20s | %-8s | %-8s | %-8s | %-22s |%n";
        String line = "+-------+----------------------+----------+----------+----------+------------------------+";
        System.out.println(line);
        System.out.printf(format, "ID", "Name", "Quantity", "Unit", "Usage", "Expiration Date");
        System.out.println(line);
    }

    private void printLine() {
        System.out.println("+-------+----------------------+----------+----------+----------+------------------------+");
    }

    private void printSingleMedicine(Medicine m) {
        printHeader();
        System.out.println(m);
        printLine();
    }

    // --- File Operations ---
    private void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(medicineFilePath))) {
            ClinicADT.MyIterator<Medicine> it = medicineList.iterator();
            while (it.hasNext()) {
                Medicine m = it.next();
                writer.printf("%s,%s,%d,%s,%s,%s%n",
                        m.getId(), m.getName(), m.getQuantity(), m.getUnit(), m.getUsage(), m.getExpiration());
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
                if (parts.length == 6) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    int qty = Integer.parseInt(parts[2].trim());
                    String unit = parts[3].trim();
                    String usage = parts[4].trim();
                    String expiration = parts[5].trim();
                    medicineList.add(new Medicine(id, name, qty, unit, usage, expiration));
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error loading from file: " + e.getMessage());
        }
    }

    // --- Accessors ---
    public Medicine getMedicineById(String id) {
        ClinicADT.MyIterator<Medicine> it = medicineList.iterator();
        while (it.hasNext()) {
            Medicine m = it.next();
            if (m.getId().equalsIgnoreCase(id)) return m;
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
    
   public void expirationReport() {
        Report.printHeader("=== Expiration Report ===");

        int expiredCount = 0;
        int within6Count = 0;
        int after6Count = 0;

        LocalDate today = LocalDate.now();
        LocalDate sixMonthsLater = today.plusMonths(6);

        try (BufferedReader reader = new BufferedReader(new FileReader(medicineFilePath))) {
            String line;

            // Table Header
            String border = "+------------+----------------------+---------------+";
            String header = String.format("| %-10s | %-20s | %-13s |", "ID", "Name", "Expiry");

            // === Expired Medicines ===
            System.out.println("\n[Expired Medicines]");
            System.out.println(border);
            System.out.println(header);
            System.out.println(border);

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 6) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    LocalDate expDate = LocalDate.parse(parts[5].trim());

                    if (expDate.isBefore(today)) {
                        expiredCount++;
                    } else if (!expDate.isAfter(sixMonthsLater)) {
                        within6Count++;
                    } else {
                        after6Count++;
                    }

                    String shortId = id.length() > 3 ? id.substring(0, 3) : id;
                    System.out.printf("| %-10s | %-20s | %-13s |%n", shortId, name, expDate);
                }
            }
            System.out.println(border);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Print summary report
        System.out.println("\nSummary Frequency Bar Chart");
        System.out.println("=============================");
        printBar("Expired   ", expiredCount);
        printBar("Within 6M ", within6Count);
        printBar("After 6M  ", after6Count);

        Report.printFooter();
    }

    // Helper method: print stars only, no count
    private void printBar(String label, int count) {
        System.out.print(label + " : ");
        for (int i = 0; i < count; i++) {
            System.out.print("* ");
        }
        System.out.println();
    }
}
