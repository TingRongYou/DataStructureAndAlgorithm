/**
 * Team Members: Ting Rong You, Yong Chong Xin, Anson Chang, Lim Wen Liang
 *
 * Main entry point for TARUMT Clinic Management System.
 */

package tarumtclinicmanagementsystem;

import entity.MedicalTreatment;
import entity.Consultation;
import control.DoctorControl;
import control.ConsultationControl;
import control.PatientControl;
import boundary.TreatmentUI;
import boundary.PatientUI;
import boundary.ConsultationUI;
import boundary.PharmacyUI;
import boundary.DoctorUI;
import adt.ClinicADT;
import adt.MyClinicADT;

import java.util.Scanner;

public class TARUMTClinicManagementSystem {

    public static void main(String[] args) {
        // === Initialize Core Controls ===
        DoctorControl doctorControl = new DoctorControl();
        PatientControl patientControl = new PatientControl();
        ClinicADT<Consultation> consultations = new MyClinicADT<>();
        ClinicADT<MedicalTreatment> treatments = new MyClinicADT<>();

        // ConsultationControl links all patient/doctor/treatment modules
        ConsultationControl consultationControl = new ConsultationControl(
                patientControl, doctorControl, consultations, treatments);

        Scanner scanner = new Scanner(System.in);
        int choice = -1;

        // === Main Menu Loop ===
        do {
            System.out.println("\n=== TARUMT Clinic Management System ===");
            System.out.println("1. Patient Management");
            System.out.println("2. Doctor Management");
            System.out.println("3. Consultation Management");
            System.out.println("4. Medical Treatment Management");
            System.out.println("5. Pharmacy Management");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");

            if (!scanner.hasNextInt()) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine();
                continue;
            }
            choice = scanner.nextInt();
            scanner.nextLine(); // consume newline
            
            // === Menu Selection ===
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
