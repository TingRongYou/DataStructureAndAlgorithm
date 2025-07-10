/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtclinicmanagementsystem;

/**
 *
 * @author User
 */
public class PharmacyControl {
    private ClinicADT<Medicine> medicineList = new MyClinicADT<>();

    public void addMedicine(Medicine med) {
        medicineList.add(med);
        System.out.println("✅ Medicine added: " + med.getName());
    }

    public boolean dispenseMedicine(String name, int amount) {
        for (int i = 0; i < medicineList.size(); i++) {
            Medicine m = medicineList.get(i);
            if (m.getName().equalsIgnoreCase(name)) {
                if (m.getQuantity() >= amount) {
                    m.setQuantity(m.getQuantity() - amount);
                    System.out.println("✅ " + amount + " units of " + name + " dispensed.");
                    return true;
                } else {
                    System.out.println("⚠ Not enough stock.");
                    return false;
                }
            }
        }
        System.out.println("❌ Medicine not found.");
        return false;
    }

    public void displayStock() {
        if (medicineList.isEmpty()) {
            System.out.println("⚠ No medicines in stock.");
            return;
        }

        System.out.printf("%-20s %-10s %-10s %s\n", "Name", "Quantity", "Unit", "Usage");
        System.out.println("---------------------------------------------------------");
        for (int i = 0; i < medicineList.size(); i++) {
            System.out.println(medicineList.get(i));
        }
    }

    public boolean removeMedicine(String name) {
        for (int i = 0; i < medicineList.size(); i++) {
            if (medicineList.get(i).getName().equalsIgnoreCase(name)) {
                medicineList.remove(i);
                System.out.println("✅ Medicine removed: " + name);
                return true;
            }
        }
        System.out.println("❌ Medicine not found.");
        return false;
    }

    public boolean restockMedicine(String name, int amount) {
        for (int i = 0; i < medicineList.size(); i++) {
            Medicine m = medicineList.get(i);
            if (m.getName().equalsIgnoreCase(name)) {
                m.setQuantity(m.getQuantity() + amount);
                System.out.println("✅ Medicine restocked: " + amount + " added to " + name);
                return true;
            }
        }
        System.out.println("❌ Medicine not found.");
        return false;
    }

    // 🔎 Report 1: List medicines low in stock
    public void printLowStockMedicines(int threshold) {
        System.out.println("=== Medicines Low In Stock (≤ " + threshold + ") ===");
        boolean found = false;
        for (int i = 0; i < medicineList.size(); i++) {
            Medicine m = medicineList.get(i);
            if (m.getQuantity() <= threshold) {
                System.out.println(m);
                found = true;
            }
        }
        if (!found) {
            System.out.println("All medicines are sufficiently stocked.");
        }
    }

    // 📋 Report 2: Display all medicines sorted by name (Bubble Sort)
    public void printAllMedicinesSortedByName() {
        if (medicineList.isEmpty()) {
            System.out.println("⚠ No medicines available.");
            return;
        }

        // Clone to avoid modifying original list
        ClinicADT<Medicine> sorted = new MyClinicADT<>();
        for (int i = 0; i < medicineList.size(); i++) {
            sorted.add(medicineList.get(i));
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

        System.out.println("=== All Medicines (Sorted by Name) ===");
        for (int i = 0; i < sorted.size(); i++) {
            System.out.println(sorted.get(i));
        }
    }
}

