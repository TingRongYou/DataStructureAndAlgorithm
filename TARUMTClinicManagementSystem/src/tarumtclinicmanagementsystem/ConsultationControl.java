package tarumtclinicmanagementsystem;

import java.time.LocalDateTime;

// Control class that manages consultation records using DynamicArray
public class ConsultationControl {

    // Custom dynamic array to store Consultation objects
    private DynamicArray<Consultation> consultations = new DynamicArray<>();

    // Adds a new consultation to the dynamic array
    public void addConsultation(String patient, String doctor, LocalDateTime date) {
        Consultation c = new Consultation(patient, doctor, date);  // Create new consultation object
        consultations.add(c);  // Add to dynamic array
        System.out.println("Consultation added: " + c);  // Display confirmation
    }

    // Removes a consultation by its unique ID
    public boolean removeConsultationById(int id) {
        for (int i = 0; i < consultations.size(); i++) {
            if (consultations.get(i).getId() == id) {  // Match consultation ID
                consultations.remove(i);  // Remove from array
                System.out.println("Consultation removed.");
                return true;
            }
        }
        System.out.println("Consultation not found.");
        return false;
    }

    // Lists all consultations stored in the dynamic array
    public void listConsultations() {
        if (consultations.isEmpty()) {  // Check if array is empty
            System.out.println("No consultations.");
            return;
        }
        System.out.println("=== All Consultations ===");
        for (int i = 0; i < consultations.size(); i++) {
            System.out.println(consultations.get(i));  // Print each consultation
        }
    }

    // Searches for consultations by patient name (case-insensitive)
    public void searchByPatient(String patientName) {
        boolean found = false;
        for (int i = 0; i < consultations.size(); i++) {
            if (consultations.get(i).getPatientName().equalsIgnoreCase(patientName)) {
                System.out.println(consultations.get(i));  // Print matching consultation
                found = true;
            }
        }
        if (!found) {
            System.out.println("No consultation found for patient: " + patientName);
        }
    }
}
