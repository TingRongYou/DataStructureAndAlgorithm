package tarumtclinicmanagementsystem;

import java.io.FileWriter;
import java.io.IOException;

public class PatientControl {
    private ClinicADT<Patient> patientQueue;

    public PatientControl() {
        patientQueue = new MyClinicADT<>();
    }

    public void registerPatient(String name, int age, String gender, String contact) {
        Patient newPatient = new Patient(name, age, gender, contact);
        patientQueue.enqueue(newPatient);
        System.out.println("Patient registered: " + newPatient);
        savePatientToFile(newPatient);
    }

    private void savePatientToFile(Patient patient) {
        try (FileWriter writer = new FileWriter("patients.txt", true)) {
            writer.write(patient.toFileString() + "\n");
        } catch (IOException e) {
            System.out.println("Error saving patient to file: " + e.getMessage());
        }
    }

    public Patient callNextPatient() {
        if (patientQueue.isEmpty()) {
            System.out.println("No patients in the queue.");
            return null;
        }
        Patient next = patientQueue.dequeue();
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

        ClinicADT<Patient> tempQueue = new MyClinicADT<>();

        while (!patientQueue.isEmpty()) {
            Patient p = patientQueue.dequeue();
            System.out.println(p);
            tempQueue.enqueue(p);
        }

        while (!tempQueue.isEmpty()) {
            patientQueue.enqueue(tempQueue.dequeue());
        }
    }

    public int getPatientCount() {
        return patientQueue.size();
    }
}
