package tarumtclinicmanagementsystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Consultation implements Comparable<Consultation> {
    private static int counter = 1;
    private final int consultationId;
    private String patientName;
    private String doctorName;
    private LocalDateTime consultationDate;

    public Consultation(String patientName, String doctorName, LocalDateTime consultationDate) {
        this.consultationId = counter++;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.consultationDate = consultationDate;
    }

    public int getId() {
        return consultationId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public LocalDateTime getConsultationDate() {
        return consultationDate;
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
               ", Patient: " + patientName +
               ", Doctor: " + doctorName +
               ", Date: " + consultationDate.format(formatter);
    }
}
