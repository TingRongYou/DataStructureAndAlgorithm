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
            System.out.println("\n1. Add Consultation");
            System.out.println("2. Remove Consultation");
            System.out.println("3. List All");
            System.out.println("4. Search by Patient");
            System.out.println("0. Exit");
            System.out.print("Choice: ");
            int ch = sc.nextInt();
            sc.nextLine(); // consume newline

            switch (ch) {
                case 1: addConsultation(); break;
                case 2: removeConsultation(); break;
                case 3: control.listConsultations(); break;
                case 4: searchConsultation(); break;
                case 0:
                    System.out.println("Exiting...");
                    return;
                default:
                    System.out.println("Invalid.");
            }
        }
    }

    private void addConsultation() {
        System.out.print("Patient Name: ");
        String p = sc.nextLine();
        System.out.print("Doctor Name: ");
        String d = sc.nextLine();
        System.out.print("Enter date (YYYY-MM-DD): ");
        String date = sc.nextLine();
        System.out.print("Enter time (HH:MM): ");
        String time = sc.nextLine();

        try {
            LocalDateTime dateTime = LocalDateTime.parse(date + "T" + time + ":00");
            control.addConsultation(p, d, dateTime);
        } catch (Exception e) {
            System.out.println("Invalid datetime format.");
        }
    }

    private void removeConsultation() {
        System.out.print("Enter Consultation ID: ");
        int id = sc.nextInt();
        control.removeConsultationById(id);
    }

    private void searchConsultation() {
        System.out.print("Enter patient name: ");
        String name = sc.nextLine();
        control.searchByPatient(name);
    }
}

