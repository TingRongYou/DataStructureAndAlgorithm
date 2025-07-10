package tarumtclinicmanagementsystem;

public class TreatmentControl {
    private ClinicADT<MedicalTreatment> allTreatments;
    private ClinicADT<MedicalTreatment> followUpQueue; // acts as FIFO

    public TreatmentControl() {
        allTreatments = new MyClinicADT<>();
        followUpQueue = new MyClinicADT<>();
    }

    public void addTreatment(MedicalTreatment treatment) {
        allTreatments.add(treatment);
        if (treatment.isFollowUpNeeded()) {
            followUpQueue.enqueue(treatment); // enqueue to simulate queue behavior
        }
    }

    // Search treatments by patient ID
    public ClinicADT<MedicalTreatment> getTreatmentsByPatient(String patientId) {
        ClinicADT<MedicalTreatment> result = new MyClinicADT<>();
        for (int i = 0; i < allTreatments.size(); i++) {
            MedicalTreatment t = allTreatments.get(i);
            if (t.getPatientId().equals(patientId)) {
                result.add(t);
            }
        }
        return result;
    }

    public MedicalTreatment processNextFollowUp() {
        if (!followUpQueue.isEmpty()) {
            return followUpQueue.dequeue();
        }
        return null;
    }
}