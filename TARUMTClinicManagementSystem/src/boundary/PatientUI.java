package boundary;

import java.util.Scanner;
import control.PatientControl;
import utility.Validation;

public class PatientUI {
    private final PatientControl control;
    private final Scanner scanner;

    public PatientUI(PatientControl control) {
        this.control = control;
        this.scanner = new Scanner(System.in);
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
        String error;
        String name;

        do {
            System.out.print("Enter name: ");
            name = scanner.nextLine().trim();
            error = Validation.validateName(name);
            if (error != null) System.out.println(error);
        } while (error != null);

        int age;
        do {
            System.out.print("Enter age: ");
            String ageInput = scanner.nextLine().trim();
            try {
                age = Integer.parseInt(ageInput);
                error = Validation.validateAge(age);
                if (error != null) System.out.println(error);
            } catch (NumberFormatException e) {
                error = "Please enter a valid number.";
                System.out.println(error);
                age = -1;
            }
        } while (error != null);

        String gender;
        do {
            System.out.print("Enter gender (M/F): ");
            gender = scanner.nextLine().trim().toUpperCase();
            error = Validation.validateGender(gender);
            if (error != null) System.out.println(error);
        } while (error != null);

        String icNumber;
        do {
            System.out.print("Enter Malaysian IC Number (e.g., YYMMDD-XX-XXXX): ");
            icNumber = scanner.nextLine().trim();
            error = Validation.validateMalaysianIC(icNumber);
            if (error != null) {
                System.out.println("IC format error: " + error);
                continue;
            }

            error = Validation.validateAgeAndICConsistency(age, icNumber);
            if (error != null) {
                System.out.println("Age/IC mismatch: " + error);
            }
        } while (error != null);

        String contact;
        do {
            System.out.print("Enter contact number (e.g., 0123456789 or 01123456789): ");
            contact = scanner.nextLine().trim();
            error = Validation.validatePhone(contact);
            if (error != null) System.out.println(error);
        } while (error != null);

        control.registerPatient(name, age, gender, icNumber, contact);
    }
}
