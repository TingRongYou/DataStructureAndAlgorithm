package tarumtclinicmanagementsystem;

public class TreatmentControl {
    private DynamicArray<MedicalTreatment> allTreatments; 
    private MyQueue<MedicalTreatment> followUpQueue; // FIFO for follow-ups
    
    public TreatmentControl(){
        allTreatments = new DynamicArray<>();
        followUpQueue = new MyQueue<>();
    }
    
    public void addTreatment(MedicalTreatment treatment){
        allTreatments.add(treatment);
        if(treatment.isFollowUpNeeded()){
            followUpQueue.enqueue(treatment);
        }
    }
    
    //search treatments 
    public DynamicArray<MedicalTreatment> getTreatmentsByPatient(String patientId){
        DynamicArray<MedicalTreatment> result = new DynamicArray<>();
        for(int i = 0; i < allTreatments.size(); i++){
            MedicalTreatment t = allTreatments.get(i);
            if(t.getPatientId().equals(patientId)){
                result.add(t);
            }
        }
        return result; 
    }
    
    public MedicalTreatment processNextFollowUp(){
        return followUpQueue.dequeue();
    }
}
