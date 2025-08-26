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
import java.time.LocalDateTime;
import utility.Validation;
import utility.Report;

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
            System.out.println("3. View Future Treatment Appointments"); 
            System.out.println("4. Process Next Follow-Up");
            System.out.println("5. List All Treatments (Sorted)");
            System.out.println("6. View Overdue Bookings ");
            System.out.println("7. Treatment Analysis Report");
            System.out.println("8. Treatment Frequency Distribution Report");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");

            try {
                choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1 -> addTreatment();
                    case 2 -> viewPatientHistory();
                    case 3 -> viewFutureAppointment();
                    case 4 -> processFollowUp();
                    case 5 -> control.printAllTreatmentsSortedByDate();
                    case 6 -> control.printFollowUpQueue();
                    case 7 -> analysisReport();
                    case 8 -> frequencyDistributionReport();
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
        System.out.println("->Each treatment takes 2 hours.");
        System.out.println("->Only available doctors during working hours will be shown.");
        System.out.println("->Only diagnosed patients are selectable for treatment.\n");
        
        // Pass control to BookingUI so treatment can be saved
        BookingUI bookingUI = new BookingUI(
            patientControl, doctorControl, consultations, treatments,
            consultationControl, control
        );
        bookingUI.run(false); // false = treatment mode
    }
    
    // ONLY PAST , DOES NOT INCLUDE FUTURE
    private void viewPatientHistory() { 
        System.out.println("\n=== Patient List ===");
        String patientFormat = "| %-10s | %-20s |\n";
        String patientLine = "+------------+----------------------+"; 

        System.out.println(patientLine);
        System.out.printf(patientFormat, "Patient ID", "Patient Name");
        System.out.println(patientLine);

        for (int i = 0; i < patientControl.getSize(); i++) {
            Patient p = patientControl.getPatient(i);
            ClinicADT<MedicalTreatment> history = control.getTreatmentsByPatient(p.getId(), false);
            if (!history.isEmpty()) {
                System.out.printf(patientFormat, p.getId(), p.getName());
            }
        }
        System.out.println(patientLine);

        String patientId;
        String error;

        do {
            System.out.print("\nEnter Patient ID (or 0 to cancel): ");
            patientId = scanner.nextLine().trim().toUpperCase();

            if (patientId.equals("0")) {
                System.out.println("Operation cancelled.");
                return;
            }

            error = Validation.validatePatientId(patientId);
            if (error != null) {
                System.out.println(error);
            }
        } while (error != null);

        ClinicADT<MedicalTreatment> result = control.getTreatmentsByPatient(patientId, false);
        System.out.println("\n=== Treatment Record ===");

        if (result.isEmpty()) {
            System.out.println("No past treatment records found for patient: " + patientId);
            return;
        }

        printTreatmentTable(result);
    }
    
    // ONLY FUTURE, DOES NOT INCLUDE PAST
    private void viewFutureAppointment() {
        System.out.println("\n=== Patient List (Future Appointments Only) ===");
        String patientFormat = "| %-10s | %-20s |\n";
        String patientLine = "+------------+----------------------+";

        System.out.println(patientLine);
        System.out.printf(patientFormat, "Patient ID", "Patient Name");
        System.out.println(patientLine);

        for (int i = 0; i < patientControl.getSize(); i++) {
            Patient p = patientControl.getPatient(i);
            ClinicADT<MedicalTreatment> futureAppointments = control.getTreatmentsByPatient(p.getId(), true); 
            if (!futureAppointments.isEmpty()) {
                System.out.printf(patientFormat, p.getId(), p.getName());
            }
        }
        System.out.println(patientLine);

        String patientId;
        String error;

        do {
            System.out.print("\nEnter Patient ID (or 0 to cancel): ");
            patientId = scanner.nextLine().trim().toUpperCase();

            if (patientId.equals("0")) {
                System.out.println("Operation cancelled.");
                return;
            }

            error = Validation.validatePatientId(patientId);
            if (error != null) {
                System.out.println(error);
            }
        } while (error != null);

        ClinicADT<MedicalTreatment> future = control.getTreatmentsByPatient(patientId, true); // future only
        System.out.println("\n=== Upcoming Treatments ===");

        if (future.isEmpty()) {
            System.out.println("No upcoming treatment records found for patient: " + patientId);
            return;
        }

        printTreatmentTable(future);
    }
    
    private void printTreatmentTable(ClinicADT<MedicalTreatment> treatments) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime now = LocalDateTime.now();

        // Table formatting
        String line = "+--------------+------------+-----------------+------------+-------------------+-----------+";
        String headerFormat = "| %-12s | %-10s | %-15s | %-10s | %-17s | %-9s |%n";
        String rowFormat = "| %-12s | %-10s | %-15s | %-10s | %-17s | %-9s |%n";

        
        System.out.println(line);
        System.out.printf(headerFormat, "Treatment ID", "Patient ID", "Patient Name", "Doctor ID", "Date", "Completed");
        System.out.println(line);

        ClinicADT.MyIterator<MedicalTreatment> iter = treatments.iterator();
        while (iter.hasNext()) {
            MedicalTreatment t = iter.next();

            // Format date with upcoming indicator
            String dateStr = t.getTreatmentDateTime().format(formatter);
            String dateDisplay = t.getTreatmentDateTime().isAfter(now) 
                ? dateStr + " ▲"  // Upcoming symbol
                : dateStr;

            // Highlight overdue follow-ups
            String completedDisplay = t.isCompleted() 
                ? "Yes" 
                : (t.getTreatmentDateTime().isBefore(now) ? "No (Overdue)" : "No");

            System.out.printf(rowFormat,
                t.getTreatmentId(),
                t.getPatientId(),
                t.getPatientName(),
                t.getDoctorId(),
                dateDisplay,
                completedDisplay);
        }
        System.out.println(line);
    }

    private void processFollowUp() {
        MedicalTreatment next = control.processNextFollowUp();
        if (next != null) {
            System.out.println("Processing follow-up treatment:");
            System.out.println("   ->ID       : " + next.getTreatmentId());
            System.out.println("   ->Patient  : " + next.getPatientName() + " (" + next.getPatientId() + ")");
            System.out.println("   ->Doctor   : " + next.getDoctorId());
            System.out.println("   ->Date     : " + next.getTreatmentDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            System.out.println("   ->Completed: " + (next.isCompleted() ? "Yes" : "No"));
        } else {
            System.out.println("No follow-up treatments in queue.");
        }
    }
    private void analysisReport() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime now = LocalDateTime.now();
        Report.printHeader("Treatment Analysis Report");

        // Table formatting (shortened columns)
        String line = "+---------------+--------------+-------------------+----------------+----------------+-------------------+--------------+";
        String headerFormat = "| %-13s | %-12s | %-17s | %-14s | %-14s | %-17s | %-12s |%n";
        String rowFormat    = "| %-13s | %-12s | %-17s | %-14s | %-14s | %-17s | %-12s |%n";

        // Print header
        System.out.println(line);
        System.out.printf(headerFormat, 
            "Treatment ID", "Patient ID", "Patient Name", "Diagnosis", "Prescription", "Date & Time", "Completed");
        System.out.println(line);

        ClinicADT.MyIterator<MedicalTreatment> iter = treatments.iterator();
        while (iter.hasNext()) {
            MedicalTreatment t = iter.next();

            // Extract fields
            String treatmentId   = String.valueOf(t.getTreatmentId());
            String patientId     = t.getPatientId();
            String patientName   = t.getPatientName();
            String diagnosis     = t.getDiagnosis();
            String prescription  = t.getPrescription();

            // --- Shorten placeholder values ---
            if ("to be diagnosed during appointment".equalsIgnoreCase(diagnosis)) {
                diagnosis = "TBD";
            }
            if ("to be prescribed during appointment".equalsIgnoreCase(prescription)) {
                prescription = "TBD";
            }

            String dateStr = t.getTreatmentDateTime().format(formatter);
            String dateDisplay = t.getTreatmentDateTime().isAfter(now) 
                    ? dateStr + " ▲"   // Mark upcoming treatments
                    : dateStr;

            // Completion handling
            String completedDisplay = t.isCompleted()
                    ? "Yes"
                    : (t.getTreatmentDateTime().isBefore(now) ? "No (Overdue)" : "No");

            // Print row
            System.out.printf(rowFormat,
                treatmentId,
                patientId,
                patientName,
                diagnosis,
                prescription,
                dateDisplay,
                completedDisplay);
        }

        System.out.println(line);
        Report.printFooter();
    }
    private void frequencyDistributionReport() {
        Report.printHeader("Treatment Frequency Distribution Report");

        // --- Table formatting ---
        String line = "+-------------------+------------+";
        String headerFormat = "| %-17s | %-10s |%n";
        String rowFormat    = "| %-17s | %-10d |%n";

        // ================================
        // Diagnosis Frequency Distribution
        // ================================
        System.out.println("\nDiagnosis Frequency Distribution:");
        System.out.println(line);
        System.out.printf(headerFormat, "Diagnosis", "Count");
        System.out.println(line);

        StringBuilder seenDiagnoses = new StringBuilder();
        StringBuilder diagnosisBars = new StringBuilder(); // store chart

        ClinicADT.MyIterator<MedicalTreatment> outer = treatments.iterator();
        while (outer.hasNext()) {
            MedicalTreatment t = outer.next();

            String diagnosis = t.getDiagnosis();
            if ("to be diagnosed during appointment".equalsIgnoreCase(diagnosis)) {
                diagnosis = "TBD";
            }

            if (seenDiagnoses.toString().contains("|" + diagnosis + "|")) {
                continue; // already counted
            }

            // Count occurrences
            int count = 0;
            ClinicADT.MyIterator<MedicalTreatment> inner = treatments.iterator();
            while (inner.hasNext()) {
                MedicalTreatment ti = inner.next();
                String d = ti.getDiagnosis();
                if ("to be diagnosed during appointment".equalsIgnoreCase(d)) {
                    d = "TBD";
                }
                if (d.equalsIgnoreCase(diagnosis)) {
                    count++;
                }
            }

            seenDiagnoses.append("|").append(diagnosis).append("|");
            System.out.printf(rowFormat, diagnosis, count);

            // Add to bar chart
            diagnosisBars.append(String.format("%-17s (%d) : %s%n", diagnosis, count, "*".repeat(count)));
        }
        System.out.println(line);

        // ===================================
        // Prescription Frequency Distribution
        // ===================================
        System.out.println("\nPrescription Frequency Distribution:");
        System.out.println(line);
        System.out.printf(headerFormat, "Prescription", "Count");
        System.out.println(line);

        StringBuilder seenPrescriptions = new StringBuilder();
        StringBuilder prescriptionBars = new StringBuilder();

        outer = treatments.iterator();
        while (outer.hasNext()) {
            MedicalTreatment t = outer.next();

            String prescription = t.getPrescription();
            if ("to be prescribed during appointment".equalsIgnoreCase(prescription)) {
                prescription = "TBD";
            }

            if (seenPrescriptions.toString().contains("|" + prescription + "|")) {
                continue; // already counted
            }

            // Count occurrences
            int count = 0;
            ClinicADT.MyIterator<MedicalTreatment> inner = treatments.iterator();
            while (inner.hasNext()) {
                MedicalTreatment ti = inner.next();
                String p = ti.getPrescription();
                if ("to be prescribed during appointment".equalsIgnoreCase(p)) {
                    p = "TBD";
                }
                if (p.equalsIgnoreCase(prescription)) {
                    count++;
                }
            }

            seenPrescriptions.append("|").append(prescription).append("|");
            System.out.printf(rowFormat, prescription, count);

            // Add to bar chart
            prescriptionBars.append(String.format("%-17s (%d) : %s%n", prescription, count, "*".repeat(count)));
        }
        System.out.println(line);

        // =========================
        // Bar Chart Displays
        // =========================
        System.out.println("\nDiagnosis Frequency Chart:");
        System.out.println("============================");
        System.out.println(diagnosisBars.toString());

        System.out.println("Prescription Frequency Chart:");
        System.out.println("=============================");
        System.out.println(prescriptionBars.toString());

        Report.printFooter();
    }
}
