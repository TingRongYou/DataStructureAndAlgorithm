package boundary;

import adt.ClinicADT;
import entity.Consultation;
import entity.Doctor;
import entity.MedicalTreatment;
import entity.Patient;
import control.ConsultationControl;
import control.DoctorControl;
import control.PatientControl;
import control.TreatmentControl;
import utility.Report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class ConsultationUI {
    private ConsultationControl consultationControl;
    private TreatmentControl treatmentControl;
    private Scanner sc;
    private PatientControl patientControl;
    private DoctorControl doctorControl;
    private ClinicADT<Consultation> consultations;
    private ClinicADT<MedicalTreatment> treatments;

    public ConsultationUI(PatientControl patientControl, DoctorControl doctorControl,
                          ClinicADT<Consultation> consultations, ClinicADT<MedicalTreatment> treatments) {
        this.patientControl = patientControl;
        this.doctorControl = doctorControl;
        this.consultations = consultations;
        this.treatments = treatments;
        this.consultationControl = new ConsultationControl(patientControl, doctorControl, consultations, treatments);
        this.treatmentControl = new TreatmentControl(treatments);
        this.sc = new Scanner(System.in);
        consultationControl.loadConsultationsFromFile();
    }

    public void run() {
        while (true) {
            displayMenu();
            int choice = getValidChoice();

            switch (choice) {
                case 1 -> addConsultation();
                case 2 -> removeConsultation();
                case 3 -> processConsultation();                 // requires a called patient
                case 4 -> consultationControl.listConsultations();
                case 5 -> searchByPatient();
                case 6 -> searchByDoctor();
                case 7 -> checkDoctorAvailability();
                case 8 -> showWorkingHours();
                case 9 -> consultationsSortedByDate();
                case 10 -> frequencyDistributionReport();
                case 11 -> callNextFromQueue();                 // call next patient (FIFO)
                case 12 -> consultationControl.viewNextPatientInQueue();
                case 13 -> consultationControl.displayQueuedPatients();
                case 0 -> {
                    System.out.println("Exiting Consultation Module...");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n===== Consultation Management =====");
        System.out.println(" 1. Add Consultation");
        System.out.println(" 2. Remove Consultation");
        System.out.println(" 3. Process Consultation");
        System.out.println(" 4. List All Consultations");
        System.out.println(" 5. Search by Patient");
        System.out.println(" 6. Search by Doctor");
        System.out.println(" 7. Check Doctor Availability for Date");
        System.out.println(" 8. View Working Hours");
        System.out.println(" 9. Consultation Analysis Report (Sorted by Date)");
        System.out.println("10. Consultation Frequency Distribution Report");
        System.out.println("11. Call Next Patient (FIFO Queue)");
        System.out.println("12. View Next Patient In the Queue");
        System.out.println("13. Display All Patients (Queue Order)");
        System.out.println(" 0. Exit");
        System.out.print("Choice: ");
    }

    private int getValidChoice() {
        while (true) {
            try {
                int choice = sc.nextInt();
                sc.nextLine(); // consume newline
                return choice;
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a number (0-13): ");
                sc.nextLine(); // clear invalid input
            }
        }
    }

    private void addConsultation() {
        System.out.println("\n=== Add New Consultation ===");
        System.out.println("Each consultation is 1 hour long.");
        System.out.println("Only available doctors during working hours will be shown.\n");

        BookingUI bookingUI = new BookingUI(patientControl, doctorControl, consultations, treatments, consultationControl, treatmentControl);
        bookingUI.run(true); // true = consultation booking
    }

    private void removeConsultation() {
        System.out.println("\n=== Remove Consultation ===");
        consultationControl.listConsultations();

        if (consultationControl.getTotalConsultations() == 0) {
            System.out.println("No consultations available to remove.");
            return;
        }

        while (true) {
            try {
                System.out.print("Enter Consultation ID to remove (or 0 to cancel): ");
                int id = sc.nextInt();
                sc.nextLine(); // consume newline

                if (id == 0) {
                    System.out.println("Removal cancelled.");
                    return;
                }

                boolean removed = consultationControl.removeConsultationById(id);
                if (removed) {
                    System.out.println("Consultation removed successfully.");
                    return;
                } else {
                    System.out.println("Consultation ID not found. Please try again.");
                }

            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a valid number.");
                sc.nextLine(); // clear invalid input
            }
        }
    }

    // =====================================================
    // PROCESS FLOW — ONLY THE CURRENTLY CALLED PATIENT ALLOWED
    // =====================================================
    private void processConsultation() {
        System.out.println("\n=== Process Consultation ===");

        // Require a called consultation first
        Integer calledCid = consultationControl.getCurrentCalledConsultationId();
        if (calledCid == null) {
            System.out.println("No patient has been called. Use 'Call Next Patient (FIFO Queue)' first.");
            return;
        }

        String patientId = consultationControl.getCurrentCalledPatientId();
        if (patientId == null) {
            System.out.println("Internal error: called consultation has no patient.");
            return;
        }

        // Confirm there is still at least one pending consult for this patient
        if (!hasUnprocessedConsultation(patientId)) {
            System.out.println("No unprocessed consultations found for the called patient (" + patientId + ").");
            System.out.println("If this is unexpected, review the queue and bookings.");
            return;
        }

        // Delegate to control; it must enforce that only the CALLED consultation is processed
        // and clear the lock after successful processing.
        try {
            consultationControl.updateConsultation(patientId);

            Patient patient = patientControl.getPatientById(patientId);
            System.out.println("\nConsultation processed successfully!");
            System.out.println("Patient: " + (patient != null ? patient.getName() : "Unknown") + " (" + patientId + ")");
        } catch (Exception e) {
            System.out.println("Failed to process consultation: " + e.getMessage());
            System.out.println("Please check if the called consultation exists and try again.");
        }
    }

    // ===== Utils used elsewhere =====

    private void displayPatientsTable() {
        final String line = "+------------+----------------------+-----------------+----------------+-----+--------+";
        final String headerFmt = "| %-10s | %-20s | %-15s | %-14s | %3s | %-6s |%n";
        final String rowFmt = "| %-10s | %-20s | %-15s | %-14s | %3d | %-6s |%n";

        System.out.println(line);
        System.out.printf(headerFmt, "Patient ID", "Name", "IC Number", "Phone", "Age", "Gender");
        System.out.println(line);

        ClinicADT.MyIterator<Patient> patientIt = patientControl.getAllPatients().iterator();
        while (patientIt.hasNext()) {
            Patient p = patientIt.next();
            System.out.printf(rowFmt, p.getId(), p.getName(), p.getIcNumber(),
                    p.getContact(), p.getAge(), p.getGender());
        }
        System.out.println(line);
    }

    private String selectPatientId() {
        while (true) {
            System.out.print("Enter Patient ID to process (or 0 to cancel): ");
            String patientId = sc.nextLine().trim();

            if (patientId.equals("0")) {
                return null;
            }

            if (patientControl.getPatientById(patientId) != null) {
                return patientId;
            }

            System.out.println("Patient ID not found. Please try again.");
        }
    }

    // ============================
    // CALL NEXT FROM QUEUE (FIFO)
    // ============================
    private void callNextFromQueue() {
        // Ask control to call the next consultation in FIFO.
        if (!consultationControl.callNextFromQueue()) {
            // Control already printed the reason (queue empty / someone already called).
            return;
        }

        Integer cid = consultationControl.getCurrentCalledConsultationId();
        if (cid == null) {
            System.out.println("Internal error: no called consultation after call.");
            return;
        }

        Consultation c = findConsultationById(cid);
        if (c == null) {
            System.out.println("Internal error: called consultation not found in memory (ID " + cid + ").");
            return;
        }

        final String line       = "+--------------+------------+----------------------+---------------------+------------+";
        final String headerFmt  = "| %-12s | %-10s | %-20s | %-19s | %-10s |%n";
        final String rowFmt     = "| %-12d | %-10s | %-20s | %-19s | %-10s |%n";
        DateTimeFormatter fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String dt               = (c.getConsultationDate() != null) ? c.getConsultationDate().format(fmt) : "N/A";

        System.out.println("\n>> Called Consultation");
        System.out.println(line);
        System.out.printf(headerFmt, "Consult ID", "PatientID", "Patient Name", "Date & Time", "Status");
        System.out.println(line);
        System.out.printf(rowFmt,
                c.getId(),
                c.getPatientId(),
                truncateString(c.getPatientName(), 20),
                dt,
                statusOf(c));
        System.out.println(line);

        System.out.println("Now processing allowed ONLY for this consultation.");
        System.out.println("Use 'Process Consultation' to proceed.");
    }

    private Consultation findConsultationById(Integer id) {
        if (id == null) return null;
        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) {
            Consultation x = it.next();
            if (x != null && x.getId() == id) return x;
        }
        return null;
    }

    private boolean hasUnprocessedConsultation(String patientId) {
        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            if (c.getPatientId().equalsIgnoreCase(patientId)) {
                String diagnosis = c.getDiagnosis();
                if (diagnosis == null
                        || diagnosis.isBlank()
                        || diagnosis.equalsIgnoreCase("Pending")
                        || diagnosis.equalsIgnoreCase("To be diagnosed during appointment")) {
                    return true;
                }
            }
        }
        return false;
    }

    // ===== Search / Reports (unchanged) =====

    private void searchByPatient() {
        System.out.println("\n=== Search Consultations by Patient ===");

        ClinicADT.MyIterator<Patient> patientIt = patientControl.getAllPatients().iterator();
        if (!patientIt.hasNext()) {
            System.out.println("No registered patients found.");
            return;
        }

        displayPatientsTable();

        String patientId = selectPatientId();
        if (patientId == null) return;

        Patient selectedPatient = patientControl.getPatientById(patientId);
        displayPatientConsultationHistory(selectedPatient);
    }

    private void displayPatientConsultationHistory(Patient patient) {
        System.out.println("\n--- Consultation History for " + patient.getName() + " ---");

        final String line = "+--------------+----------------------+------------------------+---------------------+";
        final String headerFormat = "| %-12s | %-20s | %-22s | %-19s |%n";
        final String rowFormat = "| %-12d | %-20s | %-22s | %-19s |%n";

        System.out.println(line);
        System.out.printf(headerFormat, "Consult ID", "Doctor", "Date", "Diagnosis");
        System.out.println(line);

        boolean found = false;
        ClinicADT.MyIterator<Consultation> consultIt = consultations.iterator();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        while (consultIt.hasNext()) {
            Consultation c = consultIt.next();
            if (c.getPatientId() != null && c.getPatientId().trim().equalsIgnoreCase(patient.getId().trim())) {
                String dateStr = (c.getConsultationDate() != null)
                        ? c.getConsultationDate().format(fmt)
                        : "N/A";
                String diagnosis = (c.getDiagnosis() != null &&
                        !c.getDiagnosis().equalsIgnoreCase("To be diagnosed during appointment"))
                        ? c.getDiagnosis()
                        : "Pending";

                System.out.printf(rowFormat, c.getId(), c.getDoctorName(), dateStr, diagnosis);
                found = true;
            }
        }

        if (!found) {
            System.out.println("| No consultations found for this patient.                                        |");
        }
        System.out.println(line);
    }

    private void searchByDoctor() {
        System.out.println("\n=== Search Consultations by Doctor ===");

        ClinicADT.MyIterator<Doctor> doctorIt = doctorControl.getAllDoctors().iterator();
        if (!doctorIt.hasNext()) {
            System.out.println("No registered doctors found.");
            return;
        }

        displayDoctorsTable();

        String doctorId = selectDoctorId();
        if (doctorId == null) return;

        Doctor selectedDoctor = doctorControl.getDoctorById(doctorId);
        displayDoctorConsultations(selectedDoctor);
    }

    private void displayDoctorsTable() {
        final String line = "+------------+----------------------+---------+";
        final String headerFormat = "| %-10s | %-20s | %-7s |%n";
        final String rowFormat = "| %-10s | %-20s | %-7d |%n";

        System.out.println(line);
        System.out.printf(headerFormat, "Doctor ID", "Name", "Room");
        System.out.println(line);

        ClinicADT.MyIterator<Doctor> doctorIt = doctorControl.getAllDoctors().iterator();
        while (doctorIt.hasNext()) {
            Doctor d = doctorIt.next();
            System.out.printf(rowFormat, d.getId(), d.getName(), d.getRoomNumber());
        }
        System.out.println(line);
    }

    private String selectDoctorId() {
        while (true) {
            System.out.print("Enter Doctor ID to view consultations (or 0 to cancel): ");
            String doctorId = sc.nextLine().trim();

            if (doctorId.equals("0")) {
                return null;
            }

            if (doctorControl.getDoctorById(doctorId) != null) {
                return doctorId;
            }

            System.out.println("Doctor ID not found. Please try again.");
        }
    }

    private void displayDoctorConsultations(Doctor doctor) {
        System.out.println("\n--- Consultations for Dr. " + doctor.getName() + " ---");

        final String line = "+--------------+----------------------+------------------------+---------------------+";
        final String headerFormat = "| %-12s | %-20s | %-22s | %-19s |%n";
        final String rowFormat = "| %-12d | %-20s | %-22s | %-19s |%n";

        System.out.println(line);
        System.out.printf(headerFormat, "Consult ID", "Patient", "Date", "Diagnosis");
        System.out.println(line);

        boolean found = false;
        ClinicADT.MyIterator<Consultation> consultIt = consultations.iterator();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        while (consultIt.hasNext()) {
            Consultation c = consultIt.next();
            if (c.getDoctorName() != null && c.getDoctorName().trim().equalsIgnoreCase(doctor.getName().trim())) {
                String dateStr = (c.getConsultationDate() != null)
                        ? c.getConsultationDate().format(fmt)
                        : "N/A";
                String diagnosis = (c.getDiagnosis() != null &&
                        !c.getDiagnosis().equalsIgnoreCase("To be diagnosed during appointment"))
                        ? c.getDiagnosis()
                        : "Pending";

                System.out.printf(rowFormat, c.getId(), c.getPatientName(), dateStr, diagnosis);
                found = true;
            }
        }

        if (!found) {
            System.out.println("| No consultations found for this doctor.                                         |");
        }
        System.out.println(line);
    }

    private void checkDoctorAvailability() {
        System.out.println("\n=== Check Doctor Availability ===");

        while (true) {
            System.out.print("Enter date to check (yyyy-MM-dd) or 0 to cancel: ");
            String input = sc.nextLine().trim();

            if (input.equals("0")) {
                System.out.println("Operation cancelled.");
                return;
            }

            try {
                LocalDateTime date = LocalDateTime.parse(input + " 00:00",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                consultationControl.showDoctorScheduleForDate(date);
                return;
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use 'yyyy-MM-dd' (e.g., 2024-12-25).");
            }
        }
    }

    private void showWorkingHours() {
        System.out.println("\n=== Clinic Working Hours ===");
        System.out.println("+---------------------------------------------+");
        System.out.println("|                Shift Timings                |");
        System.out.println("+---------------------------------------------+");
        System.out.println("|  Morning Shift   : 08:00 - 12:00           |");
        System.out.println("|  Afternoon Shift : 13:00 - 17:00           |");
        System.out.println("|  Night Shift     : 18:00 - 22:00           |");
        System.out.println("+---------------------------------------------+");
        System.out.println();
        System.out.println("Important Information:");
        System.out.println("• Each consultation is exactly 1 hour long");
        System.out.println("• Consultations can only be scheduled during working hours");
        System.out.println("• Only doctors on duty during that shift are available");
        System.out.println("• 12:00 - 13:00 is lunch break (no appointments)");
        System.out.println();
        System.out.println("Tips for Booking:");
        System.out.println("• Check doctor availability before scheduling");
        System.out.println("• Avoid time conflicts with existing bookings");
        System.out.println("• Doctor schedules may vary by day");
    }

    public void consultationsSortedByDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        Report.printHeader("Consultation Analysis Report");

        final String line = "+--------+----------------------+-----------------+-----------------+---------------------+---------------------+";
        final String headerFormat = "| %-6s | %-20s | %-15s | %-15s | %-19s | %-19s |%n";
        final String rowFormat = "| %-6d | %-20s | %-15s | %-15s | %-19s | %-19s |%n";

        System.out.println(line);
        System.out.printf(headerFormat, "ID", "Patient", "Doctor", "Doctor ID", "Diagnosis", "Date & Time");
        System.out.println(line);

        boolean hasData = false;
        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();

        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            hasData = true;

            String diagnosis = (c.getDiagnosis() != null) ? c.getDiagnosis() : "N/A";
            if ("To be diagnosed during appointment".equalsIgnoreCase(diagnosis)) {
                diagnosis = "Pending";
            }

            String dateStr = (c.getConsultationDate() != null)
                    ? c.getConsultationDate().format(formatter)
                    : "N/A";

            System.out.printf(rowFormat,
                    c.getId(),
                    truncateString(c.getPatientName(), 20),
                    truncateString(c.getDoctorName(), 15),
                    truncateString(c.getDoctorId(), 15),
                    truncateString(diagnosis, 19),
                    dateStr
            );
        }

        if (!hasData) {
            System.out.println("| No consultation data available.                                                                     |");
        }

        System.out.println(line);
        Report.printFooter();
    }

    public void frequencyDistributionReport() {
        Report.printHeader("Consultation Frequency Distribution Report");

        generateDiagnosisFrequencyReport();
        generateDoctorFrequencyReport();

        Report.printFooter();
    }

    private void generateDiagnosisFrequencyReport() {
        System.out.println("\nDiagnosis Frequency Distribution:");
        System.out.println("=================================");

        final String line = "+-------------------------+-------+";
        final String headerFormat = "| %-23s | %-5s |%n";
        final String rowFormat = "| %-23s | %5d |%n";

        System.out.println(line);
        System.out.printf(headerFormat, "Diagnosis", "Count");
        System.out.println(line);

        StringBuilder seenDiagnoses = new StringBuilder();
        StringBuilder diagnosisChart = new StringBuilder();
        boolean hasData = false;

        ClinicADT.MyIterator<Consultation> outer = consultations.iterator();
        while (outer.hasNext()) {
            Consultation c = outer.next();

            String diagnosis = normalizeDiagnosis(c.getDiagnosis());
            String diagnosisKey = "|" + diagnosis.toLowerCase() + "|";

            if (seenDiagnoses.toString().contains(diagnosisKey)) {
                continue;
            }

            int count = countDiagnosisOccurrences(diagnosis);
            if (count > 0) {
                hasData = true;
                seenDiagnoses.append(diagnosisKey);

                System.out.printf(rowFormat, truncateString(diagnosis, 23), count);
                diagnosisChart.append(String.format("%-20s (%2d) : %s%n",
                        truncateString(diagnosis, 20), count, generateBar(count)));
            }
        }

        if (!hasData) {
            System.out.println("| No diagnosis data available.                   |");
        }
        System.out.println(line);

        if (hasData) {
            System.out.println("\nDiagnosis Frequency Chart:");
            System.out.println("===========================");
            System.out.print(diagnosisChart.toString());
        }
    }

    private void generateDoctorFrequencyReport() {
        System.out.println("\nDoctor Consultation Frequency:");
        System.out.println("==============================");

        final String line = "+-------------------------+-------+";
        final String headerFormat = "| %-23s | %-5s |%n";
        final String rowFormat = "| %-23s | %5d |%n";

        System.out.println(line);
        System.out.printf(headerFormat, "Doctor", "Count");
        System.out.println(line);

        StringBuilder seenDoctors = new StringBuilder();
        StringBuilder doctorChart = new StringBuilder();
        boolean hasData = false;

        ClinicADT.MyIterator<Consultation> outer = consultations.iterator();
        while (outer.hasNext()) {
            Consultation c = outer.next();

            String doctor = c.getDoctorName();
            if (doctor == null) continue;

            String doctorKey = "|" + doctor.toLowerCase() + "|";

            if (seenDoctors.toString().contains(doctorKey)) {
                continue;
            }

            int count = countDoctorConsultations(doctor);
            if (count > 0) {
                hasData = true;
                seenDoctors.append(doctorKey);

                System.out.printf(rowFormat, truncateString(doctor, 23), count);
                doctorChart.append(String.format("%-20s (%2d) : %s%n",
                        truncateString(doctor, 20), count, generateBar(count)));
            }
        }

        if (!hasData) {
            System.out.println("| No doctor consultation data available.         |");
        }
        System.out.println(line);

        if (hasData) {
            System.out.println("\nDoctor Consultation Frequency Chart:");
            System.out.println("====================================");
            System.out.print(doctorChart.toString());
        }
    }

    private String normalizeDiagnosis(String diagnosis) {
        if (diagnosis == null) return "No Diagnosis";
        if ("To be diagnosed during appointment".equalsIgnoreCase(diagnosis.trim())) {
            return "Pending";
        }
        return diagnosis.trim();
    }

    private int countDiagnosisOccurrences(String targetDiagnosis) {
        int count = 0;
        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            String diagnosis = normalizeDiagnosis(c.getDiagnosis());
            if (diagnosis.equalsIgnoreCase(targetDiagnosis)) {
                count++;
            }
        }
        return count;
    }

    private int countDoctorConsultations(String doctorName) {
        int count = 0;
        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            if (c.getDoctorName() != null && c.getDoctorName().equalsIgnoreCase(doctorName)) {
                count++;
            }
        }
        return count;
    }

    private String generateBar(int count) {
        return "".repeat(Math.min(count, 50)); // Limit bar length to 50 characters
    }

    private String truncateString(String str, int maxLength) {
        if (str == null) return "";
        return str.length() <= maxLength ? str : str.substring(0, maxLength - 3) + "...";
    }

    /** PENDING if diagnosis is null/blank/"Pending"/"To be diagnosed during appointment", else PROCESSED. */
    private String statusOf(Consultation c) {
        if (c == null) return "N/A";
        String d = (c.getDiagnosis() == null) ? "" : c.getDiagnosis().trim();
        if (d.isEmpty() || d.equalsIgnoreCase("Pending")
                || d.equalsIgnoreCase("To be diagnosed during appointment")) {
            return "PENDING";
        }
        return "PROCESSED";
    }
}
