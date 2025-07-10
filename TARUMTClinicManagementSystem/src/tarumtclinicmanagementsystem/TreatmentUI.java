/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtclinicmanagementsystem;

/**
 *
 * @author User
 */

import java.time.LocalDate;
import java.util.Scanner;

public class TreatmentUI {
    private final TreatmentControl control;
    private final DoctorControl doctorControl;
    private final Scanner scanner;

    public TreatmentUI(DoctorControl doctorControl){
        this.control = new TreatmentControl();
        this.doctorControl = doctorControl;
        this.scanner = new Scanner(System.in);
    }

    public void run(){
        int choice;
        do {
            System.out.println("\n=== Medical Treatment Management ===");
            System.out.println("1. Add New Treatment");
            System.out.println("2. View Patient Treatment History");
            System.out.println("3. Process Next Follow-Up");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");

            try {
                choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1 -> addTreatment();
                    case 2 -> viewPatientHistory();
                    case 3 -> processFollowUp();
                    case 0 -> System.out.println("Returning to main menu...");
                    default -> System.out.println("Invalid choice.");
                }
            } catch(Exception e) {
                System.out.println("Error! Invalid input. Please try again.");
                scanner.nextLine();
                choice = -1;
            }
        } while (choice != 0);
    }

    private void addTreatment() {
        try {
            System.out.print("Enter Patient ID: ");
            String patientId = scanner.nextLine();
            System.out.print("Enter Diagnosis: ");
            String diagnosis = scanner.nextLine();
            System.out.print("Prescription: ");
            String prescription = scanner.nextLine();
            System.out.print("Follow-up needed? (yes/no): ");
            String followUpInput = scanner.nextLine().trim().toLowerCase();
            boolean isFollowUp = followUpInput.equals("yes");

            Doctor assignedDoc = getAvailableDoctorId();
            if (assignedDoc == null) {
                System.out.println("No available doctors. Treatment cannot be added.");
                return;
            }

            MedicalTreatment treatment = new MedicalTreatment(
                patientId, assignedDoc.getId(), diagnosis, prescription, LocalDate.now(), isFollowUp
            );

            control.addTreatment(treatment);
            System.out.println("Treatment assigned to Dr. " + assignedDoc.getName());
        } catch(Exception e){
            System.out.println("Error! " + e.getMessage());
            scanner.nextLine();
        }
    }

    private void viewPatientHistory() {
        System.out.print("Enter Patient ID: ");
        String patientId = scanner.nextLine();
        ClinicADT<MedicalTreatment> treatments = control.getTreatmentsByPatient(patientId);

        if (treatments.isEmpty()) {
            System.out.println("No treatment record found.");
        } else {
            System.out.println("\n=== Treatment History ===");
            for (int i = 0; i < treatments.size(); i++) {
                System.out.println(treatments.get(i));
            }
        }
    }

    private void processFollowUp() {
        try {
            MedicalTreatment nextFollowUp = control.processNextFollowUp();
            if (nextFollowUp == null) {
                System.out.println("No follow-ups in queue.");
            } else {
                System.out.println("Processing follow-up: \n" + nextFollowUp);
            }
        } catch (Exception e) {
            System.out.println("Error! " + e.getMessage());
        }
    }

    private Doctor getAvailableDoctorId() {
        for (int i = 0; i < doctorControl.getDoctorCount(); i++) {
            Doctor doc = doctorControl.getDoctorByIndex(i);
            if (doc != null && doc.isAvailable()) {
                return doc;
            }
        }
        return null;
    }
}

