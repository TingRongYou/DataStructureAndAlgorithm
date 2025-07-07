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
    private DoctorControl control;
    private Scanner scanner;

    public DoctorUI() {
        control = new DoctorControl();
        scanner = new Scanner(System.in);
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
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");

            choice = scanner.nextInt();
            scanner.nextLine(); // Clear newline

            switch (choice) {
                case 1:
                    System.out.print("Enter Doctor ID: ");
                    String id = scanner.nextLine();
                    System.out.print("Enter Doctor Name: ");
                    String name = scanner.nextLine();
                    System.out.print("Enter Duty Schedule: ");
                    String schedule = scanner.nextLine();
                    System.out.print("Is Available (true/false): ");
                    boolean available = scanner.nextBoolean();
                    control.addDoctor(id, name, schedule, available);
                    break;

                case 2:
                    control.displayAllDoctors();
                    System.out.print("Enter index to remove: ");
                    int removeIndex = scanner.nextInt();
                    control.removeDoctorByIndex(removeIndex);
                    break;

                case 3:
                    control.displayAllDoctors();
                    break;

                case 4:
                    control.displayAllDoctors();
                    System.out.print("Enter index to update schedule: ");
                    int schedIndex = scanner.nextInt();
                    scanner.nextLine();
                    System.out.print("Enter new schedule: ");
                    String newSched = scanner.nextLine();
                    control.updateDoctorSchedule(schedIndex, newSched);
                    break;

                case 5:
                    control.displayAllDoctors();
                    System.out.print("Enter index to update availability: ");
                    int availIndex = scanner.nextInt();
                    System.out.print("Enter availability (true/false): ");
                    boolean newAvail = scanner.nextBoolean();
                    control.updateDoctorAvailability(availIndex, newAvail);
                    break;

                case 6:
                    System.out.println("Total doctors: " + control.getDoctorCount());
                    break;

                case 0:
                    System.out.println("Exiting Doctor Management.");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

        } while (choice != 0);
    }
}

