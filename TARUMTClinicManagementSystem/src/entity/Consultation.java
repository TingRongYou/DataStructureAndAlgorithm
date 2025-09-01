package entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Consultation implements Comparable<Consultation> {
    private static int counter = 1;
    private final int consultationId;
    private String patientId;
    private String patientName;
    private String doctorName;
    private String doctorId;
    private LocalDateTime consultationDate;
    private String diagnosis;

    // IMPORTANT: include CONSULTING so the status survives reloads
    public enum Status { PENDING, CONSULTING, PROCESSED, COMPLETED; }

    private Status status = Status.PENDING;   // default when created
    private String prescriptionNotes;
    private Integer linkedTreatmentId;

    // Constructor for new consultations (auto-ID)
    public Consultation(String patientId, String patientName, String doctorName, String doctorId,
                        LocalDateTime consultationDate, String diagnosis) {
        this.consultationId = counter++;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.doctorId = doctorId;
        this.consultationDate = consultationDate;
        this.diagnosis = diagnosis;
    }

    // Constructor for loading from file (manual ID)
    public Consultation(int consultationId, String patientId, String patientName, String doctorName, String doctorId,
                        LocalDateTime consultationDate, String diagnosis) {
        this.consultationId = consultationId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.doctorId = doctorId;
        this.consultationDate = consultationDate;
        this.diagnosis = diagnosis;

        if (consultationId >= counter) counter = consultationId + 1;
    }

    // Getters
    public int getId() { return consultationId; }
    public String getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public String getDoctorName() { return doctorName; }
    public String getDoctorId() { return doctorId; }
    public LocalDateTime getConsultationDate() { return consultationDate; }
    public String getDiagnosis() { return diagnosis; }
    public Status getStatus() { return status; }
    public String getPrescriptionNotes() { return prescriptionNotes; }
    public Integer getLinkedTreatmentId() { return linkedTreatmentId; }

    // Setters
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public void setPatientName(String name) { this.patientName = name; }
    public void setDoctorName(String name) { this.doctorName = name; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public void setConsultationDate(LocalDateTime date) { this.consultationDate = date; }
    public void setDiagnosis(String diagnosis){ this.diagnosis = diagnosis; }
    public void setStatus(Status status) { this.status = status; }
    public void setPrescriptionNotes(String notes) { this.prescriptionNotes = notes; }
    public void setLinkedTreatmentId(Integer id) { this.linkedTreatmentId = id; }

    // Sorting by date
    @Override public int compareTo(Consultation other) {
        return this.consultationDate.compareTo(other.consultationDate);
    }

    // CSV-style (not used by controller writer but kept here)
    public String toCSV() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return String.format("%d,%s,%s,%s,%s,%s",
                consultationId, patientId, patientName, doctorId, doctorName, consultationDate.format(f));
    }

    @Override public String toString() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return "ID: " + consultationId +
               ", Patient: " + patientName + " (" + patientId + ")" +
               ", Doctor: " + doctorName +
               ", Date: " + consultationDate.format(f);
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Consultation)) return false;
        Consultation that = (Consultation) o;
        return consultationId == that.consultationId;
    }
    @Override public int hashCode() { return Objects.hash(consultationId); }

    public static void setCounter(int nextId) {
        if (nextId > counter) counter = nextId;
    }
}
