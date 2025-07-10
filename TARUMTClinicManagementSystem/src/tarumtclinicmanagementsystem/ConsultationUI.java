/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author User
 */

package tarumtclinicmanagementsystem;

import java.time.LocalDateTime;
import java.util.Scanner;

public class ConsultationUI {
    private ConsultationControl control;
    private Scanner sc;

    public ConsultationUI() {
        control = new ConsultationControl();
        sc = new Scanner(System.in);
    }

    public void run() {
        while (true) {
            System.out.println("\n===== Consultation Management =====");
            System.out.println("1. Add Consultation");
            System.out.println("2. Remove Consultation");
            System.out.println("3. List All Consultations");
            System.out.println("4. Search by Patient");
            System.out.println("5. Search by Doctor");                         // ✅ New
            System.out.println("6. Display All (Sorted by Date)");            // ✅ New
            System.out.println("7. Show Total Consultation Count");           // ✅ New
            System.out.println("0. Exit");
            System.out.print("Choice: ");

            int ch = sc.nextInt();
            sc.nextLine(); // consume newline

            switch (ch) {
                case 1 -> addConsultation();
                case 2 -> removeConsultation();
                case 3 -> control.listConsultations();
                case 4 -> searchByPatient();
                case 5 -> searchByDoctor();                    // ✅
                case 6 -> control.printConsultationsSortedByDate(); // ✅
                case 7 -> System.out.println("Total Consultations: " + control.getTotalConsultations()); // ✅
                case 0 -> {
                    System.out.println("Exiting Consultation Module...");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void addConsultation() {
        System.out.print("Patient Name: ");
        String patient = sc.nextLine();
        System.out.print("Doctor Name: ");
        String doctor = sc.nextLine();
        System.out.print("Enter date (YYYY-MM-DD): ");
        String date = sc.nextLine();
        System.out.print("Enter time (HH:MM): ");
        String time = sc.nextLine();

        try {
            LocalDateTime dateTime = LocalDateTime.parse(date + "T" + time + ":00");
            control.addConsultation(patient, doctor, dateTime);
        } catch (Exception e) {
            System.out.println("Invalid datetime format. Use YYYY-MM-DD and HH:MM.");
        }
    }

    private void removeConsultation() {
        System.out.print("Enter Consultation ID to remove: ");
        int id = sc.nextInt();
        sc.nextLine();
        control.removeConsultationById(id);
    }

    private void searchByPatient() {
        System.out.print("Enter patient name: ");
        String name = sc.nextLine();
        control.searchByPatient(name);
    }

    private void searchByDoctor() {
        System.out.print("Enter doctor name: ");
        String name = sc.nextLine();
        control.searchByDoctor(name);
    }
}

