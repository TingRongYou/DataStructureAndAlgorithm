/** Team Member: Ting Rong You, Yong Chong Xin, Anson Chang, Lim Wen Liang
 *  
 * 
 *
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
        DoctorControl doctorControl = new DoctorControl();
        PatientControl patientControl = new PatientControl();
        ClinicADT<Consultation> consultations = new MyClinicADT<>();
        ClinicADT<MedicalTreatment> treatments = new MyClinicADT<>();

        ConsultationControl consultationControl = new ConsultationControl(patientControl, doctorControl, consultations, treatments);

        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            System.out.println("\n=== Clinic Management System ===");
            System.out.println("1. Patient Management");
            System.out.println("2. Doctor Management");
            System.out.println("3. Consultation Management");
            System.out.println("4. Medical Treatment Management");
            System.out.println("5. Pharmacy Management");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");
            while (!scanner.hasNextInt()) {
                System.out.print("Invalid input. Enter a number: ");
                scanner.next();
            }
            choice = scanner.nextInt();

            switch (choice) {
                case 1 -> new PatientUI(patientControl).run();
                case 2 -> new DoctorUI(doctorControl).run();
                case 3 -> new ConsultationUI(patientControl, doctorControl, consultations, treatments).run();
                case 4 -> new TreatmentUI(patientControl, doctorControl, consultations, treatments, consultationControl).run();
                case 5 -> new PharmacyUI().run();
                case 0 -> System.out.println("Thank you for using the system.");
                default -> System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 0);
    }
}
