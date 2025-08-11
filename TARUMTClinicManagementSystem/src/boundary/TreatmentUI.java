package boundary;

import adt.ClinicADT;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import entity.Consultation;
import control.ConsultationControl;
import control.DoctorControl;
import entity.MedicalTreatment;
import entity.Patient;
import control.PatientControl;
import control.TreatmentControl;
import utility.Validation;

public class TreatmentUI {
    private final TreatmentControl control;
    private final DoctorControl doctorControl;
    private final PatientControl patientControl;
    private final ConsultationControl consultationControl;
    private final ClinicADT<Consultation> consultations;
    private final ClinicADT<MedicalTreatment> treatments;
    private final Scanner scanner;

    public TreatmentUI(PatientControl patientControl, DoctorControl doctorControl,
                       ClinicADT<Consultation> consultations,
                       ClinicADT<MedicalTreatment> treatments,
                       ConsultationControl consultationControl) {
        this.patientControl = patientControl;
        this.doctorControl = doctorControl;
        this.consultations = consultations;
        this.treatments = treatments;
        this.consultationControl = consultationControl;
        this.control = new TreatmentControl(treatments); // Shared treatment list
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        int choice;
        do {
            System.out.println("\n=== Medical Treatment Management ===");
            System.out.println("1. Add New Treatment");
            System.out.println("2. View Patient Treatment History");
            System.out.println("3. Process Next Follow-Up");
            System.out.println("4. List All Treatments (Sorted)");
            System.out.println("5. View Follow-Up Queue");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");

            try {
                choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1 -> addTreatment();
                    case 2 -> viewPatientHistory();
                    case 3 -> processFollowUp();
                    case 4 -> control.printAllTreatmentsSortedByDate();
                    case 5 -> control.printFollowUpQueue();
                    case 0 -> System.out.println("Returning to main menu...");
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("Error! Invalid input. Please try again.");
                choice = -1;
            }
        } while (choice != 0);
    }

    private void addTreatment() {
        System.out.println("\n=== Add New Treatment ===");
        System.out.println("Guided Medical Treatment Scheduling");
        System.out.println("->You will select patient, doctor, and treatment time.");
        System.out.println("->Each treatment takes 2 hours");
        System.out.println("->Only available doctors during working hours will be shown\n");

        // ✅ Pass `control` to BookingUI so treatment can be saved to file
        BookingUI bookingUI = new BookingUI(
            patientControl, doctorControl, consultations, treatments,
            consultationControl, control
        );
        bookingUI.run(false); // false = treatment mode
    }

    private void viewPatientHistory() {
        System.out.println("\n=== Patient List ===");
        String patientFormat = "| %-10s | %-20s |\n";
        String patientLine = "+------------+----------------------+";

        System.out.println(patientLine);
        System.out.printf(patientFormat, "Patient ID", "Patient Name");
        System.out.println(patientLine);

        for (int i = 0; i < patientControl.getSize(); i++) {
            Patient p = patientControl.getPatient(i);
            System.out.printf(patientFormat, p.getId(), p.getName());
        }

        System.out.println(patientLine);
        
        String patientId;
        String error;

        do {
            System.out.print("\nEnter Patient ID (or 0 to cancel): ");
            patientId = scanner.nextLine().trim().toUpperCase();

            if (patientId.equals("0")) {
                System.out.println("Operation cancelled.");
                patientId = null;  // or return/throw depending on context
                break;
            }

            error = Validation.validatePatientId(patientId);
            if (error != null) {
                System.out.println(error);
            }
        } while (error != null);

        ClinicADT<MedicalTreatment> result = control.getTreatmentsByPatient(patientId);

        if (result.isEmpty()) {
            System.out.println("No treatment record found for patient: " + patientId);
        } else {
            System.out.println("\n=== Treatment History for " + patientId + " ===");
            String format = "| %-11s | %-15s | %-10s | %-19s | %-9s |\n";
            String line = "+-------------+-----------------+------------+---------------------+-----------+";

            System.out.println(line);
            System.out.printf(format, "Treatment ID", "Patient Name", "Doctor ID", "Date", "Completed");
            System.out.println(line);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            for (MedicalTreatment t : result) {
                System.out.printf(format,
                    t.getTreatmentId(),
                    t.getPatientName(),
                    t.getDoctorId(),
                    t.getTreatmentDateTime().format(formatter),
                    t.isCompleted() ? "Yes" : "No"
                );
            }
            System.out.println(line);
        }
    }

    private void processFollowUp() {
        MedicalTreatment next = control.processNextFollowUp();
        if (next != null) {
            System.out.println("Processing follow-up treatment:");
            System.out.println("   ->ID       : " + next.getTreatmentId());
            System.out.println("   ->Patient  : " + next.getPatientName() + " (" + next.getPatientId() + ")");
            System.out.println("   -> Doctor   : " + next.getDoctorId());
            System.out.println("   -> Date     : " + next.getTreatmentDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            System.out.println("   -> Completed: " + (next.isCompleted() ? "Yes" : "No"));
        }
    }
}
