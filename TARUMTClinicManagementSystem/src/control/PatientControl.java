package control;

import adt.ClinicADT;
import adt.MyClinicADT;
import entity.Patient;
import entity.Consultation;
import java.io.BufferedReader;
import utility.Report;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class PatientControl {
    private ClinicADT<Patient> patientQueue;
    private final String filePath = "src/textFile/patients.txt";
    private String consultationFilePath = "src/textFile/consultations.txt";
    
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
    
    public int countPediatric() {
        int count = 0;
        ClinicADT<Patient> allPatients = getAllPatients();
        ClinicADT.MyIterator<Patient> it = allPatients.iterator();
        while (it.hasNext()) {
            if (it.next().getAge() <= 12) count++;
        }
        return count;
    }

    public int countAdolescent() {
        int count = 0;
        ClinicADT<Patient> allPatients = getAllPatients();
        ClinicADT.MyIterator<Patient> it = allPatients.iterator();
        while (it.hasNext()) {
            int age = it.next().getAge();
            if (age >= 13 && age <= 17) count++;
        }
        return count;
    }

    public int countAdult() {
        int count = 0;
        ClinicADT<Patient> allPatients = getAllPatients();
        ClinicADT.MyIterator<Patient> it = allPatients.iterator();
        while (it.hasNext()) {
            int age = it.next().getAge();
            if (age >= 18 && age <= 64) count++;
        }
        return count;
    }

    public int countGeriatric() {
        int count = 0;
        ClinicADT<Patient> allPatients = getAllPatients();
        ClinicADT.MyIterator<Patient> it = allPatients.iterator();
        while (it.hasNext()) {
            if (it.next().getAge() >= 65) count++;
        }
        return count;
    }
    public void medicalHistoryReport() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        String border = "+---------------+------------------+----------------------+-----------------+------------------------------+";
        String headerFormat = "| %-13s | %-16s | %-20s | %-15s | %-28s |%n";
        
        try (BufferedReader br = new BufferedReader(new FileReader(consultationFilePath))) {
            String line;

            Report.printHeader("Medical History Report");
            System.out.println(border);
            System.out.printf(headerFormat, "Patient ID", "Date", "Patient Name", "Doctor ID", "Diagnosis");
            System.out.println(border);

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                if (parts.length < 7) {
                    continue; // skip invalid rows
                }

                String patientId = parts[1].trim();
                String patientName = parts[2].trim();
                String doctorId = parts[4].trim();
                LocalDateTime consultationDate = LocalDateTime.parse(parts[5].trim(), formatter);
                String diagnosis = parts[6].trim();

                // Only print if diagnosis is NOT "To be diagnosed"
                if (!diagnosis.equalsIgnoreCase("To be diagnosed during appointment")) {
                    System.out.printf(headerFormat,
                            patientId, consultationDate, patientName, doctorId, diagnosis);
                }
            }
            System.out.println(border);
        } catch (IOException e) {
            System.out.println("Error reading consultations: " + e.getMessage());
        }
        
        Report.printFooter();
    }
    
}
