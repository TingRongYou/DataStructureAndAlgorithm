package tarumtclinicmanagementsystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MedicalTreatment implements Comparable<MedicalTreatment> {
    private static int counter = 1;
    private final int treatmentId;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String diagnosis;
    private String prescription;
    private LocalDateTime treatmentDateTime;
    private boolean completed;

    // Constructor for creating new treatments (auto ID)
    public MedicalTreatment(String patientId, String patientName, String doctorId,
                            String diagnosis, String prescription,
                            LocalDateTime treatmentDateTime, boolean completed) {
        this.treatmentId = counter++;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.diagnosis = diagnosis;
        this.prescription = prescription;
        this.treatmentDateTime = treatmentDateTime;
        this.completed = completed;
    }

    // ✅ Constructor for loading from file (fixed ID)
    public MedicalTreatment(int treatmentId, String patientId, String patientName, String doctorId,
                            String diagnosis, String prescription,
                            LocalDateTime treatmentDateTime, boolean completed) {
        this.treatmentId = treatmentId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.diagnosis = diagnosis;
        this.prescription = prescription;
        this.treatmentDateTime = treatmentDateTime;
        this.completed = completed;

        // ✅ Ensure counter always stays ahead
        if (treatmentId >= counter) {
            counter = treatmentId + 1;
        }
    }

    // Getters
    public int getTreatmentId() {
        return treatmentId;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public String getPrescription() {
        return prescription;
    }

    public LocalDateTime getTreatmentDateTime() {
        return treatmentDateTime;
    }

    public boolean isCompleted() {
        return completed;
    }

    // Setters
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public void setPrescription(String prescription) {
        this.prescription = prescription;
    }

    public void setTreatmentDateTime(LocalDateTime treatmentDateTime) {
        this.treatmentDateTime = treatmentDateTime;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    // Follow-up logic: return true if not completed AND more than 3 days have passed
    public boolean isFollowUpNeeded() {
        return !completed && treatmentDateTime.plusDays(3).isBefore(LocalDateTime.now());
    }

    @Override
    public int compareTo(MedicalTreatment other) {
        return this.treatmentDateTime.compareTo(other.getTreatmentDateTime());
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return "Treatment ID: " + treatmentId +
               ", Patient: " + patientName + " (" + patientId + ")" +
               ", Doctor ID: " + doctorId +
               ", Date: " + treatmentDateTime.format(formatter) +
               ", Completed: " + (completed ? "Yes" : "No") +
               ", Diagnosis: " + diagnosis +
               ", Prescription: " + prescription;
    }
}
