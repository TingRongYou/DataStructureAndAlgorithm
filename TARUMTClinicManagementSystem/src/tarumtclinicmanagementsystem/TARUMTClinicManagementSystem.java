package tarumtclinicmanagementsystem;

import entity.MedicalTreatment;
import entity.Consultation;

import control.DoctorControl;
import control.ConsultationControl;
import control.PatientControl;
import control.AppointmentControl;
import control.PharmacyControl;
import control.PharmacyQueueControl;

import boundary.TreatmentUI;
import boundary.PatientUI;
import boundary.ConsultationUI;
import boundary.PharmacyUI;
import boundary.DoctorUI;
import boundary.AppointmentUI;
import boundary.PaymentUI;

import adt.ClinicADT;
import adt.MyClinicADT;

import java.util.Scanner;

public class TARUMTClinicManagementSystem {

    // ------- small input helper -------
    private static int readInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                return Integer.parseInt(s);
            } catch (Exception e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    // ------- simple banner helper -------
    private static void banner(String title) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("            " + title);
        System.out.println("=".repeat(60));
    }

    public static void main(String[] args) {
        // === Initialize Core Controls (ONE instance each) ===
        DoctorControl doctorControl              = new DoctorControl();
        PatientControl patientControl            = new PatientControl();
        ClinicADT<Consultation> consultations    = new MyClinicADT<>();
        ClinicADT<MedicalTreatment> treatments   = new MyClinicADT<>();
        PharmacyControl pharmacyControl          = new PharmacyControl();
        PharmacyQueueControl pharmacyQueue       = new PharmacyQueueControl(); // shared

        // ConsultationControl links patient/doctor/treatment modules
        ConsultationControl consultationControl =
                new ConsultationControl(patientControl, doctorControl, consultations, treatments);

        // AppointmentControl integrates with patient/doctor
        AppointmentControl appointmentControl =
                new AppointmentControl(patientControl, doctorControl);

        // === UIs (reuse singletons) ===
        AppointmentUI  appointmentUI  = new AppointmentUI(appointmentControl, patientControl, doctorControl);
        DoctorUI       doctorUI       = new DoctorUI(doctorControl);
        PatientUI      patientUI      = new PatientUI(patientControl);
        ConsultationUI consultationUI = new ConsultationUI(
                patientControl, doctorControl, consultations, treatments, appointmentControl);
        TreatmentUI    treatmentUI    = new TreatmentUI(
                patientControl, doctorControl, treatments, appointmentControl, pharmacyControl, pharmacyQueue);
        PharmacyUI     pharmacyUI     = new PharmacyUI(
                pharmacyControl, pharmacyQueue, appointmentControl, treatments);
        PaymentUI      paymentUI      = new PaymentUI(
                pharmacyControl, appointmentControl, pharmacyQueue, treatments);

        Scanner sc = new Scanner(System.in);

        // === Role Hub ===
        int role;
        do {
            banner("TARUMT CLINIC MANAGEMENT SYSTEM");
            System.out.println(" 1) Student / Patient");
            System.out.println(" 2) Doctor");
            System.out.println(" 3) Admin");
            System.out.println(" 4) Pharmacist");
            System.out.println(" 0) Exit");
            role = readInt(sc, "Choose role: ");

            switch (role) {
                // ------------------- Student / Patient Portal -------------------
                case 1 -> {
                    int c;
                    do {
                        System.out.println("\n" + "-".repeat(60));
                        System.out.println("            STUDENT / PATIENT - PORTAL");
                        System.out.println("-".repeat(60));
                        System.out.println(" 1) Appointment");
                        System.out.println(" 2) Patient Management");
                        System.out.println(" 3) Consultation");
                        System.out.println(" 4) Treatments");
                        System.out.println(" 5) Payments");
                        System.out.println(" 0) Back to Role Hub");
                        c = readInt(sc, "Choice: ");
                        switch (c) {
                            // Use the PATIENT-specific appointment menu
                            case 1 -> appointmentUI.patientAppointmentMenu();
                            case 2 -> patientUI.patientPatientMenu();
                            case 3 -> consultationUI.patientConsultationMenu();
                            case 4 -> treatmentUI.patientTreatmentMenu();
                            case 5 -> paymentUI.patientPaymentMenu();
                            case 0 -> { /* back */ }
                            default -> System.out.println("Invalid choice.");
                        }
                    } while (c != 0);
                }

                // -------------------------- Doctor Portal -----------------------
                case 2 -> {
                    int c;
                    do {
                        System.out.println("\n" + "-".repeat(60));
                        System.out.println("                 DOCTOR - PORTAL");
                        System.out.println("-".repeat(60));
                        System.out.println(" 1) Doctor Management");
                        System.out.println(" 2) Appointment (Doctor actions)");
                        System.out.println(" 3) Consultation");
                        System.out.println(" 4) Treatments");
                        System.out.println(" 0) Back to Role Hub");
                        c = readInt(sc, "Choice: ");
                        switch (c) {
                            case 1 -> doctorUI.doctorDoctorMenu();
                            // Use the DOCTOR-specific appointment menu
                            case 2 -> appointmentUI.doctorAppointmentMenu();
                            case 3 -> consultationUI.doctorConsultationMenu();
                            case 4 -> treatmentUI.doctorTreatmentMenu();
                            case 0 -> { /* back */ }
                            default -> System.out.println("Invalid choice.");
                        }
                    } while (c != 0);
                }

                // --------------------------- Admin Portal -----------------------
                case 3 -> {
                    int c;
                    do {
                        System.out.println("\n" + "-".repeat(60));
                        System.out.println("                  ADMIN - PORTAL");
                        System.out.println("-".repeat(60));
                        System.out.println(" 1) Appointment");
                        System.out.println(" 2) Doctor Management");
                        System.out.println(" 3) Patient Management");
                        System.out.println(" 4) Consultation");
                        System.out.println(" 5) Treatments");
                        System.out.println(" 6) Pharmacy");
                        System.out.println(" 7) Payments");
                        System.out.println(" 0) Back to Role Hub");
                        c = readInt(sc, "Choice: ");
                        switch (c) {
                            // Use the ADMIN-specific appointment menu
                            case 1 -> appointmentUI.adminAppointmentMenu();
                            case 2 -> doctorUI.adminDoctorMenu();
                            case 3 -> patientUI.adminPatientMenu();
                            case 4 -> consultationUI.adminConsultationMenu();
                            case 5 -> treatmentUI.adminTreatmentMenu();
                            case 6 -> pharmacyUI.adminPharmacyMenu();
                            case 7 -> paymentUI.adminPaymentMenu();
                            case 0 -> { /* back */ }
                            default -> System.out.println("Invalid choice.");
                        }
                    } while (c != 0);
                }

                // ------------------------- Pharmacist Portal --------------------
                case 4 -> {
                    int c;
                    do {
                        System.out.println("\n" + "-".repeat(60));
                        System.out.println("               PHARMACIST - PORTAL");
                        System.out.println("-".repeat(60));
                        System.out.println(" 1) Pharmacy");
                        System.out.println(" 0) Back to Role Hub");
                        c = readInt(sc, "Choice: ");
                        switch (c) {
                            case 1 -> pharmacyUI.pharmacistPharmacyMenu();
                            case 0 -> { /* back */ }
                            default -> System.out.println("Invalid choice.");
                        }
                    } while (c != 0);
                }

                case 0 -> System.out.println("Thank you for using the system.");
                default -> System.out.println("Invalid choice.");
            }
        } while (role != 0);

        sc.close();
    }
}
