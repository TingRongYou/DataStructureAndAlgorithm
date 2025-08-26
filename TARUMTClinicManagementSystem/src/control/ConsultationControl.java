package control;

import adt.ClinicADT;
import adt.MyClinicADT;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import entity.Consultation;
import entity.Doctor;
import tarumtclinicmanagementsystem.DutySchedule;
import entity.MedicalTreatment;
import entity.Patient;
import tarumtclinicmanagementsystem.Session;

public class ConsultationControl {
    private ClinicADT<Consultation> consultations;
    private PatientControl patientControl;
    private DoctorControl doctorControl;
    private Scanner sc;
    private ClinicADT<MedicalTreatment> treatments;
    private String consultationFilePath = "src/textFile/consultations.txt";

    // === Waiting queue of pending consultation IDs (FIFO) ===
    private final java.util.Deque<Integer> waitingQueue = new java.util.ArrayDeque<>();

    // === The only consultation currently allowed to be processed ===
    private Integer currentCalledConsultationId = null;

    //validations
    private static final int MAX_DAILY_CONSULTATIONS_PER_DOCTOR = 8;
    private static final int MAX_WEEKLY_CONSULTATIONS_PER_PATIENT = 3;
    private static final int MIN_MINUTES_BETWEEN_CONSULTATIONS = 15;

    // Working hours for each session
    private static final LocalTime MORNING_START = LocalTime.of(8, 0);
    private static final LocalTime MORNING_END = LocalTime.of(12, 0);
    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
    private static final LocalTime AFTERNOON_END = LocalTime.of(17, 0);
    private static final LocalTime NIGHT_START = LocalTime.of(18, 0);
    private static final LocalTime NIGHT_END = LocalTime.of(22, 0);

    // Consultation duration in hours
    private static final int CONSULTATION_DURATION = 1;
    
     // Holds processed consultation IDs like "|1|7|23|"
    private String processedIdx = "|";

    public ConsultationControl(PatientControl patientControl, DoctorControl doctorControl,
                               ClinicADT<Consultation> consultations, ClinicADT<MedicalTreatment> treatments) {
        this.patientControl = patientControl;
        this.doctorControl = doctorControl;
        this.consultations = consultations;
        this.treatments = treatments;
        this.sc = new Scanner(System.in);

        loadConsultationsFromFile();
    }

    // Alternative constructor for backward compatibility
    public ConsultationControl(PatientControl patientControl, DoctorControl doctorControl) {
        this(patientControl, doctorControl, new MyClinicADT<>(), new MyClinicADT<>());
    }

    // =========================
    // === Queue/Lock Access ===
    // =========================
    public Integer getCurrentCalledConsultationId() {
        return currentCalledConsultationId;
    }

    public String getCurrentCalledPatientId() {
        if (currentCalledConsultationId == null) return null;
        Consultation c = findById(currentCalledConsultationId);
        return (c == null) ? null : c.getPatientId();
    }

    private Consultation findById(int id) {
        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) {
            Consultation c = it.next();
            if (c.getId() == id) return c;
        }
        return null;
    }

    /** Call the next consultation in FIFO queue (does NOT delete patient). */
    public boolean callNextFromQueue() {
        if (currentCalledConsultationId != null) {
            System.out.println("A patient is already called (Consultation ID: "
                    + currentCalledConsultationId + "). Finish processing first.");
            return false;
        }
        Integer next = waitingQueue.pollFirst(); // pop head (FIFO)
        if (next == null) {
            System.out.println("No pending consultations in the queue.");
            return false;
        }
        Consultation c = findById(next);
        if (c == null) {
            System.out.println("Warning: consultation not found for queued ID " + next);
            return false;
        }
        currentCalledConsultationId = next;
        System.out.println(">> Called: Patient " + c.getPatientName() + " (" + c.getPatientId()
                + "), Consultation ID: " + c.getId());
        return true;
    }

    // =========================
    // ====== Add / Book  ======
    // =========================
    public void addConsultationFlow() {
        ClinicADT<Patient> allPatients = patientControl.getAllPatients();

        if (allPatients.isEmpty()) {
            System.out.println("No patients registered. Please register a patient first.");
            return;
        }

        System.out.println("\nRegistered Patients:");
        displayPatientTable(allPatients);

        Patient selectedPatient = selectPatient();
        if (selectedPatient == null) return;

        LocalDateTime dateTime = getValidDateTime();
        if (dateTime == null) return;

        // Check patient frequency limits
        if (hasPatientExceededFrequency(selectedPatient.getId())) {
            System.out.println("Patient has exceeded the weekly consultation limit (max 3 per week).");
            return;
        }

        // Check if patient already booked at that time
        if (isPatientAlreadyBooked(selectedPatient.getId(), dateTime.toLocalDate(), true)) {
            System.out.println("Patient already has a consultation booked on that date.");
            return;
        }

        Doctor selectedDoctor = selectAvailableDoctor(dateTime);
        if (selectedDoctor == null) return;

        // Final validations before adding
        if (hasDoctorReachedDailyLimit(selectedDoctor.getId(), dateTime.toLocalDate())) {
            System.out.println("Doctor has reached the daily consultation limit (max 8 per day).");
            return;
        }

        if (!hasMinimumGap(selectedDoctor, dateTime)) {
            System.out.println("Doctor needs at least " + MIN_MINUTES_BETWEEN_CONSULTATIONS + " minutes between consultations.");
            return;
        }

        addConsultation(selectedPatient.getId(), selectedPatient.getName(),
                selectedDoctor.getName(), selectedDoctor.getId(), dateTime, "To be diagnosed during appointment");
    }

    private Patient selectPatient() {
        System.out.print("Enter Patient ID from the list: ");
        String patientId = sc.nextLine().trim().toUpperCase();

        Patient selectedPatient = patientControl.getPatientById(patientId);
        if (selectedPatient == null) {
            System.out.println("Invalid Patient ID.");
        }
        return selectedPatient;
    }

    private Doctor selectAvailableDoctor(LocalDateTime dateTime) {
        ClinicADT<Doctor> availableDoctors = getAvailableDoctors(dateTime);

        if (availableDoctors.isEmpty()) {
            System.out.println("No doctors available at the selected time.");
            System.out.println("Please choose a different time slot.");
            return null;
        }

        System.out.println("\nAvailable Doctors for "
                + dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + ":");
        displayAvailableDoctors(availableDoctors);

        System.out.print("Select Doctor ID: ");
        String doctorId = sc.nextLine().trim().toUpperCase();

        ClinicADT.MyIterator<Doctor> iterator = availableDoctors.iterator();
        while (iterator.hasNext()) {
            Doctor doc = iterator.next();
            if (doc.getId().equalsIgnoreCase(doctorId)) {
                if (isDoctorBooked(doc.getName(), dateTime)) {
                    System.out.println("Doctor " + doc.getName() + " is already booked for this time slot.");
                    return null;
                }
                return doc;
            }
        }

        System.out.println("Invalid Doctor ID or doctor not available at this time.");
        return null;
    }

    private LocalDateTime getValidDateTime() {
        while (true) {
            try {
                System.out.print("Enter date and time (yyyy-MM-dd HH:mm): ");
                String input = sc.nextLine().trim();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime dateTime = LocalDateTime.parse(input, formatter);

                if (dateTime.isBefore(LocalDateTime.now())) {
                    System.out.println("Cannot schedule consultation in the past.");
                    continue;
                }

                if (!isWorkingHours(dateTime.toLocalTime())) {
                    System.out.println("Time is outside working hours.");
                    displayWorkingHours();
                    continue;
                }

                return dateTime;
            } catch (Exception e) {
                System.out.println("Invalid format. Use 'yyyy-MM-dd HH:mm'.");
            }
        }
    }

    private void displayWorkingHours() {
        System.out.println("Working hours:");
        System.out.println("Morning: 08:00-12:00");
        System.out.println("Afternoon: 13:00-17:00");
        System.out.println("Night: 18:00-22:00");
    }

    private boolean isWorkingHours(LocalTime time) {
        return isInTimeRange(time, MORNING_START, MORNING_END) ||
               isInTimeRange(time, AFTERNOON_START, AFTERNOON_END) ||
               isInTimeRange(time, NIGHT_START, NIGHT_END);
    }

    private boolean isInTimeRange(LocalTime time, LocalTime start, LocalTime end) {
        return !time.isBefore(start) && time.isBefore(end);
    }

    private Session getSessionForTime(LocalTime time) {
        if (isInTimeRange(time, MORNING_START, MORNING_END)) {
            return Session.MORNING;
        } else if (isInTimeRange(time, AFTERNOON_START, AFTERNOON_END)) {
            return Session.AFTERNOON;
        } else if (isInTimeRange(time, NIGHT_START, NIGHT_END)) {
            return Session.NIGHT;
        }
        return null;
    }

    private ClinicADT<Doctor> getAvailableDoctors(LocalDateTime dateTime) {
        ClinicADT<Doctor> availableDoctors = new MyClinicADT<>();
        Session requiredSession = getSessionForTime(dateTime.toLocalTime());

        if (requiredSession == null) {
            return availableDoctors; // return empty ClinicADT
        }

        ClinicADT.MyIterator<Doctor> iterator = doctorControl.getAllDoctors().iterator();
        while (iterator.hasNext()) {
            Doctor doctor = iterator.next();
            if (doctor != null && isDoctorOnDuty(doctor, dateTime, requiredSession)) {
                availableDoctors.add(doctor);
            }
        }

        return availableDoctors;
    }

    private boolean isDoctorOnDuty(Doctor doctor, LocalDateTime dateTime, Session requiredSession) {
        DutySchedule schedule = doctor.getDutySchedule();
        Session doctorSession = schedule.getSessionForDay(dateTime.getDayOfWeek());
        return doctorSession == requiredSession;
    }

    private void displayAvailableDoctors(ClinicADT<Doctor> doctors) {
        String format = "| %-10s | %-15s | %-6s | %-8s | %-16s |\n";
        String line = "+------------+-----------------+--------+----------+------------------+";

        System.out.println(line);
        System.out.printf(format, "Doctor ID", "Name", "Room", "Gender", "Phone");
        System.out.println(line);

        ClinicADT.MyIterator<Doctor> iterator = doctors.iterator();
        while (iterator.hasNext()) {
            Doctor doc = iterator.next();
            System.out.printf(format,
                    doc.getId(),
                    doc.getName(),
                    doc.getRoomNumber(),
                    doc.getGender(),
                    doc.getPhoneNumber());
        }

        System.out.println(line);
    }

    private void displayPatientTable(ClinicADT<Patient> patients) {
        String format = "| %-10s | %-20s | %-3s | %-6s | %-12s |\n";
        String line = "+------------+----------------------+-----+--------+--------------+";

        System.out.println(line);
        System.out.printf(format, "Patient ID", "Name", "Age", "Gender", "Contact");
        System.out.println(line);

        ClinicADT.MyIterator<Patient> iterator = patients.iterator();
        while (iterator.hasNext()) {
            Patient p = iterator.next();
            System.out.printf(format,
                    p.getId(),
                    p.getName(),
                    p.getAge(),
                    p.getGender(),
                    p.getContact());
        }

        System.out.println(line);
    }

    private boolean isDoctorBooked(String doctorName, LocalDateTime dateTime) {
        LocalDateTime consultationEnd = dateTime.plusHours(CONSULTATION_DURATION);

        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation consultation = iterator.next();
            if (consultation.getDoctorName().equalsIgnoreCase(doctorName)) {
                LocalDateTime existingStart = consultation.getConsultationDate();
                LocalDateTime existingEnd = existingStart.plusHours(CONSULTATION_DURATION);

                if (hasTimeOverlap(dateTime, consultationEnd, existingStart, existingEnd)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasTimeOverlap(LocalDateTime start1, LocalDateTime end1,
                                   LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    public void addConsultation(String patientId, String patientName, String doctorName, String doctorId, LocalDateTime date , String diagnosis) {
        Consultation consultation = new Consultation(patientId, patientName, doctorName, doctorId, date, diagnosis);
        consultations.add(consultation);
        saveConsultationToFile(consultation, false);
        displayConsultationConfirmation(consultation);
        if (isPending(consultation)) {
            waitingQueue.addLast(consultation.getId());
        }
    }

    private void displayConsultationConfirmation(Consultation consultation) {
        System.out.println("\nConsultation added successfully!");
        System.out.println(consultation);
        System.out.println("Duration: " + CONSULTATION_DURATION + " hour (ends at "
                + consultation.getConsultationDate().plusHours(CONSULTATION_DURATION)
                .format(DateTimeFormatter.ofPattern("HH:mm")) + ")");
        System.out.println("Status  : " + statusOf(consultation));
    }

    // =================================================
    // === PROCESS: ONLY the currently CALLED allowed ==
    // =================================================
    public void updateConsultation(String patientId) {
        // HARD GUARD 1: must have a called consultation
        if (currentCalledConsultationId == null) {
            System.out.println("No patient has been called. Use 'Call Next Patient' first.");
            return;
        }
        // HARD GUARD 2: called patient must match the passed-in patientId
        String calledPid = getCurrentCalledPatientId();
        if (calledPid == null || !calledPid.equalsIgnoreCase(patientId.trim())) {
            System.out.println("Only the CALLED patient can be processed. Called patient: "
                    + (calledPid == null ? "None" : calledPid));
            return;
        }

        // Find the *called* consultation
        Consultation consultationToProcess = findById(currentCalledConsultationId);
        if (consultationToProcess == null) {
            System.out.println("Internal error: called consultation not found.");
            currentCalledConsultationId = null; // clear bad state
            return;
        }
        // Extra safety: ensure it's still pending
        if (!isPending(consultationToProcess)) {
            System.out.println("Called consultation is already processed.");
            currentCalledConsultationId = null;
            return;
        }

        // Show the single called consultation (include Status)
        final String line =
                "+------------+----------------------+--------------+--------------+---------------------+---------------------+";
        final String headerFmt =
                "| %-10s | %-20s | %-12s | %-12s | %-19s | %-19s |%n";
        final String rowFmt =
                "| %10d | %-20s | %-12s | %-12s | %-19s | %-19s |%n";

        System.out.println("\nCalled consultation to process:");
        System.out.println(line);
        System.out.printf(headerFmt, "Consult ID", "Patient", "Doctor", "Doctor ID", "Date & Time", "Status");
        System.out.println(line);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String dateStr = (consultationToProcess.getConsultationDate() != null)
                ? consultationToProcess.getConsultationDate().format(fmt) : "N/A";
        System.out.printf(rowFmt,
                consultationToProcess.getId(),
                consultationToProcess.getPatientName(),
                consultationToProcess.getDoctorName(),
                consultationToProcess.getDoctorId(),
                dateStr,
                statusOf(consultationToProcess));
        System.out.println(line);

        // Choose diagnosis (menu + manual, or free-typed)
        String diagnosis = chooseDiagnosisInteractive();
        if (diagnosis == null) {
            System.out.println("Operation cancelled.");
            return;
        }

        // Update diagnosis
        consultationToProcess.setDiagnosis(diagnosis);
        System.out.println("Diagnosis updated successfully!");

        // Persist entire list (overwrite)
        saveConsultationToFile(consultationToProcess, false);

        // Clear the lock so next patient can be called
        currentCalledConsultationId = null;
    }

    /** A consultation is pending if diagnosis is null/blank or equals the placeholder text. */
    private boolean isPending(Consultation c) {
        String d = (c.getDiagnosis() == null) ? "" : c.getDiagnosis().trim();
        return d.isEmpty() || d.equalsIgnoreCase("Pending")
                || d.equalsIgnoreCase("To be diagnosed during appointment");
    }

    /** Status string for display. */
    private String statusOf(Consultation c) {
        return isPending(c) ? "PENDING" : "PROCESSED";
    }

    /** Menu + manual entry; also accepts direct typed diagnosis text. Returns null if user cancels. */
    private String chooseDiagnosisInteractive() {
        final String[] commonDiagnoses = {
                "Food Poisoning", "Common Cold", "COVID-19", "Dengue", "Allergies",
                "Hypertension", "Diabetes", "Migraine", "Gastritis", "Bronchitis"
        };

        while (true) {
            System.out.println("\nTypes of Diagnosis");
            System.out.println("=========================");
            for (int i = 0; i < commonDiagnoses.length; i++) {
                System.out.printf("%2d. %s%n", i + 1, commonDiagnoses[i]);
            }
            System.out.printf("%2d. Other (enter manually)%n", commonDiagnoses.length + 1);
            System.out.println(" 0. Cancel");

            System.out.print("Enter Diagnosis: ");
            String input = sc.nextLine().trim();

            if (input.equals("0")) return null;

            try {
                int opt = Integer.parseInt(input);
                if (opt >= 1 && opt <= commonDiagnoses.length) {
                    return commonDiagnoses[opt - 1];
                } else if (opt == commonDiagnoses.length + 1) {
                    // Manual
                    while (true) {
                        System.out.print("Enter custom diagnosis: ");
                        String custom = sc.nextLine().trim();
                        if (!custom.isEmpty()) return custom;
                        System.out.println("Diagnosis cannot be empty. Try again.");
                    }
                } else {
                    System.out.println("Invalid option. Please choose 0–" + (commonDiagnoses.length + 1) + ".");
                }
            } catch (NumberFormatException e) {
                // Treat as free-typed diagnosis
                if (!input.isEmpty()) return input;
                System.out.println("Please enter a valid option or diagnosis text.");
            }
        }
    }

    private void saveConsultationToFile(Consultation consultation, boolean appendMode) {
        try (FileWriter fw = new FileWriter(consultationFilePath, appendMode)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            if (!appendMode) {
                ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
                while (iterator.hasNext()) {
                    Consultation c = iterator.next();
                    String line = String.format("%d,%s,%s,%s,%s,%s,%s%n",
                            c.getId(),
                            c.getPatientId(),
                            c.getPatientName(),
                            c.getDoctorName(),
                            c.getDoctorId(),
                            c.getConsultationDate().format(formatter),
                            c.getDiagnosis() != null ? c.getDiagnosis() : "N/A");
                    fw.write(line);
                }

            } else {
                // Append single consultation
                String line = String.format("%d,%s,%s,%s,%s,%s,%s%n",
                        consultation.getId(),
                        consultation.getPatientId(),
                        consultation.getPatientName(),
                        consultation.getDoctorName(),
                        consultation.getDoctorId(),
                        consultation.getConsultationDate().format(formatter),
                        consultation.getDiagnosis() != null ? consultation.getDiagnosis() : "N/A");
                fw.write(line);
            }
        } catch (IOException e) {
            System.out.println("Error saving consultation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean removeConsultationById(int id) {
        for (int i = 0; i < consultations.size(); i++) {
            if (consultations.get(i).getId() == id) {
                Consultation removed = consultations.get(i);
                consultations.remove(i);
                System.out.println("Consultation removed: " + removed);
                // also ensure it's not in queue or the current called
                waitingQueue.remove(id);
                if (currentCalledConsultationId != null && currentCalledConsultationId == id) {
                    currentCalledConsultationId = null;
                }
                // persist file after removal
                saveConsultationToFile(null, false);
                return true;
            }
        }
        System.out.println("Consultation ID not found.");
        return false;
    }

    public void listConsultations() {
        if (consultations.isEmpty()) {
            System.out.println("No consultations scheduled.");
            return;
        }

        System.out.println("\n=== All Consultations ===");
        displayConsultationTable();
    }

    /** MAIN table (now with Status column). */
    private void displayConsultationTable() {
        String format = "| %-4s | %-12s | %-20s | %-20s | %-20s | %-9s | %-10s |\n";
        String line   = "+------+--------------+----------------------+----------------------+----------------------+-----------+------------+";

        System.out.println(line);
        System.out.printf(format, "ID", "Patient ID", "Patient Name", "Doctor", "Date & Time", "Duration", "Status");
        System.out.println(line);

        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            String dt = (c.getConsultationDate() == null) ? "N/A" : c.getConsultationDate().format(fmt);
            System.out.printf(format,
                    c.getId(),
                    c.getPatientId(),
                    c.getPatientName(),
                    c.getDoctorName(),
                    dt,
                    CONSULTATION_DURATION + " hr",
                    statusOf(c));
        }

        System.out.println(line);
    }

    public void processPatient() {
        ClinicADT.MyIterator<Consultation> process = consultations.iterator();
        while (process.hasNext()) {
            // placeholder
            process.next();
        }
    }

    public void searchByPatient(String patientName) {
        ClinicADT<Consultation> found = new MyClinicADT<>();

        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            if (c.getPatientName().toLowerCase().contains(patientName.toLowerCase())) {
                found.add(c);
            }
        }

        if (found.isEmpty()) {
            System.out.println("No consultations found for patient: " + patientName);
            return;
        }

        System.out.println("\n=== Consultations for Patient: " + patientName + " ===");
        displayConsultationDetails(found);
    }

    public void searchByDoctor(Scanner sc, DoctorControl doctorControl, ClinicADT<Consultation> consultations) {
        if (doctorControl.getAllDoctors().isEmpty()) {
            System.out.println("No doctors registered.");
            return;
        }

        // Step 1: Display all doctors
        System.out.println("\n=== All Registered Doctors ===");
        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");
        System.out.printf("| %-10s | %-14s | %-6s | %-10s | %-10s | %-16s | %-12s |\n",
                "Doctor ID", "Name", "Room", "Available", "Gender", "IC Number", "Phone");
        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");

        ClinicADT.MyIterator<Doctor> doctorIterator = doctorControl.getAllDoctors().iterator();
        while (doctorIterator.hasNext()) {
            Doctor doc = doctorIterator.next();
            System.out.printf("| %-10s | %-14s | %-6d | %-10s | %-10s | %-16s | %-12s |\n",
                    doc.getId(), doc.getName(), doc.getRoomNumber(),
                    doc.isAvailable() ? "Yes" : "No",
                    doc.getGender(), doc.getIcNumber(), doc.getPhoneNumber());
        }
        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");

        // Step 2: Prompt for Doctor ID
        Doctor doctor = null;
        String doctorId;
        while (true) {
            System.out.print("Enter Doctor ID to search consultations (or 0 to cancel): ");
            doctorId = sc.nextLine().trim();

            if (doctorId.equals("0")) {
                System.out.println("Operation cancelled.");
                return;
            }

            doctor = doctorControl.getDoctorById(doctorId);
            if (doctor == null) {
                System.out.println("Doctor ID not found.\n");
            } else {
                break;
            }
        }

        // Step 3: Search consultations
        ClinicADT<Consultation> found = new MyClinicADT<>();
        ClinicADT.MyIterator<Consultation> consultationIterator = consultations.iterator();
        while (consultationIterator.hasNext()) {
            Consultation c = consultationIterator.next();
            if (c.getDoctorId().equalsIgnoreCase(doctorId)) {
                found.add(c);
            }
        }

        // Step 4: Display results
        if (found.isEmpty()) {
            System.out.println("No consultations found for Dr. " + doctor.getName() + " (" + doctor.getId() + ").");
        } else {
            System.out.println("\n=== Consultations for Dr. " + doctor.getName() + " (" + doctor.getId() + ") ===");
            displayConsultationDetails(found);
        }
    }

    /** Detail table used by searches (now with Status column). */
    private void displayConsultationDetails(ClinicADT<Consultation> consultationList) {
        String format = "| %-4s | %-20s | %-15s | %-16s | %-8s | %-8s | %-8s | %-10s |\n";
        String line = "+------+----------------------+-----------------+------------------+----------+----------+----------+------------+";

        System.out.println(line);
        System.out.printf(format, "ID", "Patient", "Doctor", "Date", "Start", "End", "Duration", "Status");
        System.out.println(line);

        ClinicADT.MyIterator<Consultation> iterator = consultationList.iterator();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            LocalDateTime start = c.getConsultationDate();
            LocalDateTime end = start.plusHours(CONSULTATION_DURATION);

            String date = (start == null) ? "N/A" : start.toLocalDate().toString();
            String startStr = (start == null) ? "N/A" : start.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            String endStr   = (start == null) ? "N/A" : end.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));

            System.out.printf(format,
                    c.getId(),
                    c.getPatientName(),
                    c.getDoctorName(),
                    date,
                    startStr,
                    endStr,
                    CONSULTATION_DURATION + " hr",
                    statusOf(c));
        }
        System.out.println(line);
    }

    public void showDoctorScheduleForDate(LocalDateTime date) {
        if (date.toLocalDate().isBefore(LocalDateTime.now().toLocalDate())) {
            System.out.println("Cannot check availability for past dates.");
            return;
        }
        System.out.println("\n=== Doctor Availability for " + 
            date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd (EEEE)")) + " ===");

        displaySessionAvailability(date, Session.MORNING, "Morning Shift (08:00-12:00)");
        displaySessionAvailability(date, Session.AFTERNOON, "Afternoon Shift (13:00-17:00)");
        displaySessionAvailability(date, Session.NIGHT, "Night Shift (18:00-22:00)");
    }

    private void displaySessionAvailability(LocalDateTime date, Session session, String header) {
        ClinicADT<Doctor> sessionDoctors = getAvailableDoctorsForSession(date, session);

        System.out.println("\n" + header);
        String line = "+----------------+------------+";
        String format = "| %-14s | %-10s |\n";

        System.out.println(line);
        System.out.printf(format, "Doctor Name", "Room No.");
        System.out.println(line);

        if (sessionDoctors.isEmpty()) {
            System.out.printf("| %-24s |\n", "No doctors available");
        } else {
            ClinicADT.MyIterator<Doctor> iterator = sessionDoctors.iterator();
            while (iterator.hasNext()) {
                Doctor doc = iterator.next();
                System.out.printf(format, doc.getName(), doc.getRoomNumber());
            }
        }
        System.out.println(line);
    }

    private ClinicADT<Doctor> getAvailableDoctorsForSession(LocalDateTime date, Session session) {
        ClinicADT<Doctor> sessionDoctors = new MyClinicADT<>();
        ClinicADT<Doctor> allDoctors = doctorControl.getAllDoctors();

        ClinicADT.MyIterator<Doctor> iterator = allDoctors.iterator();
        while (iterator.hasNext()) {
            Doctor doctor = iterator.next();
            if (doctor != null && isDoctorOnDuty(doctor, date, session)) {
                sessionDoctors.add(doctor);
            }
        }

        return sessionDoctors;
    }

    public int getTotalConsultations() {
        return consultations.size();
    }

    public ClinicADT<Consultation> getAllConsultations() {
        return consultations;
    }

    // Utility method to get consultation statistics
    public void displayConsultationStatistics() {
        System.out.println("\n=== Consultation Statistics ===");
        System.out.println("Total Consultations: " + consultations.size());

        if (consultations.isEmpty()) {
            System.out.println("No consultation data available.");
            return;
        }
        
        int morningCount = 0, afternoonCount = 0, nightCount = 0;
        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            LocalTime time = c.getConsultationDate().toLocalTime();

            if (isInTimeRange(time, MORNING_START, MORNING_END)) {
                morningCount++;
            } else if (isInTimeRange(time, AFTERNOON_START, AFTERNOON_END)) {
                afternoonCount++;
            } else if (isInTimeRange(time, NIGHT_START, NIGHT_END)) {
                nightCount++;
            }
        }

        System.out.println("By Session:");
        System.out.println("  Morning: " + morningCount);
        System.out.println("  Afternoon: " + afternoonCount);
        System.out.println("  Night: " + nightCount);
    }

    public ClinicADT<Patient> getPatientsWithConsultations() {
        ClinicADT<Patient> result = new MyClinicADT<>();

        ClinicADT.MyIterator<Consultation> consultationIterator = consultations.iterator();
        while (consultationIterator.hasNext()) {
            Consultation c = consultationIterator.next();
            String patientId = c.getPatientId();

            boolean exists = false;
            ClinicADT.MyIterator<Patient> patientIterator = result.iterator();
            while (patientIterator.hasNext()) {
                if (patientIterator.next().getId().equalsIgnoreCase(patientId)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                Patient p = patientControl.getPatientById(patientId);
                if (p != null) {
                    result.add(p);
                }
            }
        }
        return result;
    }

    private boolean isPatientAlreadyBooked(String patientId, LocalDate selectedDate, boolean isConsultation) {
        int consultationCount = 0;
        int treatmentCount = 0;

        ClinicADT.MyIterator<Consultation> consultationIterator = consultations.iterator();
        while (consultationIterator.hasNext()) {
            Consultation c = consultationIterator.next();
            if (c.getPatientId().equalsIgnoreCase(patientId) &&
                c.getConsultationDate().toLocalDate().equals(selectedDate)) {
                consultationCount++;
            }
        }

        if (treatments != null) {
            ClinicADT.MyIterator<MedicalTreatment> treatmentIterator = treatments.iterator();
            while (treatmentIterator.hasNext()) {
                MedicalTreatment t = treatmentIterator.next();
                if (t.getPatientId().equalsIgnoreCase(patientId) &&
                    t.getTreatmentDateTime().toLocalDate().equals(selectedDate)) {
                    treatmentCount++;
                }
            }
        }
        
        return (consultationCount > 0 || treatmentCount > 0);
    }
    
    public void loadConsultationsFromFile() {
        consultations.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(consultationFilePath))) {
            String line;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 6) {
                    System.out.println("Invalid line in consultations file: " + line);
                    continue;
                }

                try {
                    int consultationId = Integer.parseInt(parts[0].trim());
                    String patientId = parts[1].trim();
                    String patientName = parts[2].trim();
                    String doctorName = parts[3].trim();
                    String doctorId = parts[4].trim();
                    LocalDateTime consultationDate = LocalDateTime.parse(parts[5].trim(), formatter);
                    String diagnosis = parts[6].trim();

                    Consultation consultation = new Consultation(
                                consultationId, patientId, patientName, doctorName, doctorId, consultationDate, diagnosis);
                    consultations.add(consultation);
                } catch (Exception e) {
                    System.out.println("Error parsing consultation line: " + line);
                    e.printStackTrace();
                }
            }
            // rebuild queue AFTER the whole file is read
            rebuildWaitingQueueFromPending();
        } catch (IOException e) {
            System.out.println("Error loading consultations: " + e.getMessage());
        }
    }
    
    private void rebuildWaitingQueueFromPending() {
        waitingQueue.clear();
        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) {
            Consultation c = it.next();
            if (isPending(c)) {
                waitingQueue.addLast(c.getId());
            }
        }
    }
    
    // Validation methods
    private boolean hasDoctorReachedDailyLimit(String doctorId, LocalDate date) {
        int count = 0;
        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            if (c.getDoctorId().equalsIgnoreCase(doctorId) && 
                c.getConsultationDate().toLocalDate().equals(date)) {
                count++;
                if (count >= MAX_DAILY_CONSULTATIONS_PER_DOCTOR) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean hasPatientExceededFrequency(String patientId) {
        LocalDate oneWeekAgo = LocalDate.now().minusDays(7);
        int count = 0;
        
        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            if (c.getPatientId().equalsIgnoreCase(patientId) && 
                !c.getConsultationDate().toLocalDate().isBefore(oneWeekAgo)) {
                count++;
                if (count >= MAX_WEEKLY_CONSULTATIONS_PER_PATIENT) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasMinimumGap(Doctor doctor, LocalDateTime newDateTime) {
        LocalDateTime bufferStart = newDateTime.minusMinutes(MIN_MINUTES_BETWEEN_CONSULTATIONS);
        LocalDateTime bufferEnd = newDateTime.plusHours(CONSULTATION_DURATION)
                                                .plusMinutes(MIN_MINUTES_BETWEEN_CONSULTATIONS);

        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            if (c.getDoctorId().equalsIgnoreCase(doctor.getId())) {
                LocalDateTime existingStart = c.getConsultationDate();
                LocalDateTime existingEnd = existingStart.plusHours(CONSULTATION_DURATION);

                if ((existingStart.isAfter(bufferStart) && existingStart.isBefore(bufferEnd)) ||
                    (existingEnd.isAfter(bufferStart) && existingEnd.isBefore(bufferEnd)) ||
                    (existingStart.isBefore(bufferStart) && existingEnd.isAfter(bufferEnd))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Show the next patient in the FIFO queue without removing them. (Now shows Status) */
    public void viewNextPatientInQueue() {
        Integer next = waitingQueue.peekFirst(); // just peek
        if (next == null) {
            System.out.println("No pending consultations in the queue.");
            return;
        }

        Consultation c = findById(next);
        if (c == null) {
            System.out.println("Warning: queued consultation not found (ID " + next + ").");
            return;
        }

        final String line      = "+--------------+------------+----------------------+----------------------+---------------------+------------+";
        final String headerFmt = "| %-12s | %-10s | %-20s | %-20s | %-19s | %-10s |%n";
        final String rowFmt    = "| %12d | %-10s | %-20s | %-20s | %-19s | %-10s |%n";
        DateTimeFormatter fmt  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String dt              = (c.getConsultationDate() != null) ? c.getConsultationDate().format(fmt) : "N/A";

        System.out.println("\nNext patient in queue (peek):");
        System.out.println(line);
        System.out.printf(headerFmt, "Consult ID", "PatientID", "Patient Name", "Doctor", "Date & Time", "Status");
        System.out.println(line);
        System.out.printf(rowFmt, c.getId(), c.getPatientId(), truncate(c.getPatientName(),20),
                          truncate(c.getDoctorName(),20), dt, statusOf(c));
        System.out.println(line);
    }
    
    /** Display all queued pending consultations in FIFO order (head first). (Now shows Status) */
    public void displayQueuedPatients() {
        if (waitingQueue.isEmpty()) {
            System.out.println("No pending consultations in the queue.");
            return;
        }

        final String line      = "+-----+--------------+------------+----------------------+----------------------+---------------------+------------+";
        final String headerFmt = "| Pos | %-12s | %-10s | %-20s | %-20s | %-19s | %-10s |%n";
        final String rowFmt    = "| %3d | %12d | %-10s | %-20s | %-20s | %-19s | %-10s |%n";
        DateTimeFormatter fmt  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        System.out.println("\nQueued patients (FIFO):");
        System.out.println(line);
        System.out.printf(headerFmt, "Consult ID", "PatientID", "Patient Name", "Doctor", "Date & Time", "Status");
        System.out.println(line);

        int pos = 1;
        for (Integer id : waitingQueue) {
            Consultation c = findById(id);
            if (c == null) {
                System.out.printf("| %3d | %12s | %-10s | %-20s | %-20s | %-19s | %-10s |%n",
                        pos++, "N/A", "N/A", "Unknown", "Unknown", "N/A", "PENDING");
                continue;
            }
            String dt = (c.getConsultationDate() != null) ? c.getConsultationDate().format(fmt) : "N/A";
            System.out.printf(rowFmt, pos++, c.getId(), c.getPatientId(),
                              truncate(c.getPatientName(),20), truncate(c.getDoctorName(),20), dt, statusOf(c));
        }
        System.out.println(line);
    }
    
    private boolean isMarkedProcessed(int id) {
        String key = "|" + id + "|";
        return processedIdx.indexOf(key) >= 0;
    }

    private void markProcessed(int id) {
        if (!isMarkedProcessed(id)) {
            processedIdx += id + "|";
        }
    }
    
    // Add this method to ConsultationControl.java

    /**
     * Marks a consultation as processed (used for treatment creation)
     * This prevents it from showing up in treatment booking lists
     */
    public void markConsultationAsProcessed(int consultationId) {
        markProcessed(consultationId);
        // Save the updated state to file
        saveConsultationToFile(null, false);
    }

    /**
     * Remove a consultation by ID (used when treatment is created from it)
     */
    public boolean removeProcessedConsultation(String patientId, String doctorId, LocalDateTime consultationDate) {
        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            if (c.getPatientId().equalsIgnoreCase(patientId) && 
                c.getDoctorId().equalsIgnoreCase(doctorId) &&
                c.getConsultationDate().equals(consultationDate)) {

                // Remove from main list
                consultations.remove(i);

                // Remove from queue if present
                waitingQueue.remove(c.getId());

                // Clear current called if it's this consultation
                if (currentCalledConsultationId != null && currentCalledConsultationId == c.getId()) {
                    currentCalledConsultationId = null;
                }

                // Save changes to file
                saveConsultationToFile(null, false);

                System.out.println("Processed consultation removed: Patient " + c.getPatientName());
                return true;
            }
        }
        return false;
    }

    /**
     * Get only unprocessed consultations for a patient (for treatment booking)
     */
    public ClinicADT<Consultation> getUnprocessedConsultationsForPatient(String patientId) {
        ClinicADT<Consultation> result = new MyClinicADT<>();

        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            if (c.getPatientId().equalsIgnoreCase(patientId)) {
                // Only include if it has a valid diagnosis but hasn't been used for treatment yet
                if (hasValidDiagnosis(c) && !isMarkedProcessed(c.getId())) {
                    result.add(c);
                }
            }
        }
        return result;
    }

    /**
     * Check if a consultation has a valid diagnosis
     */
    private boolean hasValidDiagnosis(Consultation c) {
        String diagnosis = c.getDiagnosis();
        return diagnosis != null && 
               !diagnosis.trim().isEmpty() &&
               !diagnosis.equalsIgnoreCase("To be diagnosed during appointment") &&
               !diagnosis.equalsIgnoreCase("Pending");
    }
    
    //List the patients who had finished the consultatio diagnosis
    public void listConsultations(boolean onlyProcessed) {
        if (consultations.isEmpty()) {
            System.out.println("No consultations scheduled.");
            return;
        }

        final String line =
            "+------+--------------+----------------------+----------------------+----------------------+-----------+-----------+";
        final String header =
            "| ID   | Patient ID    | Patient Name           | Doctor               | Date & Time          | Duration  | Status    |";
        final java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        System.out.println("\n=== All Consultations ===");
        System.out.println(line);
        System.out.println(header);
        System.out.println(line);

        boolean printedAny = false;

        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) {
            Consultation c = it.next();

            // A consultation is processed if either:
            //  - your existing status logic says so (!isPending(c)), OR
            //  - we've marked it processed after creating a treatment.
            boolean processed = (!isPending(c)) || isMarkedProcessed(c.getId());

            if (onlyProcessed && !processed) continue;     // show only processed when requested
            if (!onlyProcessed && processed) continue;     // hide processed in the default "All" listing

            String dateStr = (c.getConsultationDate() != null) ? c.getConsultationDate().format(fmt) : "N/A";
            String status  = processed ? "PROCESSED" : "PENDING";

            System.out.printf("| %-4d | %-12s | %-20s | %-20s | %-20s | %-9s | %-9s |%n",
                    c.getId(),
                    c.getPatientId(),
                    truncate(c.getPatientName(), 20),
                    truncate(c.getDoctorName(), 20),
                    dateStr,
                    "1 hr",
                    status);

            printedAny = true;
        }

        if (!printedAny) {
            System.out.println("| No consultations match the filter.                                                                    |");
        }

        System.out.println(line);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
