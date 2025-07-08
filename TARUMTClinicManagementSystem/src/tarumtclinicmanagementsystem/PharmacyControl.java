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
    private DynamicArray<Medicine> medicineList = new DynamicArray<>();

    public void addMedicine(Medicine med) {
        medicineList.add(med);
        System.out.println("Medicine added.");
    }

    public boolean dispenseMedicine(String name, int amount) {
        for (int i = 0; i < medicineList.size(); i++) {
            Medicine m = medicineList.get(i);
            if (m.getName().equalsIgnoreCase(name)) {
                if (m.getQuantity() >= amount) {
                    m.setQuantity(m.getQuantity() - amount);
                    System.out.println(amount + " units of " + name + " dispensed.");
                    return true;
                } else {
                    System.out.println("Not enough stock.");
                    return false;
                }
            }
        }
        System.out.println("Medicine not found.");
        return false;
    }

    public void displayStock() {
        if (medicineList.isEmpty()) {
            System.out.println("No medicines in stock.");
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
                System.out.println("Medicine removed.");
                return true;
            }
        }
        System.out.println("Medicine not found.");
        return false;
    }

    public boolean restockMedicine(String name, int amount) {
        for (int i = 0; i < medicineList.size(); i++) {
            Medicine m = medicineList.get(i);
            if (m.getName().equalsIgnoreCase(name)) {
                m.setQuantity(m.getQuantity() + amount);
                System.out.println("Medicine restocked.");
                return true;
            }
        }
        System.out.println("Medicine not found.");
        return false;
    }
}

