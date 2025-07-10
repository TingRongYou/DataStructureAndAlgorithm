package tarumtclinicmanagementsystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MedicalTreatment {
    private static int treatmentCounter = 1;
    private final String treatmentId;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String diagnosis;
    private String prescription;
    private LocalDateTime dateTime;
    private boolean isFollowUpNeeded;

    public MedicalTreatment(String patientId, String patientName, String doctorId, String diagnosis, String prescription, LocalDateTime dateTime, Boolean isFollowUpNeeded) {
        this.treatmentId = "T" + String.format("%03d", treatmentCounter++);
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.diagnosis = diagnosis;
        this.prescription = prescription;
        this.dateTime = dateTime;
        this.isFollowUpNeeded = isFollowUpNeeded;
    }

    // Getters
    public String getTreatmentId() {
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

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public Boolean isFollowUpNeeded() {
        return isFollowUpNeeded;
    }

    // Setters
    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public void setPrescription(String prescription) {
        this.prescription = prescription;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public void setIsFollowUpNeeded(Boolean isFollowUpNeeded) {
        this.isFollowUpNeeded = isFollowUpNeeded;
    }
    
    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return "Treatment ID: " + treatmentId +
                ", Patient ID: " + patientId +
                ", Patient Name: " + patientName +
                ", Doctor ID: " + doctorId +
                ", Date/Time: " + dateTime.format(formatter) +
                ", Diagnosis: " + diagnosis +
                ", Prescription: " + prescription +
                ", Follow Up: " + (isFollowUpNeeded ? "Yes" : "No");
    }
}
