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
                        selectedDoctor.getName(), selectedDoctor.getId(), dateTime);
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

        System.out.println("\nAvailable Doctors for " + 
                           dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + ":");
        displayAvailableDoctors(availableDoctors);

        System.out.print("Select Doctor ID: ");
        String doctorId = sc.nextLine().trim().toUpperCase();

        // Use proper iteration with your custom ADT
        // FIX: Correctly reference the nested interface
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

        // FIX: Replaced for-loop with iterator
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

        // FIX: Correctly reference the nested interface
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

        // FIX: Correctly reference the nested interface
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

        // FIX: Correctly reference the nested interface
        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation consultation = iterator.next();
            if (consultation.getDoctorName().equalsIgnoreCase(doctorName)) {
                LocalDateTime existingStart = consultation.getConsultationDate();
                LocalDateTime existingEnd = existingStart.plusHours(CONSULTATION_DURATION);

                // Check for time overlap
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

    public void addConsultation(String patientId, String patientName, String doctorName, String doctorId, LocalDateTime date) {
        Consultation consultation = new Consultation(patientId, patientName, doctorName, doctorId, date);
        consultations.add(consultation);
        saveConsultationToFile(consultation);
        displayConsultationConfirmation(consultation);
    }

    private void displayConsultationConfirmation(Consultation consultation) {
        System.out.println("\nConsultation added successfully!");
        System.out.println(consultation);
        System.out.println("Duration: " + CONSULTATION_DURATION + " hour (ends at " + 
                           consultation.getConsultationDate().plusHours(CONSULTATION_DURATION)
                           .format(DateTimeFormatter.ofPattern("HH:mm")) + ")");
    }

    private void saveConsultationToFile(Consultation consultation) {
        try (FileWriter fw = new FileWriter(consultationFilePath, true)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String line = String.format("%d,%s,%s,%s,%s,%s%n",
                    consultation.getId(),
                    consultation.getPatientId(),
                    consultation.getPatientName(),
                    consultation.getDoctorName(),
                    consultation.getDoctorId(),
                    consultation.getConsultationDate().format(formatter));
            fw.write(line);
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

    private void displayConsultationTable() {
        String format = "| %-4s | %-20s | %-20s | %-20s | %-9s |\n";
        String line = "+------+----------------------+----------------------+----------------------+-----------+";

        System.out.println(line);
        System.out.printf(format, "ID", "Patient", "Doctor", "Date & Time", "Duration");
        System.out.println(line);

        // FIX: Replaced for-loop with iterator
        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            System.out.printf(format,
                    c.getId(),
                    c.getPatientName(),
                    c.getDoctorName(),
                    c.getConsultationDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    CONSULTATION_DURATION + " hr");
        }

        System.out.println(line);
    }

    public void searchByPatient(String patientName) {
        ClinicADT<Consultation> found = new MyClinicADT<>();
        
        // FIX: Replaced for-loop with iterator
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

    private void displayConsultationDetails(ClinicADT<Consultation> consultationList) {
        String format = "| %-4s | %-20s | %-15s | %-16s | %-8s | %-8s | %-8s |\n";
        String line = "+------+----------------------+-----------------+------------------+----------+----------+----------+";

        System.out.println(line);
        System.out.printf(format, "ID", "Patient", "Doctor", "Date", "Start", "End", "Duration");
        System.out.println(line);

        // FIX: Replaced for-loop with iterator
        ClinicADT.MyIterator<Consultation> iterator = consultationList.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            LocalDateTime start = c.getConsultationDate();
            LocalDateTime end = start.plusHours(CONSULTATION_DURATION);

            System.out.printf(format,
                    c.getId(),
                    c.getPatientName(),
                    c.getDoctorName(),
                    start.toLocalDate(),
                    start.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                    end.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                    CONSULTATION_DURATION + " hr");
        }
        System.out.println(line);
    }

    public void printConsultationsSortedByDate() {
        if (consultations.isEmpty()) {
            System.out.println("No consultations to sort.");
            return;
        }

        // Create a copy for sorting
        ClinicADT<Consultation> sorted = new MyClinicADT<>();
        // FIX: Replaced for-loop with iterator for copying
        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            sorted.add(iterator.next());
        }
        
        // Sort using custom comparator
        // FIX: Correctly reference the nested interface
        sorted.sort(new ClinicADT.MyComparator<Consultation>() {
            @Override
            public int compare(Consultation c1, Consultation c2) {
                return c1.getConsultationDate().compareTo(c2.getConsultationDate());
            }
        });

        System.out.println("\n=== Consultations (Sorted by Date) ===");
        displaySortedConsultations(sorted);
    }

    private void displaySortedConsultations(ClinicADT<Consultation> sorted) {
        String format = "| %-4s | %-20s | %-20s | %-20s | %-9s |\n";
        String line = "+------+----------------------+----------------------+----------------------+-----------+";

        System.out.println(line);
        System.out.printf(format, "ID", "Patient", "Doctor", "Date & Time", "Duration");
        System.out.println(line);

        // FIX: Replaced for-loop with iterator
        ClinicADT.MyIterator<Consultation> iterator = sorted.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            System.out.printf(format,
                    c.getId(),
                    c.getPatientName(),
                    c.getDoctorName(),
                    c.getConsultationDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    CONSULTATION_DURATION + " hr");
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
            // FIX: Replaced for-loop with iterator
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

        // FIX: Correctly reference the nested interface
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
        
        // Count consultations by session
        int morningCount = 0, afternoonCount = 0, nightCount = 0;
        // FIX: Replaced for-loop with iterator
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

        // FIX: Replaced for-loop with iterator
        ClinicADT.MyIterator<Consultation> consultationIterator = consultations.iterator();
        while (consultationIterator.hasNext()) {
            Consultation c = consultationIterator.next();
            String patientId = c.getPatientId();

            // Check if already added
            boolean exists = false;
            // FIX: Replaced for-loop with iterator
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

        // Check consultations
        // FIX: Replaced for-loop with iterator
        ClinicADT.MyIterator<Consultation> consultationIterator = consultations.iterator();
        while (consultationIterator.hasNext()) {
            Consultation c = consultationIterator.next();
            if (c.getPatientId().equalsIgnoreCase(patientId) &&
                c.getConsultationDate().toLocalDate().equals(selectedDate)) {
                consultationCount++;
            }
        }

        // Check treatments
        if (treatments != null) {
            // FIX: Replaced for-loop with iterator
            ClinicADT.MyIterator<MedicalTreatment> treatmentIterator = treatments.iterator();
            while (treatmentIterator.hasNext()) {
                MedicalTreatment t = treatmentIterator.next();
                if (t.getPatientId().equalsIgnoreCase(patientId) &&
                    t.getTreatmentDateTime().toLocalDate().equals(selectedDate)) {
                    treatmentCount++;
                }
            }
        }
        
        // Rules - patient can only have one appointment per day
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

                    Consultation consultation = new Consultation(
                                consultationId, patientId, patientName, doctorName, doctorId, consultationDate);
                    consultations.add(consultation);
                } catch (Exception e) {
                    System.out.println("Error parsing consultation line: " + line);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading consultations: " + e.getMessage());
        }
    }
    
    // Validation methods
    private boolean hasDoctorReachedDailyLimit(String doctorId, LocalDate date) {
        int count = 0;
        // FIX: Replaced for-loop with iterator
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
        
        // FIX: Replaced for-loop with iterator
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

        // FIX: Replaced for-loop with iterator
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
}