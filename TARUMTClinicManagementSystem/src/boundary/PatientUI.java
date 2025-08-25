package boundary;

import java.util.Scanner;
import control.PatientControl;
import entity.Patient;
import utility.Validation;
import utility.Report;
import control.ConsultationControl;

public class PatientUI {
    private final PatientControl control;
    private final Scanner scanner;
    private ConsultationControl c;

    public PatientUI(PatientControl control) {
        this.control = control;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        int choice = -1;
        do {
            System.out.println("\n====== Patient Management ======");
            System.out.println("1. Register New Patient");
            System.out.println("2. Call Next Patient");
            System.out.println("3. View Next Patient in Queue");
            System.out.println("4. Display All Patients (Queue Order)");
            System.out.println("5. Show Patient Count");
            System.out.println("6. View All Patients (Sorted by Name)");
            System.out.println("7. Patient Age Frequency Distribution Report");
            System.out.println("8. Patient Medical History Report");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");

            if (!scanner.hasNextInt()) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine();
                continue;
            }
            
            choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1 -> registerPatient();
                case 2 -> control.callNextPatient();
                case 3 -> control.viewNextPatient();
                case 4 -> control.displayAllPatients();
                case 5 -> System.out.println("Total patients in queue: " + control.getPatientCount());
                case 6 -> control.printAllPatientsSortedByName();
                case 7 -> ageDistributionReport();
                case 8 -> control.medicalHistoryReport();
                case 0 -> System.out.println("Exiting Patient Management Module.");
                default -> System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 0);
    }

    private void registerPatient() {
        String error;
        String name;

        // ===== Name =====
        do {
            System.out.print("Enter name (or 0 to cancel): ");
            name = scanner.nextLine().trim();
            if (name.equals("0")) return; // cancel registration
            error = Validation.validateName(name);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        // ===== Age =====
        int age;
        do {
            System.out.print("Enter age (or 0 to cancel): ");
            String ageInput = scanner.nextLine().trim();
            if (ageInput.equals("0")) return;
            try {
                age = Integer.parseInt(ageInput);
                error = Validation.validateAge(age);
                if (error != null) System.out.println(error + "\n");
            } catch (NumberFormatException e) {
                error = "Please enter a valid number.";
                System.out.println(error + "\n");
                age = -1;
            }
        } while (error != null);

        // ===== Gender =====
        String gender;
        do {
            System.out.print("Enter gender (M/F) (or 0 to cancel): ");
            gender = scanner.nextLine().trim().toUpperCase();
            if (gender.equals("0")) return;
            error = Validation.validateGender(gender);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        // ===== IC Number =====
        String icNumber;
        do {
            System.out.print("Enter Malaysian IC Number (e.g., YYMMDD-XX-XXXX) (or 0 to cancel): ");
            icNumber = scanner.nextLine().trim();
            if (icNumber.equals("0")) return;

            error = Validation.validateMalaysianIC(icNumber);
            if (error != null) {
                System.out.println("IC format error: " + error + "\n");
                continue;
            }

            error = Validation.validateAgeAndICConsistency(age, icNumber);
            if (error != null) {
                System.out.println("Age/IC mismatch: " + error + "\n");
            }
        } while (error != null);

        // ===== Contact Number =====
        String contact;
        do {
            System.out.print("Enter contact number (e.g., 0123456789 or 01123456789) (or 0 to cancel): ");
            contact = scanner.nextLine().trim();
            if (contact.equals("0")) return;
            error = Validation.validatePhone(contact);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        // ===== Register =====
        control.registerPatient(name, age, gender, icNumber, contact);
    }
    
    public void ageDistributionReport() {
        // --- Get counts from patientControl ---
        int pediatric  = control.countPediatric();    // 0–12
        int adolescent = control.countAdolescent();   // 13–17
        int adult      = control.countAdult();        // 18–64
        int geriatric  = control.countGeriatric();    // 65+

        // --- Print table header ---
        Report.printHeader("Patient Age Group Report");

        String line = "+--------------------+--------------------+";
        String headerFormat = "| %-18s | %-18s |%n";
        String rowFormat    = "| %-18s | %-18d |%n";

        System.out.println(line);
        System.out.printf(headerFormat, "Age Group", "Number of Patients");
        System.out.println(line);

        // --- Print rows ---
        System.out.printf(rowFormat, "Pediatric (0-12)", pediatric);
        System.out.printf(rowFormat, "Adolescent (13-17)", adolescent);
        System.out.printf(rowFormat, "Adult (18-64)", adult);
        System.out.printf(rowFormat, "Geriatric (65+)", geriatric);

        int total = pediatric + adolescent + adult + geriatric;
        System.out.println(line);
        System.out.printf("| %-18s | %-18d |%n", "TOTAL", total);
        System.out.println(line);

        // --- Determine max and min ---
        int maxCount = pediatric;
        int minCount = pediatric;

        if (adolescent > maxCount) maxCount = adolescent;
        if (adult > maxCount) maxCount = adult;
        if (geriatric > maxCount) maxCount = geriatric;

        if (adolescent < minCount) minCount = adolescent;
        if (adult < minCount) minCount = adult;
        if (geriatric < minCount) minCount = geriatric;

        // --- Build group labels ---
        String maxGroups = "";
        if (pediatric == maxCount) maxGroups += "Pediatric ";
        if (adolescent == maxCount) maxGroups += "Adolescent ";
        if (adult == maxCount) maxGroups += "Adult ";
        if (geriatric == maxCount) maxGroups += "Geriatric ";

        String minGroups = "";
        if (pediatric == minCount) minGroups += "Pediatric ";
        if (adolescent == minCount) minGroups += "Adolescent ";
        if (adult == minCount) minGroups += "Adult ";
        if (geriatric == minCount) minGroups += "Geriatric ";

        // --- Print summary ---
        System.out.println("\nGroup(s) with Most Patients: " + maxGroups + "(" + maxCount + ")");
        System.out.println("Group(s) with Least Patients: " + minGroups + "(" + minCount + ")");
        
        // --- Print bar chart with numeric count ---
        System.out.println("\nPatient Distribution Chart (Each * = 1 patient):\n");
        System.out.printf("%-16s | %-3d | ", "Pediatric", pediatric);
        for (int i = 0; i < pediatric; i++) System.out.print("* ");
        System.out.println();

        System.out.printf("%-16s | %-3d | ", "Adolescent", adolescent);
        for (int i = 0; i < adolescent; i++) System.out.print("* ");
        System.out.println();

        System.out.printf("%-16s | %-3d | ", "Adult", adult);
        for (int i = 0; i < adult; i++) System.out.print("* ");
        System.out.println();

        System.out.printf("%-16s | %-3d | ", "Geriatric", geriatric);
        for (int i = 0; i < geriatric; i++) System.out.print("* ");
        System.out.println();
        System.out.println("+----------------+-----+-----------------------------+");
        Report.printFooter();
    }

}
