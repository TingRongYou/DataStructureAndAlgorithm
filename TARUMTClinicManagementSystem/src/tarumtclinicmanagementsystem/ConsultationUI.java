package tarumtclinicmanagementsystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class ConsultationUI {
    private ConsultationControl control;
    private Scanner sc;
    private PatientControl patientControl;
    private DoctorControl doctorControl;

    public ConsultationUI(PatientControl patientControl, DoctorControl doctorControl) {
        this.patientControl = patientControl;
        this.doctorControl = doctorControl;
        this.control = new ConsultationControl(patientControl, doctorControl);
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
                case 3 -> control.listConsultations();
                case 4 -> searchByPatient();
                case 5 -> searchByDoctor();
                case 6 -> control.printConsultationsSortedByDate();
                case 7 -> System.out.println("Total Consultations: " + control.getTotalConsultations());
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
        System.out.println("📋 This will guide you through scheduling a consultation");
        System.out.println("⏰ Each consultation is 1 hour long");
        System.out.println("🩺 Only available doctors during working hours will be shown");
        System.out.println();
        
        control.addConsultationFlow();
    }

    private void removeConsultation() {
        System.out.println("\n=== Remove Consultation ===");
        
        // Show current consultations first
        control.listConsultations();
        
        if (control.getTotalConsultations() == 0) {
            return;
        }
        
        System.out.print("Enter Consultation ID to remove: ");
        int id = sc.nextInt();
        sc.nextLine();
        control.removeConsultationById(id);
    }

    private void searchByPatient() {
        System.out.println("\n=== Search Consultations by Patient ===");
        System.out.print("Enter patient name: ");
        String name = sc.nextLine();
        control.searchByPatient(name);
    }

    private void searchByDoctor() {
        System.out.println("\n=== Search Consultations by Doctor ===");
        System.out.print("Enter doctor name: ");
        String name = sc.nextLine();
        control.searchByDoctor(name);
    }

    private void checkDoctorAvailability() {
        System.out.println("\n=== Check Doctor Availability ===");
        
        while (true) {
            try {
                System.out.print("Enter date to check (yyyy-MM-dd): ");
                String input = sc.nextLine().trim();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDateTime date = LocalDateTime.parse(input + " 00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                
                control.showDoctorScheduleForDate(date);
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