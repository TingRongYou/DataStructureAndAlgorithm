package tarumtclinicmanagementsystem;

<<<<<<< HEAD
import java.io.FileWriter;
import java.io.IOException;

=======
import java.util.Comparator;

/**
 *
 * @author Acer
 */
>>>>>>> 799e9b2fc56aec04e669a1dff14c09088c36bc85
public class PatientControl {
    private ClinicADT<Patient> patientQueue;

    public PatientControl() {
        patientQueue = new MyClinicADT<>();
    }

<<<<<<< HEAD
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
=======
    public void registerPatient(String name, String id) {
        Patient newPatient = new Patient(name, id);
        patientQueue.enqueue(newPatient);  // FIFO behavior
        System.out.println("Patient registered and added to the queue.");
>>>>>>> 799e9b2fc56aec04e669a1dff14c09088c36bc85
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
<<<<<<< HEAD
}
=======

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
>>>>>>> 799e9b2fc56aec04e669a1dff14c09088c36bc85
