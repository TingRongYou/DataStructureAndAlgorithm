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
                    boolean validDoctor;
                    do {
                        displayAllDoctors();
                        System.out.print("Enter Doctor ID to remove (or 0 to cancel): ");
                        String doctorID = scanner.nextLine().trim().toUpperCase();

                        if (doctorID.equals("0")) {
                            System.out.println("Removal cancelled.");
                            break; // Exit this case
                        }

                        validDoctor = doctorControl.getDoctorById(doctorID) != null;
                        if (!validDoctor) {
                            System.out.println("Doctor ID not found. Please try again.");
                        } else {
                            doctorControl.removeDoctorById(doctorID);
                        }
                    } while (!validDoctor);
                }
                case 3 -> displayAllDoctors();
                case 4 -> {
                    displayAllDoctors();
                    viewDoctorSchedule();
                }
                case 5 -> {
                    boolean validDoctor;
                    do {
                        displayAllDoctors();
                        System.out.print("Enter Doctor ID to update schedule (or 0 to cancel): ");
                        String doctorId = scanner.nextLine().trim().toUpperCase();

                        if (doctorId.equals("0")) {
                            System.out.println("Update cancelled.");
                            break;
                        }

                        validDoctor = doctorControl.getDoctorById(doctorId) != null;
                        if (!validDoctor) {
                            System.out.println("Doctor ID not found. Please try again.");
                        } else {
                            doctorControl.updateDoctorScheduleById(doctorId, scanner);
                        }
                    } while (!validDoctor);
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

        // ===== Doctor Name =====
        do {
            System.out.print("Enter Doctor Name (or 0 to cancel): ");
            name = scanner.nextLine();
            if (name.equals("0")) return;
            error = Validation.validateName(name);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        // ===== Room Number =====
        int room;
        do {
            System.out.print("Enter Room Number (1–10) (or 0 to cancel): ");
            String roomInput = scanner.nextLine().trim();
            if (roomInput.equals("0")) return;
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
    }

    private void viewDoctorSchedule() {
        String doctorId;
        String error;
        Doctor doctor = null;

        do {
            System.out.print("Enter Doctor ID to view schedule table (or 0 to cancel): ");
            doctorId = scanner.nextLine().trim().toUpperCase();
            
            if (doctorId.equals("0")) {
                System.out.println("Operation cancelled.");
                return;  // exit method if user cancels
            }
            error = Validation.validateDoctorId(doctorId);
            if (error == null) {
                doctor = doctorControl.getDoctorById(doctorId);
                if (doctor == null) {
                    error = "Doctor ID not found.\n";
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
