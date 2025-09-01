package boundary;

import java.util.Scanner;

import adt.ClinicADT;
import control.DoctorControl;
import entity.Doctor;
import tarumtclinicmanagementsystem.DutySchedule;
import tarumtclinicmanagementsystem.Session;
import utility.Report;
import utility.Validation;

public class DoctorUI {
    // =========================
    // ======= CONSTANTS =======
    // =========================
    private static final int MAX_ROOMS = 10;

    // =========================
    // ====== DEPENDENCIES =====
    // =========================
    private final DoctorControl doctorControl;
    private final Scanner scanner;

    public DoctorUI(DoctorControl doctorControl) {
        this.doctorControl = doctorControl;
        this.scanner = new Scanner(System.in);
    }

    // =========================
    // ========= MENU ==========
    // =========================
    public void run() {
        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("             TARUMT CLINIC DOCTOR MANAGEMENT");
            System.out.println("=".repeat(60));
            System.out.println("1. Add Doctor");
            System.out.println("2. Remove Doctor");
            System.out.println("3. View Doctor Schedule Table"); // Include this
            System.out.println("4. Update Doctor Schedule");
            System.out.println("5. Display Doctors Sorted by Name");
            System.out.println("6. Display Only Available Doctors");
            System.out.println("7. Generate Report");
            System.out.println("0. Exit");

            int choice = readInt("Choice: ");
            switch (choice) {
                case 1 -> registerDoctor();
                case 2 -> removeDoctor();
                case 3 -> viewDoctorSchedule();
                case 4 -> updateDoctorSchedule();
                case 5 -> doctorControl.printDoctorsSortedByName();  // ADT sort inside control
                case 6 -> doctorControl.printAvailableDoctors();     // ADT filter inside control
                case 7 -> generateReport();
                case 0 -> { System.out.println("Exiting Doctor Management."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
    
   public void doctorDoctorMenu() {
        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("              DOCTOR - DOCTORS");
            System.out.println("=".repeat(60));
            System.out.println(" 1) View Doctor Schedule Table");
            System.out.println(" 2) Update My Schedule");
            System.out.println(" 3) Display Doctors Sorted by Name");
            System.out.println(" 4) Display Only Available Doctors");
            System.out.println(" 0) Back");
            int choice = readInt("Choice: ");
            switch (choice) {
                case 1 -> viewDoctorSchedule();
                case 2 -> updateDoctorSchedule();
                case 3 -> doctorControl.printDoctorsSortedByName();
                case 4 -> doctorControl.printAvailableDoctors();
                case 0 -> { System.out.println("Returning..."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    public void adminDoctorMenu() {
    while (true) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("               ADMIN - DOCTORS");
        System.out.println("=".repeat(60));
        System.out.println(" 1) Add Doctor");
        System.out.println(" 2) Remove Doctor");
        System.out.println(" 3) View Doctor Schedule Table");
        System.out.println(" 4) Update Doctor Schedule");
        System.out.println(" 5) Display Doctors Sorted by Name");
        System.out.println(" 6) Display Only Available Doctors");
        System.out.println(" 7) Generate Report");
        System.out.println(" 0) Back");
        int choice = readInt("Choice: ");
        switch (choice) {
            case 1 -> registerDoctor();
            case 2 -> removeDoctor();
            case 3 -> viewDoctorSchedule();
            case 4 -> updateDoctorSchedule();
            case 5 -> doctorControl.printDoctorsSortedByName();
            case 6 -> doctorControl.printAvailableDoctors();
            case 7 -> generateReport();
            case 0 -> { System.out.println("Returning..."); return; }
            default -> System.out.println("Invalid choice.");
        }
    }
}
    
    

    // =========================
    // ======= REPORT HUB ======
    // =========================
    private void generateReport() {
        while (true) {
            System.out.println("\n=== Generate Report ===");
            System.out.println("1. Doctors Workload Frequency Report");
            System.out.println("2. Doctors Information Report");
            System.out.println("3. Doctors Room Allocation Report");
            System.out.println("4. Doctors On-Duty Today Report");
            System.out.println("5. Doctors Contact List Report");
            System.out.println("0. Back");

            int choice = readInt("Choice: ");
            switch (choice) {
                case 1 -> doctorWorkloadReport();
                case 2 -> doctorInformationReport();
                case 3 -> doctorRoomAllocationReport();
                case 4 -> doctorOnDutyTodayReport();
                case 5 -> doctorContactListReport();
                case 0 -> { System.out.println("Returning to Doctor Management menu..."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // =========================
    // ==== CORE OPERATIONS ====
    // =========================
    private void registerDoctor() {
        // ----- Name -----
        String name;
        while (true) {
            name = readLine("Enter Doctor Name (or 0 to cancel): ");
            if ("0".equals(name)) return;
            String err = Validation.validateName(name);
            if (err == null) break;
            Report.cprintln(err + "\n");
        }

        // ----- Room (auto-assign if blank) -----
        int room;
        while (true) {
            String roomInput = readLine("Enter Room Number (1-" + MAX_ROOMS + ") [Enter=auto, 0=cancel]: ");
            if ("0".equals(roomInput)) return;

            if (roomInput.isBlank()) {
                room = getNextAvailableRoom();
                if (room == -1) {
                    Report.cprintln("No available rooms. Cannot register doctor.");
                    return;
                }
                Report.cprintln("Assigned Room: " + room);
                break;
            }

            try {
                room = Integer.parseInt(roomInput.trim());
            } catch (NumberFormatException e) {
                Report.cprintln("Please enter a valid number\n");
                continue;
            }

            String roomErr = Validation.validateRoomNumber(room);
            if (roomErr != null) {
                Report.cprintln(roomErr + "\n");
                continue;
            }
            if (!doctorControl.checkRoomAvailability(room)) {
                Report.cprintln("Room " + room + " is currently occupied. Please choose another.\n");
                continue;
            }
            break;
        }

        // ----- Gender -----
        String gender;
        while (true) {
            gender = readLine("Enter Gender (M/F) (or 0 to cancel): ").toUpperCase();
            if ("0".equals(gender)) return;
            String err = Validation.validateGender(gender);
            if (err == null) break;
            Report.cprintln(err + "\n");
        }

        // ----- IC Number -----
        String icNumber;
        while (true) {
            icNumber = readLine("Enter IC Number (format: XXXXXX-XX-XXXX) (or 0 to cancel): ");
            if ("0".equals(icNumber)) return;
            String err = Validation.validateMalaysianIC(icNumber);
            if (err == null) break;
            Report.cprintln(err + "\n");
        }

        // ----- Phone -----
        String phoneNum;
        while (true) {
            phoneNum = readLine("Enter Phone Number (e.g., 0123456789) (or 0 to cancel): ");
            if ("0".equals(phoneNum)) return;
            String err = Validation.validatePhone(phoneNum);
            if (err == null) break;
            Report.cprintln(err + "\n");
        }

        // ----- Create -----
        doctorControl.addDoctor(name, room, gender, icNumber, phoneNum);
        Report.cprintln("Doctor registered successfully!");
    }

    private void removeDoctor() {
        boolean loop = true;
        while (loop) {
            displayAllDoctors();
            String doctorID = readLine("Enter Doctor ID to remove (or 0 to cancel): ").toUpperCase();

            if ("0".equals(doctorID)) {
                Report.cprintln("Removal cancelled.");
                break;
            }

            Doctor d = doctorControl.getDoctorById(doctorID);
            if (d == null) {
                Report.cprintln("Doctor ID not found. Please try again.");
            } else {
                doctorControl.removeDoctorById(doctorID);
                Report.cprintln("Doctor removed successfully.");
                loop = false;
            }
        }
    }

    private void viewDoctorSchedule() {
        displayAllDoctors();

        Doctor doctor;
        while (true) {
            String doctorId = readLine("Enter Doctor ID to view schedule table (or 0 to cancel): ").toUpperCase();
            if ("0".equals(doctorId)) {
                Report.cprintln("Operation cancelled.");
                return;
            }
            doctor = doctorControl.getDoctorById(doctorId);
            if (doctor != null) break;
            Report.cprintln("Doctor ID not found. Try again.");
        }

        Report.cprintln("");
        doctor.getDutySchedule().printScheduleTable(doctor.getName());
    }

    private void updateDoctorSchedule() {
        displayAllDoctors();
        String doctorId = readLine("Enter Doctor ID to update schedule (or 0 to cancel): ").toUpperCase();
        if ("0".equals(doctorId)) return;

        Doctor doctor = doctorControl.getDoctorById(doctorId);
        if (doctor == null) {
            Report.cprintln("Doctor ID not found.");
            return;
        }

        // Show current schedule
        doctor.getDutySchedule().printScheduleTable(doctor.getName());

        // For each day, prompt a new session or skip
        for (java.time.DayOfWeek day : java.time.DayOfWeek.values()) {
            String in = readLine("Enter session for " + day + " (REST/MORNING/AFTERNOON/NIGHT) or leave blank to skip: ").toUpperCase();
            if (in.isBlank()) continue;
            try {
                Session newSession = Session.valueOf(in);
                doctor.getDutySchedule().setDaySession(day, newSession);
            } catch (IllegalArgumentException e) {
                Report.cprintln("Invalid session. Skipping " + day);
            }
        }

        Report.cprintln("Schedule updated successfully!");
        doctor.getDutySchedule().printScheduleTable(doctor.getName());
    }

    // =========================
    // ======= LIST VIEWS ======
    // =========================
    private void displayAllDoctors() {
        ClinicADT<Doctor> allDoctors = doctorControl.getAllDoctors();
        if (allDoctors.isEmpty()) {
            System.out.println("No doctors found.");
            return;
        }

        // Define table separator and format for each row
        String line = "+------------+------------------------+--------+---------------------+-------------------+------+"; // Adjusted width for better spacing
        String fmt  = "| %-10s | %-22s | %-6s | %-19s | %-17s | %-4s |"; // Adjusted column widths for consistency

        // Print table header
        System.out.println("Registered Doctors:");
        Report.cprintln(line);
        Report.cprintf(fmt, "DoctorID", "Name", "Gender", "IC Number", "Phone", "Room");
        Report.cprintln(line);

        // Iterate through each doctor and display their details
        ClinicADT.MyIterator<Doctor> it = allDoctors.iterator();
        while (it.hasNext()) {
            Doctor d = it.next();
            Report.cprintf(fmt,
                    d.getId(),
                    d.getName(),
                    d.getGender(),
                    d.getIcNumber(),
                    d.getPhoneNumber(),
                    d.getRoomNumber());
        }

        System.out.println(line); // End of table
    }

    // =========================
    // ======== REPORTS ========
    // =========================
    private void doctorWorkloadReport() {
        Report.printHeader("Doctor Duty Frequency Report");

        // Summary table
        String line = "+------------------+--------------------+";
        String headerFormat = "| %-16s | %-18s |";
        String rowFormat    = "| %-16s | %-18d |";

        Report.cprintln(line);
        Report.cprintf(headerFormat, "Doctor", "Duty Sessions/Week");
        Report.cprintln(line);

        int totalDuties = 0;
        ClinicADT<Doctor> allDoctors = doctorControl.getAllDoctors();
        ClinicADT.MyIterator<Doctor> it = allDoctors.iterator();

        while (it.hasNext()) {
            Doctor doc = it.next();
            int dutyCount = 0;

            DutySchedule sch = doc.getDutySchedule();
            dutyCount += isOnDuty(sch.getSessionForDay(java.time.DayOfWeek.MONDAY))     ? 1 : 0;
            dutyCount += isOnDuty(sch.getSessionForDay(java.time.DayOfWeek.TUESDAY))    ? 1 : 0;
            dutyCount += isOnDuty(sch.getSessionForDay(java.time.DayOfWeek.WEDNESDAY))  ? 1 : 0;
            dutyCount += isOnDuty(sch.getSessionForDay(java.time.DayOfWeek.THURSDAY))   ? 1 : 0;
            dutyCount += isOnDuty(sch.getSessionForDay(java.time.DayOfWeek.FRIDAY))     ? 1 : 0;
            dutyCount += isOnDuty(sch.getSessionForDay(java.time.DayOfWeek.SATURDAY))   ? 1 : 0;
            dutyCount += isOnDuty(sch.getSessionForDay(java.time.DayOfWeek.SUNDAY))     ? 1 : 0;

            Report.cprintf(rowFormat, doc.getName(), dutyCount);
            totalDuties += dutyCount;
        }

        Report.cprintln(line);
        Report.cprintf("| %-16s | %-18d |", "TOTAL", totalDuties);
        Report.cprintln(line);

        // Weekly bar grid
        Report.cprintln("");
        Report.cprintln("Duty Frequency by Day:");
        Report.cprintln("+------------------+-----------------------------+");
        Report.cprintln("| Doctor             Mon Tue Wed Thu Fri Sat Sun |");
        Report.cprintln("+------------------+-----------------------------+");

        it = allDoctors.iterator();
        int mon=0,tue=0,wed=0,thu=0,fri=0,sat=0,sun=0;

        while (it.hasNext()) {
            Doctor doc = it.next();
            DutySchedule ds = doc.getDutySchedule();

            StringBuilder row = new StringBuilder();
            row.append(String.format("| %-18s", doc.getName()));

            Session sMon = ds.getSessionForDay(java.time.DayOfWeek.MONDAY);
            row.append(isOnDuty(sMon) ? "  * " : "    "); if (isOnDuty(sMon)) mon++;

            Session sTue = ds.getSessionForDay(java.time.DayOfWeek.TUESDAY);
            row.append(isOnDuty(sTue) ? "  * " : "    "); if (isOnDuty(sTue)) tue++;

            Session sWed = ds.getSessionForDay(java.time.DayOfWeek.WEDNESDAY);
            row.append(isOnDuty(sWed) ? "  * " : "    "); if (isOnDuty(sWed)) wed++;

            Session sThu = ds.getSessionForDay(java.time.DayOfWeek.THURSDAY);
            row.append(isOnDuty(sThu) ? "  * " : "    "); if (isOnDuty(sThu)) thu++;

            Session sFri = ds.getSessionForDay(java.time.DayOfWeek.FRIDAY);
            row.append(isOnDuty(sFri) ? "  * " : "    "); if (isOnDuty(sFri)) fri++;

            Session sSat = ds.getSessionForDay(java.time.DayOfWeek.SATURDAY);
            row.append(isOnDuty(sSat) ? "  * " : "    "); if (isOnDuty(sSat)) sat++;

            Session sSun = ds.getSessionForDay(java.time.DayOfWeek.SUNDAY);
            row.append(isOnDuty(sSun) ? "  * " : "    "); if (isOnDuty(sSun)) sun++;

            row.append(" |");
            Report.cprintln(row.toString());
        }

        Report.cprintln("+------------------+-----------------------------+");

        // max / min day(s)
        int max = Math.max(Math.max(Math.max(mon,tue), Math.max(wed,thu)), Math.max(Math.max(fri,sat), sun));
        int min = Math.min(Math.min(Math.min(mon,tue), Math.min(wed,thu)), Math.min(Math.min(fri,sat), sun));

        StringBuilder maxDays = new StringBuilder();
        if (mon==max) maxDays.append("Mon ");
        if (tue==max) maxDays.append("Tue ");
        if (wed==max) maxDays.append("Wed ");
        if (thu==max) maxDays.append("Thu ");
        if (fri==max) maxDays.append("Fri ");
        if (sat==max) maxDays.append("Sat ");
        if (sun==max) maxDays.append("Sun ");

        StringBuilder minDays = new StringBuilder();
        if (mon==min) minDays.append("Mon ");
        if (tue==min) minDays.append("Tue ");
        if (wed==min) minDays.append("Wed ");
        if (thu==min) minDays.append("Thu ");
        if (fri==min) minDays.append("Fri ");
        if (sat==min) minDays.append("Sat ");
        if (sun==min) minDays.append("Sun ");

        Report.cprintln("");
        Report.cprintln("Day(s) with Most Doctors: " + maxDays + "(" + max + ")");
        Report.cprintln("Day(s) with Least Doctors: " + minDays + "(" + min + ")");

        Report.printFooter();
    }

    private void doctorInformationReport() {
        Report.printHeader("Doctors Information Report");

        ClinicADT<Doctor> all = doctorControl.getAllDoctors();
        if (all.isEmpty()) {
            Report.cprintln("(No doctors found.)");
            Report.printFooter();
            return;
        }

        String line = "+----------+----------------------+--------+-----------------+--------------+------+";
        String header = String.format("| %-8s | %-20s | %-6s | %-15s | %-12s | %-4s |",
                "DoctorID","Name","Gender","IC Number","Phone","Room");

        Report.cprintln(line);
        Report.cprintln(header);
        Report.cprintln(line);

        ClinicADT.MyIterator<Doctor> it = all.iterator();
        while (it.hasNext()) {
            Doctor d = it.next();
            String row = String.format("| %-8s | %-20s | %-6s | %-15s | %-12s | %-4s |",
                    d.getId(), d.getName(), d.getGender(), d.getIcNumber(), d.getPhoneNumber(), d.getRoomNumber());
            Report.cprintln(row);
        }

        Report.cprintln(line);
        Report.printFooter();
    }

   private void doctorRoomAllocationReport() {
        Report.printHeader("Doctors Room Allocation Report");

        // Define a formatted line and row for the table
        String line = "+----------+------------------------+--------+-----------------+--------------+------+";
        String fmt  = "| %-8s | %-20s | %-6s | %-15s | %-12s | %-4s |";

        Report.cprintln(line);
        Report.cprintf(fmt, "DoctorID", "Name", "Gender", "IC Number", "Phone", "Room");
        Report.cprintln(line);

        // Iterate over the doctors and display the table with their data
        ClinicADT.MyIterator<Doctor> it = doctorControl.getAllDoctors().iterator();
        while (it.hasNext()) {
            Doctor d = it.next();
            Report.cprintf(fmt,
                    d.getId(),
                    d.getName(),
                    d.getGender(),
                    d.getIcNumber(),
                    d.getPhoneNumber(),
                    d.getRoomNumber());
        }

        Report.cprintln(line);
        Report.printFooter();
    }



    private void doctorContactListReport() {
        Report.printHeader("Doctors Contact List Report");

        // Define a formatted line and row for the table
        String line = "+------------------+--------------+";
        String fmt  = "| %-16s | %-12s |";

        Report.cprintln(line);
        Report.cprintf(fmt, "Doctor", "Phone");
        Report.cprintln(line);

        // Iterate over the doctors and display the table with their contact information
        ClinicADT.MyIterator<Doctor> it = doctorControl.getAllDoctors().iterator();
        while (it.hasNext()) {
            Doctor d = it.next();
            Report.cprintf(fmt, d.getName(), d.getPhoneNumber());
        }

        Report.cprintln(line);
        Report.printFooter();
    }

   private void doctorOnDutyTodayReport() {
        Report.printHeader("Doctors On-Duty Today Report");

        // Define a formatted line and row for the table with consistent column widths
        String line = "+------------------+-----------+";
        String fmt  = "| %-16s | %-9s |";  // Adjusting column width for better alignment

        // Print the header and table line
        Report.cprintln(line);
        Report.cprintf(fmt, "Doctor", "Session");
        Report.cprintln(line);

        // Get today's day of the week
        java.time.DayOfWeek today = java.time.LocalDate.now().getDayOfWeek();
        ClinicADT.MyIterator<Doctor> it = doctorControl.getAllDoctors().iterator();
        while (it.hasNext()) {
            Doctor d = it.next();
            Session s = d.getDutySchedule().getSessionForDay(today);
            // Print each doctor with their session, aligned properly
            Report.cprintf(fmt, d.getName(), s == null ? "REST" : s.toString());
        }

        Report.cprintln(line);
        Report.printFooter();
    }



    // =========================
    // ======= UTILITIES =======
    // =========================
    private int getNextAvailableRoom() {
        for (int i = 1; i <= MAX_ROOMS; i++) {
            if (doctorControl.checkRoomAvailability(i)) return i;
        }
        return -1; // none
    }

    private boolean isOnDuty(Session s) {
        return s != null && s != Session.REST && !s.toString().isBlank();
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int readInt(String prompt) {
        while (true) {
            String s = readLine(prompt);
            try { return Integer.parseInt(s); }
            catch (NumberFormatException e) { Report.cprintln("Please enter a valid number."); }
        }
    }
}
