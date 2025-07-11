package tarumtclinicmanagementsystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class ConsultationUI {
    private ConsultationControl consultationControl;
    private Scanner sc;
    private PatientControl patientControl;
    private DoctorControl doctorControl;
    private ClinicADT<Consultation> consultations;
    private ClinicADT<MedicalTreatment> treatments;

    public ConsultationUI(PatientControl patientControl, DoctorControl doctorControl,
                           ClinicADT<Consultation> consultations, ClinicADT<MedicalTreatment> treatments) {
        this.patientControl = patientControl;
        this.doctorControl = doctorControl;
        this.consultations = consultations;
        this.treatments = treatments;
        this.consultationControl = new ConsultationControl(patientControl, doctorControl, consultations);
        this.sc = new Scanner(System.in);
    }

    public void run() {
        while (true) {
            System.out.println("\n===== Consultation Management =====");
            System.out.println("1. Add Consultation");
            System.out.println("2. Remove Consultation");
            System.out.println("3. List All Consultations");
            System.out.println("4. Search by Patient");
            System.out.println("5. Search by Doctor");
            System.out.println("6. Display All (Sorted by Date)");
            System.out.println("7. Show Total Consultation Count");
            System.out.println("8. Check Doctor Availability for Date");
            System.out.println("9. View Working Hours");
            System.out.println("0. Exit");
            System.out.print("Choice: ");

            int ch = sc.nextInt();
            sc.nextLine(); // consume newline

            switch (ch) {
                case 1 -> addConsultation();
                case 2 -> removeConsultation();
                case 3 -> consultationControl.listConsultations();
                case 4 -> searchByPatient();
                case 5 -> searchByDoctor();
                case 6 -> consultationControl.printConsultationsSortedByDate();
                case 7 -> System.out.println("Total Consultations: " + consultationControl.getTotalConsultations());
                case 8 -> checkDoctorAvailability();
                case 9 -> showWorkingHours();
                case 0 -> {
                    System.out.println("Exiting Consultation Module...");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void addConsultation() {
        System.out.println("\n=== Add New Consultation ===");
        System.out.println("This will guide you through scheduling a consultation");
        System.out.println("Each consultation is 1 hour long");
        System.out.println("Only available doctors during working hours will be shown");
        System.out.println();

        BookingUI bookingUI = new BookingUI(patientControl, doctorControl, consultations, treatments, consultationControl);
        bookingUI.run(true); // true = only allow consultation
    }

    private void removeConsultation() {
        System.out.println("\n=== Remove Consultation ===");

        // Show current consultations first
        consultationControl.listConsultations();

        if (consultationControl.getTotalConsultations() == 0) {
            return;
        }

        System.out.print("Enter Consultation ID to remove: ");
        int id = sc.nextInt();
        sc.nextLine();
        consultationControl.removeConsultationById(id);
    }

    private void searchByPatient() {
        System.out.println("\n=== Search Consultations by Patient ===");

        // Step 1: Fetch all patients who have at least one consultation
        ClinicADT<Patient> consultedPatients = consultationControl.getPatientsWithConsultations();

        if (consultedPatients.isEmpty()) {
            System.out.println("❌ No patients with consultations found.");
            return;
        }

        // Step 2: Display the patient table
        System.out.println("\n📋 Patients with Consultations:");
        String format = "| %-10s | %-20s | %-3s | %-6s | %-12s |\n";
        String line = "+------------+----------------------+-----+--------+--------------+";

        System.out.println(line);
        System.out.printf(format, "Patient ID", "Name", "Age", "Gender", "Contact");
        System.out.println(line);

        for (int i = 0; i < consultedPatients.size(); i++) {
            Patient p = consultedPatients.get(i);
            System.out.printf(format,
                    p.getId(),
                    p.getName(),
                    p.getAge(),
                    p.getGender(),
                    p.getContact());
        }
        System.out.println(line);

        // Step 3: Ask for Patient ID
        System.out.print("\nEnter Patient ID to view consultation history: ");
        String inputId = sc.nextLine().trim().toUpperCase();

        Patient selected = patientControl.getPatientById(inputId);
        if (selected == null) {
            System.out.println("❌ Invalid Patient ID.");
            return;
        }

        // Step 4: Show consultations for selected patient
        consultationControl.searchByPatient(selected.getName());
    }

    private void searchByDoctor() {
        System.out.println("\n=== Search Consultations by Doctor ===");
        System.out.print("Enter doctor name: ");
        String name = sc.nextLine();
        consultationControl.searchByDoctor(name);
    }

    private void checkDoctorAvailability() {
        System.out.println("\n=== Check Doctor Availability ===");

        while (true) {
            try {
                System.out.print("Enter date to check (yyyy-MM-dd): ");
                String input = sc.nextLine().trim();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDateTime date = LocalDateTime.parse(input + " 00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

                consultationControl.showDoctorScheduleForDate(date);
                break;
            } catch (Exception e) {
                System.out.println("❌ Invalid date format. Please use 'yyyy-MM-dd'.");
            }
        }
    }

    private void showWorkingHours() {
        System.out.println("\n=== Clinic Working Hours ===");
        System.out.println("🌅 Morning Shift:   08:00 - 12:00");
        System.out.println("🌞 Afternoon Shift: 13:00 - 17:00");
        System.out.println("🌙 Night Shift:     18:00 - 22:00");
        System.out.println();
        System.out.println("⏰ Each consultation is exactly 1 hour long");
        System.out.println("📅 Consultations can only be scheduled during working hours");
        System.out.println("👨‍⚕️ Only doctors on duty for the selected time slot will be available");
        System.out.println();
        System.out.println("💡 Tips:");
        System.out.println("   • Check doctor availability before scheduling");
        System.out.println("   • Ensure the selected time doesn't conflict with existing appointments");
        System.out.println("   • Doctors have different schedules for different days");
    }
}
