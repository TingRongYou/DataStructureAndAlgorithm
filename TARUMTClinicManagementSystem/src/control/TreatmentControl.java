package control;

import adt.ClinicADT;
import adt.MyClinicADT;
import entity.MedicalTreatment;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TreatmentControl {
    private final ClinicADT<MedicalTreatment> allTreatments;
    private final ClinicADT<MedicalTreatment> followUpQueue;
    private final String treatmentFilePath = "src/textFile/treatments.txt";

    public TreatmentControl(ClinicADT<MedicalTreatment> treatments) {
        this.allTreatments = treatments;
        this.followUpQueue = new MyClinicADT<>();
        loadTreatmentsFromFile();
    }

    // --- Add Treatment ---
    public void addTreatment(MedicalTreatment treatment) {
        allTreatments.add(treatment);
        if (treatment.isOverdue()) {
            followUpQueue.enqueue(treatment);
        }
        saveTreatmentToFile(treatment, true);
        System.out.println("Treatment recorded.");
    }

    // --- Process Follow-Up ---
    public MedicalTreatment processNextFollowUp() {
        if (!followUpQueue.isEmpty()) {
            MedicalTreatment next = followUpQueue.dequeue();
            
            // Update the treatment in allTreatments to mark it as completed
            ClinicADT.MyIterator<MedicalTreatment> it = allTreatments.iterator();
            while (it.hasNext()) {
                MedicalTreatment t = it.next();
                if (t.getTreatmentId() == next.getTreatmentId()) {
                    t.setCompleted(true);  // Mark as completed
                    saveTreatmentToFile(null, false);  // Save the updated status to file
                    break;
                }
            }
            
            System.out.println("Follow-up for: " + next.getPatientName());
            return next;
        } else {
            return null;
        }
    }

    // --- Get Treatments by Patient --- 
    public ClinicADT<MedicalTreatment> getTreatmentsByPatient(String patientId, boolean includeFuture) {
        ClinicADT<MedicalTreatment> result = new MyClinicADT<>();
        LocalDateTime now = LocalDateTime.now();

        ClinicADT.MyIterator<MedicalTreatment> it = allTreatments.iterator();
        while (it.hasNext()) {
            MedicalTreatment t = it.next();
            if (t.getPatientId().equalsIgnoreCase(patientId)) {
                //if past
                if (!includeFuture  && t.getTreatmentDateTime().isBefore(now)) {
                    result.add(t);
                }
                else if (includeFuture  && t.getTreatmentDateTime().isAfter(now)) { // if future
                    result.add(t);
                }
            }
        }
        return result;
    }

    // --- Print Follow-Up Queue ---
    public void printFollowUpQueue() {
        System.out.println("\n=== Follow-Up Queue (Overdue Bookings) ===");
        if (followUpQueue.isEmpty()) {
            System.out.println("No patients require follow-up. (No Overdue Bookings)");
            return;
        }

        ClinicADT<MedicalTreatment> temp = new MyClinicADT<>();
        String format = "| %-12s | %-10s | %-15s | %-10s | %-17s | %-9s |%n";
        String line = "+--------------+------------+-----------------+------------+-------------------+-----------+";

        System.out.println(line);
        System.out.printf(format, "Treatment ID", "Patient ID", "Patient Name", "Doctor ID", "Date", "Completed");
        System.out.println(line);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        while (!followUpQueue.isEmpty()) {
            MedicalTreatment t = followUpQueue.dequeue();
            System.out.printf(format,
                    t.getTreatmentId(),
                    t.getPatientId(),
                    t.getPatientName(),
                    t.getDoctorId(),
                    t.getTreatmentDateTime().format(formatter),
                    t.isCompleted() ? "Yes" : "No"
            );
            temp.enqueue(t);
        }

        System.out.println(line);

        while (!temp.isEmpty()) {
            followUpQueue.enqueue(temp.dequeue());
        }
    }

    // --- Print All Treatments Sorted by Date ---
    public void printAllTreatmentsSortedByDate() {
        if (allTreatments.isEmpty()) {
            System.out.println("No treatments to display.");
            return;
        }

        ClinicADT<MedicalTreatment> sorted = new MyClinicADT<>();
        ClinicADT.MyIterator<MedicalTreatment> it = allTreatments.iterator();
        while (it.hasNext()) {
            sorted.add(it.next());
        }

        // Bubble sort by treatment date
        for (int i = 0; i < sorted.size() - 1; i++) {
            for (int j = 0; j < sorted.size() - i - 1; j++) {
                MedicalTreatment t1 = sorted.get(j);
                MedicalTreatment t2 = sorted.get(j + 1);
                if (t1.getTreatmentDateTime().isAfter(t2.getTreatmentDateTime())) {
                    sorted.set(j, t2);
                    sorted.set(j + 1, t1);
                }
            }
        }

        String format = "| %-12s | %-10s | %-15s | %-10s | %-17s | %-9s |\n";
        String line = "+--------------+------------+-----------------+------------+-------------------+-----------+";

        System.out.println("\n=== All Treatments (Sorted by Date) ===");
        System.out.println(line);
        System.out.printf(format, "Treatment ID", "Patient ID", "Patient Name", "Doctor ID", "Date", "Completed");
        System.out.println(line);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        ClinicADT.MyIterator<MedicalTreatment> sortedIt = sorted.iterator();
        while (sortedIt.hasNext()) {
            MedicalTreatment t = sortedIt.next();
            System.out.printf(format,
                    t.getTreatmentId(),
                    t.getPatientId(),
                    t.getPatientName(),
                    t.getDoctorId(),
                    t.getTreatmentDateTime().format(formatter),
                    t.isCompleted() ? "Yes" : "No"
            );
        }

        System.out.println(line);
    }

    // --- Save and Update Treatment to File ---
    private void saveTreatmentToFile(MedicalTreatment treatment, boolean appendMode) {
        try (FileWriter fw = new FileWriter(treatmentFilePath, appendMode)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            if (!appendMode) {
                // Save ALL treatments (overwrite mode)
                ClinicADT.MyIterator<MedicalTreatment> it = allTreatments.iterator();
                while (it.hasNext()) {
                    MedicalTreatment t = it.next();
                    String line = String.format("%d,%s,%s,%s,%s,%s,%s,%b%n",
                            t.getTreatmentId(),
                            t.getPatientId(),
                            t.getPatientName(),
                            t.getDoctorId(),
                            t.getDiagnosis() != null ? t.getDiagnosis() : "N/A",
                            t.getPrescription() != null ? t.getPrescription() : "N/A",
                            t.getTreatmentDateTime().format(formatter),
                            t.isCompleted());
                    fw.write(line);
                }
            } else {
                // Append single treatment
                String line = String.format("%d,%s,%s,%s,%s,%s,%s,%b%n",
                        treatment.getTreatmentId(),
                        treatment.getPatientId(),
                        treatment.getPatientName(),
                        treatment.getDoctorId(),
                        treatment.getDiagnosis() != null ? treatment.getDiagnosis() : "N/A",
                        treatment.getPrescription() != null ? treatment.getPrescription() : "N/A",
                        treatment.getTreatmentDateTime().format(formatter),
                        treatment.isCompleted());
                fw.write(line);
            }
        } catch (IOException e) {
            System.err.println("Error saving treatment:");
            e.printStackTrace();
        }
    }

    // --- Load Treatments from File ---
    public void loadTreatmentsFromFile() {
        allTreatments.clear();
        followUpQueue.clear();

        File file = new File(treatmentFilePath);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(treatmentFilePath))) {
            String line;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 8) {
                    System.out.println("Invalid treatment line: " + line);
                    continue;
                }

                try {
                    int id = Integer.parseInt(parts[0].trim());
                    String patientId = parts[1].trim();
                    String patientName = parts[2].trim();
                    String doctorId = parts[3].trim();
                    String diagnosis = parts[4].trim();
                    String prescription = parts[5].trim();
                    LocalDateTime dateTime = LocalDateTime.parse(parts[6].trim(), formatter);
                    boolean completed = Boolean.parseBoolean(parts[7].trim());

                    MedicalTreatment treatment = new MedicalTreatment(
                            id, patientId, patientName, doctorId, diagnosis, prescription, dateTime, completed
                    );
                    allTreatments.add(treatment);

                    if (treatment.isOverdue()) {
                        followUpQueue.enqueue(treatment);
                    }

                } catch (Exception e) {
                    System.out.println("Error parsing treatment line: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading treatments:");
            e.printStackTrace();
        }
    }
}
