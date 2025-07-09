/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtclinicmanagementsystem;

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
        patientQueue.enqueue(newPatient);  // Same behavior as before
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
}