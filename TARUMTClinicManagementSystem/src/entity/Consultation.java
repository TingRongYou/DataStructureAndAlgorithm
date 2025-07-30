package entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Consultation implements Comparable<Consultation> {
    private static int counter = 1;
    private final int consultationId;
    private String patientId;         // ✅ Add this
    private String patientName;
    private String doctorName;
    private String doctorId;
    private LocalDateTime consultationDate;

    public Consultation(String patientId, String patientName, String doctorName, String doctorId, LocalDateTime consultationDate) {
        this.consultationId = counter++;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.doctorId = doctorId;
        this.consultationDate = consultationDate;
    }
    
    public Consultation(int consultationId, String patientId, String patientName, String doctorName, String doctorId, LocalDateTime consultationDate) {
        this.consultationId = consultationId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.doctorId = doctorId;
        this.consultationDate = consultationDate;

        if (consultationId >= counter) {
            counter = consultationId + 1;
        }
    }

    public int getId() {
        return consultationId;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getDoctorName() {
        return doctorName;
    }
    
    public String getDoctorId(){
        return doctorId;
    }

    public LocalDateTime getConsultationDate() {
        return consultationDate;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public void setPatientName(String name) {
        this.patientName = name;
    }

    public void setDoctorName(String name) {
        this.doctorName = name;
    }

    public void setConsultationDate(LocalDateTime date) {
        this.consultationDate = date;
    }

    // Allow consultation to be sorted by date/time
    @Override
    public int compareTo(Consultation other) {
        return this.consultationDate.compareTo(other.consultationDate);
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return "ID: " + consultationId +
               ", Patient: " + patientName + " (" + patientId + ")" +
               ", Doctor: " + doctorName +
               ", Date: " + consultationDate.format(formatter);
    }
}
