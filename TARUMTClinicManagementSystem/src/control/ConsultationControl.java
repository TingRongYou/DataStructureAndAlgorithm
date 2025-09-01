package control;

import adt.ClinicADT;
import adt.MyClinicADT;
import entity.Appointment;
import entity.Consultation;
import entity.Doctor;
import entity.MedicalTreatment;
import entity.Patient;
import tarumtclinicmanagementsystem.DutySchedule;
import tarumtclinicmanagementsystem.Session;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import static entity.Appointment.AppointmentStatus.*;
import utility.Report;

public class ConsultationControl {
    private ClinicADT<Consultation> consultations;
    private PatientControl patientControl;
    private DoctorControl doctorControl;
    private ClinicADT<MedicalTreatment> treatments;
    private final Scanner sc;

    // Optional: to mirror Appointment status on lists (may be null)
    private final AppointmentControl appointmentControl;

    // ---- File path (OS-safe) ----
    private static final Path CONSULTATIONS_PATH =
            Paths.get("src", "textFile", "consultations.txt");

    // === Waiting queue of pending consultation IDs (ordered by nearest to now) ===
    private final MyClinicADT<Integer> waitingQueue = new MyClinicADT<>();

    // === The only consultation currently allowed to be processed ===
    private Integer currentCalledConsultationId = null;

    // validations
    private static final int MAX_DAILY_CONSULTATIONS_PER_DOCTOR = 8;
    private static final int MAX_WEEKLY_CONSULTATIONS_PER_PATIENT = 3;
    private static final int MIN_MINUTES_BETWEEN_CONSULTATIONS = 15;

    // Working hours
    private static final LocalTime MORNING_START = LocalTime.of(8, 0);
    private static final LocalTime MORNING_END   = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_START   = LocalTime.NOON;                // 12:00
    private static final LocalTime LUNCH_END     = LocalTime.of(13, 0);
    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
    private static final LocalTime AFTERNOON_END   = LocalTime.of(17, 0);
    private static final LocalTime EVENING_GAP_START = LocalTime.of(17, 0);
    private static final LocalTime EVENING_GAP_END   = LocalTime.of(18, 0);
    private static final LocalTime NIGHT_START    = LocalTime.of(18, 0);
    private static final LocalTime NIGHT_END      = LocalTime.of(22, 0);

    // Consultation duration in minutes (fixed)
    private static final int CONSULTATION_DURATION_MIN = 60;

    // Holds processed consultation IDs like "|1|7|23|"
    private String processedIdx = "|";

    // ========================= Constructors =========================
    public ConsultationControl(PatientControl patientControl,
                               DoctorControl doctorControl,
                               ClinicADT<Consultation> consultations,
                               ClinicADT<MedicalTreatment> treatments) {
        this(patientControl, doctorControl, consultations, treatments, null);
    }
    public ConsultationControl(PatientControl patientControl, DoctorControl doctorControl) {
        this(patientControl, doctorControl, new MyClinicADT<>(), new MyClinicADT<>(), null);
    }
    /** Preferred: pass AppointmentControl so statuses mirror correctly in list screens. */
    public ConsultationControl(PatientControl patientControl,
                               DoctorControl doctorControl,
                               ClinicADT<Consultation> consultations,
                               ClinicADT<MedicalTreatment> treatments,
                               AppointmentControl appointmentControl) {
        this.patientControl = patientControl;
        this.doctorControl = doctorControl;
        this.consultations = consultations;
        this.treatments = treatments;
        this.appointmentControl = appointmentControl;  // may be null
        this.sc = new Scanner(System.in);
        loadConsultationsFromFile(); // quiet on first run; auto-creates file
    }

    // =========================
    // === Queue/Lock Access ===
    // =========================
    public Integer getCurrentCalledConsultationId() { return currentCalledConsultationId; }

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

    /** Call the next consultation based on the ordered queue (does NOT delete patient). */
    public boolean callNextFromQueue() {
        if (currentCalledConsultationId != null) {
            System.out.println("A patient is already called (Consultation ID: "
                    + currentCalledConsultationId + "). Finish processing first.");
            return false;
        }
        rebuildWaitingQueueFromPending();

        Integer next = null;
        try { next = waitingQueue.dequeue(); } catch (Exception e) { next = null; }

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
        setStatusAndPersist(c, Consultation.Status.CONSULTING);
        // Make sure listConsultations shows it as CONSULTING immediately
        c.setStatus(Consultation.Status.CONSULTING);
        saveConsultationToFile(null, false);          // full rewrite with status

        System.out.println(">> Called: Patient " + c.getPatientName() + " (" + c.getPatientId()
                + "), Consultation ID: " + c.getId());
        return true;
    }

    // ===============================================================
    // === APPOINTMENT HANDOFF: lock & display the "CONSULTING" case ==
    // ===============================================================
    /** Lock nearest pending consultation for the patient and show a focused row. */
    public boolean showConsultingFromAppointment(String patientId) {
        if (patientId == null || patientId.isBlank()) {
            System.out.println("Invalid patient ID.");
            return false;
        }
        if (!setExternalCalledPatientId(patientId)) {
            System.out.println("No pending consultation found (or another patient is being processed).");
            return false;
        }
        Consultation c = findById(currentCalledConsultationId);
        if (c == null) {
            System.out.println("Internal error: locked consultation not found.");
            currentCalledConsultationId = null;
            return false;
        }

        setStatusAndPersist(c, Consultation.Status.CONSULTING);
        // Make sure listConsultations shows it as CONSULTING immediately
        c.setStatus(Consultation.Status.CONSULTING);

        saveConsultationToFile(null, false);          // full rewrite with status
        rebuildWaitingQueueFromPending();

        final String line      = "+--------------+------------+----------------------+----------------------+---------------------+------------+";
        final String headerFmt = "| %-12s | %-10s | %-20s | %-20s | %-19s | %-10s |%n";
        final String rowFmt    = "| %12d | %-10s | %-20s | %-20s | %-19s | %-10s |%n";
        DateTimeFormatter fmt  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String dt              = (c.getConsultationDate() != null) ? c.getConsultationDate().format(fmt) : "N/A";

        System.out.println("\nCurrently consulting (handover from Appointment):");
        System.out.println(line);
        System.out.printf(headerFmt, "Consult ID", "PatientID", "Patient Name", "Doctor", "Date & Time", "Status");
        System.out.println(line);
        System.out.printf(rowFmt,
                c.getId(), c.getPatientId(),
                truncate(c.getPatientName(), 20),
                truncate(c.getDoctorName(), 20),
                dt, "CONSULTING");
        System.out.println(line);

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

        if (hasPatientExceededFrequency(selectedPatient.getId())) {
            System.out.println("Patient has exceeded the weekly consultation limit (max 3 per week).");
            return;
        }

        if (isPatientAlreadyBooked(selectedPatient.getId(), dateTime.toLocalDate(), true)) {
            System.out.println("Patient already has a consultation booked on that date.");
            return;
        }

        Doctor selectedDoctor = selectAvailableDoctor(dateTime);
        if (selectedDoctor == null) return;

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
        if (selectedPatient == null) System.out.println("Invalid Patient ID.");
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
        System.out.println("Morning : 08:00-12:00");
        System.out.println("Lunch   : 12:00-13:00 (no consultations)");
        System.out.println("Afternoon: 13:00-17:00");
        System.out.println("Gap     : 17:00-18:00 (no consultations)");
        System.out.println("Night   : 18:00-22:00");
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
        if (isInTimeRange(time, MORNING_START, MORNING_END)) return Session.MORNING;
        if (isInTimeRange(time, AFTERNOON_START, AFTERNOON_END)) return Session.AFTERNOON;
        if (isInTimeRange(time, NIGHT_START, NIGHT_END)) return Session.NIGHT;
        return null;
    }
    private ClinicADT<Doctor> getAvailableDoctors(LocalDateTime dateTime) {
        ClinicADT<Doctor> availableDoctors = new MyClinicADT<>();
        Session requiredSession = getSessionForTime(dateTime.toLocalTime());
        if (requiredSession == null) return availableDoctors;

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
                    doc.getId(), doc.getName(), doc.getRoomNumber(),
                    doc.getGender(), doc.getPhoneNumber());
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
                    p.getId(), p.getName(), p.getAge(), p.getGender(), p.getContact());
        }
        System.out.println(line);
    }

    private boolean isDoctorBooked(String doctorName, LocalDateTime dateTime) {
        LocalDateTime consultationEnd = dateTime.plusMinutes(CONSULTATION_DURATION_MIN);
        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation consultation = iterator.next();
            if (consultation.getDoctorName().equalsIgnoreCase(doctorName)) {
                LocalDateTime existingStart = consultation.getConsultationDate();
                LocalDateTime existingEnd = existingStart.plusMinutes(CONSULTATION_DURATION_MIN);
                if (hasTimeOverlap(dateTime, consultationEnd, existingStart, existingEnd)) return true;
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
        consultation.setStatus(Consultation.Status.PENDING);
        consultations.add(consultation);
        saveConsultationToFile(consultation, true);
        displayConsultationConfirmation(consultation);
        rebuildWaitingQueueFromPending();
    }
    private void displayConsultationConfirmation(Consultation consultation) {
        System.out.println("\nConsultation added successfully!");
        System.out.println(consultation);
        System.out.println("Duration: 60 minutes (ends at "
                + consultation.getConsultationDate().plusMinutes(CONSULTATION_DURATION_MIN)
                .format(DateTimeFormatter.ofPattern("HH:mm")) + ")");
        System.out.println("Status  : " + statusOfForDisplay(consultation));
    }

    // =================================================
    // === PROCESS: ONLY the currently CALLED allowed ==
    // =================================================
    public void updateConsultation(String patientId) {
        if (currentCalledConsultationId == null) {
            System.out.println("No patient has been called. Use 'Call Next Patient' first or handoff from Appointment.");
            return;
        }
        String calledPid = getCurrentCalledPatientId();
        if (calledPid == null || !calledPid.equalsIgnoreCase(patientId.trim())) {
            System.out.println("Only the CALLED/CONSULTING patient can be processed. Called patient: "
                    + (calledPid == null ? "None" : calledPid));
            return;
        }

        Consultation consultationToProcess = findById(currentCalledConsultationId);
        if (consultationToProcess == null) {
            System.out.println("Internal error: called consultation not found.");
            currentCalledConsultationId = null;
            return;
        }
        if (!isPending(consultationToProcess)) {
            System.out.println("Called consultation is already processed.");
            currentCalledConsultationId = null;
            return;
        }

        String diagnosis = chooseDiagnosisInteractive();
        if (diagnosis == null) {
            System.out.println("Operation cancelled.");
            return;
        }

        consultationToProcess.setDiagnosis(diagnosis);
        System.out.println("Diagnosis updated successfully!");

        setStatusAndPersist(consultationToProcess, Consultation.Status.PROCESSED);
        mirrorAppointmentStatus(consultationToProcess, Appointment.AppointmentStatus.PENDING_PAYMENT);
        rebuildWaitingQueueFromPending();

        // ask treatment
        while (true) {
            System.out.print("Treatment required by Dr. " + consultationToProcess.getDoctorName() + "? (Y/N): ");
            String ans = sc.nextLine().trim();
            if (ans.equalsIgnoreCase("Y")) {
                createTreatmentImmediately(consultationToProcess);
                break;
            } else if (ans.equalsIgnoreCase("N")) {
                break;
            } else {
                System.out.println("Please enter Y or N.");
            }
        }

        // Optional: minutes actually used → compression
        int used = CONSULTATION_DURATION_MIN;
        while (true) {
            System.out.print("Enter minutes actually used (1-60) [or blank=60]: ");
            String in = sc.nextLine().trim();
            if (in.isEmpty()) { used = 60; break; }
            try {
                int v = Integer.parseInt(in);
                if (v >= 1 && v <= 60) { used = v; break; }
                System.out.println("Please enter a number 1..60.");
            } catch (Exception e) {
                System.out.println("Invalid number.");
            }
        }
        if (used < CONSULTATION_DURATION_MIN) {
            compressTailForDoctorDay(consultationToProcess, used);
        }

        currentCalledConsultationId = null;
        System.out.println("Consultation completed for " + consultationToProcess.getPatientName() +
                " (ID: " + consultationToProcess.getId() + ") - Status: PROCESSED");

        Consultation verification = findById(consultationToProcess.getId());
        if (verification != null) {
            System.out.println("Final verification - Consultation status: " + verification.getStatus());
        }
    }

    /**
     * Simplified processing: only symptoms + needTreatment?
     * Writes an auto diagnosis so lists show PROCESSED; persists and unlocks.
     */
    public boolean processCalledWithSymptomsOnly(String symptoms, boolean treatmentNeeded) {
        if (currentCalledConsultationId == null) {
            System.out.println("No patient is currently called.");
            return false;
        }
        Consultation c = findById(currentCalledConsultationId);
        if (c == null) {
            currentCalledConsultationId = null;
            return false;
        }

        String autoDx = treatmentNeeded
                ? "Needs treatment"
                : "No treatment";
        c.setDiagnosis(autoDx);

        markProcessed(c.getId());
        setStatusAndPersist(c, Consultation.Status.PROCESSED);
        rebuildWaitingQueueFromPending();

        currentCalledConsultationId = null;
        System.out.println("Consultation processed: " + c.getPatientName() + " - Status: PROCESSED");
        return true;
    }

    private void createTreatmentImmediately(Consultation c) {
        System.out.println("\n=== Schedule Treatment for " + c.getPatientName() + " ===");
        System.out.println("Doctor: " + c.getDoctorName() + " (" + c.getDoctorId() + ")");
        System.out.println("Diagnosis: " + c.getDiagnosis());
        System.out.println("Treatment Duration: 2 hours");
        System.out.println();
    }

    /** A consultation is pending if status is PENDING. */
    private boolean isPending(Consultation c) {
        return c != null && c.getStatus() == Consultation.Status.PENDING;
    }

    // ===== Status mapping (Appointment-driven when available) =====
    private Appointment findApptForConsultation(Consultation c) {
        if (appointmentControl == null || c == null) return null;
        ClinicADT<Appointment> all = appointmentControl.getAll();
        ClinicADT.MyIterator<Appointment> it = all.iterator();
        while (it.hasNext()) {
            Appointment a = it.next();
            if (a.getPatientId().equalsIgnoreCase(c.getPatientId())
                    && a.getDoctorId().equalsIgnoreCase(c.getDoctorId())
                    && a.getScheduledDateTime() != null
                    && a.getScheduledDateTime().equals(c.getConsultationDate())) {
                return a;
            }
        }
        return null;
    }

    /** Status string for display (Appointment-driven where possible). */
    private String statusOfForDisplay(Consultation c) {
        if (c.getStatus() == Consultation.Status.CONSULTING) return "CONSULTING";
        if (c.getStatus() == Consultation.Status.PROCESSED)  return "PROCESSED";
        if (c.getStatus() == Consultation.Status.COMPLETED)  return "COMPLETED";

        Appointment a = findApptForConsultation(c);
        if (a != null) {
            switch (a.getStatus()) {
                case BOOKED:          return "BOOKED";
                case CHECKED_IN:      return "QUEUED";
                case CONSULTING:      return "CONSULTING";
                case TREATMENT:       return "TREATMENT";
                case PENDING_PAYMENT: return "PAYMENT";
                case COMPLETED:       return "COMPLETED";
            }
        }
        return "PENDING";
    }

    /** Menu + manual entry; accepts direct typed diagnosis text. Returns null if user cancels. */
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
                if (!input.isEmpty()) return input;
                System.out.println("Please enter a valid option or diagnosis text.");
            }
        }
    }

    // ========================== Persistence ==========================
    private static void ensureFileExists(Path p) {
        try {
            if (p.getParent() != null) Files.createDirectories(p.getParent());
            if (!Files.exists(p)) Files.createFile(p);
        } catch (IOException e) {
            System.err.println("Unable to create consultations file: " + e.getMessage());
        }
    }

    /** Centralized, consistent CSV value sanitization. */
    private static String cleanCSV(String value, String def) {
        if (value == null || value.trim().isEmpty()) return def;
        return value.replace(',', ' ').replace('\n', ' ').replace('\r', ' ').trim();
    }

    /** Write ONE row (append) or the WHOLE file (truncate+rewrite). */
    private synchronized void saveConsultationToFile(Consultation consultation, boolean appendMode) {
        ensureFileExists(CONSULTATIONS_PATH);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        try {
            if (!appendMode) {
                try (BufferedWriter bw = Files.newBufferedWriter(
                        CONSULTATIONS_PATH,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {

                    ClinicADT.MyIterator<Consultation> it = consultations.iterator();
                    while (it.hasNext()) {
                        Consultation c = it.next();

                        String dateStr = (c.getConsultationDate() == null)
                                ? ""
                                : c.getConsultationDate().format(formatter);

                        String line = String.format(
                                "%d,%s,%s,%s,%s,%s,%s,%s%n",
                                c.getId(),
                                cleanCSV(c.getPatientId(), "N/A"),
                                cleanCSV(c.getPatientName(), "N/A"),
                                cleanCSV(c.getDoctorName(), "N/A"),
                                cleanCSV(c.getDoctorId(), "N/A"),
                                dateStr,
                                cleanCSV(c.getDiagnosis(), "To be diagnosed during appointment"),
                                (c.getStatus() == null ? Consultation.Status.PENDING : c.getStatus()).name()
                        );
                        bw.write(line);
                    }
                    bw.flush();
                }
            } else {
                try (BufferedWriter bw = Files.newBufferedWriter(
                        CONSULTATIONS_PATH,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)) {

                    String dateStr = (consultation.getConsultationDate() == null)
                            ? ""
                            : consultation.getConsultationDate().format(formatter);

                    String line = String.format(
                            "%d,%s,%s,%s,%s,%s,%s,%s%n",
                            consultation.getId(),
                            cleanCSV(consultation.getPatientId(), "N/A"),
                            cleanCSV(consultation.getPatientName(), "N/A"),
                            cleanCSV(consultation.getDoctorName(), "N/A"),
                            cleanCSV(consultation.getDoctorId(), "N/A"),
                            dateStr,
                            cleanCSV(consultation.getDiagnosis(), "To be diagnosed during appointment"),
                            (consultation.getStatus() == null ? Consultation.Status.PENDING : consultation.getStatus()).name()
                    );
                    bw.write(line);
                    bw.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("Error saving consultation: " + e.getMessage());
        }
    }

    public void loadConsultationsFromFile() {
        consultations.clear();
        ensureFileExists(CONSULTATIONS_PATH);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        try (BufferedReader br = Files.newBufferedReader(CONSULTATIONS_PATH)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length < 7) continue;

                try {
                    int consultationId = Integer.parseInt(parts[0].trim());
                    String patientId   = parts[1].trim();
                    String patientName = parts[2].trim();
                    String doctorName  = parts[3].trim();
                    String doctorId    = parts[4].trim();
                    LocalDateTime consultationDate = parts[5].trim().isEmpty()
                            ? null
                            : LocalDateTime.parse(parts[5].trim(), formatter);
                    String diagnosis   = parts[6].trim();

                    Consultation c = new Consultation(
                            consultationId, patientId, patientName, doctorName, doctorId, consultationDate, diagnosis);

                    if (parts.length >= 8) {
                        String st = parts[7].trim();
                        try {
                            c.setStatus(Consultation.Status.valueOf(st));
                        } catch (Exception ignored) { c.setStatus(Consultation.Status.PENDING); }
                    } else {
                        c.setStatus(Consultation.Status.PENDING);
                    }

                    consultations.add(c);

                    if (c.getStatus() == Consultation.Status.CONSULTING && currentCalledConsultationId == null) {
                        currentCalledConsultationId = c.getId();
                    }
                } catch (Exception ignored) {}
            }
        } catch (IOException e) {
            System.out.println("Error loading consultations: " + e.getMessage());
        }

        rebuildWaitingQueueFromPending();
    }

    public boolean removeConsultationById(int id) {
        int idx = 0;
        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) {
            Consultation c = it.next();
            if (c.getId() == id) {
                consultations.remove(idx);
                try { waitingQueue.remove(id); } catch (Exception ignored) {}
                if (currentCalledConsultationId != null && currentCalledConsultationId == id) {
                    currentCalledConsultationId = null;
                }
                saveConsultationToFile(null, false);
                rebuildWaitingQueueFromPending();
                System.out.println("Consultation removed: " + c);
                return true;
            }
            idx++;
        }
        System.out.println("Consultation ID not found.");
        return false;
    }

    public void listConsultations() {
        if (consultations.isEmpty()) {
            System.out.println("No consultations scheduled.");
            return;
        }
        Report.cprintln("====== All Consultations ======");
        displayConsultationTable();
    }

    // ========= PAGINATED master table (10 per page) =========
    private void displayConsultationTable() {
        final String LINE =
            "+------+--------------+----------------------+----------------------+----------------------+-----------+------------+";
        final String HEADER_FMT =
            "| %-4s | %-12s | %-20s | %-20s | %-20s | %-9s | %-10s |\n";
        final String ROW_FMT =
            "| %4d | %-12s | %-20s | %-20s | %-20s | %-9s | %-10s |\n";

        // Build an ADT snapshot and sort (null dates last) — same order as before
        MyClinicADT<Consultation> snap = new MyClinicADT<>();
        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) snap.add(it.next());

        snap.sort((a, b) -> {
            LocalDateTime ta = a.getConsultationDate();
            LocalDateTime tb = b.getConsultationDate();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;   // null (N/A) last
            if (tb == null) return -1;
            return ta.compareTo(tb);
        });

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        final int PAGE = 10;
        int printed = 0;

        System.out.println(LINE);
        System.out.printf(HEADER_FMT, "ID", "Patient ID", "Patient Name", "Doctor", "Date & Time", "Duration", "Status");
        System.out.println(LINE);

        ClinicADT.MyIterator<Consultation> it2 = snap.iterator();
        while (it2.hasNext()) {
            Consultation c = it2.next();
            String pid   = safe(c.getPatientId(), 12);
            String pname = safe(c.getPatientName(), 20);
            String dname = safe(c.getDoctorName(), 20);
            String dt    = (c.getConsultationDate() == null) ? "N/A" : c.getConsultationDate().format(fmt);
            String dur   = "60 min";
            String stat  = safe(statusOfForDisplay(c), 10);

            System.out.printf(ROW_FMT, c.getId(), pid, pname, dname, dt, dur, stat);
            printed++;

            if (printed % PAGE == 0 && it2.hasNext()) {
                System.out.println(LINE);
                if (!promptContinuePage(printed, estimateSize(snap))) break;
                System.out.println(LINE);
                System.out.printf(HEADER_FMT, "ID", "Patient ID", "Patient Name", "Doctor", "Date & Time", "Duration", "Status");
                System.out.println(LINE);
            }
        }
        System.out.println(LINE);
    }

    private boolean promptContinuePage(int shown, int total) {
        System.out.print("Shown " + shown + "/" + total + ". Press ENTER to show next 10 (or type q to quit): ");
        String in = sc.nextLine();
        return !(in != null && in.trim().equalsIgnoreCase("q"));
    }

    private String safe(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return (max <= 1) ? s.substring(0, max) : s.substring(0, max - 1) + "…";
    }

    // =====================  DYNAMIC SCHEDULE COMPRESSION  =====================
    private void compressTailForDoctorDay(Consultation justFinished, int minutesUsed) {
        if (justFinished == null) return;
        if (minutesUsed >= CONSULTATION_DURATION_MIN) return;

        final String doctorId = justFinished.getDoctorId();
        final LocalDate theDay = justFinished.getConsultationDate().toLocalDate();

        // Collect doctor/day consultations (>= current start)
        MyClinicADT<Consultation> sameDoctorDay = new MyClinicADT<>();
        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) {
            Consultation c = it.next();
            if (c.getDoctorId().equalsIgnoreCase(doctorId)
                    && c.getConsultationDate() != null
                    && c.getConsultationDate().toLocalDate().equals(theDay)
                    && !c.getConsultationDate().isBefore(justFinished.getConsultationDate())) {
                sameDoctorDay.add(c);
            }
        }
        if (sameDoctorDay.isEmpty()) return;

        // Sort by consultationDate ascending (bubble logic replaced by ADT.sort with same ordering)
        sameDoctorDay.sort((a, b) -> a.getConsultationDate().compareTo(b.getConsultationDate()));

        // find current index
        int idxCurrent = -1, idx = 0;
        ClinicADT.MyIterator<Consultation> itIdx = sameDoctorDay.iterator();
        while (itIdx.hasNext()) {
            if (itIdx.next().getId() == justFinished.getId()) { idxCurrent = idx; break; }
            idx++;
        }
        if (idxCurrent == -1) return;

        LocalDateTime currentStart = justFinished.getConsultationDate();
        LocalDateTime actualEnd = alignToAllowedWindows(currentStart.plusMinutes(minutesUsed));
        LocalDateTime nextStart = actualEnd;

        // Walk forward and pull up while respecting windows — unchanged logic
        for (int i = idxCurrent + 1; i < estimateSize(sameDoctorDay); i++) {
            Consultation c = sameDoctorDay.get(i);
            LocalDateTime originalStart = c.getConsultationDate();
            if (!nextStart.isBefore(originalStart)) nextStart = originalStart; // do not push later
            nextStart = alignToAllowedWindows(nextStart);
            if (!nextStart.toLocalTime().isBefore(NIGHT_END)) break;

            if (nextStart.isBefore(originalStart)) c.setConsultationDate(nextStart);

            nextStart = c.getConsultationDate().plusMinutes(CONSULTATION_DURATION_MIN);
            nextStart = alignToAllowedWindows(nextStart);
            if (!nextStart.toLocalTime().isBefore(NIGHT_END)) break;
        }

        saveConsultationToFile(null, false);
        rebuildWaitingQueueFromPending();

        System.out.println("\nSchedule compressed for Dr. " + justFinished.getDoctorName() + " on " + theDay + ".");
        System.out.println("Subsequent consultations were pulled forward where possible.");
    }
    private LocalDateTime alignToAllowedWindows(LocalDateTime t) {
        LocalTime time = t.toLocalTime();
        if (!time.isBefore(LUNCH_START) && time.isBefore(LUNCH_END)) {
            return LocalDateTime.of(t.toLocalDate(), LUNCH_END);
        }
        if (!time.isBefore(EVENING_GAP_START) && time.isBefore(EVENING_GAP_END)) {
            return LocalDateTime.of(t.toLocalDate(), EVENING_GAP_END);
        }
        if (time.isBefore(MORNING_START)) {
            return LocalDateTime.of(t.toLocalDate(), MORNING_START);
        }
        return t;
    }

    // ========================  Existing Utilities  ========================
    public void searchByPatient(String patientName) {
    // 1) Copy all consultations into a working ADT
    ClinicADT<Consultation> sorted = new MyClinicADT<>();
    ClinicADT.MyIterator<Consultation> it = consultations.iterator();
    while (it.hasNext()) {
        sorted.add(it.next());
    }
    
    // 2) Sort by patient name (case-insensitive) using ADT's sort
    ClinicADT.MyComparator<Consultation> comparator = new ClinicADT.MyComparator<Consultation>() {
        @Override 
        public int compare(Consultation a, Consultation b) {
            return a.getPatientName().compareToIgnoreCase(b.getPatientName());
        }
    };
    sorted.sort(comparator);
    
    // 3) Use ADT's search method to find any matching consultation
    // Create a dummy consultation for searching
    Consultation searchKey = new Consultation(null, patientName, null, null, null, null);
    int foundIndex = ((MyClinicADT<Consultation>) sorted).search(searchKey, comparator);
    
    ClinicADT<Consultation> found = new MyClinicADT<>();
    
    if (foundIndex >= 0) {
        // 4) Expand left to find the first matching consultation
        int first = foundIndex;
        while (first > 0 && sorted.get(first - 1).getPatientName().equalsIgnoreCase(patientName)) {
            first--;
        }
        
        // 5) Expand right to find the last matching consultation
        int last = foundIndex;
        while (last < sorted.size() - 1 && sorted.get(last + 1).getPatientName().equalsIgnoreCase(patientName)) {
            last++;
        }
        
        // 6) Collect exact matches (for partial matching)
        for (int i = first; i <= last; i++) {
            Consultation c = sorted.get(i);
            if (c.getPatientName().toLowerCase().contains(patientName.toLowerCase())) {
                found.add(c);
            }
        }
    } else {
        // 7) If no exact match found, do partial search in sorted array
        ClinicADT.MyIterator<Consultation> iterator = sorted.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            if (c.getPatientName().toLowerCase().contains(patientName.toLowerCase())) {
                found.add(c);
            }
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
    
    // 1) Copy all consultations into a working ADT
    ClinicADT<Consultation> sorted = new MyClinicADT<>();
    ClinicADT.MyIterator<Consultation> it = consultations.iterator();
    while (it.hasNext()) {
        sorted.add(it.next());
    }
    
    // 2) Sort by doctor ID (case-insensitive) using ADT's sort
    ClinicADT.MyComparator<Consultation> comparator = new ClinicADT.MyComparator<Consultation>() {
        @Override 
        public int compare(Consultation a, Consultation b) {
            return a.getDoctorId().compareToIgnoreCase(b.getDoctorId());
        }
    };
    sorted.sort(comparator);
    
    // 3) Use ADT's search method to find any matching consultation
    // Create a dummy consultation for searching
    Consultation searchKey = new Consultation(null, null, doctorId, null, null, null);
    int foundIndex = ((MyClinicADT<Consultation>) sorted).search(searchKey, comparator);
    
    ClinicADT<Consultation> found = new MyClinicADT<>();
    
    if (foundIndex >= 0) {
        // 4) Expand left to find the first matching consultation
        int first = foundIndex;
        while (first > 0 && sorted.get(first - 1).getDoctorId().equalsIgnoreCase(doctorId)) {
            first--;
        }
        
        // 5) Expand right to find the last matching consultation
        int last = foundIndex;
        while (last < sorted.size() - 1 && sorted.get(last + 1).getDoctorId().equalsIgnoreCase(doctorId)) {
            last++;
        }
        
        // 6) Collect matches into result ADT
        for (int i = first; i <= last; i++) {
            found.add(sorted.get(i));
        }
    }
    
    if (found.isEmpty()) {
        System.out.println("No consultations found for Dr. " + doctor.getName() + " (" + doctor.getId() + ").");
    } else {
        System.out.println("\n=== Consultations for Dr. " + doctor.getName() + " (" + doctor.getId() + ") ===");
        displayConsultationDetails(found);
    }
}

    // ========= PAGINATED detail table (10 per page) =========
    private void displayConsultationDetails(ClinicADT<Consultation> consultationList) {
        String format = "| %-4s | %-20s | %-15s | %-16s | %-8s | %-8s | %-8s | %-10s |\n";
        String line = "+------+----------------------+-----------------+------------------+----------+----------+----------+------------+";
        final int PAGE = 10;

        System.out.println(line);
        System.out.printf(format, "ID", "Patient", "Doctor", "Date", "Start", "End", "Duration", "Status");
        System.out.println(line);

        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        int count = 0;

        ClinicADT.MyIterator<Consultation> iterator = consultationList.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            LocalDateTime start = c.getConsultationDate();
            LocalDateTime end = start.plusMinutes(CONSULTATION_DURATION_MIN);

            String date = (start == null) ? "N/A" : start.toLocalDate().toString();
            String startStr = (start == null) ? "N/A" : start.toLocalTime().format(tf);
            String endStr   = (start == null) ? "N/A" : end.toLocalTime().format(tf);

            System.out.printf(format,
                    c.getId(),
                    c.getPatientName(),
                    c.getDoctorName(),
                    date,
                    startStr,
                    endStr,
                    "60 min",
                    statusOfForDisplay(c));

            count++;
            if (count % PAGE == 0 && iterator.hasNext()) {
                System.out.println(line);
                if (!promptContinuePage(count, estimateSize(consultationList))) break;
                System.out.println(line);
                System.out.printf(format, "ID", "Patient", "Doctor", "Date", "Start", "End", "Duration", "Status");
                System.out.println(line);
            }
        }
        System.out.println(line);
    }

    // ========= PAGINATED queue view (10 per page) =========
    public void displayQueuedPatients() {
        rebuildWaitingQueueFromPending();

        boolean empty;
        try { empty = waitingQueue.isEmpty(); } catch (Exception e) { empty = true; }
        if (empty) {
            System.out.println("No pending consultations in the queue.");
            return;
        }

        final String line      = "+-----+--------------+------------+----------------------+----------------------+---------------------+------------+";
        final String headerFmt = "| Pos | %-12s | %-10s | %-20s | %-20s | %-19s | %-10s |%n";
        final String rowFmt    = "| %3d | %12d | %-10s | %-20s | %-20s | %-19s | %-10s |%n";
        DateTimeFormatter fmt  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        final int PAGE = 10;

        System.out.println("\nQueued patients (nearest time first):");
        System.out.println(line);
        System.out.printf(headerFmt, "Consult ID", "PatientID", "Patient Name", "Doctor", "Date & Time", "Status");
        System.out.println(line);

        int pos = 1, printed = 0;
        MyClinicADT.MyIterator<Integer> it = waitingQueue.iterator();
        while (it.hasNext()) {
            Integer id = it.next();
            Consultation c = findById(id);

            if (c == null) {
                System.out.printf("| %3d | %12s | %-10s | %-20s | %-20s | %-19s | %-10s |%n",
                        pos++, "N/A", "N/A", "Unknown", "Unknown", "N/A", "PENDING");
            } else {
                String dt = (c.getConsultationDate() != null) ? c.getConsultationDate().format(fmt) : "N/A";
                System.out.printf(rowFmt,
                        pos++,
                        c.getId(),
                        c.getPatientId(),
                        truncate(c.getPatientName(), 20),
                        truncate(c.getDoctorName(), 20),
                        dt,
                        statusOfForDisplay(c));
            }

            printed++;
            if (printed % PAGE == 0 && it.hasNext()) {
                System.out.println(line);
                if (!promptContinuePage(printed, estimateSize(waitingQueue))) break;
                System.out.println(line);
                System.out.printf(headerFmt, "Consult ID", "PatientID", "Patient Name", "Doctor", "Date & Time", "Status");
                System.out.println(line);
            }
        }
        System.out.println(line);
    }

    // Helper to estimate size of ClinicADT without exposing size() externally
    private int estimateSize(ClinicADT<?> list) {
        int n = 0;
        ClinicADT.MyIterator<?> it = list.iterator();
        while (it.hasNext()) { it.next(); n++; }
        return n;
    }

    /**
     * Completely remove ALL consultations and queues.
     * Immediately persists to consultations.txt so nothing comes back after restart.
     */
    public void removeAllConsultations() {
        consultations.clear();
        waitingQueue.clear();
        currentCalledConsultationId = null;
        saveConsultationToFile(null, false); // truncate file
        System.out.println("All consultations removed and file cleared.");
    }


    public void viewNextPatientInQueue() {
        rebuildWaitingQueueFromPending();

        Integer next = null;
        try { next = waitingQueue.peek(); } catch (Exception e) { next = null; }

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
        System.out.printf(rowFmt,
                c.getId(),
                c.getPatientId(),
                truncate(c.getPatientName(), 20),
                truncate(c.getDoctorName(), 20),
                dt,
                statusOfForDisplay(c));
        System.out.println(line);
    }

    private boolean isMarkedProcessed(int id) {
        String key = "|" + id + "|";
        return processedIdx.indexOf(key) >= 0;
    }
    private void markProcessed(int id) {
        if (!isMarkedProcessed(id)) processedIdx += id + "|";
    }
    public void markConsultationAsProcessed(int consultationId) {
        markProcessed(consultationId);
        saveConsultationToFile(null, false);
        rebuildWaitingQueueFromPending();
    }

    public boolean removeProcessedConsultation(String patientId, String doctorId, LocalDateTime consultationDate) {
        int idx = 0;
        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) {
            Consultation c = it.next();
            if (c.getPatientId().equalsIgnoreCase(patientId)
                    && c.getDoctorId().equalsIgnoreCase(doctorId)
                    && c.getConsultationDate().equals(consultationDate)) {

                consultations.remove(idx);
                try { waitingQueue.remove(c.getId()); } catch (Exception ignored) {}
                if (currentCalledConsultationId != null && currentCalledConsultationId == c.getId()) {
                    currentCalledConsultationId = null;
                }
                saveConsultationToFile(null, false);
                rebuildWaitingQueueFromPending();

                System.out.println("Processed consultation removed: Patient " + c.getPatientName());
                return true;
            }
            idx++;
        }
        return false;
    }

    public ClinicADT<Consultation> getUnprocessedConsultationsForPatient(String patientId) {
        ClinicADT<Consultation> result = new MyClinicADT<>();

        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            if (c.getPatientId().equalsIgnoreCase(patientId)) {
                if (hasValidDiagnosis(c) && !isMarkedProcessed(c.getId())) {
                    result.add(c);
                }
            }
        }
        return result;
    }

    public void displayCurrentlyConsulting() {
        if (currentCalledConsultationId == null) {
            System.out.println("No patient is currently consulting.");
            return;
        }
        Consultation c = findById(currentCalledConsultationId);
        if (c == null) {
            System.out.println("No patient is currently consulting.");
            return;
        }

        final String line      = "+--------------+------------+----------------------+----------------------+---------------------+------------+";
        final String headerFmt = "| %-12s | %-10s | %-20s | %-20s | %-19s | %-10s |%n";
        final String rowFmt    = "| %12d | %-10s | %-20s | %-20s | %-19s | %-10s |%n";
        DateTimeFormatter fmt  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        String dt = (c.getConsultationDate() != null) ? c.getConsultationDate().format(fmt) : "N/A";

        System.out.println("\n=== Currently Consulting ===");
        System.out.println(line);
        System.out.printf(headerFmt, "Consult ID", "PatientID", "Patient Name", "Doctor", "Date & Time", "Status");
        System.out.println(line);
        System.out.printf(rowFmt,
                c.getId(),
                c.getPatientId(),
                truncate(c.getPatientName(), 20),
                truncate(c.getDoctorName(), 20),
                dt,
                "CONSULTING");
        System.out.println(line);
    }

    private boolean hasValidDiagnosis(Consultation c) {
        String diagnosis = c.getDiagnosis();
        return diagnosis != null
                && !diagnosis.trim().isEmpty()
                && !diagnosis.equalsIgnoreCase("To be diagnosed during appointment")
                && !diagnosis.equalsIgnoreCase("Pending");
    }

    public void listConsultations(boolean onlyProcessed) {
        if (consultations.isEmpty()) {
            System.out.println("No consultations scheduled.");
            return;
        }

        final String line =
            "+------+--------------+----------------------+----------------------+----------------------+-----------+-----------+";
        final String header =
            "| ID   | Patient ID    | Patient Name           | Doctor               | Date & Time          | Duration  | Status    |";
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        final int PAGE = 10;

        System.out.println("\n=== All Consultations ===");
        System.out.println(line);
        System.out.println(header);
        System.out.println(line);

        boolean printedAny = false;
        int printed = 0;

        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) {
            Consultation c = it.next();

            boolean processed = (!isPending(c)) || isMarkedProcessed(c.getId());
            if (onlyProcessed && !processed) continue;
            if (!onlyProcessed && processed) continue;

            String dateStr = (c.getConsultationDate() != null) ? c.getConsultationDate().format(fmt) : "N/A";

            System.out.printf("| %-4d | %-12s | %-20s | %-20s | %-20s | %-9s | %-9s |\n",
                    c.getId(),
                    c.getPatientId(),
                    truncate(c.getPatientName(), 20),
                    truncate(c.getDoctorName(), 20),
                    dateStr,
                    "60 min",
                    statusOfForDisplay(c));

            printedAny = true;
            printed++;
            if (printed % PAGE == 0 && it.hasNext()) {
                System.out.println(line);
                if (!promptContinuePage(printed, estimateSize(consultations))) break;
                System.out.println(line);
                System.out.println(header);
                System.out.println(line);
            }
        }

        if (!printedAny) {
            System.out.println("| No consultations match the filter.                                                                    |");
        }
        System.out.println(line);
    }

    /**
     * Allow Appointment module to set the currently called patient by ID.
     * If another different patient is already called, returns false.
     * If found, locks the nearest PENDING consultation for that patient.
     */
    public boolean setExternalCalledPatientId(String patientId) {
        if (patientId == null || patientId.isBlank()) return false;

        if (currentCalledConsultationId != null) {
            Consultation already = findById(currentCalledConsultationId);
            if (already != null && patientId.equalsIgnoreCase(already.getPatientId())) {
                return true; // same patient remains locked
            }
            return false;    // someone else is locked
        }

        Consultation best = null;
        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) {
            Consultation c = it.next();
            if (c == null) continue;
            if (!patientId.equalsIgnoreCase(c.getPatientId())) continue;
            if (!isPending(c)) continue;

            if (best == null || c.getConsultationDate().isBefore(best.getConsultationDate())) {
                best = c;
            }
        }

        if (best == null) return false;

        currentCalledConsultationId = best.getId();
        return true;
    }

    private void rebuildWaitingQueueFromPending() {
        waitingQueue.clear();

        // Collect all pending consultations into a temp ADT
        MyClinicADT<Consultation> pend = new MyClinicADT<>();
        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) {
            Consultation c = it.next();
            if (isPending(c)) pend.add(c);
        }
        if (pend.isEmpty()) return;

        // Sort by:
        // 1) absolute minutes distance from now (nearest first)
        // 2) earlier time first (when distance ties)
        // 3) smaller ID first (when both above tie)
        final LocalDateTime now = LocalDateTime.now();
        pend.sort((x, y) -> {
            long dx = Math.abs(java.time.Duration.between(now, x.getConsultationDate()).toMinutes());
            long dy = Math.abs(java.time.Duration.between(now, y.getConsultationDate()).toMinutes());
            if (dx != dy) return Long.compare(dx, dy);
            int t = x.getConsultationDate().compareTo(y.getConsultationDate());
            if (t != 0) return t;
            return Integer.compare(x.getId(), y.getId());
        });

        // Enqueue IDs in that order (FIFO)
        ClinicADT.MyIterator<Consultation> it2 = pend.iterator();
        while (it2.hasNext()) waitingQueue.enqueue(it2.next().getId());
    }

    // --- ensure a Consultation exists for an appointment, then lock it ---
    public boolean setExternalCalledForAppointment(
            String patientId, String patientName,
            String doctorName, String doctorId,
            LocalDateTime scheduledDateTime,
            boolean createIfMissing) {

        Consultation exact = null;
        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) {
            Consultation c = it.next();
            if (c == null) continue;
            if (patientId.equalsIgnoreCase(c.getPatientId())
                    && doctorId.equalsIgnoreCase(c.getDoctorId())
                    && c.getConsultationDate() != null
                    && c.getConsultationDate().equals(scheduledDateTime)) {
                exact = c;
                break;
            }
        }

        if (exact == null) {
            if (!createIfMissing) return false;   // <<--- do NOT recreate
            exact = new Consultation(
                    patientId, patientName, doctorName, doctorId,
                    scheduledDateTime, "To be diagnosed during appointment");
            exact.setStatus(Consultation.Status.CONSULTING);
            consultations.add(exact);
            saveConsultationToFile(null, false);
            rebuildWaitingQueueFromPending();
        } else if (exact.getStatus() != Consultation.Status.CONSULTING) {
            setStatusAndPersist(exact, Consultation.Status.CONSULTING);
            rebuildWaitingQueueFromPending();
        }

        currentCalledConsultationId = exact.getId();
        return true;
    }


    /** OPTIONAL helper: try to lock by patient + exact datetime first, else nearest pending. */
    public boolean setExternalCalledByPatientAndTime(
            String patientId, java.time.LocalDateTime scheduledDateTime) {

        Consultation best = null;
        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) {
            Consultation c = it.next();
            if (c == null) continue;
            if (patientId.equalsIgnoreCase(c.getPatientId())
                    && c.getConsultationDate() != null
                    && c.getConsultationDate().equals(scheduledDateTime)
                    && isPending(c)) {
                best = c;
                break;
            }
        }
        if (best == null) {
            it = consultations.iterator();
            while (it.hasNext()) {
                Consultation c = it.next();
                if (c == null) continue;
                if (!patientId.equalsIgnoreCase(c.getPatientId())) continue;
                if (!isPending(c)) continue;
                if (best == null || c.getConsultationDate().isBefore(best.getConsultationDate())) {
                    best = c;
                }
            }
        }
        if (best == null) return false;
        currentCalledConsultationId = best.getId();
        return true;
    }

    /** Default-and-sanitize for CSV fields (replace commas, trim, allow fallback). */
    private static String safe(String s, String def) {
        if (s == null) return def;
        String t = s.trim();
        if (t.isEmpty()) return def;
        return t.replace(',', ' ');
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return (s.length() <= max) ? s : s.substring(0, Math.max(0, max - 3)) + "...";
    }

    private boolean hasDoctorReachedDailyLimit(String doctorId, LocalDate date) {
        int count = 0;
        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) {
            Consultation c = it.next();
            if (c.getDoctorId().equalsIgnoreCase(doctorId)
                    && c.getConsultationDate().toLocalDate().equals(date)) {
                count++;
                if (count >= MAX_DAILY_CONSULTATIONS_PER_DOCTOR) return true;
            }
        }
        return false;
    }

    private boolean hasPatientExceededFrequency(String patientId) {
        LocalDate oneWeekAgo = LocalDate.now().minusDays(7);
        int count = 0;
        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) {
            Consultation c = it.next();
            if (c.getPatientId().equalsIgnoreCase(patientId)
                    && !c.getConsultationDate().toLocalDate().isBefore(oneWeekAgo)) {
                count++;
                if (count >= MAX_WEEKLY_CONSULTATIONS_PER_PATIENT) return true;
            }
        }
        return false;
    }

    private boolean isPatientAlreadyBooked(String patientId, LocalDate selectedDate, boolean includeTreatments) {
        int consultationCount = 0;
        int treatmentCount = 0;

        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) {
            Consultation c = it.next();
            if (c.getPatientId().equalsIgnoreCase(patientId)
                    && c.getConsultationDate().toLocalDate().equals(selectedDate)) {
                consultationCount++;
            }
        }

        if (includeTreatments && treatments != null) {
            ClinicADT.MyIterator<MedicalTreatment> tit = treatments.iterator();
            while (tit.hasNext()) {
                MedicalTreatment t = tit.next();
                if (t.getPatientId().equalsIgnoreCase(patientId)
                        && t.getTreatmentDateTime().toLocalDate().equals(selectedDate)) {
                    treatmentCount++;
                }
            }
        }
        return (consultationCount > 0 || treatmentCount > 0);
    }

    private boolean hasMinimumGap(Doctor doctor, LocalDateTime newStart) {
        LocalDateTime bufferStart = newStart.minusMinutes(MIN_MINUTES_BETWEEN_CONSULTATIONS);
        LocalDateTime bufferEnd   = newStart.plusMinutes(CONSULTATION_DURATION_MIN)
                                           .plusMinutes(MIN_MINUTES_BETWEEN_CONSULTATIONS);

        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) {
            Consultation c = it.next();
            if (c.getDoctorId().equalsIgnoreCase(doctor.getId())) {
                LocalDateTime s = c.getConsultationDate();
                LocalDateTime e = s.plusMinutes(CONSULTATION_DURATION_MIN);

                if ((s.isAfter(bufferStart) && s.isBefore(bufferEnd)) ||
                    (e.isAfter(bufferStart) && e.isBefore(bufferEnd)) ||
                    (s.isBefore(bufferStart) && e.isAfter(bufferEnd))) {
                    return false;
                }
            }
        }
        return true;
    }

    // ===== Console centering (match your report width) =====
    private static final int WIDTH = 120;
    private static String center(String s) {
        if (s == null) s = "";
        int pad = Math.max(0, (WIDTH - s.length()) / 2);
        return " ".repeat(pad) + s;
    }
    private static String repeat(String s, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder(n * s.length());
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }
    private static String padCenter(String s, int w) {
        if (s == null) s = "";
        if (s.length() >= w) return s;
        int left = (w - s.length()) / 2;
        int right = w - s.length() - left;
        return " ".repeat(left) + s + " ".repeat(right);
    }

    // ===== Vertical bar chart (with top cap "--" and side borders) =====
    private static String fit(String s, int w) {
        if (s == null) s = "";
        return (s.length() <= w) ? s : s.substring(0, Math.max(0, w - 1)) + "…";
    }

    private void renderVerticalBarChart(String title, String[] labels, int[] counts, int maxHeight) {
        if (labels == null || counts == null || labels.length != counts.length || labels.length == 0) return;

        // find max value and longest label length (for column width)
        int max = 0, maxLabel = 0;
        for (int i = 0; i < counts.length; i++) if (counts[i] > max) max = counts[i];
        for (int i = 0; i < labels.length; i++) {
            String s = (labels[i] == null ? "" : labels[i].trim());
            if (s.length() > maxLabel) maxLabel = s.length();
        }
        if (max == 0) max = 1;

        // auto height if caller passes <=0; clamp to [3, 10] so chart is compact
        int height = (maxHeight <= 0) ? max : maxHeight;
        height = Math.max(3, Math.min(10, height));
        // if max is smaller than our chosen height, shrink to max to remove blank rows
        height = Math.min(height, max);

        // column width based on longest label (bounded)
        final int colW = Math.max(10, Math.min(16, maxLabel + 2));

        // scale unit = ceil(max / height)
        final int unit = (max + height - 1) / height;

        final String CAP = "+--+";
        final String BAR = "|  |";
        final String BOT = "+--+";
        final String SP  = "    ";
        final String SIDE = "|";

        System.out.println();
        System.out.println(center(title));
        System.out.println(center(repeat("-", title.length())));

        // Calculate chart width for borders
        int chartWidth = labels.length * colW;

        // Top border
        System.out.println(center("+" + repeat("-", chartWidth) + "+"));

        // numbers row on top (with side borders)
        String top = SIDE;
        for (int i = 0; i < counts.length; i++) top += padCenter(String.valueOf(counts[i]), colW);
        top += SIDE;
        System.out.println(center(top));

        // draw from top row down (with side borders)
        for (int h = height; h >= 1; h--) {
            String row = SIDE;
            for (int i = 0; i < counts.length; i++) {
                int barH = (counts[i] + unit - 1) / unit; // scaled height
                if (barH >= h) {
                    if (barH == h) {
                        row += padCenter(CAP, colW); // top of this bar
                    } else {
                        row += padCenter(BAR, colW); // middle section of bar
                    }
                } else {
                    row += padCenter(SP, colW); // empty space above bar
                }
            }
            row += SIDE;
            System.out.println(center(row));
        }

        // Add bottom caps for all bars
        String bottomRow = SIDE;
        for (int i = 0; i < counts.length; i++) {
            int barH = (counts[i] + unit - 1) / unit;
            if (barH > 0) {
                bottomRow += padCenter(BOT, colW);
            } else {
                bottomRow += padCenter(SP, colW);
            }
        }
        bottomRow += SIDE;
        System.out.println(center(bottomRow));

        // base & labels (with side borders and truncate to fit)
        System.out.println(center("+" + repeat("-", chartWidth) + "+"));
        String lab = SIDE;
        for (int i = 0; i < labels.length; i++) lab += padCenter(fit(labels[i], colW - 2), colW);
        lab += SIDE;
        System.out.println(center(lab));

        // Bottom border
        System.out.println(center("+" + repeat("-", chartWidth) + "+"));
    }

    // ====== REPLACEMENT: statistics + per-doctor bar chart (ADT-only) ======
    public void displayConsultationStatistics() {
        if (consultations.isEmpty()) {
            System.out.println("No consultations available for statistics.");
            return;
        }

        int total = 0, pending = 0, processed = 0;

        // Build unique doctor list and counts with your ADT
        adt.MyClinicADT<String> doctors = new adt.MyClinicADT<>();
        adt.MyClinicADT<Integer> counts  = new adt.MyClinicADT<>();

        ClinicADT.MyIterator<Consultation> it = consultations.iterator();
        while (it.hasNext()) {
            Consultation c = it.next();
            total++;

            if (isPending(c)) pending++; else processed++;

            String dname = (c.getDoctorName() == null ? "-" : c.getDoctorName().trim());
            if (dname.isEmpty()) dname = "-";

            // find or add
            int idx = -1;
            for (int i = 0; i < doctors.size(); i++) {
                if (doctors.get(i).equalsIgnoreCase(dname)) { idx = i; break; }
            }
            if (idx == -1) { doctors.add(dname); counts.add(1); }
            else           { counts.set(idx, counts.get(idx) + 1); }
        }

        System.out.println();
        System.out.println(center("=== Consultation Statistics ==="));
        String line = repeat("*", WIDTH);
        System.out.println(center(line));
        System.out.println(center(String.format("%-24s : %d", "Total consultations", total)));
        System.out.println(center(String.format("%-24s : %d", "Pending",             pending)));
        System.out.println(center(String.format("%-24s : %d", "Processed",           processed)));
        System.out.println(center(line));

        // Prepare arrays for the chart
        int n = doctors.size();
        String[] labels = new String[n];
        int[]    vals   = new int[n];
        for (int i = 0; i < n; i++) {
            labels[i] = doctors.get(i);
            vals[i]   = counts.get(i);
        }

        // Render centered vertical bar chart
        renderVerticalBarChart("Consultations per Doctor", labels, vals, 0);
    }


    // === Show schedule/availability of all doctors for a given date ===
    public void showDoctorScheduleForDate(LocalDateTime date) {
        if (date.toLocalDate().isBefore(LocalDate.now())) {
            System.out.println("Cannot check availability for past dates.");
            return;
        }
        System.out.println("\n=== Doctor Availability for " +
                date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd (EEEE)")) + " ===");

        displaySessionAvailability(date, Session.MORNING,   "Morning Shift   (08:00 - 12:00)");
        displaySessionAvailability(date, Session.AFTERNOON, "Afternoon Shift (13:00 - 17:00)");
        displaySessionAvailability(date, Session.NIGHT,     "Night Shift     (18:00 - 22:00)");
    }

    // Helper to show doctors in one session
    private void displaySessionAvailability(LocalDateTime date, Session session, String header) {
        ClinicADT<Doctor> sessionDoctors = getAvailableDoctorsForSession(date, session);

        System.out.println("\n" + header);
        String line = "+----------------------+----------+";
        String fmt  = "| %-20s | %-8s |\n";

        System.out.println(line);
        System.out.printf(fmt, "Doctor Name", "Room");
        System.out.println(line);

        if (sessionDoctors.isEmpty()) {
            System.out.printf("| %-29s |\n", "No doctors available");
        } else {
            ClinicADT.MyIterator<Doctor> it = sessionDoctors.iterator();
            while (it.hasNext()) {
                Doctor doc = it.next();
                System.out.printf(fmt, doc.getName(), doc.getRoomNumber());
            }
        }
        System.out.println(line);
    }

    private ClinicADT<Doctor> getAvailableDoctorsForSession(LocalDateTime date, Session session) {
        ClinicADT<Doctor> result = new MyClinicADT<>();
        ClinicADT<Doctor> allDocs = doctorControl.getAllDoctors();

        ClinicADT.MyIterator<Doctor> it = allDocs.iterator();
        while (it.hasNext()) {
            Doctor doctor = it.next();
            if (doctor != null && isDoctorOnDuty(doctor, date, session)) {
                result.add(doctor);
            }
        }
        return result;
    }

    // --- tiny helper so we never forget to persist a status change ---
    private synchronized void setStatusAndPersist(Consultation c, Consultation.Status s) {
        if (c == null) return;
        c.setStatus(s);
        saveConsultationToFile(null, false);
    }

    private void mirrorAppointmentStatus(Consultation c, Appointment.AppointmentStatus st) {
        if (appointmentControl == null || c == null) return;
        Appointment a = findApptForConsultation(c);
        if (a != null) {
            a.setStatus(st);
            // persist to appointments.txt so nothing resurrects next run
            appointmentControl.persistForExternalMutation();
        }
    }
}
