package tarumtclinicmanagementsystem;

import java.time.LocalDateTime;
import java.util.Comparator;

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

    // Lists all consultations in unsorted order
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

    // ✅ Report 1: List consultations sorted by date
    public void printConsultationsSortedByDate() {
        if (consultations.isEmpty()) {
            System.out.println("No consultations to sort.");
            return;
        }

        // Clone the consultations list to sort
        ClinicADT<Consultation> sorted = new MyClinicADT<>();
        for (int i = 0; i < consultations.size(); i++) {
            sorted.add(consultations.get(i));
        }

        sorted.sort(new Comparator<>() {
            @Override
            public int compare(Consultation c1, Consultation c2) {
                return c1.getConsultationDate().compareTo(c2.getConsultationDate());
            }
        });

        System.out.println("=== Consultations (Sorted by Date) ===");
        for (int i = 0; i < sorted.size(); i++) {
            System.out.println(sorted.get(i));
        }
    }

    // ✅ Report 2: Search by doctor name
    public void searchByDoctor(String doctorName) {
        boolean found = false;
        for (int i = 0; i < consultations.size(); i++) {
            if (consultations.get(i).getDoctorName().equalsIgnoreCase(doctorName)) {
                System.out.println(consultations.get(i));
                found = true;
            }
        }
        if (!found) {
            System.out.println("No consultation found for doctor: " + doctorName);
        }
    }

    public int getTotalConsultations() {
        return consultations.size();
    }
}
