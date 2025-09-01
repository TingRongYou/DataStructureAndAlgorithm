package boundary;

import java.util.Scanner;

import control.PatientControl;
import utility.Validation;
import utility.Report;
import adt.ClinicADT;
import entity.Patient;

public class PatientUI {
    private final PatientControl control;
    private final Scanner scanner;

    public PatientUI(PatientControl control) {
        this.control = control;
        this.scanner = new Scanner(System.in);
    }

    // ===========================
    // Main menu (with Reports sub-menu)
    // ===========================
    public void patientPatientMenu() {
        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("              PATIENT - PATIENTS");
            System.out.println("=".repeat(60));
            System.out.println(" 1) Register New Patient");
            System.out.println(" 0) Back");
            int choice = safeReadInt("Choice: ");
            switch (choice) {
                case 1 -> registerPatient();
                case 0 -> { System.out.println("Returning..."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
    
     public void adminPatientMenu() {
        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("               ADMIN - PATIENTS");
            System.out.println("=".repeat(60));
            System.out.println(" 1) Register New Patient");
            System.out.println(" 2) Show Patient Count");
            System.out.println(" 3) View All Patients (Sorted by Name)");
            System.out.println(" 4) Generate Reports");
            System.out.println(" 0) Back");
            int choice = safeReadInt("Choice: ");
            switch (choice) {
                case 1 -> registerPatient();
                case 2 -> System.out.println("Total registered patients: " + control.getPatientCount());
                case 3 -> control.printAllPatientsSortedByName();
                case 4 -> reportsMenu();
                case 0 -> { System.out.println("Returning..."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
     
    public void run() {
        int choice;
        do {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("             TARUMT CLINIC PATIENT MANAGEMENT");
            System.out.println("=".repeat(60));
            System.out.println("1. Register New Patient");
            System.out.println("2. Show Patient Count");
            System.out.println("3. View All Patients (Sorted by Name)");
            System.out.println("4. Generate Reports");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");

            while (!scanner.hasNextInt()) {
                System.out.print("Invalid input. Please enter a number: ");
                scanner.next();
            }
            choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1 -> registerPatient();
                case 2 -> System.out.println("Total registered patients: " + control.getPatientCount());
                case 3 -> control.printAllPatientsSortedByName(); // uses ADT .sort(MyComparator)
                case 4 -> reportsMenu();
                case 0 -> System.out.println("Exiting Patient Management Module.");
                default -> System.out.println("Invalid choice.");
            }
        } while (choice != 0);
    }

    // ===========================
    // Reports sub-menu
    // ===========================
    private void reportsMenu() {
        int choice;
        do {
            System.out.println("\n=== Patient Reports ===");
            System.out.println("1. Age Frequency Distribution");
            System.out.println("2. Medical History Report");
            System.out.println("3. Gender Distribution Report");
            System.out.println("4. Contact Directory Report");
            System.out.println("5. Senior Patients (65+) Report");
            System.out.println("0. Back");
            System.out.print("Choice: ");

            while (!scanner.hasNextInt()) {
                System.out.print("Invalid input. Please enter a number: ");
                scanner.next();
            }
            choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1 -> ageDistributionReport();
                case 2 -> control.medicalHistoryReport(); // assumed ADT-only inside
                case 3 -> genderDistributionReport();
                case 4 -> contactDirectoryReport();
                case 5 -> seniorPatientsReport();
                case 0 -> System.out.println("Returning to Patient Management menu...");
                default -> System.out.println("Invalid choice.");
            }
        } while (choice != 0);
    }

    // ===========================
    // Registration flow
    // ===========================
    private void registerPatient() {
        String error;
        String name;

        // ===== Name =====
        do {
            System.out.print("Enter name (or 0 to cancel): ");
            name = scanner.nextLine().trim();
            if (name.equals("0")) return;
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

        // ===== IC =====
        String icNumber;
        do {
            System.out.print("Enter Malaysian IC (YYMMDD-XX-XXXX) (or 0 to cancel): ");
            icNumber = scanner.nextLine().trim();
            if (icNumber.equals("0")) return;

            error = Validation.validateMalaysianIC(icNumber);
            if (error != null) {
                System.out.println("IC format error: " + error + "\n");
                continue;
            }
            error = Validation.validateAgeAndICConsistency(age, icNumber);
            if (error != null) System.out.println("Age/IC mismatch: " + error + "\n");
        } while (error != null);

        // ===== Contact =====
        String contact;
        do {
            System.out.print("Enter contact (e.g., 0123456789) (or 0 to cancel): ");
            contact = scanner.nextLine().trim();
            if (contact.equals("0")) return;
            error = Validation.validatePhone(contact);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        control.registerPatient(name, age, gender, icNumber, contact);
    }

    // ===========================
    // Report 1: Age distribution (centered)
    // ===========================
    public void ageDistributionReport() {
    int pediatric  = control.countPediatric();   // 0–12
    int adolescent = control.countAdolescent();  // 13–17
    int adult      = control.countAdult();       // 18–64
    int geriatric  = control.countGeriatric();   // 65+

    Report.printHeader("Patient Age Group Report");

    // Print table headers with proper alignment
    String line = "+---------------------+---------------------+";
    String header = "| %-19s | %-19s |%n";  // Aligning headers with 19 characters for both columns
    String row    = "| %-19s | %-19d |%n";  // Aligning rows with 19 characters for both columns

    Report.cprintln(line);
    Report.cprintf(header, "Age Group", "Number of Patients");
    Report.cprintln(line);
    Report.cprintf(row, "Pediatric (0-12)", pediatric);
    Report.cprintf(row, "Adolescent (13-17)", adolescent);
    Report.cprintf(row, "Adult (18-64)", adult);
    Report.cprintf(row, "Geriatric (65+)", geriatric);

    int total = pediatric + adolescent + adult + geriatric;
    Report.cprintln(line);
    Report.cprintf("| %-19s | %-19d |%n", "TOTAL", total);
    Report.cprintln(line);

    // Summary
    int max = Math.max(Math.max(pediatric, adolescent), Math.max(adult, geriatric));
    int min = Math.min(Math.min(pediatric, adolescent), Math.min(adult, geriatric));
    StringBuilder maxG = new StringBuilder();
    if (pediatric == max)  maxG.append("Pediatric ");
    if (adolescent == max) maxG.append("Adolescent ");
    if (adult == max)      maxG.append("Adult ");
    if (geriatric == max)  maxG.append("Geriatric ");
    StringBuilder minG = new StringBuilder();
    if (pediatric == min)  minG.append("Pediatric ");
    if (adolescent == min) minG.append("Adolescent ");
    if (adult == min)      minG.append("Adult ");
    if (geriatric == min)  minG.append("Geriatric ");

    Report.cprintln("");
    Report.cprintf("Group(s) with Most Patients: %-15s (%d)%n", maxG.toString(), max);
    Report.cprintf("Group(s) with Least Patients: %-15s (%d)%n", minG.toString(), min);
    Report.cprintln("");

    // Neat boxed bar chart
    String[] labels = {"Pediatric", "Adolescent", "Adult", "Geriatric"};
    int[] counts    = {pediatric, adolescent, adult, geriatric};
    drawCenteredBarChart(" (Each * = 1 patient) ", labels, counts);

    Report.printFooter();
}

    // ===========================
    // Report 2: Gender Distribution
    // ===========================
    private void genderDistributionReport() {
        int male = 0, female = 0, other = 0;

        ClinicADT<Patient> all = control.getAllPatients();
        ClinicADT.MyIterator<Patient> it = all.iterator();
        while (it.hasNext()) {
            String g = String.valueOf(it.next().getGender()).trim().toUpperCase();
            if (g.equals("M")) male++;
            else if (g.equals("F")) female++;
            else other++;
        }

        Report.printHeader("Patient Gender Distribution Report");

        String line = "+----------+--------------------+";
        String header = "| %-8s | %-18s |%n";
        String row    = "| %-8s | %-18d |%n";

        Report.cprintln(line);
        Report.cprintf(header, "Gender", "Number of Patients");
        Report.cprintln(line);
        Report.cprintf(row, "Male", male);
        Report.cprintf(row, "Female", female);
        Report.cprintf(row, "Other", other);
        Report.cprintln(line);
        Report.cprintf("| %-8s | %-18d |%n", "TOTAL", male + female + other);
        Report.cprintln(line);
        Report.cprintln("");

        String[] labels = {"Male", "Female", "Other"};
        int[] counts    = {male,   female,   other};
        drawCenteredBarChart("(Each * = 1 patient)", labels, counts);

        Report.printFooter();
    }

    // ===========================
    // Report 3: Contact Directory
    // ===========================
    private void contactDirectoryReport() {
    // Assuming control.countPatients() returns the list of patients
    ClinicADT<Patient> allPatients = control.getAllPatients(); 

    if (allPatients.isEmpty()) {
        Report.cprintln("(No patients found.)");
        Report.printFooter();
        return;
    }

    // Define table columns width
    final int COL_NAME = 24, COL_CONTACT = 16;
    String sep = "+" 
            + "-".repeat(COL_NAME + 2) + "+" 
            + "-".repeat(COL_CONTACT + 2) + "+";

    String headerFmt = "| %-" + COL_NAME + "s | %-" + COL_CONTACT + "s |%n"; // Ensure that "Patient" column takes up 24 characters and "Contact" takes up 16
    String rowFmt    = "| %-" + COL_NAME + "s | %-" + COL_CONTACT + "s |%n";  // Consistent alignment for rows

    Report.printHeader("Patient Contact List Report");

    // Print the table headers
    Report.cprintln(sep);
    Report.cprintf(headerFmt, "Patient", "Contact");
    Report.cprintln(sep);

    // Iterate over all patients and print their details
    ClinicADT.MyIterator<Patient> it = allPatients.iterator();
    while (it.hasNext()) {
        Patient patient = it.next();
        Report.cprintf(rowFmt, patient.getName(), patient.getContact());
    }

    Report.cprintln(sep);
    Report.printFooter();
}

    // ===========================
    // Report 4: Senior Patients (65+)
    // ===========================
    private void seniorPatientsReport() {
        Report.printHeader("Senior Patients (65+)");

        final int COL_ID = 8, COL_NAME = 22, COL_AGE = 5, COL_CONTACT = 16;
        String sep = "+" 
                + "-".repeat(COL_ID + 2) + "+" 
                + "-".repeat(COL_NAME + 2) + "+" 
                + "-".repeat(COL_AGE + 2) + "+" 
                + "-".repeat(COL_CONTACT + 2) + "+";

        String headFmt = "| %-" + COL_ID + "s | %-" + COL_NAME + "s | %-" + COL_AGE + "s | %-" + COL_CONTACT + "s |%n";
        String rowFmt  = "| %-" + COL_ID + "s | %-" + COL_NAME + "s | %-" + COL_AGE + "d | %-" + COL_CONTACT + "s |%n";

        Report.cprintln(sep);
        Report.cprintf(headFmt, "ID", "Name", "Age", "Contact");
        Report.cprintln(sep);

        int count = 0;
        ClinicADT.MyIterator<Patient> it = control.getAllPatients().iterator();
        while (it.hasNext()) {
            Patient p = it.next();
            if (p.getAge() >= 65) {
                Report.cprintf(rowFmt, p.getId(), p.getName(), p.getAge(), p.getContact());
                count++;
            }
        }

        Report.cprintln(sep);

        // Center “TOTAL SENIORS: n” inside the box precisely
        int innerWidth = sep.length() - 2; // remove '+' ends
        String totalText = String.format("TOTAL SENIORS: %d", count);
        int padLeft = Math.max(0, (innerWidth - totalText.length()) / 2);
        int padRight = Math.max(0, innerWidth - totalText.length() - padLeft);
        Report.cprintln("|" + " ".repeat(padLeft) + totalText + " ".repeat(padRight) + "|");

        Report.cprintln(sep);
        Report.printFooter();
    }

    // ===========================
    // Helper: centered bar line with fixed widths
    // ===========================
    private void drawCenteredBarChart(String label, String[] labels, int[] counts) {
    int max = 0;
    for (int count : counts) {
        max = Math.max(max, count);
    }

    // Print each bar, aligned to the center with proper spacing
    Report.cprintln("+------------------------------------------------+");
    for (int i = 0; i < labels.length; i++) {
        String bar = "*".repeat(counts[i]);  // Create a bar of '*' characters
        // Print with alignment: Labels are left-aligned, and the bar is right-aligned to the max count
        Report.cprintf(" | %-12s | %-31s |\n", labels[i], bar);
    }
    Report.cprintln("+------------------------------------------------+");
    }
    // tiny helper to avoid duplicate nextInt/nextLine mixups
    private int safeReadInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = scanner.nextLine().trim();
            try { return Integer.parseInt(s); } catch (Exception e) { System.out.println("Please enter a number."); }
        }
    }
}
