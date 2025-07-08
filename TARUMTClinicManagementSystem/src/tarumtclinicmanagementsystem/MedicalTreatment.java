package tarumtclinicmanagementsystem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MedicalTreatment {
    private final String treatmentId; //or int? 
    private String patientId;
    private String doctorId;
    private String diagnosis;
    private String prescription;
    private LocalDate date;
    private boolean isFollowUpNeeded;
    
    public MedicalTreatment(String treatmentId, String patientId, String doctorId, String diagnosis, String prescription, LocalDate date, Boolean isFollowUpNeeded){
        this.treatmentId = treatmentId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.diagnosis = diagnosis;
        this.prescription = prescription;
        this.date = date;
        this.isFollowUpNeeded = isFollowUpNeeded; 
    }
    
    public String getTreatmentId(){
        return treatmentId;
    }
    
    public String getPatientId(){
        return patientId;
    }
    
    public String getDoctorId(){
        return doctorId;
    }
    
    public String getDiagnosis(){
        return diagnosis;
    }
    
    public LocalDate getDate(){
        return date;
    }
    
    public Boolean isFollowUpNeeded(){ 
        return isFollowUpNeeded;
    }
    
    public void setDiagnosis(String diagnosis){
        this.diagnosis = diagnosis;
    }
    
    public void setPrescription(String prescription){
        this.prescription = prescription;
    }
    
    public void setDate(LocalDate date){
        this.date = date;
    }
    
    public void setIsFollowUpNeeded(Boolean isFollowUpNeeded){
        this.isFollowUpNeeded = isFollowUpNeeded;
    }
    
    @Override
    public String toString(){
        return "Treatment ID: " + treatmentId +
                ", Patient ID: " + patientId +
                ", Doctor ID: " + doctorId + 
                ", Date: " + date.format(DateTimeFormatter.ISO_DATE) + 
                ", Diagnosis: " + diagnosis + 
                ", Prescription: " + prescription + 
                ", Is Follow Up Needed? " + (isFollowUpNeeded ? "Yes" : "No");
    }
}
