/** Team Member: Ting Rong You, Yong Chong Xin, Anson Chang, Lim Wen Liang
 *  
 * 
 */

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package tarumtclinicmanagementsystem;

import java.util.Scanner;

/**
 *
 * @author Acer
 */
public class TARUMTClinicManagementSystem {

    public static void main(String[] args) {
        //added new lines here
        DoctorControl doctorControl = new DoctorControl();
        TreatmentUI treatmentUI = new TreatmentUI(doctorControl);
        
        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            System.out.println("\n=== Clinic Management System ===");
            System.out.println("1. Patient Management");
            System.out.println("2. Doctor Management");
            System.out.println("3. Consultation Management");
            System.out.println("4. Medical Treatment Management"); //added
            System.out.println("5. Pharmacy Management");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");
            choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    PatientUI patientUI = new PatientUI();
                    patientUI.run();
                    break;
                case 2:
                    DoctorUI doctorUI = new DoctorUI(doctorControl);
                    doctorUI.run();
                    break;
                case 3:
                    ConsultationUI consultationUI = new ConsultationUI();
                    consultationUI.run();
                    break;
                case 4: 
                    //added
                    treatmentUI.run();
                    break;
                case 5:
                    PharmacyUI pharmacyUI = new PharmacyUI();
                    pharmacyUI.run();
                case 0:
                    System.out.println("Thank you for using the system.");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }

        } while (choice != 0);
    }
}
