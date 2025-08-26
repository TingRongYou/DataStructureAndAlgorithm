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
        this.sc = new Scanner(System.in);
        consultationControl.loadConsultationsFromFile();
    }

    public void run() {
        while (true) {
            System.out.println("\n===== Consultation Management =====");
            System.out.println(" 1. Add Consultation");
            System.out.println(" 2. Remove Consultation");
            System.out.println(" 3. Process Consultation");
            System.out.println(" 4. List All Consultations");
            System.out.println(" 5. Search by Patient");
            System.out.println(" 6. Search by Doctor");
            System.out.println(" 7. Check Doctor Availability for Date");
            System.out.println(" 8. View Working Hours");
            System.out.println(" 9. Consultation Analysis Report(Sorted by Date)"); //replace for now, might switch to report
            System.out.println("10. Consultation Frequency Distrbution Report");
            System.out.println("0. Exit");
            System.out.print("Choice: ");

            int ch = sc.nextInt();
            sc.nextLine(); // consume newline

            switch (ch) {
                case 1 -> addConsultation();
                case 2 -> removeConsultation();
                case 3 -> processConsultation();
                case 4 -> consultationControl.listConsultations();
                case 5 -> searchByPatient();
                case 6 -> searchByDoctor();
                case 7 -> checkDoctorAvailability();
                case 8 -> showWorkingHours();
                case 9 -> consultationsSortedByDate(); // switch to a report
                case 10 -> frequencyDistributionReport();
                case 0 -> {
                    System.out.println("Exiting Consultation Module...");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void addConsultation() {
        System.out.println("\n=== Add New Consultation ===");
        System.out.println("Each consultation is 1 hour long.");
        System.out.println("Only available doctors during working hours will be shown.\n");

        BookingUI bookingUI = new BookingUI(patientControl, doctorControl, consultations, treatments, consultationControl, treatmentControl);
        bookingUI.run(true); // true = only allow consultation
    }

    private void removeConsultation() {
        System.out.println("\n=== Remove Consultation ===");
        consultationControl.listConsultations();

        if (consultationControl.getTotalConsultations() == 0) return;

        while (true) {
            try {
                System.out.print("Enter Consultation ID to remove (or 0 to cancel): ");
                int id = sc.nextInt();
                sc.nextLine(); // consume newline

                if (id == 0) {
                    System.out.println("Removal cancelled.");
                    break;
                }

                boolean removed = consultationControl.removeConsultationById(id);
                if (removed) {
                    System.out.println("Consultation removed successfully.");
                    break;
                } else {
                    System.out.println("Consultation ID not found. Try again.");
                }

            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a valid number.");
                sc.nextLine(); // clear invalid input
            }
        }
    }
    
    private void processConsultation(){
        System.out.println("\n=== Process Consultation ===");

        ClinicADT.MyIterator<Patient> patientIt = patientControl.getAllPatients().iterator();
        if (!patientIt.hasNext()) {
            System.out.println("No registered patients found.");
            return;
        }

        // Display patients
        System.out.println("+------------+----------------------+--------------+--------------+-----+--------+");
        System.out.printf("| %-10s | %-20s | %-12s | %-12s | %-3s | %-6s |\n",
                "Patient ID", "Name", "IC Number", "Phone", "Age", "Gender");
        System.out.println("+------------+----------------------+--------------+--------------+-----+--------+");

        while (patientIt.hasNext()) {
            Patient p = patientIt.next();
            System.out.printf("| %-10s | %-20s | %-12s | %-12s | %-3d | %-6s |\n",
                    p.getId(), p.getName(), p.getIcNumber(), p.getContact(), p.getAge(), p.getGender());
        }
        System.out.println("+------------+----------------------+--------------+--------------+-----+--------+");

        String patientId;
        while (true) {
            System.out.print("Enter Patient ID to process (or 0 to cancel): ");
            patientId = sc.nextLine().trim();

            if (patientId.equals("0")) {
                System.out.println("Operation cancelled.");
                return;
            }

            if (patientControl.getPatientById(patientId) == null) {
                System.out.println("Patient ID not found. Please try again.");
            } else {
                break;
            }
        }
        consultationControl.updateConsultation(patientId);
    }

    private void searchByPatient() {
        System.out.println("\n=== Search Consultations by Patient ===");

        ClinicADT.MyIterator<Patient> patientIt = patientControl.getAllPatients().iterator();
        if (!patientIt.hasNext()) {
            System.out.println("No registered patients found.");
            return;
        }

        // Display patients
        System.out.println("+------------+----------------------+--------------+--------------+-----+--------+");
        System.out.printf("| %-10s | %-20s | %-12s | %-12s | %-3s | %-6s |\n",
                "Patient ID", "Name", "IC Number", "Phone", "Age", "Gender");
        System.out.println("+------------+----------------------+--------------+--------------+-----+--------+");

        while (patientIt.hasNext()) {
            Patient p = patientIt.next();
            System.out.printf("| %-10s | %-20s | %-12s | %-12s | %-3d | %-6s |\n",
                    p.getId(), p.getName(), p.getIcNumber(), p.getContact(), p.getAge(), p.getGender());
        }
        System.out.println("+------------+----------------------+--------------+--------------+-----+--------+");

        String patientId;
        while (true) {
            System.out.print("Enter Patient ID to view consultation history (or 0 to cancel): ");
            patientId = sc.nextLine().trim();

            if (patientId.equals("0")) {
                System.out.println("Operation cancelled.");
                return;
            }

            if (patientControl.getPatientById(patientId) == null) {
                System.out.println("Patient ID not found. Please try again.");
            } else {
                break;
            }
        }

        Patient selectedPatient = patientControl.getPatientById(patientId);
        if (selectedPatient == null) return;

        boolean found = false;
        ClinicADT.MyIterator<Consultation> consultIt = consultations.iterator();

        System.out.println("\n--- Consultation History for " + selectedPatient.getName() + " ---");
        System.out.println("+--------------+----------------------+------------------------+");
        System.out.printf("| %-12s | %-20s | %-22s |\n", "Consult ID", "Doctor", "Date");
        System.out.println("+--------------+----------------------+------------------------+");

        while (consultIt.hasNext()) {
            Consultation c = consultIt.next();
            if (c.getPatientId() != null && c.getPatientId().trim().equalsIgnoreCase(patientId.trim())) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                String dateStr = (c.getConsultationDate() != null)
                        ? c.getConsultationDate().format(fmt)
                        : "N/A";
                System.out.printf("| %-12d | %-20s | %-22s |\n",
                        c.getId(), c.getDoctorName(), dateStr);
                found = true;
            }
        }

        if (!found) {
            System.out.println("| No consultations found for this patient.                       |");
        }

        System.out.println("+--------------+----------------------+------------------------+");
    }

    private void searchByDoctor() {
        System.out.println("\n=== Search Consultations by Doctor ===");

        ClinicADT.MyIterator<Doctor> doctorIt = doctorControl.getAllDoctors().iterator();
        if (!doctorIt.hasNext()) {
            System.out.println("No registered doctors found.");
            return;
        }

        // Display doctors
        System.out.println("+------------+----------------+--------+");
        System.out.printf("| %-10s | %-14s | %-6s |\n", "Doctor ID", "Name", "Room");
        System.out.println("+------------+----------------+--------+");

        while (doctorIt.hasNext()) {
            Doctor d = doctorIt.next();
            System.out.printf("| %-10s | %-14s | %-6d |\n", d.getId(), d.getName(), d.getRoomNumber());
        }
        System.out.println("+------------+----------------+--------+");

        String doctorId;
        while (true) {
            System.out.print("Enter Doctor ID to view consultations (or 0 to cancel): ");
            doctorId = sc.nextLine().trim();

            if (doctorId.equals("0")) {
                System.out.println("Operation cancelled.");
                return;
            }

            if (doctorControl.getDoctorById(doctorId) == null) {
                System.out.println("Doctor ID not found. Please try again.");
            } else {
                break;
            }
        }

        Doctor selectedDoctor = doctorControl.getDoctorById(doctorId);
        if (selectedDoctor == null) return;

        boolean found = false;
        ClinicADT.MyIterator<Consultation> consultIt = consultations.iterator();

        System.out.println("\n--- Consultations for Dr. " + selectedDoctor.getName() + " ---");
        System.out.println("+--------------+----------------------+------------------------+");
        System.out.printf("| %-12s | %-20s | %-22s |\n", "Consult ID", "Patient", "Date");
        System.out.println("+--------------+----------------------+------------------------+");

        while (consultIt.hasNext()) {
            Consultation c = consultIt.next();
            if (c.getDoctorName() != null && c.getDoctorName().trim().equalsIgnoreCase(selectedDoctor.getName().trim())) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                String dateStr = (c.getConsultationDate() != null)
                        ? c.getConsultationDate().format(fmt)
                        : "N/A";
                System.out.printf("| %-12d | %-20s | %-22s |\n",
                        c.getId(), c.getPatientName(), dateStr);
                found = true;
            }
        }

        if (!found) {
            System.out.println("| No consultations found for this doctor.                    |");
        }

        System.out.println("+--------------+----------------------+------------------------+");
    }

    private void checkDoctorAvailability() {
        System.out.println("\n=== Check Doctor Availability ===");

        while (true) {
            System.out.print("Enter date to check (yyyy-MM-dd) or 0 to cancel: ");
            String input = sc.nextLine().trim();

            if (input.equals("0")) {
                System.out.println("Operation cancelled.");
                break;
            }

            try {
                LocalDateTime date = LocalDateTime.parse(input + " 00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                consultationControl.showDoctorScheduleForDate(date);
                break;
            } catch (Exception e) {
                System.out.println("Invalid date format. Please use 'yyyy-MM-dd'.");
            }
        }
    }

    private void showWorkingHours() {
        System.out.println("=== Clinic Working Hours ===\n");
        System.out.println("+---------------------------------------------+");
        System.out.println("|                Shift Timings                |");
        System.out.println("+---------------------------------------------+");
        System.out.println("|  Morning Shift   : 08:00 - 12:00            |");
        System.out.println("|  Afternoon Shift : 13:00 - 17:00            |");
        System.out.println("|  Night Shift     : 18:00 - 22:00            |");
        System.out.println("+---------------------------------------------+\n");

        System.out.println("-> Each consultation is exactly 1 hour long.");
        System.out.println("-> Consultations can only be scheduled during working hours.");
        System.out.println("-> Only doctors on duty during that shift are available.\n");
        System.out.println(" Tips:");
        System.out.println("    -> Check doctor availability before scheduling.");
        System.out.println("    -> Avoid time clashes with other bookings.");
        System.out.println("    -> Doctor schedules vary by day.");
    }
    
    public void consultationsSortedByDate(){
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime now = LocalDateTime.now();
            Report.printHeader("Consultation Analysis Report");

            // Table formatting
            String line = "+-------------------+----------------------+-----------------+-----------------+---------------------------+---------------------+";
            String headerFormat = "| %-17s | %-20s | %-15s | %-15s | %-25s | %-19s |%n";
            String rowFormat    = "| %-17s | %-20s | %-15s | %-15s | %-25s | %-19s |%n";

            // Print header
            System.out.println(line);
            System.out.printf(headerFormat, 
                " Consultation ID", "Patient", "Doctor", "Doctor ID", "Diagnosis", "Date & Time");
            System.out.println(line);

            // Iterate through consultations
            ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
            while (iterator.hasNext()) {
                Consultation c = iterator.next();

                // Extract fields
                String id          = String.valueOf(c.getId());
                String patientName = c.getPatientName();
                String doctorName  = c.getDoctorName();
                String diagnosis   = c.getDiagnosis() != null ? c.getDiagnosis() : "N/A";
                String doctorId    = c.getDoctorId();
                String dateStr = c.getConsultationDate().format(formatter);

                // Replace placeholder diagnosis
                if ("To be diagnosed during appointment".equalsIgnoreCase(diagnosis)) {
                    diagnosis = "TBD";
                }

                // Print row
                System.out.printf(rowFormat,
                    id,
                    patientName,
                    doctorName,
                    doctorId,
                    diagnosis,
                    dateStr
                );
            }

            System.out.println(line);
            Report.printFooter();
    }
    
    public void frequencyDistributionReport(){
        Report.printHeader("Consultation Frequency Distribution Report");

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

        ClinicADT.MyIterator<Consultation> outer = consultations.iterator();
        while (outer.hasNext()) {
            Consultation c = outer.next();

            String diagnosis = c.getDiagnosis() != null ? c.getDiagnosis() : "N/A";
            if ("to be diagnosed during appointment".equalsIgnoreCase(diagnosis)) {
                diagnosis = "TBD";
            }

            // Skip if already counted
            if (seenDiagnoses.toString().contains("|" + diagnosis + "|")) {
                continue;
            }

            // Count occurrences
            int count = 0;
            ClinicADT.MyIterator<Consultation> inner = consultations.iterator();
            while (inner.hasNext()) {
                Consultation ci = inner.next();
                String d = ci.getDiagnosis() != null ? ci.getDiagnosis() : "N/A";
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

        // ================================
        // Doctor Frequency Distribution
        // ================================
        System.out.println("\nDoctor Consultation Frequency:");
        System.out.println(line);
        System.out.printf(headerFormat, "Doctor", "Count");
        System.out.println(line);

        StringBuilder seenDoctors = new StringBuilder();
        StringBuilder doctorBars = new StringBuilder();

        outer = consultations.iterator();
        while (outer.hasNext()) {
            Consultation c = outer.next();

            String doctor = c.getDoctorName();

            if (seenDoctors.toString().contains("|" + doctor + "|")) {
                continue; // already counted
            }

            int count = 0;
            ClinicADT.MyIterator<Consultation> inner = consultations.iterator();
            while (inner.hasNext()) {
                Consultation ci = inner.next();
                if (ci.getDoctorName().equalsIgnoreCase(doctor)) {
                    count++;
                }
            }

            seenDoctors.append("|").append(doctor).append("|");
            System.out.printf(rowFormat, doctor, count);

            doctorBars.append(String.format("%-17s (%d) : %s%n", doctor, count, "*".repeat(count)));
        }
        System.out.println(line);

        // =========================
        // Bar Chart Displays
        // =========================
        System.out.println("\nDiagnosis Frequency Chart:");
        System.out.println("============================");
        System.out.println(diagnosisBars.toString());

        System.out.println("Doctor Consultation Frequency Chart:");
        System.out.println("====================================");
        System.out.println(doctorBars.toString());

        Report.printFooter();
    }
}
