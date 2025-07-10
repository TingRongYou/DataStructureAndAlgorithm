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
            System.out.println("\n=== Doctor Management Module ===");
            System.out.println("1. Add Doctor");
            System.out.println("2. Remove Doctor");
            System.out.println("3. Display All Doctors");
            System.out.println("4. Update Doctor Schedule");
            System.out.println("5. Update Availability");
            System.out.println("6. Show Doctor Count");
            System.out.println("7. Display Doctors Sorted by Name");         // ✅ New
            System.out.println("8. Display Only Available Doctors");         // ✅ New
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");

            while (!scanner.hasNextInt()) {
                System.out.print("Please enter a valid number: ");
                scanner.next();
            }
            choice = scanner.nextInt();
            scanner.nextLine(); // Clear newline

            switch (choice) {
                case 1 -> {
                    System.out.print("Enter Doctor ID: ");
                    String id = scanner.nextLine();
                    System.out.print("Enter Doctor Name: ");
                    String name = scanner.nextLine();
                    System.out.print("Enter Duty Schedule: ");
                    String schedule = scanner.nextLine();
                    System.out.print("Is Available (true/false): ");
                    boolean available = scanner.nextBoolean();
                    doctorControl.addDoctor(id, name, schedule, available);
                }

                case 2 -> {
                    doctorControl.displayAllDoctors();
                    System.out.print("Enter index to remove: ");
                    int removeIndex = scanner.nextInt();
                    doctorControl.removeDoctorByIndex(removeIndex);
                }

                case 3 -> doctorControl.displayAllDoctors();

                case 4 -> {
                    doctorControl.displayAllDoctors();
                    System.out.print("Enter index to update schedule: ");
                    int schedIndex = scanner.nextInt();
                    scanner.nextLine();
                    System.out.print("Enter new schedule: ");
                    String newSched = scanner.nextLine();
                    doctorControl.updateDoctorSchedule(schedIndex, newSched);
                }

                case 5 -> {
                    doctorControl.displayAllDoctors();
                    System.out.print("Enter index to update availability: ");
                    int availIndex = scanner.nextInt();
                    System.out.print("Enter availability (true/false): ");
                    boolean newAvail = scanner.nextBoolean();
                    doctorControl.updateDoctorAvailability(availIndex, newAvail);
                }

                case 6 -> System.out.println("Total doctors: " + doctorControl.getDoctorCount());

                case 7 -> doctorControl.printDoctorsSortedByName();          // ✅ New

                case 8 -> doctorControl.printAvailableDoctors();             // ✅ New

                case 0 -> System.out.println("Exiting Doctor Management.");

                default -> System.out.println("Invalid choice.");
            }

        } while (choice != 0);
    }
}

