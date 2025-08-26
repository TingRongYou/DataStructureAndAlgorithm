package control;

import adt.ClinicADT;
import adt.MyClinicADT;
import entity.MedicalTreatment;
import entity.Consultation;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class TreatmentControl {
    private final ClinicADT<MedicalTreatment> allTreatments;
    private final ClinicADT<MedicalTreatment> followUpQueue;
    private final String treatmentFilePath = "src/textFile/treatments.txt";
    private final Scanner sc = new Scanner(System.in);

    // Optional: injected UI for calendar/slot picking (set from boundary)
    private boundary.BookingUI bookingUI;

    public TreatmentControl(ClinicADT<MedicalTreatment> treatments) {
        this.allTreatments = treatments;
        this.followUpQueue = new MyClinicADT<>();
        loadTreatmentsFromFile();
    }

    // Inject a BookingUI instance (call from boundary after constructing BookingUI)
    public void setBookingUI(boundary.BookingUI bookingUI) {
        this.bookingUI = bookingUI;
    }

    // --- Add Treatment ---
    public void addTreatment(MedicalTreatment treatment) {
        allTreatments.add(treatment);
        refreshFollowUpQueue();          // Update overdue queue
        saveTreatmentToFile(treatment, true);
        System.out.println("Treatment recorded.");
    }

    // --- Enhanced Process Follow-Up with Rescheduling (uses injected BookingUI if available) ---
    public MedicalTreatment processNextFollowUp() {
        if (this.bookingUI == null) {
            return null;
        }
        return processNextFollowUp(this.bookingUI);
    }

    // Overload that takes an explicit BookingUI instance
    public MedicalTreatment processNextFollowUp(boundary.BookingUI bookingUI) {
        refreshFollowUpQueue(); // Ensure queue is up-to-date

        if (followUpQueue.isEmpty()) {
            System.out.println("No overdue treatments found.");
            return null;
        }

        // Display all overdue treatments
        System.out.println("\n=== Overdue Treatments (Follow-up Required) ===");
        String line = "+--------------+------------+-----------------+------------+-------------------+----------+";
        String headerFormat = "| %-12s | %-10s | %-15s | %-10s | %-17s | %-8s |%n";
        String rowFormat    = "| %-12s | %-10s | %-15s | %-10s | %-17s | %-8s |%n";

        System.out.println(line);
        System.out.printf(headerFormat, "Treatment ID", "Patient ID", "Patient Name", "Doctor ID", "Scheduled Date", "Status");
        System.out.println(line);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // Create a temporary queue to display and restore
        ClinicADT<MedicalTreatment> temp = new MyClinicADT<>();
        while (!followUpQueue.isEmpty()) {
            MedicalTreatment t = followUpQueue.dequeue();
            System.out.printf(rowFormat,
                    t.getTreatmentId(),
                    t.getPatientId(),
                    t.getPatientName(),
                    t.getDoctorId(),
                    t.getTreatmentDateTime().format(fmt),
                    "Overdue");
            temp.enqueue(t);
        }
        // Restore the queue
        while (!temp.isEmpty()) {
            followUpQueue.enqueue(temp.dequeue());
        }
        System.out.println(line);

        // Prompt user to select treatment ID
        int selectedId;
        while (true) {
            System.out.print("\nEnter Treatment ID to reschedule and process (or 0 to cancel): ");
            String input = sc.nextLine().trim();
            if (input.equals("0")) {
                System.out.println("Operation cancelled.");
                return null;
            }
            try {
                selectedId = Integer.parseInt(input);
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
            }
        }

        // Find the selected treatment
        MedicalTreatment selectedTreatment = null;
        ClinicADT.MyIterator<MedicalTreatment> it = allTreatments.iterator();
        while (it.hasNext()) {
            MedicalTreatment t = it.next();
            if (t.getTreatmentId() == selectedId && t.isOverdue() && !t.isCompleted()) {
                selectedTreatment = t;
                break;
            }
        }

        if (selectedTreatment == null) {
            System.out.println("Treatment ID " + selectedId + " not found in overdue treatments.");
            return null;
        }

        // === Use BookingUI to choose a new valid slot (calendar + available slots) ===
        LocalDateTime newDateTime = bookingUI.pickSlotForReschedule(selectedTreatment);
        if (newDateTime == null) {
            System.out.println("Reschedule cancelled.");
            return null;
        }

        // Update the treatment with new date and mark as completed
        selectedTreatment.setTreatmentDateTime(newDateTime);
        selectedTreatment.setCompleted(true);

        // Refresh queue and persist file
        refreshFollowUpQueue();
        saveTreatmentToFile(null, false);

        System.out.println("\nTreatment rescheduled and processed successfully!");
        System.out.println("+--------------+----------------------+-------------------+-----------+");
        final String head   = "| %-12s | %-20s | %-17s | %-9s |%n";
        final String rowFmt = "| %-12d | %-20s | %-17s | %-9s |%n";

        System.out.println(line);
        System.out.printf(head, "Treatment ID", "Patient", "New Date", "Status");
        System.out.println(line);
        System.out.printf(rowFmt,
                selectedTreatment.getTreatmentId(),
                selectedTreatment.getPatientName(),
                newDateTime.format(fmt),   
                "Completed");
        System.out.println(line);

        return selectedTreatment;
    }

    // --- Get Treatments by Patient (past / future) ---
    public ClinicADT<MedicalTreatment> getTreatmentsByPatient(String patientId, boolean includeFuture) {
        ClinicADT<MedicalTreatment> result = new MyClinicADT<>();
        LocalDateTime now = LocalDateTime.now();

        ClinicADT.MyIterator<MedicalTreatment> it = allTreatments.iterator();
        while (it.hasNext()) {
            MedicalTreatment t = it.next();
            if (t.getPatientId().equalsIgnoreCase(patientId)) {
                if (!includeFuture && t.getTreatmentDateTime().isBefore(now)) {
                    result.add(t);
                } else if (includeFuture && t.getTreatmentDateTime().isAfter(now)) {
                    result.add(t);
                }
            }
        }
        return result;
    }

    // --- PRINT: Follow-Up Queue ---
    public void printFollowUpQueue() {
        refreshFollowUpQueue(); // Ensure queue is current

        System.out.println("\n=== Follow-Up Queue (Overdue Bookings) ===");
        if (followUpQueue.isEmpty()) {
            System.out.println("No patients require follow-up. (No Overdue Bookings)");
            return;
        }

        ClinicADT<MedicalTreatment> temp = new MyClinicADT<>();
        String format = "| %-12s | %-10s | %-15s | %-10s | %-17s | %-9s |%n";
        String line = "+--------------+------------+-----------------+------------+-------------------+-----------+";

        System.out.println(line);
        System.out.printf(format, "Treatment ID", "Patient ID", "Patient Name", "Doctor ID", "Date", "Status");
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
                    "Overdue"
            );
            temp.enqueue(t);
        }

        System.out.println(line);

        // Restore queue
        while (!temp.isEmpty()) {
            followUpQueue.enqueue(temp.dequeue());
        }
    }

    // --- PRINT: All Treatments Sorted by Date ---
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

        // Bubble sort by date
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
        System.out.printf(format, "Treatment ID", "Patient ID", "Patient Name", "Doctor ID", "Date", "Status");
        System.out.println(line);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime now = LocalDateTime.now();

        ClinicADT.MyIterator<MedicalTreatment> sortedIt = sorted.iterator();
        while (sortedIt.hasNext()) {
            MedicalTreatment t = sortedIt.next();
            String status;
            if (t.isCompleted()) {
                status = "Completed";
            } else if (t.getTreatmentDateTime().isBefore(now)) {
                status = "Overdue";
            } else {
                status = "Scheduled";
            }

            System.out.printf(format,
                    t.getTreatmentId(),
                    t.getPatientId(),
                    t.getPatientName(),
                    t.getDoctorId(),
                    t.getTreatmentDateTime().format(formatter),
                    status
            );
        }

        System.out.println(line);
    }

    // --- Process Treatment for Diagnosed Patient ---
    public void processTreatmentForDiagnosedPatient(ClinicADT<Consultation> consultations, String patientId) {
        if (consultations == null || consultations.isEmpty()) {
            System.out.println("No consultations found in the system.");
            return;
        }
        if (patientId == null || patientId.isBlank()) {
            System.out.println("Invalid patient ID.");
            return;
        }

        Consultation latestDiagnosed = null;
        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) {
            Consultation c = it.next();
            if (c == null) continue;
            if (!patientId.equalsIgnoreCase(c.getPatientId())) continue;

            String d = (c.getDiagnosis() == null) ? "" : c.getDiagnosis().trim();
            boolean diagnosed = !d.isBlank()
                    && !d.equalsIgnoreCase("Pending")
                    && !d.equalsIgnoreCase("To be diagnosed during appointment");

            if (diagnosed) {
                if (latestDiagnosed == null ||
                        c.getConsultationDate().isAfter(latestDiagnosed.getConsultationDate())) {
                    latestDiagnosed = c;
                }
            }
        }

        if (latestDiagnosed == null) {
            System.out.println("No diagnosed consultation found for patient " + patientId +
                    ". Complete the consultation diagnosis first.");
            return;
        }

        System.out.println("\nProcessing treatment for diagnosed consultation:");
        System.out.println("  Patient  : " + latestDiagnosed.getPatientName() + " (" + latestDiagnosed.getPatientId() + ")");
        System.out.println("  Doctor   : " + latestDiagnosed.getDoctorId());
        System.out.println("  Diagnosis: " + latestDiagnosed.getDiagnosis());

        String prescription;
        while (true) {
            System.out.print("Enter prescription (or 0 to cancel): ");
            prescription = sc.nextLine().trim();
            if (prescription.equals("0")) {
                System.out.println("Operation cancelled.");
                return;
            }
            if (!prescription.isBlank()) break;
            System.out.println("Prescription cannot be empty.");
        }

        LocalDateTime when;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        while (true) {
            System.out.print("Enter treatment date & time (yyyy-MM-dd HH:mm) (or 0 to cancel): ");
            String input = sc.nextLine().trim();
            if (input.equals("0")) {
                System.out.println("Operation cancelled.");
                return;
            }
            try {
                when = LocalDateTime.parse(input, dtf);
                if (when.isBefore(LocalDateTime.now())) {
                    System.out.println("Date/time cannot be in the past.");
                    continue;
                }
                break;
            } catch (DateTimeParseException ex) {
                System.out.println("Invalid format. Use yyyy-MM-dd HH:mm (e.g., 2025-08-30 14:00).");
            }
        }

        MedicalTreatment treatment = new MedicalTreatment(
                latestDiagnosed.getPatientId(),
                latestDiagnosed.getPatientName(),
                latestDiagnosed.getDoctorId(),
                latestDiagnosed.getDiagnosis(),
                prescription,
                when,
                false
        );

        // 1) Save the new treatment
        addTreatment(treatment);

        // 2) Remove the diagnosed consultation that generated this treatment
        boolean removed = removeDiagnosedConsultation(consultations, latestDiagnosed);
        if (removed) {
            System.out.println("Diagnosed consultation removed from the list.");
        } else {
            System.out.println("Warning: diagnosed consultation not found to remove.");
        }

        System.out.println("Treatment created for patient " + latestDiagnosed.getPatientName()
                + " on " + when.format(dtf) + ".");
    }

    // Remove a specific diagnosed consultation from the passed-in list.
    // Returns true if a record was removed.
    private boolean removeDiagnosedConsultation(ClinicADT<Consultation> consultations, Consultation target) {
        if (consultations == null || consultations.size() == 0 || target == null) return false;

        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            if (sameConsultation(c, target)) {
                consultations.remove(i);
                return true;
            }
        }
        return false;
    }

    // Compare by patientId, doctorId, and consultationDate.
    private boolean sameConsultation(Consultation a, Consultation b) {
        if (a == b) return true;
        if (a == null || b == null) return false;

        String pA = a.getPatientId(), pB = b.getPatientId();
        String dA = a.getDoctorId(),  dB = b.getDoctorId();
        LocalDateTime tA = a.getConsultationDate();
        LocalDateTime tB = b.getConsultationDate();

        boolean samePatient = (pA != null && pB != null) && pA.equalsIgnoreCase(pB);
        boolean sameDoctor  = (dA != null && dB != null) && dA.equalsIgnoreCase(dB);
        boolean sameTime    = (tA == null) ? (tB == null) : tA.equals(tB);

        return samePatient && sameDoctor && sameTime;
    }

    // --- Enhanced: Get All Pending Bookings (only scheduled/future treatments) ---
    public ClinicADT<MedicalTreatment> getAllPendingBookings() {
        ClinicADT<MedicalTreatment> result = new MyClinicADT<>();
        LocalDateTime now = LocalDateTime.now();

        ClinicADT.MyIterator<MedicalTreatment> it = allTreatments.iterator();
        while (it.hasNext()) {
            MedicalTreatment t = it.next();
            // Only include treatments that are not completed AND not overdue
            if (t != null && !t.isCompleted() && !t.getTreatmentDateTime().isBefore(now)) {
                result.add(t);
            }
        }
        return result;
    }

    // --- Enhanced: Process Booked Treatment (only if not overdue) ---
    public boolean processBookedTreatmentById(int treatmentId) {
        LocalDateTime now = LocalDateTime.now();

        ClinicADT.MyIterator<MedicalTreatment> it = allTreatments.iterator();
        while (it.hasNext()) {
            MedicalTreatment t = it.next();
            if (t != null && t.getTreatmentId() == treatmentId && !t.isCompleted()) {
                // Check if treatment is overdue
                if (t.getTreatmentDateTime().isBefore(now)) {
                    System.out.println("Cannot process overdue treatment. Use Follow-up option to reschedule first.");
                    return false;
                }

                // Process the treatment
                t.setCompleted(true);
                refreshFollowUpQueue();
                saveTreatmentToFile(null, false);
                return true;
            }
        }
        return false;
    }

    // --- Enhanced: Refresh Follow-Up Queue (overdue treatments only) ---
    private void refreshFollowUpQueue() {
        followUpQueue.clear();
        LocalDateTime now = LocalDateTime.now();

        ClinicADT.MyIterator<MedicalTreatment> it = allTreatments.iterator();
        while (it.hasNext()) {
            MedicalTreatment t = it.next();
            // Add to queue if treatment date has passed and not completed
            if (t != null && t.getTreatmentDateTime().isBefore(now) && !t.isCompleted()) {
                followUpQueue.enqueue(t);
            }
        }
    }

    // --- Save and Update Treatment to File ---
    private void saveTreatmentToFile(MedicalTreatment treatment, boolean appendMode) {
        try (FileWriter fw = new FileWriter(treatmentFilePath, appendMode)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            if (!appendMode) {
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

                } catch (Exception e) {
                    System.out.println("Error parsing treatment line: " + line);
                }
            }

            // Refresh follow-up queue after loading
            refreshFollowUpQueue();

        } catch (IOException e) {
            System.err.println("Error loading treatments:");
            e.printStackTrace();
        }
    }
}
