package tarumtclinicmanagementsystem;

public class TreatmentControl {
    private ClinicADT<MedicalTreatment> allTreatments;
    private ClinicADT<MedicalTreatment> followUpQueue; // FIFO queue

    public TreatmentControl() {
        allTreatments = new MyClinicADT<>();
        followUpQueue = new MyClinicADT<>();
    }

    public void addTreatment(MedicalTreatment treatment) {
        allTreatments.add(treatment);
        if (treatment.isFollowUpNeeded()) {
            followUpQueue.enqueue(treatment);
        }
        System.out.println("✅ Treatment recorded.");
    }

    public MedicalTreatment processNextFollowUp() {
        if (!followUpQueue.isEmpty()) {
            MedicalTreatment next = followUpQueue.dequeue();
            System.out.println("🔔 Follow-up for: " + next.getPatientName());
            return next;
        } else {
            System.out.println("No follow-up treatments.");
            return null;
        }
    }

    public ClinicADT<MedicalTreatment> getTreatmentsByPatient(String patientId) {
        ClinicADT<MedicalTreatment> result = new MyClinicADT<>();
        for (int i = 0; i < allTreatments.size(); i++) {
            MedicalTreatment t = allTreatments.get(i);
            if (t.getPatientId().equalsIgnoreCase(patientId)) {
                result.add(t);
            }
        }
        return result;
    }

    public void listAllTreatments() {
        System.out.println("=== All Treatments ===");
        if (allTreatments.isEmpty()) {
            System.out.println("No treatments recorded.");
            return;
        }
        for (int i = 0; i < allTreatments.size(); i++) {
            System.out.println(allTreatments.get(i));
        }
    }

    // 📋 Report 1: Show follow-up queue
    public void printFollowUpQueue() {
        System.out.println("=== Follow-Up Queue ===");
        if (followUpQueue.isEmpty()) {
            System.out.println("No patients require follow-up.");
            return;
        }

        ClinicADT<MedicalTreatment> temp = new MyClinicADT<>();
        while (!followUpQueue.isEmpty()) {
            MedicalTreatment t = followUpQueue.dequeue();
            System.out.println(t);
            temp.enqueue(t); // preserve order
        }
        while (!temp.isEmpty()) {
            followUpQueue.enqueue(temp.dequeue());
        }
    }

    // 📋 Report 2: Sort treatments by date (Bubble sort)
    public void printAllTreatmentsSortedByDate() {
        if (allTreatments.isEmpty()) {
            System.out.println("No treatments to display.");
            return;
        }

        // Clone to avoid modifying original list
        ClinicADT<MedicalTreatment> sorted = new MyClinicADT<>();
        for (int i = 0; i < allTreatments.size(); i++) {
            sorted.add(allTreatments.get(i));
        }

        // Bubble sort by treatment date
        for (int i = 0; i < sorted.size() - 1; i++) {
            for (int j = 0; j < sorted.size() - i - 1; j++) {
                MedicalTreatment t1 = sorted.get(j);
                MedicalTreatment t2 = sorted.get(j + 1);
                if (t1.getDateTime().isAfter(t2.getDateTime())) {
                    sorted.set(j, t2);
                    sorted.set(j + 1, t1);
                }
            }
        }

        System.out.println("=== All Treatments (Sorted by Date) ===");
        for (int i = 0; i < sorted.size(); i++) {
            System.out.println(sorted.get(i));
        }
    }
}