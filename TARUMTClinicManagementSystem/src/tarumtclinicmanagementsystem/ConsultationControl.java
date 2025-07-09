package tarumtclinicmanagementsystem;

import java.time.LocalDateTime;

// Control class that manages consultation records using DynamicArray
public class ConsultationControl {

    private ClinicADT<Consultation> consultations;

    public ConsultationControl() {
        consultations = new MyClinicADT<>();
    }

    // Adds a new consultation to the dynamic list
    public void addConsultation(String patient, String doctor, LocalDateTime date) {
        Consultation c = new Consultation(patient, doctor, date);
        consultations.add(c);
        System.out.println("Consultation added: " + c);
    }

    // Removes a consultation by its unique ID
    public boolean removeConsultationById(int id) {
        for (int i = 0; i < consultations.size(); i++) {
            if (consultations.get(i).getId() == id) {
                consultations.remove(i);
                System.out.println("Consultation removed.");
                return true;
            }
        }
        System.out.println("Consultation not found.");
        return false;
    }

    // Lists all consultations
    public void listConsultations() {
        if (consultations.isEmpty()) {
            System.out.println("No consultations.");
            return;
        }

        System.out.println("=== All Consultations ===");
        for (int i = 0; i < consultations.size(); i++) {
            System.out.println(consultations.get(i));
        }
    }

    // Searches for consultations by patient name
    public void searchByPatient(String patientName) {
        boolean found = false;
        for (int i = 0; i < consultations.size(); i++) {
            if (consultations.get(i).getPatientName().equalsIgnoreCase(patientName)) {
                System.out.println(consultations.get(i));
                found = true;
            }
        }
        if (!found) {
            System.out.println("No consultation found for patient: " + patientName);
        }
    }
}
