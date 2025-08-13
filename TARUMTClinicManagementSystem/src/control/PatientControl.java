package control;

import adt.ClinicADT;
import adt.MyClinicADT;
import entity.Patient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class PatientControl {
    private ClinicADT<Patient> patientQueue;
    private final String filePath = "src/textFile/patients.txt";

    public PatientControl() {
        patientQueue = new MyClinicADT<>();
        File directory = new File("src/textFile");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        loadFromFile();
    }

    public void registerPatient(String name, int age, String gender, String icNumber, String contact) {
        Patient newPatient = new Patient(name, age, gender, icNumber, contact);
        patientQueue.enqueue(newPatient);
        System.out.println("Patient registered:\n" + newPatient);
        saveAllToFile();
    }

    public Patient callNextPatient() {
        if (patientQueue.isEmpty()) {
            System.out.println("No patients in the queue.");
            return null;
        }
        Patient next = patientQueue.dequeue();
        System.out.println("Calling patient: " + next);
        saveAllToFile();
        return next;
    }

    public void viewNextPatient() {
        if (patientQueue.isEmpty()) {
            System.out.println("No patients in the queue.");
        } else {
            System.out.println("Next patient: " + patientQueue.peek());
        }
    }

    public void displayAllPatients() {
        int total = patientQueue.size();
        System.out.println("\nTotal Patients: " + total);

        if (total == 0) {
            System.out.println("No patients in the queue.");
            return;
        }

        // Column formatting
        final int COL_NO = 4;
        final int COL_ID = 10;
        final int COL_NAME = 25;
        final int COL_AGE = 5;
        final int COL_GENDER = 8;
        final int COL_IC = 15;
        final int COL_CONTACT = 16;

        String separator = "+" + "-".repeat(COL_NO + 2) + "+"
                               + "-".repeat(COL_ID + 2) + "+"
                               + "-".repeat(COL_NAME + 2) + "+"
                               + "-".repeat(COL_AGE + 2) + "+"
                               + "-".repeat(COL_GENDER + 2) + "+"
                               + "-".repeat(COL_IC + 2) + "+"
                               + "-".repeat(COL_CONTACT + 2) + "+";

        System.out.println(separator);
        System.out.printf("| %-" + COL_NO + "s | %-" + COL_ID + "s | %-" + COL_NAME + "s | %-" + COL_AGE + "s | %-" + COL_GENDER + "s | %-" + COL_IC + "s | %-" + COL_CONTACT + "s |\n",
                "No.", "ID", "Name", "Age", "Gender", "IC Number", "Contact");
        System.out.println(separator);

        ClinicADT.MyIterator<Patient> iterator = patientQueue.iterator();
        int position = 1;
        while (iterator.hasNext()) {
            Patient p = iterator.next();
            System.out.printf("| %-" + COL_NO + "d | %-" + COL_ID + "s | %-" + COL_NAME + "s | %-" + COL_AGE + "d | %-" + COL_GENDER + "s | %-" + COL_IC + "s | %-" + COL_CONTACT + "s |\n",
                    position++, p.getId(), p.getName(), p.getAge(), p.getGender(), p.getIcNumber(), p.getContact());
        }

        System.out.println(separator);
    }

    public Patient getPatientById(String id) {
        ClinicADT.MyIterator<Patient> iter = patientQueue.iterator();
        while (iter.hasNext()) {
            Patient p = iter.next();
            if (p.getId().equalsIgnoreCase(id)) return p;
        }
        return null;
    }

    public void printAllPatientsSortedByName() {
        if (patientQueue.isEmpty()) {
            System.out.println("No patients.");
            return;
        }

        // Copy all patients into a new ClinicADT
        ClinicADT<Patient> sorted = new MyClinicADT<>();
        ClinicADT.MyIterator<Patient> iter = patientQueue.iterator();
        while (iter.hasNext()) {
            sorted.add(iter.next());
        }

        // Sort using custom comparator (ignore case)
        sorted.sort(new ClinicADT.MyComparator<Patient>() {
            @Override
            public int compare(Patient p1, Patient p2) {
                return p1.getName().compareToIgnoreCase(p2.getName());
            }
        });

        // Column formatting
        final int COL_NO = 4;
        final int COL_ID = 10;
        final int COL_NAME = 25;
        final int COL_AGE = 5;
        final int COL_GENDER = 8;
        final int COL_IC = 15;
        final int COL_CONTACT = 16;

        String separator = "+" + "-".repeat(COL_NO + 2) + "+"
                               + "-".repeat(COL_ID + 2) + "+"
                               + "-".repeat(COL_NAME + 2) + "+"
                               + "-".repeat(COL_AGE + 2) + "+"
                               + "-".repeat(COL_GENDER + 2) + "+"
                               + "-".repeat(COL_IC + 2) + "+"
                               + "-".repeat(COL_CONTACT + 2) + "+";

        System.out.println("\nPatients Sorted by Name:");
        System.out.println(separator);
        System.out.printf("| %-" + COL_NO + "s | %-" + COL_ID + "s | %-" + COL_NAME + "s | %-" + COL_AGE + "s | %-" + COL_GENDER + "s | %-" + COL_IC + "s | %-" + COL_CONTACT + "s |\n",
                "No.", "ID", "Name", "Age", "Gender", "IC Number", "Contact");
        System.out.println(separator);

        iter = sorted.iterator();
        int index = 1;
        while (iter.hasNext()) {
            Patient p = iter.next();
            System.out.printf("| %-" + COL_NO + "d | %-" + COL_ID + "s | %-" + COL_NAME + "s | %-" + COL_AGE + "d | %-" + COL_GENDER + "s | %-" + COL_IC + "s | %-" + COL_CONTACT + "s |\n",
                    index++, p.getId(), p.getName(), p.getAge(), p.getGender(), p.getIcNumber(), p.getContact());
        }

        System.out.println(separator);
    }


    public int getPatientCount() {
        return patientQueue.size();
    }

    public int getSize() {
        return patientQueue.size();
    }

    public Patient getPatient(int index) {
        return patientQueue.get(index);
    }

    private void saveAllToFile() {
        try (FileWriter writer = new FileWriter(filePath)) {
            ClinicADT.MyIterator<Patient> iter = patientQueue.iterator();
            while (iter.hasNext()) {
                Patient p = iter.next();
                writer.write(p.toFileString() + "\n");
            }
        } catch (IOException e) {
            System.out.println("Failed to save patients: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        File file = new File(filePath);
        int maxId = 1000;

        if (!file.exists()) {
            System.out.println("Patient file not found. Starting fresh.");
            return;
        }

        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", -1);
                if (parts.length == 6) {
                    String id = parts[0];
                    String name = parts[1];
                    int age = Integer.parseInt(parts[2]);
                    String gender = parts[3];
                    String icNumber = parts[4];
                    String contact = parts[5];

                    Patient patient = new Patient(id, name, age, gender, icNumber, contact);
                    patientQueue.enqueue(patient);

                    try {
                        int numId = Integer.parseInt(id.substring(1));
                        if (numId >= maxId) {
                            maxId = numId + 1;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid ID format in file: " + id);
                    }
                } else {
                    System.out.println("Skipping malformed line in patient file: " + line);
                }
            }

            Patient.setIdCounter(maxId);
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error reading patients from file: " + e.getMessage());
        }
    }

    public ClinicADT<Patient> getAllPatients() {
        ClinicADT<Patient> allPatients = new MyClinicADT<>();
        ClinicADT.MyIterator<Patient> iter = patientQueue.iterator();
        while (iter.hasNext()) {
            allPatients.add(iter.next());
        }
        return allPatients;
    }
}
