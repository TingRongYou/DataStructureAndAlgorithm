package boundary;

import java.util.Scanner;
import entity.Doctor;
import control.DoctorControl;
import utility.Validation;
import adt.ClinicADT;
import java.time.DayOfWeek;
import tarumtclinicmanagementsystem.Session;

public class DoctorUI {
    private final DoctorControl doctorControl;
    private final Scanner scanner;

    public DoctorUI(DoctorControl doctorControl) {
        this.doctorControl = doctorControl;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        int choice;
        do {
            System.out.println("\n=== Doctor Management Module ===");
            System.out.println("1. Add Doctor");
            System.out.println("2. Remove Doctor");
            System.out.println("3. Display All Doctors");
            System.out.println("4. View Doctor Schedule Table");
            System.out.println("5. Update Doctor Schedule");
            System.out.println("6. Show Doctor Count");
            System.out.println("7. Display Doctors Sorted by Name");
            System.out.println("8. Display Only Available Doctors");
            System.out.println("0. Exit");
            System.out.print("Choice: ");

            while (!scanner.hasNextInt()) {
                System.out.print("Invalid input. Please enter a number: ");
                scanner.next();
            }
            choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1 -> registerDoctor();
                case 2 -> removeDoctor();
                case 3 -> displayAllDoctors();
                case 4 -> viewDoctorSchedule();
                case 5 -> updateDoctorSchedule();
                case 6 -> System.out.println("Total doctors: " + doctorControl.getDoctorCount());
                case 7 -> doctorControl.printDoctorsSortedByName();
                case 8 -> doctorControl.printAvailableDoctors();
                case 0 -> System.out.println("Exiting Doctor Management.");
                default -> System.out.println("Invalid choice.");
            }
        } while (choice != 0);
    }

    private void registerDoctor() {
        String error;
        String name;

        // ===== Doctor Name =====
        do {
            System.out.print("Enter Doctor Name (or 0 to cancel): ");
            name = scanner.nextLine();
            if (name.equals("0")) return;
            error = Validation.validateName(name);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        // ===== Room Number (auto-assign if blank) =====
        int room = -1;
        do {
            System.out.print("Enter Room Number (1–10) (press Enter to auto-assign, 0 to cancel): ");
            String roomInput = scanner.nextLine().trim();
            if (roomInput.equals("0")) return;
            if (roomInput.isEmpty()) {
                room = getNextAvailableRoom();
                if (room == -1) {
                    System.out.println("No available rooms. Cannot register doctor.");
                    return;
                }
                System.out.println("Assigned Room: " + room);
                error = null;
            } else {
                try {
                    room = Integer.parseInt(roomInput);
                    error = Validation.validateRoomNumber(room);
                    if (error != null) {
                        System.out.println(error + "\n");
                    } else if (!doctorControl.checkRoomAvailability(room)) {
                        error = "Room " + room + " is currently occupied. Please choose another.";
                        System.out.println(error + "\n");
                    }
                } catch (NumberFormatException e) {
                    error = "Please enter a valid number";
                    System.out.println(error + "\n");
                    room = -1;
                }
            }
        } while (error != null);

        // ===== Gender =====
        String gender;
        do {
            System.out.print("Enter Gender (M/F) (or 0 to cancel): ");
            gender = scanner.nextLine().trim().toUpperCase();
            if (gender.equals("0")) return;
            error = Validation.validateGender(gender);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        // ===== IC Number =====
        String icNumber;
        do {
            System.out.print("Enter IC Number (format: XXXXXX-XX-XXXX) (or 0 to cancel): ");
            icNumber = scanner.nextLine().trim();
            if (icNumber.equals("0")) return;
            error = Validation.validateMalaysianIC(icNumber);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        // ===== Phone Number =====
        String phoneNum;
        do {
            System.out.print("Enter Phone Number (e.g., 0123456789) (or 0 to cancel): ");
            phoneNum = scanner.nextLine().trim();
            if (phoneNum.equals("0")) return;
            error = Validation.validatePhone(phoneNum);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        // ===== Add Doctor =====
        doctorControl.addDoctor(name, room, gender, icNumber, phoneNum);
        System.out.println("Doctor registered successfully!");
    }

    private int getNextAvailableRoom() {
        for (int i = 1; i <= 10; i++) {
            if (doctorControl.checkRoomAvailability(i)) return i;
        }
        return -1; // no room available
    }

    private void displayAllDoctors() {
        ClinicADT<Doctor> allDoctors = doctorControl.getAllDoctors();
        if (allDoctors.isEmpty()) {
            System.out.println("No doctors found.");
            return;
        }

        String format = "| %-8s | %-20s | %-6s | %-15s | %-12s | %-4s |\n";
        String line = "+----------+----------------------+--------+-----------------+--------------+------+";;

        System.out.println("\nRegistered Doctors:");
        System.out.println(line);
        System.out.printf(format, "DoctorID", "Name", "Gender", "IC Number", "Phone", "Room");
        System.out.println(line);

        ClinicADT.MyIterator<Doctor> it = allDoctors.iterator();
        while (it.hasNext()) {
            Doctor d = it.next();
            System.out.printf(format,
                    d.getId(),
                    d.getName(),
                    d.getGender(),
                    d.getIcNumber(),
                    d.getPhoneNumber(),
                    d.getRoomNumber());
        }

        System.out.println(line);
    }

    private void viewDoctorSchedule() {
        displayAllDoctors();

        Doctor doctor = null;
        String doctorId;
        do {
            System.out.print("Enter Doctor ID to view schedule table (or 0 to cancel): ");
            doctorId = scanner.nextLine().trim().toUpperCase();
            if (doctorId.equals("0")) {
                System.out.println("Operation cancelled.");
                return;
            }

            doctor = doctorControl.getDoctorById(doctorId);
            if (doctor == null) System.out.println("Doctor ID not found. Try again.");
        } while (doctor == null);

        System.out.println();
        doctor.getDutySchedule().printScheduleTable(doctor.getName());
    }

    private void updateDoctorSchedule() {
        displayAllDoctors();
        System.out.println();
        System.out.print("Enter Doctor ID to update schedule (or 0 to cancel): ");
        String doctorId = scanner.nextLine().trim();
        if (doctorId.equals("0")) return;

        Doctor doctor = doctorControl.getDoctorById(doctorId);
        if (doctor == null) {
            System.out.println("Doctor ID not found.");
            return;
        }

        // Show current schedule
        doctor.getDutySchedule().printScheduleTable(doctor.getName());

        for (DayOfWeek day : DayOfWeek.values()) {
            System.out.printf("Enter session for %s (REST/MORNING/AFTERNOON/NIGHT) or leave blank to skip: ", day);
            String sessionInput = scanner.nextLine().trim().toUpperCase();
            if (sessionInput.isEmpty()) continue;

            try {
                Session newSession = Session.valueOf(sessionInput);
                doctor.getDutySchedule().setDaySession(day, newSession);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid session. Skipping " + day);
            }
        }

        System.out.println("Schedule updated successfully!");
        doctor.getDutySchedule().printScheduleTable(doctor.getName());
    }

    private void removeDoctor() {
        boolean validDoctor;
        do {
            displayAllDoctors();
            System.out.print("Enter Doctor ID to remove (or 0 to cancel): ");
            String doctorID = scanner.nextLine().trim().toUpperCase();

            if (doctorID.equals("0")) {
                System.out.println("Removal cancelled.");
                break;
            }

            validDoctor = doctorControl.getDoctorById(doctorID) != null;
            if (!validDoctor) {
                System.out.println("Doctor ID not found. Please try again.");
            } else {
                doctorControl.removeDoctorById(doctorID);
                System.out.println("Doctor removed successfully.");
            }
        } while (!validDoctor);
    }
}
