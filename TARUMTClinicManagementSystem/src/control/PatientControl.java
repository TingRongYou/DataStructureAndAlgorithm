package control;

import adt.ClinicADT;
import adt.MyClinicADT;
import entity.Patient;
import utility.Report;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PatientControl {
    private ClinicADT<Patient> patientQueue;
    private final String filePath = "src/textFile/patients.txt";
    private final String consultationFilePath = "src/textFile/consultations.txt";

    public PatientControl() {
        patientQueue = new MyClinicADT<>();
        File directory = new File("src/textFile");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        loadFromFile();
    }

    // =============== CRUD / Basics ===============

    public void registerPatient(String name, int age, String gender, String icNumber, String contact) {
        Patient newPatient = new Patient(name, age, gender, icNumber, contact);
        patientQueue.enqueue(newPatient);
        System.out.println("Patient registered:\n" + newPatient);
        saveAllToFile();
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

    public Patient getPatientById(String id) {
        if (id == null || id.isBlank()) return null;
        ClinicADT.MyIterator<Patient> iter = patientQueue.iterator();
        while (iter.hasNext()) {
            Patient p = iter.next();
            if (p.getId().equalsIgnoreCase(id)) return p;
        }
        return null;
    }

    public ClinicADT<Patient> getAllPatients() {
        ClinicADT<Patient> allPatients = new MyClinicADT<>();
        ClinicADT.MyIterator<Patient> iter = patientQueue.iterator();
        while (iter.hasNext()) {
            allPatients.add(iter.next());
        }
        return allPatients;
    }

    // =============== Displays ===============

    public void printAllPatientsSortedByName() {
        if (patientQueue.isEmpty()) {
            System.out.println("No patients.");
            return;
        }

        // Copy and sort by name (ignore case)
        ClinicADT<Patient> sorted = new MyClinicADT<>();
        ClinicADT.MyIterator<Patient> iter = patientQueue.iterator();
        while (iter.hasNext()) {
            sorted.add(iter.next());
        }
        sorted.sort(new ClinicADT.MyComparator<Patient>() {
            @Override
            public int compare(Patient p1, Patient p2) {
                return p1.getName().compareToIgnoreCase(p2.getName());
            }
        });

        final int COL_NO = 4;
        final int COL_ID = 10;
        final int COL_NAME = 25;
        final int COL_AGE = 5;
        final int COL_GENDER = 8;
        final int COL_IC = 15;
        final int COL_CONTACT = 16;

        String separator = "+"
                + "-".repeat(COL_NO + 2) + "+"
                + "-".repeat(COL_ID + 2) + "+"
                + "-".repeat(COL_NAME + 2) + "+"
                + "-".repeat(COL_AGE + 2) + "+"
                + "-".repeat(COL_GENDER + 2) + "+"
                + "-".repeat(COL_IC + 2) + "+"
                + "-".repeat(COL_CONTACT + 2) + "+";

        Report.cprintln("====== Patients Sorted by Name ======");
        System.out.println(separator);
        System.out.printf("| %-" + COL_NO + "s | %-" + COL_ID + "s | %-" + COL_NAME + "s | %-" + COL_AGE + "s | %-" + COL_GENDER + "s | %-" + COL_IC + "s | %-" + COL_CONTACT + "s |%n",
                "No.", "ID", "Name", "Age", "Gender", "IC Number", "Contact");
        System.out.println(separator);

        iter = sorted.iterator();
        int index = 1;
        while (iter.hasNext()) {
            Patient p = iter.next();
            System.out.printf("| %-" + COL_NO + "d | %-" + COL_ID + "s | %-" + COL_NAME + "s | %-" + COL_AGE + "d | %-" + COL_GENDER + "s | %-" + COL_IC + "s | %-" + COL_CONTACT + "s |%n",
                    index++, p.getId(), p.getName(), p.getAge(), p.getGender(), p.getIcNumber(), p.getContact());
        }

        System.out.println(separator);
    }

    // =============== Persistence ===============

    private void saveAllToFile() {
        try (FileWriter writer = new FileWriter(filePath)) {
            ClinicADT.MyIterator<Patient> iter = patientQueue.iterator();
            while (iter.hasNext()) {
                Patient p = iter.next();
                writer.write(p.toFileString() + System.lineSeparator());
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

        try (java.util.Scanner sc = new java.util.Scanner(file)) {
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

    // =============== Simple Analytics ===============

    public int countPediatric() {
        int count = 0;
        ClinicADT.MyIterator<Patient> it = patientQueue.iterator();
        while (it.hasNext()) {
            if (it.next().getAge() <= 12) count++;
        }
        return count;
    }

    public int countAdolescent() {
        int count = 0;
        ClinicADT.MyIterator<Patient> it = patientQueue.iterator();
        while (it.hasNext()) {
            int age = it.next().getAge();
            if (age >= 13 && age <= 17) count++;
        }
        return count;
    }

    public int countAdult() {
        int count = 0;
        ClinicADT.MyIterator<Patient> it = patientQueue.iterator();
        while (it.hasNext()) {
            int age = it.next().getAge();
            if (age >= 18 && age <= 64) count++;
        }
        return count;
    }

    public int countGeriatric() {
        int count = 0;
        ClinicADT.MyIterator<Patient> it = patientQueue.iterator();
        while (it.hasNext()) {
            if (it.next().getAge() >= 65) count++;
        }
        return count;
    }

    // =============== Reports ===============

    public void medicalHistoryReport() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        String border = "+---------------+------------------+----------------------+-----------------+------------------------------+";
        String headerFormat = "| %-13s | %-16s | %-20s | %-15s | %-28s |%n";
        String rowFormat    = "| %-13s | %-16s | %-20s | %-15s | %-28s |%n";

        Report.printHeader("Medical History Report");
        System.out.println(border);
        System.out.printf(headerFormat, "Patient ID", "Date", "Patient Name", "Doctor ID", "Diagnosis");
        System.out.println(border);

        try (BufferedReader br = new BufferedReader(new FileReader(consultationFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] parts = line.split(",");
                if (parts.length < 7) continue; // skip invalid rows

                String patientId   = parts[1].trim();
                String patientName = parts[2].trim();
                String doctorId    = parts[4].trim();
                LocalDateTime consultationDate = LocalDateTime.parse(parts[5].trim(), formatter);
                String diagnosis   = parts[6].trim();

                // Only show rows with completed diagnosis (not the placeholder)
                if (!diagnosis.equalsIgnoreCase("To be diagnosed during appointment")
                        && !diagnosis.equalsIgnoreCase("Pending")
                        && !diagnosis.isBlank()) {
                    System.out.printf(rowFormat,
                            patientId,
                            consultationDate.format(formatter),
                            patientName,
                            doctorId,
                            diagnosis);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading consultations: " + e.getMessage());
        }

        System.out.println(border);
        Report.printFooter();
    }
}
