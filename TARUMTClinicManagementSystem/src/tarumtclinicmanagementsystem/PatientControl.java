/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtclinicmanagementsystem;

import java.util.Comparator;

/**
 *
 * @author Acer
 */
public class PatientControl {
    private ClinicADT<Patient> patientQueue;

    public PatientControl() {
        patientQueue = new MyClinicADT<>();
    }

    public void registerPatient(String name, String id) {
        Patient newPatient = new Patient(name, id);
        patientQueue.enqueue(newPatient);  // FIFO behavior
        System.out.println("Patient registered and added to the queue.");
    }

    public Patient callNextPatient() {
        if (patientQueue.isEmpty()) {
            System.out.println("No patients in the queue.");
            return null;
        }
        Patient next = patientQueue.dequeue();  // FIFO
        System.out.println("Calling patient: " + next);
        return next;
    }

    public void viewNextPatient() {
        if (patientQueue.isEmpty()) {
            System.out.println("No patients in the queue.");
        } else {
            System.out.println("Next patient in queue: " + patientQueue.peek());
        }
    }

    public void displayAllPatients() {
        System.out.println("Total patients in queue: " + patientQueue.size());

        // Copy and preserve queue state
        ClinicADT<Patient> tempQueue = new MyClinicADT<>();

        while (!patientQueue.isEmpty()) {
            Patient p = patientQueue.dequeue();
            System.out.println(p);
            tempQueue.enqueue(p); // Keep the original order
        }

        while (!tempQueue.isEmpty()) {
            patientQueue.enqueue(tempQueue.dequeue()); // Restore queue
        }
    }

    public int getPatientCount() {
        return patientQueue.size();
    }

    // ✅ Corrected method to sort the current patient queue by name
    public void printAllPatientsSortedByName() {
        if (patientQueue.isEmpty()) {
            System.out.println("No patients in the queue.");
            return;
        }

        // Copy patients to a temporary list for sorting
        ClinicADT<Patient> sorted = new MyClinicADT<>();
        ClinicADT<Patient> tempQueue = new MyClinicADT<>();

        while (!patientQueue.isEmpty()) {
            Patient p = patientQueue.dequeue();
            sorted.add(p);
            tempQueue.enqueue(p); // Backup original order
        }

        // Restore the original queue
        while (!tempQueue.isEmpty()) {
            patientQueue.enqueue(tempQueue.dequeue());
        }

        // Sort the copied list using comparator
        sorted.sort(new Comparator<Patient>() {
            @Override
            public int compare(Patient p1, Patient p2) {
                return p1.getName().compareToIgnoreCase(p2.getName());
            }
        });

        // Print sorted list
        System.out.println("=== Patient List (Sorted by Name) ===");
        for (int i = 0; i < sorted.size(); i++) {
            System.out.println(sorted.get(i));
        }
    }
}