package tarumtclinicmanagementsystem;

import java.util.Scanner;

public class PatientUI {
    private PatientControl control;
    private Scanner scanner;

    public PatientUI(PatientControl control) {
        this.control = control;
        scanner = new Scanner(System.in);
    }

    public void run() {
        int choice;
        do {
            System.out.println("\n====== Patient Management ======");
            System.out.println("1. Register New Patient");
            System.out.println("2. Call Next Patient");
            System.out.println("3. View Next Patient in Queue");
            System.out.println("4. Display All Patients (Queue Order)");
            System.out.println("5. Show Patient Count");
            System.out.println("6. View All Patients (Sorted by Name)");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");

            while (!scanner.hasNextInt()) {
                System.out.print("Invalid input. Enter a number: ");
                scanner.next();
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
                case 0 -> System.out.println("Exiting Patient Management Module.");
                default -> System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 0);
    }

    private void registerPatient() {
        System.out.print("Enter name: ");
        String name = scanner.nextLine().trim();

        // Validate name is not empty
        while (name.isEmpty()) {
            System.out.print("Name cannot be empty. Enter name: ");
            name = scanner.nextLine().trim();
        }

        int age;
        while (true) {
            System.out.print("Enter age: ");
            if (scanner.hasNextInt()) {
                age = scanner.nextInt();
                scanner.nextLine(); // Consume newline
                if (age > 0 && age <= 150) {
                    break;
                } else {
                    System.out.println("Age must be between 1 and 150.");
                }
            } else {
                System.out.println("Invalid age. Please enter a number.");
                scanner.next(); // Consume invalid input
            }
        }

        String gender;
        while (true) {
            System.out.print("Enter gender (M/F): ");
            gender = scanner.nextLine().trim().toUpperCase();
            if (gender.equals("M") || gender.equals("F")) {
                break;
            } else {
                System.out.println("Please enter M or F.");
            }
        }

        // --- New: Malaysian IC Number Input and Validation ---
        String icNumber;
        while (true) {
            System.out.print("Enter Malaysian IC Number (e.g., YYMMDD-XX-XXXX): ");
            icNumber = scanner.nextLine().trim();
            // Regex for Malaysian IC: YYMMDD-XX-XXXX
            // ^\\d{6} - starts with 6 digits (YYMMDD)
            // -       - literal hyphen
            // \\d{2}  - 2 digits (XX for state code)
            // -       - literal hyphen
            // \\d{4}$ - 4 digits (XXXX for serial number)
            if (icNumber.matches("^\\d{6}-\\d{2}-\\d{4}$")) {
                break;
            } else {
                System.out.println("Invalid Malaysian IC number format. Please use YYMMDD-XX-XXXX.");
            }
        }
        // --- End of New Section ---

        String contact;
        while (true) {
            System.out.print("Enter contact number (Malaysian format - 01xxxxxxxx or 01xxxxxxxxx): ");
            contact = scanner.nextLine().trim();
            // Regex for Malaysian mobile numbers: 01 followed by 8 or 9 digits
            if (contact.matches("01\\d{8,9}")) {
                break;
            } else {
                System.out.println("Invalid phone number format. Please use Malaysian format (e.g., 0123456789 or 01234567890).");
            }
        }

        // --- Modified: Pass icNumber to PatientControl ---
        control.registerPatient(name, age, gender, icNumber, contact);
    }
}