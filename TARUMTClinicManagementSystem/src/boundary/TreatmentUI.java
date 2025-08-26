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

    // Reuse a single BookingUI and wire it into TreatmentControl for rescheduling flow
    private final BookingUI bookingUI;

    public TreatmentUI(PatientControl patientControl, DoctorControl doctorControl,
                       ClinicADT<Consultation> consultations,
                       ClinicADT<MedicalTreatment> treatments,
                       ConsultationControl consultationControl) {
        this.patientControl = patientControl;
        this.doctorControl = doctorControl;
        this.consultations = consultations;
        this.treatments = treatments;
        this.consultationControl = consultationControl;
        this.control = new TreatmentControl(treatments);
        this.scanner = new Scanner(System.in);

        // Create BookingUI once and inject it into TreatmentControl
        this.bookingUI = new BookingUI(
            this.patientControl,
            this.doctorControl,
            this.consultations,
            this.treatments,
            this.consultationControl,
            this.control
        );
        this.control.setBookingUI(this.bookingUI);
    }
    

    public void run() {
        int choice;
        do {
            System.out.println("\n=== Medical Treatment Management ===");
            System.out.println("1. Add New Treatment");
            System.out.println("2. Process Treatment (Scheduled Patients Only)");
            System.out.println("3. View Patient Treatment History");
            System.out.println("4. View Future Treatment Appointments");
            System.out.println("5. Process Follow-Up (Reschedule Overdue Treatments)");
            System.out.println("6. List All Treatments (Sorted)");
            System.out.println("7. View Overdue Bookings");
            System.out.println("8. Treatment Analysis Report");
            System.out.println("9. Treatment Frequency Distribution Report");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");

            try {
                choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1 -> addTreatment();
                    case 2 -> processScheduledTreatment();
                    case 3 -> viewPatientHistory();
                    case 4 -> viewFutureAppointment();
                    case 5 -> processFollowUp();
                    case 6 -> control.printAllTreatmentsSortedByDate();
                    case 7 -> control.printFollowUpQueue();
                    case 8 -> analysisReport();
                    case 9 -> frequencyDistributionReport();
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

        // Reuse shared BookingUI (already injected into TreatmentControl)
        bookingUI.run(false); // false = treatment mode
    }

    // === Process SCHEDULED treatments only (not overdue) ===
    private void processScheduledTreatment() {
        System.out.println("\n=== Process Treatment (Scheduled Patients Only) ===");
        System.out.println("Note: Overdue treatments cannot be processed here. Use Follow-up option instead.\n");

        ClinicADT<MedicalTreatment> pending = control.getAllPendingBookings();
        if (pending.isEmpty()) {
            System.out.println("No scheduled treatments to process.");
            System.out.println("(Overdue treatments require rescheduling via Follow-up option)");
            return;
        }

        String line = "+--------------+------------+----------------------+----------+-------------------+----------+";
        String head = "| %-12s | %-10s | %-20s | %-8s | %-17s | %-8s |%n";
        String row  = "| %-12s | %-10s | %-20s | %-8s | %-17s | %-8s |%n";

        System.out.println(line);
        System.out.printf(head, "Treatment ID", "Patient ID", "Patient Name", "Doctor", "Date & Time", "Status");
        System.out.println(line);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime now = LocalDateTime.now();

        ClinicADT.MyIterator<MedicalTreatment> it = pending.iterator();
        while (it.hasNext()) {
            MedicalTreatment t = it.next();
            String dateStr = t.getTreatmentDateTime().format(fmt);
            String status = t.getTreatmentDateTime().isAfter(now) ? "Ready" : "Due Now";

            System.out.printf(row,
                    t.getTreatmentId(),
                    t.getPatientId(),
                    t.getPatientName(),
                    t.getDoctorId(),
                    dateStr,
                    status);
        }
        System.out.println(line);

        int tid;
        while (true) {
            System.out.print("Enter Treatment ID to process (or 0 to cancel): ");
            String in = scanner.nextLine().trim();
            try {
                tid = Integer.parseInt(in);
                if (tid == 0) {
                    System.out.println("Operation cancelled.");
                    return;
                }
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Try again.");
            }
        }

        boolean ok = control.processBookedTreatmentById(tid);
        if (ok) {
            System.out.println("Treatment ID " + tid + " marked as COMPLETED.");
        } else {
            System.out.println("Treatment ID " + tid + " could not be processed.");
            System.out.println("   (May be overdue or already completed)");
        }
    }

    // ONLY PAST treatments with enhanced status display
    private void viewPatientHistory() {
        System.out.println("\n=== Patient List (With Treatment History) ===");
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
        System.out.println("\n=== Treatment History ===");

        if (result.isEmpty()) {
            System.out.println("No past treatment records found for patient: " + patientId);
            return;
        }

        printTreatmentTableWithStatus(result);
    }

    // === UPDATED: direct list of ALL future treatments (no Patient ID prompt) ===
    private void viewFutureAppointment() {
        System.out.println("\n=== Upcoming Treatments (All Patients) ===");

        ClinicADT<MedicalTreatment> future = new adt.MyClinicADT<>();
        LocalDateTime now = LocalDateTime.now();

        ClinicADT.MyIterator<MedicalTreatment> it = treatments.iterator();
        while (it.hasNext()) {
            MedicalTreatment t = it.next();
            if (t.getTreatmentDateTime().isAfter(now)) {
                future.add(t);
            }
        }

        if (future.isEmpty()) {
            System.out.println("No upcoming treatment records found.");
            return;
        }

        printTreatmentTableWithStatus(future);
    }

    // Enhanced treatment table with proper status indicators
    private void printTreatmentTableWithStatus(ClinicADT<MedicalTreatment> treatments) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime now = LocalDateTime.now();

        String line = "+--------------+------------+-----------------+------------+-------------------+-----------+";
        String headerFormat = "| %-12s | %-10s | %-15s | %-10s | %-17s | %-9s |%n";
        String rowFormat = "| %-12s | %-10s | %-15s | %-10s | %-17s | %-9s |%n";

        System.out.println(line);
        System.out.printf(headerFormat, "Treatment ID", "Patient ID", "Patient Name", "Doctor ID", "Date", "Status");
        System.out.println(line);

        ClinicADT.MyIterator<MedicalTreatment> iter = treatments.iterator();
        while (iter.hasNext()) {
            MedicalTreatment t = iter.next();

            String dateStr = t.getTreatmentDateTime().format(formatter);
            String status;

            if (t.isCompleted()) {
                status = "Completed";
            } else if (t.getTreatmentDateTime().isBefore(now)) {
                status = "Overdue";
            } else {
                status = "Pending";
            }

            System.out.printf(rowFormat,
                t.getTreatmentId(),
                t.getPatientId(),
                t.getPatientName(),
                t.getDoctorId(),
                dateStr,
                status);
        }
        System.out.println(line);
    }

    // Enhanced follow-up processing with rescheduling (uses injected BookingUI)
    private void processFollowUp() {
        System.out.println("\n=== Process Follow-Up (Reschedule Overdue Treatments) ===");
        System.out.println("This option allows you to reschedule and process overdue treatments.");
        System.out.println("Overdue treatments must be rescheduled before they can be processed.\n");

        MedicalTreatment processed = control.processNextFollowUp(); // uses bookingUI set in constructor
        if (processed == null) {
            System.out.println("No follow-up treatments processed.");
        }
    }

    private void analysisReport() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime now = LocalDateTime.now();
        Report.printHeader("Treatment Analysis Report");

        String line = "+---------------+--------------+-------------------+----------------+----------------+-------------------+--------------+";
        String headerFormat = "| %-13s | %-12s | %-17s | %-14s | %-14s | %-17s | %-12s |%n";
        String rowFormat    = "| %-13s | %-12s | %-17s | %-14s | %-14s | %-17s | %-12s |%n";

        System.out.println(line);
        System.out.printf(headerFormat,
            "Treatment ID", "Patient ID", "Patient Name", "Diagnosis", "Prescription", "Date & Time", "Status");
        System.out.println(line);

        ClinicADT.MyIterator<MedicalTreatment> iter = treatments.iterator();
        while (iter.hasNext()) {
            MedicalTreatment t = iter.next();

            String treatmentId   = String.valueOf(t.getTreatmentId());
            String patientId     = t.getPatientId();
            String patientName   = t.getPatientName();
            String diagnosis     = t.getDiagnosis();
            String prescription  = t.getPrescription();

            if ("to be diagnosed during appointment".equalsIgnoreCase(diagnosis)) {
                diagnosis = "TBD";
            }
            if ("to be prescribed during appointment".equalsIgnoreCase(prescription)) {
                prescription = "TBD";
            }

            String dateStr = t.getTreatmentDateTime().format(formatter);
            String status;

            if (t.isCompleted()) {
                status = "Completed";
            } else if (t.getTreatmentDateTime().isBefore(now)) {
                status = "Overdue";
            } else {
                status = "Scheduled";
            }

            System.out.printf(rowFormat,
                treatmentId,
                patientId,
                patientName,
                diagnosis,
                prescription,
                dateStr,
                status);
        }

        System.out.println(line);

        int totalTreatments = 0;
        int completedTreatments = 0;
        int overdueTreatments = 0;
        int scheduledTreatments = 0;

        ClinicADT.MyIterator<MedicalTreatment> statIter = treatments.iterator();
        while (statIter.hasNext()) {
            MedicalTreatment t = statIter.next();
            totalTreatments++;

            if (t.isCompleted()) {
                completedTreatments++;
            } else if (t.getTreatmentDateTime().isBefore(now)) {
                overdueTreatments++;
            } else {
                scheduledTreatments++;
            }
        }

        System.out.println("\n=== Treatment Summary ===");
        System.out.println("Total Treatments: " + totalTreatments);
        System.out.println("Completed: " + completedTreatments + " (" +
            (totalTreatments > 0 ? String.format("%.1f%%", (completedTreatments * 100.0 / totalTreatments)) : "0%") + ")");
        System.out.println("Overdue: " + overdueTreatments + " (" +
            (totalTreatments > 0 ? String.format("%.1f%%", (overdueTreatments * 100.0 / totalTreatments)) : "0%") + ")");
        System.out.println("Scheduled: " + scheduledTreatments + " (" +
            (totalTreatments > 0 ? String.format("%.1f%%", (scheduledTreatments * 100.0 / totalTreatments)) : "0%") + ")");

        Report.printFooter();
    }

    private void frequencyDistributionReport() {
        Report.printHeader("Treatment Frequency Distribution Report");

        String line = "+-------------------+------------+";
        String headerFormat = "| %-17s | %-10s |%n";
        String rowFormat    = "| %-17s | %-10d |%n";

        System.out.println("\nDiagnosis Frequency Distribution:");
        System.out.println(line);
        System.out.printf(headerFormat, "Diagnosis", "Count");
        System.out.println(line);

        StringBuilder seenDiagnoses = new StringBuilder();
        StringBuilder diagnosisBars = new StringBuilder();

        ClinicADT.MyIterator<MedicalTreatment> outer = treatments.iterator();
        while (outer.hasNext()) {
            MedicalTreatment t = outer.next();

            String diagnosis = t.getDiagnosis();
            if ("to be diagnosed during appointment".equalsIgnoreCase(diagnosis)) {
                diagnosis = "TBD";
            }

            if (seenDiagnoses.toString().contains("|" + diagnosis + "|")) {
                continue;
            }

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
            diagnosisBars.append(String.format("%-17s (%d) : %s%n", diagnosis, count, "*".repeat(count)));
        }
        System.out.println(line);

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
                continue;
            }

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
            prescriptionBars.append(String.format("%-17s (%d) : %s%n", prescription, count, "*".repeat(count)));
        }
        System.out.println(line);

        System.out.println("\nDiagnosis Frequency Chart:");
        System.out.println("============================");
        System.out.println(diagnosisBars.toString());

        System.out.println("Prescription Frequency Chart:");
        System.out.println("=============================");
        System.out.println(prescriptionBars.toString());

        Report.printFooter();
    }
}
