package tarumtclinicmanagementsystem;

import java.util.Scanner;

public class PatientUI {
    private PatientControl control;
    private Scanner scanner;

    public PatientUI() {
        control = new PatientControl();
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
<<<<<<< HEAD
                System.out.print("Invalid input. Enter a number: ");
=======
                System.out.print("Invalid input. Please enter a number: ");
>>>>>>> 799e9b2fc56aec04e669a1dff14c09088c36bc85
                scanner.next();
            }
            choice = scanner.nextInt();
            scanner.nextLine(); // Clear newline

            switch (choice) {
<<<<<<< HEAD
                case 1:
                    System.out.print("Enter name: ");
                    String name = scanner.nextLine();

                    System.out.print("Enter age: ");
                    int age = scanner.nextInt();
                    scanner.nextLine();

                    System.out.print("Enter gender (M/F): ");
                    String gender = scanner.nextLine();

                    System.out.print("Enter contact number: ");
                    String contact = scanner.nextLine();

                    control.registerPatient(name, age, gender, contact);
                    break;
=======
                case 1 -> {
                    System.out.print("Enter patient name: ");
                    String name = scanner.nextLine();
                    System.out.print("Enter patient ID: ");
                    String id = scanner.nextLine();
                    control.registerPatient(name, id);
                }
>>>>>>> 799e9b2fc56aec04e669a1dff14c09088c36bc85

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
}
