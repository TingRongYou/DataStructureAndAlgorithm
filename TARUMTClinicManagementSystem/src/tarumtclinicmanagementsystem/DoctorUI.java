/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtclinicmanagementsystem;

/**
 *
 * @author Acer
 */

import java.util.Scanner;

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
            System.out.println("\n=== Doctor Management Menu ===");
            System.out.println("1. Register Doctor");
            System.out.println("2. Display All Doctors");
            System.out.println("3. View Doctor Schedule Table");
            System.out.println("0. Exit");
            System.out.print("Choice: ");

            while (!scanner.hasNextInt()) {
                System.out.print("Invalid input. Please enter a number: ");
                scanner.next(); // consume invalid
            }
            choice = scanner.nextInt();
            scanner.nextLine(); // clear input buffer

            switch (choice) {
                case 1:
                    registerDoctor();
                    break;
                case 2:
                    doctorControl.displayAllDoctors();
                    break;
                case 3:
                    doctorControl.displayAllDoctors();
                    viewDoctorSchedule();
                    break;
                case 0:
                    System.out.println("Exiting Doctor Management.");
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        } while (choice != 0);
    }

    private void registerDoctor() {
    System.out.print("Enter Doctor Name: ");
    String name = scanner.nextLine();

    int room;
    while (true) {
        System.out.print("Enter Room Number (1–10): ");
        if (scanner.hasNextInt()) {
            room = scanner.nextInt();
            scanner.nextLine(); // consume newline
            if (room >= 1 && room <= 10) break;
            else System.out.println("Room must be between 1 and 10.");
        } else {
            System.out.println("Please enter a valid number for room.");
            scanner.next(); // discard invalid input
        }
    }

        String gender;
        while (true) {
            System.out.print("Enter Gender (M/F): ");
            gender = scanner.nextLine().trim().toUpperCase();
            if (gender.equals("M") || gender.equals("F")) break;
            else System.out.println("Invalid gender. Please enter 'M' or 'F'.");
        }

        String icNumber;
        while (true) {
            System.out.print("Enter IC Number (format: XXXXXX-XX-XXXX): ");
            icNumber = scanner.nextLine().trim();
            if (icNumber.matches("\\d{6}-\\d{2}-\\d{4}")) break;
            else System.out.println("Invalid IC format.");
        }

        String phoneNum;
        while (true) {
            System.out.print("Enter Phone Number (starts with 01, 10–11 digits): ");
            phoneNum = scanner.nextLine().trim();
            if (phoneNum.matches("01\\d{8,9}")) break;
            else System.out.println("Invalid Malaysian phone number.");
        }

        doctorControl.addDoctor(name, room, gender, icNumber, phoneNum);
    }


    private void viewDoctorSchedule() {
       System.out.print("Enter Doctor ID to view schedule table: ");
        String doctorId = scanner.nextLine().trim().toUpperCase();

        Doctor doctor = doctorControl.getDoctorById(doctorId);
        if (doctor != null) {
            System.out.println();
            doctor.getDutySchedule().printScheduleTable(doctor.getName());
        } else {
            System.out.println("Doctor ID not found.");
        }
    }
}


