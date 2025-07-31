package boundary;

import java.util.Scanner;
import entity.Doctor;
import control.DoctorControl;
import utility.Validation;
import adt.ClinicADT;

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
                case 2 -> {
                    displayAllDoctors();
                    System.out.print("Enter Doctor ID to remove: ");
                    String doctorID = scanner.nextLine().trim().toUpperCase();
                    doctorControl.removeDoctorById(doctorID);
                }
                case 3 -> displayAllDoctors();
                case 4 -> {
                    displayAllDoctors();
                    viewDoctorSchedule();
                }
                case 5 -> {
                    displayAllDoctors();
                    System.out.print("Enter Doctor ID to update schedule: ");
                    String doctorId = scanner.nextLine().trim().toUpperCase();
                    doctorControl.updateDoctorScheduleById(doctorId, scanner);
                }
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

        do {
            System.out.print("Enter Doctor Name: ");
            name = scanner.nextLine();
            error = Validation.validateName(name);
            if (error != null) System.out.println(error);
        } while (error != null);

        int room;
        do {
            System.out.print("Enter Room Number (1–10): ");
            try {
                room = Integer.parseInt(scanner.nextLine());
                error = Validation.validateRoomNumber(room);
                if (error != null) {
                    System.out.println(error);
                } else if (!doctorControl.checkRoomAvailability(room)) {
                    error = "Room " + room + " is currently occupied. Please choose another.";
                    System.out.println(error);
                }
            } catch (NumberFormatException e) {
                error = "Please enter a valid number";
                System.out.println(error);
                room = -1;
            }
        } while (error != null);

        String gender;
        do {
            System.out.print("Enter Gender (M/F): ");
            gender = scanner.nextLine().trim().toUpperCase();
            error = Validation.validateGender(gender);
            if (error != null) System.out.println(error);
        } while (error != null);

        String icNumber;
        do {
            System.out.print("Enter IC Number (format: XXXXXX-XX-XXXX): ");
            icNumber = scanner.nextLine().trim();
            error = Validation.validateMalaysianIC(icNumber);
            if (error != null) System.out.println(error);
        } while (error != null);

        String phoneNum;
        do {
            System.out.print("Enter Phone Number (e.g., 0123456789): ");
            phoneNum = scanner.nextLine().trim();
            error = Validation.validatePhone(phoneNum);
            if (error != null) System.out.println(error);
        } while (error != null);

        doctorControl.addDoctor(name, room, gender, icNumber, phoneNum);
    }

    private void viewDoctorSchedule() {
        String doctorId;
        String error;
        Doctor doctor = null;

        do {
            System.out.print("Enter Doctor ID to view schedule table: ");
            doctorId = scanner.nextLine().trim().toUpperCase();
            error = Validation.validateDoctorId(doctorId);
            if (error == null) {
                doctor = doctorControl.getDoctorById(doctorId);
                if (doctor == null) {
                    error = "Doctor ID not found.";
                }
            }
            if (error != null) System.out.println(error);
        } while (error != null);

        System.out.println();
        doctor.getDutySchedule().printScheduleTable(doctor.getName());
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

        for (Doctor d : allDoctors) { // uses iterator()
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
}
