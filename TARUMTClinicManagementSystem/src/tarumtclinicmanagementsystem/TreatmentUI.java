package tarumtclinicmanagementsystem;

import java.util.Scanner;

public class TreatmentUI {
    private final TreatmentControl control;
    private final DoctorControl doctorControl;
    private final PatientControl patientControl;
    private final ConsultationControl consultationControl;
    private final ClinicADT<Consultation> consultations;
    private final ClinicADT<MedicalTreatment> treatments;
    private final Scanner scanner;

    public TreatmentUI(PatientControl patientControl, DoctorControl doctorControl,
                       ClinicADT<Consultation> consultations, ClinicADT<MedicalTreatment> treatments, ConsultationControl consultationControl) {
        this.control = new TreatmentControl();
        this.patientControl = patientControl;
        this.doctorControl = doctorControl;
        this.consultations = consultations;
        this.treatments = treatments;
        this.consultationControl = consultationControl;
        this.scanner = new Scanner(System.in);
    }

    public void run(){
        int choice;
        do {
            System.out.println("\n=== Medical Treatment Management ===");
            System.out.println("1. Add New Treatment");
            System.out.println("2. View Patient Treatment History");
            System.out.println("3. Process Next Follow-Up");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");

            try {
                choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1 -> addTreatment();
                    case 2 -> viewPatientHistory();
                    case 3 -> processFollowUp();
                    case 0 -> System.out.println("Returning to main menu...");
                    default -> System.out.println("Invalid choice.");
                }
            } catch(Exception e) {
                System.out.println("Error! Invalid input. Please try again.");
                choice = -1;
            }
        } while (choice != 0);
    }

    private void addTreatment() {
        System.out.println("\n=== Add New Treatment ===");
        System.out.println("📋 This will guide you through scheduling a medical treatment");
        System.out.println("⏰ Each treatment takes 2 hours");
        System.out.println("🩺 Only available doctors during working hours will be shown\n");

        BookingUI bookingUI = new BookingUI(patientControl, doctorControl, consultations, treatments, consultationControl);
        bookingUI.run(false); // false = treatment mode
    }

    private void viewPatientHistory() {
        System.out.print("Enter Patient ID: ");
        String patientId = scanner.nextLine();
        ClinicADT<MedicalTreatment> result = control.getTreatmentsByPatient(patientId);

        if (result.isEmpty()) {
            System.out.println("No treatment record found.");
        } else {
            System.out.println("\n=== Treatment History ===");
            for (int i = 0; i < result.size(); i++) {
                System.out.println(result.get(i));
            }
        }
    }

    private void processFollowUp() {
        try {
            var next = control.processNextFollowUp();
            if (next == null) {
                System.out.println("No follow-ups in queue.");
            } else {
                System.out.println("Processing follow-up:\n" + next);
            }
        } catch (Exception e) {
            System.out.println("Error! " + e.getMessage());
        }
    }
}
