package entity;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class Appointment {

    public enum AppointmentType { ONLINE, WALK_IN }
    public enum AppointmentStatus { BOOKED, CHECKED_IN, CONSULTING, TREATMENT, PENDING_PAYMENT, COMPLETED }

    private static final AtomicInteger SEQ = new AtomicInteger(1000);
    private final int appointmentId;
    private final String patientId;
    private final String patientName;
    private final String doctorId;
    private final String doctorName;
    private LocalDateTime scheduledDateTime;
    private final AppointmentType type;
    private AppointmentStatus status;
    private LocalDateTime checkInTime;
    private String symptoms;
    private String diagnosis;
    private boolean treatmentNeeded;
    private boolean medicineNeeded;
    private boolean treatmentDone;

    public Appointment(String patientId, String patientName,
                       String doctorId, String doctorName,
                       LocalDateTime scheduledDateTime,
                       AppointmentType type) {
        this.appointmentId = SEQ.getAndIncrement();
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.scheduledDateTime = scheduledDateTime;
        this.type = type;
        this.status = (type == AppointmentType.WALK_IN) ? AppointmentStatus.CHECKED_IN : AppointmentStatus.BOOKED;
        if (this.status == AppointmentStatus.CHECKED_IN) {
            this.checkInTime = LocalDateTime.now();
        }
        this.treatmentDone = false; // default
    }

    // --- Flow helpers ---
    // Check-In, status change from 'BOOKED' to 'CHECKED_IN'
    public boolean checkIn() {
        if (status == AppointmentStatus.BOOKED) {
            status = AppointmentStatus.CHECKED_IN;
            checkInTime = LocalDateTime.now();
            return true;
        }
        return false;
    }
    
    // Call from queue, status change from 'CHECKED-IN' to 'CONSULTING'
    public boolean startConsultation() {
        if (status == AppointmentStatus.CHECKED_IN) {
            status = AppointmentStatus.CONSULTING;
            return true;
        }
        return false;
    }
    
    /*Complete Consultation, if no need treatement status change to 'PENDING_PAYMENT'
      Need Treatment, status change to 'TREATMENT'
    */
    public boolean completeConsultation(String symptoms, String diagnosis,
                                        boolean treatmentNeeded, boolean medicineNeeded) {
        if (status == AppointmentStatus.CONSULTING) {
            this.symptoms = symptoms;
            this.diagnosis = diagnosis;
            this.treatmentNeeded = treatmentNeeded;
            this.medicineNeeded = medicineNeeded;
            status = treatmentNeeded ? AppointmentStatus.TREATMENT : AppointmentStatus.PENDING_PAYMENT;
            return true;
        }
        return false;
    }
    
    // Complete Treatment, status change to 'PENDING_PAYMENT'
    public void completeTreatment() {
        if (status == AppointmentStatus.TREATMENT) {
            this.treatmentDone = true;
            status = AppointmentStatus.PENDING_PAYMENT;
        }
    }
    
    // Complete Payment, status change to 'COMPLETED'
    public void completePayment() {
        status = AppointmentStatus.COMPLETED;
    }

    // --- Getters / Setters ---
    public int getAppointmentId() { return appointmentId; }
    public String getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public String getDoctorId() { return doctorId; }
    public String getDoctorName() { return doctorName; }
    public LocalDateTime getScheduledDateTime() { return scheduledDateTime; }
    public AppointmentType getType() { return type; }
    public AppointmentStatus getStatus() { return status; }
    public LocalDateTime getCheckInTime() { return checkInTime; }

    public void setStatus(AppointmentStatus status) { this.status = status; }
    public void setScheduledDateTime(LocalDateTime t) { this.scheduledDateTime = t; }

    public String getSymptoms() { return symptoms; }
    public String getDiagnosis() { return diagnosis; }
    public boolean isTreatmentNeeded() { return treatmentNeeded; }
    public boolean isMedicineNeeded() { return medicineNeeded; }

    // expose the treatment-done flag
    public boolean isTreatmentDone() { return treatmentDone; }
    public void setTreatmentDone(boolean treatmentDone) { this.treatmentDone = treatmentDone; }

    @Override
    public String toString() {
        return "Appointment{" +
                "id=" + appointmentId +
                ", patient=" + patientName + " (" + patientId + ")" +
                ", doctor=" + doctorName + " (" + doctorId + ")" +
                ", time=" + scheduledDateTime +
                ", type=" + type +
                ", status=" + status +
                ", treatmentDone=" + treatmentDone +
                '}';
    }
}
