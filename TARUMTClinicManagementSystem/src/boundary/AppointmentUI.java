package boundary;

import adt.ClinicADT;
import adt.MyClinicADT;
import control.AppointmentControl;
import control.DoctorControl;
import control.PatientControl;
import entity.Appointment;
import entity.Consultation;
import entity.Doctor;
import entity.MedicalTreatment;
import entity.Patient;
import utility.Report;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class AppointmentUI {

    private final AppointmentControl appointmentControl;
    private final PatientControl patientControl;
    private final DoctorControl doctorControl;
    private final Scanner scanner;
    private final Report report = new Report();

    // Needed for overlap checks used by doctor availability
    private final ClinicADT<Consultation> consultations = new MyClinicADT<>();
    private final ClinicADT<MedicalTreatment> treatments = new MyClinicADT<>();

    public AppointmentUI(AppointmentControl appointmentControl,
                         PatientControl patientControl,
                         DoctorControl doctorControl) {
        this.appointmentControl = appointmentControl;
        this.patientControl = patientControl;
        this.doctorControl = doctorControl;
        this.scanner = new Scanner(System.in);
    }

    // =========================
    // ========  MENU  =========
    // =========================
    
    public void patientAppointmentMenu() {
        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("            PATIENT - APPOINTMENT PORTAL");
            System.out.println("=".repeat(60));
            System.out.println(" 1) Create New Appointment (Pick Slot & Doctor)");
            System.out.println(" 2) Patient Check-In (Online appointments)");
            System.out.println(" 3) View Waiting Queue");
            System.out.println(" 0) Back");
            System.out.print("Choice: ");

            int choice = getInt();
            switch (choice) {
                case 1 -> createAppointment();
                case 2 -> checkInPatient();
                case 3 -> appointmentControl.printQueue();
                case 0 -> { System.out.println("Returning..."); return; }
                default -> System.out.println("Invalid choice.");
            }
            if (choice != 0) pause();
        }
    }
    
    public void doctorAppointmentMenu() {
        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("             DOCTOR - APPOINTMENT PORTAL");
            System.out.println("=".repeat(60));
            System.out.println(" 1) Call Next Patient (move to CONSULTING)");
            System.out.println(" 2) View Waiting Queue");
            System.out.println(" 3) View All Appointments");
            System.out.println(" 0) Back");
            System.out.print("Choice: ");

            int choice = getInt();
            switch (choice) {
                case 1 -> {
                    boolean ok = appointmentControl.callNext();
                    if (ok) {
                        System.out.println("Handoff complete: patient is now in CONSULTING.");
                        System.out.println("Tip: Continue in Consultation module.");
                    }
                }
                case 2 -> appointmentControl.printQueue();
                case 3 -> displayAllAppointments();
                case 0 -> { System.out.println("Returning..."); return; }
                default -> System.out.println("Invalid choice.");
            }
            if (choice != 0) pause();
        }
    }
     
    public void adminAppointmentMenu() {
        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("              ADMIN - APPOINTMENT PORTAL");
            System.out.println("=".repeat(60));
            System.out.println(" 1) Search & Reports");
            System.out.println(" 2) View All Appointments");
            System.out.println(" 0) Back");
            System.out.print("Choice: ");

            int choice = getInt();
            switch (choice) {
                case 1 -> searchAndReportsMenu();
                case 2 -> displayAllAppointments();
                case 0 -> { System.out.println("Returning..."); return; }
                default -> System.out.println("Invalid choice.");
            }
            if (choice != 0) pause();
        }
    }
     
    public void run() {
        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("             TARUMT CLINIC APPOINTMENT SYSTEM");
            System.out.println("=".repeat(60));
            System.out.println("1.  Create New Appointment (Pick Slot & Doctor)");
            System.out.println("2.  Patient Check-In (Online appointments)");
            System.out.println("3.  View Waiting Queue");
            System.out.println("4.  Call Next Patient (moves to CONSULTING)");
            System.out.println("5.  Search & Reports");
            System.out.println("6.  View All Appointments");
            System.out.println("0.  Exit to Main System");
            System.out.print("Enter your choice: ");

            int choice = getInt();
            switch (choice) {
                case 1 -> createAppointment();
                case 2 -> checkInPatient();
                case 3 -> appointmentControl.printQueue();
                case 4 -> {
                    boolean ok = appointmentControl.callNext();
                    if (ok) {
                        System.out.println("Handoff: patient is now in CONSULTING. Continue in Consultation module.");
                    }
                }
                case 5 -> searchAndReportsMenu();
                case 6 -> displayAllAppointments();
                case 0 -> { System.out.println("Returning to main system..."); return; }
                default -> System.out.println("Invalid choice.");
            }
            if (choice != 0) pause();
        }
    }

    // =========================
    // ====== CREATE FLOW ======
    // =========================

    private static class SlotChoice { LocalDateTime start; Doctor doctor; }

    private void createAppointment() {
        System.out.println("=== Create New Appointment ===");

        Patient p = pickPatient();
        if (p == null) return;

        LocalDate date = pickDate();
        if (date == null) return;

        Appointment.AppointmentType type = pickType();  // Ask type BEFORE showing slots

        SlotChoice choice = pickOneHourSlot(date);
        if (choice == null) return;

        Appointment a = appointmentControl.addAppointment(
                p.getId(), p.getName(),
                choice.doctor.getId(), choice.doctor.getName(),
                choice.start, type
        );

        if (a == null) return;

        printConfirm(a);
        if (type == Appointment.AppointmentType.WALK_IN)
            System.out.println("Walk-in: auto checked-in and queued.");
        else
            System.out.println("Online: remember to check-in on arrival.");
    }

    private Appointment.AppointmentType pickType() {
        while (true) {
            System.out.print("Select type: 1) ONLINE  2) WALK_IN : ");
            String s = scanner.nextLine().trim();
            if ("1".equals(s)) return Appointment.AppointmentType.ONLINE;
            if ("2".equals(s)) return Appointment.AppointmentType.WALK_IN;
            System.out.println("Invalid.");
        }
    }

    private void printConfirm(Appointment a) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String line = "+----+----------------------+----------------------+---------------------+--------------+";
        String fmt  = "| %-2s | %-20s | %-20s | %-19s | %-12s |%n";
        System.out.println("\nBooked:");
        System.out.println(line);
        System.out.printf(fmt, "ID", "Patient", "Doctor", "Scheduled Time", "Status");
        System.out.println(line);
        System.out.printf(fmt,
                a.getAppointmentId(),
                cut(a.getPatientName(),20),
                cut(a.getDoctorName(),20),
                a.getScheduledDateTime().format(dtf),
                a.getStatus().name());
        System.out.println(line);
    }

    // =========================
    // ====== CHECK-IN UI ======
    // =========================

    private void checkInPatient() {
        System.out.println("\n=== Patient Check-In ===");

        ClinicADT<Appointment> online = appointmentControl.getOnlineAppointmentsPendingCheckIn();
        if (online == null || online.size() == 0) {
            System.out.println("No ONLINE appointments pending check-in.");
            return;
        }

        String line = "+--------+----------------------+----------------------+---------------------+------------+";
        String hdr  = "| %-6s | %-20s | %-20s | %-19s | %-10s |%n";
        String row  = "| %-6d | %-20s | %-20s | %-19s | %-10s |%n";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        System.out.println("\nONLINE Appointments (BOOKED) — select one to check-in:");
        System.out.println(line);
        System.out.printf(hdr, "ID", "Patient", "Doctor", "Date & Time", "Status");
        System.out.println(line);

        ClinicADT.MyIterator<Appointment> oit = online.iterator();
        while (oit.hasNext()) {
            Appointment a = oit.next();
            System.out.printf(row,
                    a.getAppointmentId(),
                    cut(a.getPatientName(), 20),
                    cut(a.getDoctorName(), 20),
                    a.getScheduledDateTime().format(dtf),
                    a.getStatus().name());
        }
        System.out.println(line);

        System.out.print("Enter Appointment ID to check in (or 0 to cancel): ");
        int id = getInt();
        if (id == 0) return;

        boolean ok = appointmentControl.checkIn(id);
        if (!ok) {
            System.out.println("Unable to check in (invalid ID or already checked in).");
            return;
        }

        Appointment picked = null;
        ClinicADT.MyIterator<Appointment> allIt = appointmentControl.getAll().iterator();
        while (allIt.hasNext()) {
            Appointment a = allIt.next();
            if (a.getAppointmentId() == id) { picked = a; break; }
        }

        System.out.println("\nChecked in successfully:");
        System.out.println(line);
        System.out.printf(hdr, "ID", "Patient", "Doctor", "Date & Time", "Status");
        System.out.println(line);
        if (picked != null) {
            System.out.printf(row,
                    picked.getAppointmentId(),
                    cut(picked.getPatientName(), 20),
                    cut(picked.getDoctorName(), 20),
                    picked.getScheduledDateTime().format(dtf),
                    picked.getStatus().name());
        }
        System.out.println(line);
    }

    // =========================
    // == PROCESS CALLED CASE ==
    // =========================
    /** Simplified: asks symptoms only, then y/n if treatment is needed. */
    private void processCalledConsultation() {
        System.out.println("\n=== Process Called Consultation ===");

        Appointment called = appointmentControl.getCalled();
        if (called == null || called.getStatus() != Appointment.AppointmentStatus.CONSULTING) {
            System.out.println("No patient is currently in CONSULTING. Use 'Call Next Patient' first.");
            return;
        }

        System.out.println("Patient : " + called.getPatientName() + " (" + called.getPatientId() + ")");
        System.out.println("Doctor  : " + called.getDoctorName());
        System.out.println("Time    : " + called.getScheduledDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        String symptoms = prompt("Enter symptoms (or 0 to cancel): ");
        if (symptoms.equals("0")) return;

        boolean treatmentNeeded = yesNo("Treatment needed? (y/n): ");

        boolean ok = appointmentControl.completeConsultation(symptoms, treatmentNeeded);
        if (!ok) {
            System.out.println("Failed to complete consultation.");
            return;
        }

        if (treatmentNeeded) {
            System.out.println("Status changed to TREATMENT. Patient sent to Treatment Waiting Queue.");
        } else {
            System.out.println("No treatment required. Status changed to PENDING PAYMENT.");
        }
    }

    // =========================
    // ==== SEARCH & REPORTS ===
    // =========================

    private void searchAndReportsMenu() {
        while (true) {
            System.out.println("\n=== Search & Reports Menu ===");
            System.out.println("1. Search by Doctor (Binary Search)");
            System.out.println("2. Search by Time Slot (Binary Search)");
            System.out.println("3. Appointment Statistics");
            System.out.println("4. Generate Reports");
            System.out.println("0. Back to Main Menu");
            System.out.print("Enter your choice: ");

            int choice = getInt();
            switch (choice) {
                case 1 -> searchByDoctor();
                case 2 -> searchByTimeSlot();
                case 3 -> displayStatistics();
                case 4 -> generateReports();
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
            if (choice != 0) pause();
        }
    }

    private void searchByDoctor() {
        System.out.println("\n=== Search Appointments by Doctor ===");

        ClinicADT<Doctor> doctors = doctorControl.getAllDoctors();
        if (doctors == null || doctors.size() == 0) {
            System.out.println("No doctors registered.");
            return;
        }

        String line = "+----+------------+----------------------+--------+";
        String fmt  = "| %-2s | %-10s | %-20s | %-6s |%n";
        System.out.println("\nAvailable Doctors:");
        System.out.println(line);
        System.out.printf(fmt, "No", "ID", "Name", "Room");
        System.out.println(line);

        int i = 1;
        ClinicADT.MyIterator<Doctor> dit = doctors.iterator();
        while (dit.hasNext()) {
            Doctor d = dit.next();
            System.out.printf(fmt, i++, d.getId(), cut(d.getName(), 20), d.getRoomNumber());
        }
        System.out.println(line);

        System.out.print("Enter Doctor ID to search: ");
        String doctorId = scanner.nextLine().trim();
        if (doctorId.isEmpty()) return;

        // Delegates to AppointmentControl, which should implement ADT-backed search
        ClinicADT<Appointment> results = appointmentControl.searchAppointmentsByDoctor(doctorId);
        if (results.size() == 0) {
            System.out.println("No appointments found for Doctor ID: " + doctorId);
            return;
        }

        System.out.println("\n=== Search Results ===");
        displayAppointmentTable(results, "Doctor: " + doctorId);
    }

    private void searchByTimeSlot() {
        System.out.println("\n=== Search Appointments by Time Slot ===");
        try {
            System.out.print("Enter start date and time (yyyy-MM-dd HH:mm): ");
            String startStr = scanner.nextLine().trim();
            LocalDateTime startTime = LocalDateTime.parse(startStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            System.out.print("Enter end date and time (yyyy-MM-dd HH:mm): ");
            String endStr = scanner.nextLine().trim();
            LocalDateTime endTime = LocalDateTime.parse(endStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            if (startTime.isAfter(endTime)) {
                System.out.println("Start time must be before end time.");
                return;
            }

            // Delegates to AppointmentControl, which should implement ADT-backed search
            ClinicADT<Appointment> results = appointmentControl.searchAppointmentsByTimeSlot(startTime, endTime);
            if (results.size() == 0) {
                System.out.println("No appointments found in the specified time slot.");
                return;
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            displayAppointmentTable(results, "Time Slot: " + startTime.format(fmt) + " to " + endTime.format(fmt));

        } catch (Exception e) {
            System.out.println("Invalid date/time format. Use yyyy-MM-dd HH:mm");
        }
    }

    private void displayAppointmentTable(ClinicADT<Appointment> appointments, String title) {
        String line = "+------+----------------------+----------------------+---------------------+--------------+";
        String fmt  = "| %-4s | %-20s | %-20s | %-19s | %-12s |%n";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        System.out.println("\nResults for: " + title);
        System.out.println("Total found: " + appointments.size());
        System.out.println(line);
        System.out.printf(fmt, "ID", "Patient", "Doctor", "Scheduled Time", "Status");
        System.out.println(line);

        ClinicADT.MyIterator<Appointment> it = appointments.iterator();
        while (it.hasNext()) {
            Appointment a = it.next();
            System.out.printf(fmt,
                    a.getAppointmentId(),
                    cut(a.getPatientName(), 20),
                    cut(a.getDoctorName(), 20),
                    a.getScheduledDateTime().format(dtf),
                    a.getStatus().name());
        }
        System.out.println(line);
    }

    private void generateReports() {
        System.out.println("\n=== Generate Reports ===");
        System.out.println("1. Daily Report");
        System.out.println("2. Doctor Workload Report");
        System.out.println("3. Patient Visit History");
        System.out.println("4. Status Distribution Report");
        System.out.print("Select report type: ");

        int choice = getInt();
        switch (choice) {
            case 1 -> generateDailyReport();
            case 2 -> generateDoctorWorkloadReport();
            case 3 -> generatePatientVisitReport();
            case 4 -> generateStatusReport();
            default -> System.out.println("Invalid choice.");
        }
    }

    private void generateDailyReport() {
        LocalDate date = LocalDate.now();
        System.out.print("Enter date (yyyy-MM-dd) or press Enter for today: ");
        String input = scanner.nextLine().trim();
        if (!input.isEmpty()) {
            try {
                date = LocalDate.parse(input);
            } catch (Exception e) {
                System.out.println("Invalid date format.");
                return;
            }
        }
        report.printHeader("Daily Report");
        ClinicADT<Appointment> dayAppointments = new MyClinicADT<>();
        ClinicADT.MyIterator<Appointment> allIt = appointmentControl.getAll().iterator();
        while (allIt.hasNext()) {
            Appointment a = allIt.next();
            if (a.getScheduledDateTime().toLocalDate().equals(date)) {
                dayAppointments.add(a);
            }
        }

        Report.cprintf("====== Daily Report for " + date + " ======");
        displayAppointmentTable(dayAppointments, "Date: " + date);

        int booked = 0, checkedIn = 0, consulting = 0, completed = 0, pendingPay = 0, treatment = 0;
        ClinicADT.MyIterator<Appointment> di = dayAppointments.iterator();
        while (di.hasNext()) {
            switch (di.next().getStatus()) {
                case BOOKED -> booked++;
                case CHECKED_IN -> checkedIn++;
                case CONSULTING -> consulting++;
                case COMPLETED -> completed++;
                case PENDING_PAYMENT -> pendingPay++;
                case TREATMENT -> treatment++;
                default -> {}
            }
        }

        System.out.println("\nDaily Summary:");
        System.out.println("Total Appointments: " + dayAppointments.size());
        System.out.println("Booked: " + booked + ", Checked-in: " + checkedIn);
        System.out.println("Consulting: " + consulting + ", Treatment: " + treatment);
        System.out.println("Pending Payment: " + pendingPay + ", Completed: " + completed);
        report.printFooter();
    }

    private void generateDoctorWorkloadReport() {
        ClinicADT<Doctor> doctors = doctorControl.getAllDoctors();
        if (doctors.size() == 0) {
            System.out.println("No doctors registered.");
            return;
        }
        report.printHeader( "Doctor Workload Report");
        Report.cprintln("====== Doctor Workload Report ======");
        String line = "+------------+----------------------+-------+-------+-------+-------+";
        String fmt  = "| %-10s | %-20s | %-5s | %-5s | %-5s | %-5s |%n";
        Report.cprintln(line);
        Report.cprintf(fmt, "Doctor ID", "Name", "Total", "Today", "Week", "Month");
        Report.cprintln(line);

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(7);
        LocalDate monthStart = today.minusDays(30);

        ClinicADT<Appointment> all = appointmentControl.getAll();

        ClinicADT.MyIterator<Doctor> dit = doctors.iterator();
        while (dit.hasNext()) {
            Doctor doc = dit.next();
            int total = 0, todayCount = 0, weekCount = 0, monthCount = 0;

            ClinicADT.MyIterator<Appointment> ait = all.iterator();
            while (ait.hasNext()) {
                Appointment a = ait.next();
                if (a.getDoctorId().equals(doc.getId())) {
                    total++;
                    LocalDate apptDate = a.getScheduledDateTime().toLocalDate();
                    if (apptDate.equals(today)) todayCount++;
                    if (!apptDate.isBefore(weekStart)) weekCount++;
                    if (!apptDate.isBefore(monthStart)) monthCount++;
                }
            }

            Report.cprintf(fmt, doc.getId(), cut(doc.getName(), 20),
                    total, todayCount, weekCount, monthCount);
        }
        Report.cprintln(line);
        report.printFooter();
    }

    private void generatePatientVisitReport() {
        System.out.print("Enter Patient ID to generate visit history: ");
        String patientId = scanner.nextLine().trim();
        if (patientId.isEmpty()) return;
        Report.printHeader("Patient Visit History");
        ClinicADT<Appointment> patientAppointments = new MyClinicADT<>();
        ClinicADT.MyIterator<Appointment> allIt = appointmentControl.getAll().iterator();

        while (allIt.hasNext()) {
            Appointment a = allIt.next();
            if (a.getPatientId().equalsIgnoreCase(patientId)) {
                patientAppointments.add(a);
            }
        }

        if (patientAppointments.size() == 0) {
            System.out.println("No appointments found for Patient ID: " + patientId);
            return;
        }

        Report.cprintln("====== Patient Visit History ======");
        displayAppointmentTable(patientAppointments, "Patient ID: " + patientId);
        Report.printFooter();
    }

    private void generateStatusReport() {
        Report.printHeader("Status Distribution");
        ClinicADT<Appointment> all = appointmentControl.getAll();

        int booked = 0, checkedIn = 0, consulting = 0, pendingPay = 0, treatment = 0, completed = 0;
        ClinicADT.MyIterator<Appointment> it = all.iterator();
        while (it.hasNext()) {
            switch (it.next().getStatus()) {
                case BOOKED -> booked++;
                case CHECKED_IN -> checkedIn++;
                case CONSULTING -> consulting++;
                case PENDING_PAYMENT -> pendingPay++;
                case TREATMENT -> treatment++;
                case COMPLETED -> completed++;
                default -> {}
            }
        }

        String line = "+---------------------------+----------+";
        String fmt  = "| %-25s | %8d |%n";
        Report.cprintf("====== Status Distribution Report ======");
        Report.cprintln(line);
        Report.cprintf(fmt,"Booked", booked);
        Report.cprintf(fmt,"Checked-in", checkedIn);
        Report.cprintf(fmt,"Consulting", consulting);
        Report.cprintf(fmt,"Treatment", treatment);
        Report.cprintf(fmt,"Pending Payment", pendingPay);
        Report.cprintf(fmt,"Completed", completed);
        Report.cprintf(line);

        var next = appointmentControl.peekNext();
        System.out.println("\nNext in queue: " + (next==null ? "-" : ("Appt #"+next.getAppointmentId()+" ("+next.getPatientName()+")")));
        var called = appointmentControl.getCalled();
        System.out.println("Currently consulting: " + (called==null ? "-" : ("Appt #"+called.getAppointmentId()+" ("+called.getPatientName()+")")));
        Report.printFooter();
    }

    private void displayAllAppointments() {
        System.out.println("\n=== All Appointments ===");
        ClinicADT<Appointment> list = appointmentControl.getAll();
        if (list == null || list.size() == 0) {
            System.out.println("(none)");
            return;
        }
        displayAppointmentTable(list, "All Appointments");

        System.out.println("\nWould you like to search these appointments?");
        System.out.print("1) Search by Doctor  2) Search by Time Slot  0) No: ");
        int choice = getInt();
        switch (choice) {
            case 1 -> searchByDoctor();
            case 2 -> searchByTimeSlot();
            default -> {}
        }
    }

    // =========================
    // ======= PICKERS =========
    // =========================

    private Patient pickPatient() {
        ClinicADT<Patient> all = patientControl.getAllPatients();
        if (all == null || all.size() == 0) { System.out.println("No patients registered."); return null; }

        String line = "+----+------------+----------------------+-----+--------+";
        String fmt  = "| %-2s | %-10s | %-20s | %-3s | %-6s |%n";
        System.out.println("\nPatients:");
        System.out.println(line);
        System.out.printf(fmt, "No", "ID", "Name", "Age", "Gender");
        System.out.println(line);

        int row = 1;
        ClinicADT.MyIterator<Patient> it = all.iterator();
        while (it.hasNext()) {
            Patient p = it.next();
            System.out.printf(fmt, row++, p.getId(), cut(p.getName(), 20), p.getAge(), p.getGender());
        }
        System.out.println(line);

        System.out.print("Select No. (0=cancel): ");
        int sel = getInt();
        if (sel <= 0 || sel > all.size()) return null;
        return all.get(sel-1); // still ADT access
    }

    private LocalDate pickDate() {
        System.out.println("\nSelect date:");
        System.out.print("1) Today  2) Tomorrow  3) Enter yyyy-MM-dd :");
        String s = scanner.nextLine().trim();
        if ("1".equals(s)) return LocalDate.now();
        if ("2".equals(s)) return LocalDate.now().plusDays(1);
        if ("3".equals(s)) {
            System.out.print("Date (yyyy-MM-dd): ");
            try {
                return LocalDate.parse(scanner.nextLine().trim());
            } catch (Exception e) {
                System.out.println("Invalid date.");
                return null;
            }
        }
        return null;
    }

    // 10-per-page slot table; press Enter to view more or key in slot number
    private SlotChoice pickOneHourSlot(LocalDate date) {
        ClinicADT<SlotChoice> choices = new MyClinicADT<>();
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        final String line = "+----+-------------------+----------------------+---------+";
        final String fmt  = "| %-2s | %-17s | %-20s | %-7s |%n";

        System.out.println("\nAvailable 1-hour Slots:");
        System.out.println(line);
        System.out.printf(fmt, "No", "Time Slot", "Doctor", "Room");
        System.out.println(line);

        LocalDateTime now = LocalDateTime.now();
        int printedInPage = 0;  // rows in current page
        int total = 0;          // total rows printed

        // Build & stream-print slots (08–21 starts; 12 is lunch break)
        for (int hour = 8; hour <= 21; hour++) {
            if (hour == 12) continue; // lunch
            LocalDateTime start = date.atTime(hour, 0);
            if (start.isBefore(now)) continue;

            ClinicADT.MyIterator<Doctor> dit = doctorControl.getAllDoctors().iterator();
            while (dit.hasNext()) {
                Doctor d = dit.next();

                // Skip if doctor not available or already booked by an appointment
                if (!doctorControl.isDoctorAvailableForAppointment(d, start, 1, consultations, treatments))
                    continue;
                if (isAppointmentBooked(d.getId(), start))  // ensure already-booked slot doesn't appear
                    continue;

                // Keep this choice
                SlotChoice ch = new SlotChoice();
                ch.start = start;
                ch.doctor = d;
                choices.add(ch);

                // Print a row
                total++;
                printedInPage++;
                String range = start.format(timeFmt) + " - " + start.plusHours(1).format(timeFmt);
                System.out.printf(fmt, total, range, "Dr. " + cut(d.getName(), 18), d.getRoomNumber());

                // Page break each 10 rows: allow choose now or press Enter to see more
                if (printedInPage == 10) {
                    System.out.println(line);
                    System.out.print("Enter slot No. (1-" + total + "), 0=cancel, or press Enter to view more: ");
                    String in = scanner.nextLine().trim();
                    if (in.equals("0")) return null;

                    if (!in.isEmpty()) {
                        int sel = -1;
                        try { sel = Integer.parseInt(in); } catch (Exception ignored) {}
                        if (sel >= 1 && sel <= choices.size()) return choices.get(sel - 1);
                        System.out.println("Invalid selection. Showing more...");
                    }

                    // Start a fresh page header
                    printedInPage = 0;
                    System.out.println();
                    System.out.println(line);
                    System.out.printf(fmt, "No", "Time Slot", "Doctor", "Room");
                    System.out.println(line);
                }
            }
        }

        // No slots at all?
        if (choices.size() == 0) {
            System.out.println(line);
            explainNoSlots(date);
            return null;
        }

        // Final footer for the last (possibly partial) page
        System.out.println(line);

        // Final prompt loop: choose, cancel, or Enter to reprint last page for clarity
        while (true) {
            System.out.print("Enter slot No. (1-" + choices.size() + "), 0=cancel, or press Enter to reprint last page: ");
            String in = scanner.nextLine().trim();
            if (in.equals("0")) return null;

            if (in.isEmpty()) {
                // Reprint only the last 10 rows so user can see them again
                int startIdx = Math.max(0, choices.size() - 10);

                System.out.println();
                System.out.println(line);
                System.out.printf(fmt, "No", "Time Slot", "Doctor", "Room");
                System.out.println(line);

                for (int i = startIdx; i < choices.size(); i++) {
                    SlotChoice c = choices.get(i);
                    String range = c.start.format(timeFmt) + " - " + c.start.plusHours(1).format(timeFmt);
                    System.out.printf(fmt, (i + 1), range, "Dr. " + cut(c.doctor.getName(), 18), c.doctor.getRoomNumber());
                }
                System.out.println(line);
                continue;
            }

            int sel = -1;
            try { sel = Integer.parseInt(in); } catch (Exception ignored) {}
            if (sel >= 1 && sel <= choices.size()) return choices.get(sel - 1);

            System.out.println("Invalid selection.");
        }
    }

    // =========================
    // ======= REPORTS =========
    // =========================

    private void displayStatistics() {
        ClinicADT<Appointment> list = appointmentControl.getAll();
        int total = (list == null) ? 0 : list.size();
        int waiting=0, consulting=0, completed=0, pendingPayment=0, treatment=0;

        if (list != null) {
            ClinicADT.MyIterator<Appointment> it = list.iterator();
            while (it.hasNext()) {
                switch (it.next().getStatus()) {
                    case CHECKED_IN -> waiting++;
                    case CONSULTING -> consulting++;
                    case COMPLETED -> completed++;
                    case PENDING_PAYMENT -> pendingPayment++;
                    case TREATMENT -> treatment++;
                    default -> {}
                }
            }
        }

        String line = "+---------------------------+----------+";
        String fmt  = "| %-25s | %8d |%n";
        System.out.println(line);
        System.out.printf(fmt,"Total Appointments", total);
        System.out.printf(fmt,"Patients in Queue", waiting);
        System.out.printf(fmt,"Consulting Now", consulting);
        System.out.printf(fmt,"Treatment", treatment);
        System.out.printf(fmt,"Pending Payment", pendingPayment);
        System.out.printf(fmt,"Completed", completed);
        System.out.println(line);
        var next = appointmentControl.peekNext();
        System.out.println("\nNext in queue: " + (next==null ? "-" : ("Appt #"+next.getAppointmentId()+" ("+next.getPatientName()+")")));
        var called = appointmentControl.getCalled();
        System.out.println("Currently consulting: " + (called==null ? "-" : ("Appt #"+called.getAppointmentId()+" ("+called.getPatientName()+")")));
    }

    // =========================
    // ====== HELPERS ==========
    // =========================

    private boolean isAppointmentBooked(String doctorId, LocalDateTime start) {
        ClinicADT.MyIterator<Appointment> it = appointmentControl.getAll().iterator();
        while (it.hasNext()) {
            Appointment a = it.next();
            if (a.getDoctorId().equalsIgnoreCase(doctorId)
                    && a.getScheduledDateTime().equals(start)) {
                return true;
            }
        }
        return false;
    }

    // ASCII "..." keeps table widths consistent across consoles
    private String cut(String s, int w) {
        if (s == null) return "";
        if (s.length() <= w) return s;
        if (w <= 3) return ".".repeat(Math.max(0, w));
        return s.substring(0, w - 3) + "...";
    }

    private String prompt(String label) {
        System.out.print(label);
        return scanner.nextLine().trim();
        }

    private boolean yesNo(String label) {
        while (true) {
            System.out.print(label);
            String s = scanner.nextLine().trim();
            if (s.equalsIgnoreCase("y") || s.equalsIgnoreCase("yes")) return true;
            if (s.equalsIgnoreCase("n") || s.equalsIgnoreCase("no")) return false;
            System.out.println("Please answer y/n.");
        }
    }

    private int getInt() {
        while (true) {
            String s = scanner.nextLine().trim();
            try { return Integer.parseInt(s); }
            catch (Exception e) { System.out.print("Enter a number: "); }
        }
    }

    private void pause() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private void explainNoSlots(LocalDate date) {
        System.out.println("No available slots.");
        if (date.equals(LocalDate.now())) {
            LocalTime nowT = LocalTime.now();
            if (!nowT.isBefore(LocalTime.of(21,0))) {
                System.out.println("Hint: It is already past the last start time (21:00). Try tomorrow.");
                return;
            }
        }
        int onDuty = 0;
        ClinicADT.MyIterator<Doctor> dit = doctorControl.getAllDoctors().iterator();
        while (dit.hasNext()) {
            Doctor d = dit.next();
            int[] probe = {9,15,19};
            boolean any = false;
            for (int h : probe) {
                LocalDateTime t = date.atTime(h,0);
                if (doctorControl.isDoctorAvailableForAppointment(d, t, 1, consultations, treatments)) {
                    any = true; break;
                }
            }
            if (any) onDuty++;
        }
        if (onDuty == 0) {
            System.out.println("Hint: No doctors appear to be on duty for the chosen date.");
        } else {
            System.out.println("Hint: Doctors are on duty, but remaining hours are either in the past (today) or fully booked.");
        }
    }
}
