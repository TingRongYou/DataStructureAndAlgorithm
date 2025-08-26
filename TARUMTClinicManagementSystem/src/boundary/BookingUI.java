package boundary;

import adt.ClinicADT;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import entity.Consultation;
import control.ConsultationControl;
import entity.Doctor;
import control.DoctorControl;
import entity.MedicalTreatment;
import entity.Patient;
import control.PatientControl;
import control.TreatmentControl;

public class BookingUI {
    private static final int CONSULTATION_DURATION = 1;
    private static final int TREATMENT_DURATION = 2;

    private final PatientControl patientControl;
    private final DoctorControl doctorControl;
    private final ClinicADT<Consultation> consultations;
    private final ClinicADT<MedicalTreatment> treatments;
    private final ConsultationControl consultationControl;
    private final TreatmentControl treatmentControl;
    private final Scanner scanner;

    public BookingUI(PatientControl patientControl, DoctorControl doctorControl,
                     ClinicADT<Consultation> consultations, ClinicADT<MedicalTreatment> treatments,
                     ConsultationControl consultationControl, TreatmentControl treatmentControl) {
        this.patientControl = patientControl;
        this.doctorControl = doctorControl;
        this.consultations = consultations;
        this.treatments = treatments;
        this.consultationControl = consultationControl;
        this.treatmentControl = treatmentControl;
        this.scanner = new Scanner(System.in);
    }

    public void run(boolean isConsultation) {
        System.out.println("\nRegistered Patients:");
        String patientId;
        Patient patient;

        if (isConsultation) {
            patientControl.printAllPatientsSortedByName();
        } else {
            // For treatments, show only patients with unprocessed diagnosed consultations
            showPatientsWithUnprocessedConsultations();
        }

        while (true) {
            System.out.print("\nEnter Patient ID from the list (or 0 to cancel): ");
            patientId = scanner.nextLine().trim();

            if (patientId.equals("0")) return;

            patient = patientControl.getPatientById(patientId);

            if (patient != null) {
                // For treatments, double-check they have unprocessed consultations
                if (!isConsultation && !hasUnprocessedDiagnosedConsultation(patientId)) {
                    System.out.println("Patient has no unprocessed diagnosed consultations available for treatment.");
                    continue;
                }
                break;
            }

            System.out.println("Patient not found. Please try again.");
        }

        int duration = isConsultation ? CONSULTATION_DURATION : TREATMENT_DURATION;
        String serviceType = isConsultation ? "Consultation" : "Medical Treatment";

        LocalDate selectedDate = showCalendarAndSelectDate(duration);
        if (selectedDate == null) return;

        LocalDateTime chosenSlot = showAvailableTimeSlotsAndSelect(selectedDate, duration, patient, isConsultation, serviceType);
        if (chosenSlot == null) return;

        System.out.println("\nBooking Confirmation:");
        String line = "+------------+--------------------------+";
        String format = "| %-10s | %-24s |\n";

        System.out.println(line);
        System.out.printf(format, "Field", "Details");
        System.out.println(line);
        System.out.printf(format, "Patient", patient.getName() + " (" + patient.getId() + ")");
        System.out.printf(format, "Service", serviceType);
        System.out.printf(format, "Date", chosenSlot.toLocalDate());
        System.out.printf(format, "Time", chosenSlot.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        System.out.printf(format, "Duration", duration + " hour(s)");
        System.out.println(line);

        // If treatment was created, mark the consultation as processed
        if (!isConsultation) {
            markConsultationAsProcessedForTreatment(patientId);
        }
    }

    // ========= NEW: Reschedule flow used by TreatmentControl =========
    // Shows date & slot pickers restricted to the same doctor as the overdue treatment.
    // Returns the chosen LocalDateTime or null if the user cancels.
    public LocalDateTime pickSlotForReschedule(MedicalTreatment overdue) {
        if (overdue == null) return null;

        System.out.println("\n=== Reschedule Overdue Treatment ===");
        System.out.println("Patient : " + overdue.getPatientName() + " (" + overdue.getPatientId() + ")");
        System.out.println("Doctor  : " + overdue.getDoctorId());
        System.out.println("Current : " + overdue.getTreatmentDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        // 1) Pick a date (only dates with at least one free 2-hr slot for this doctor)
        LocalDate date = showCalendarAndSelectDateForDoctor(overdue.getDoctorId(), TREATMENT_DURATION);
        if (date == null) return null;

        // 2) Pick a time slot for that doctor on the chosen date (no booking side-effects)
        Patient p = patientControl.getPatientById(overdue.getPatientId());
        if (p == null) {
            System.out.println("Patient not found.");
            return null;
        }
        return showAvailableTimeSlotsForDoctorAndSelect(date, TREATMENT_DURATION, p, overdue.getDoctorId());
    }

    // ========= existing helpers (unchanged) =========

    // New method to show only patients with unprocessed diagnosed consultations
    private void showPatientsWithUnprocessedConsultations() {
        System.out.println("\n=== Patients with UnDiagnosed Consultations ===");

        String format = "| %-10s | %-20s | %-25s | %-12s |\n";
        String line = "+------------+----------------------+---------------------------+--------------+";

        System.out.println(line);
        System.out.printf(format, "Patient ID", "Patient Name", "Latest Diagnosis", "Consult Date");
        System.out.println(line);

        boolean foundAny = false;

        // Get all patients
        for (int i = 0; i < patientControl.getSize(); i++) {
            Patient p = patientControl.getPatient(i);

            // Check if patient has unprocessed diagnosed consultation
            Consultation latest = getLatestUnprocessedDiagnosedConsultation(p.getId());
            if (latest != null) {
                foundAny = true;
                String diagnosis = latest.getDiagnosis();
                if (diagnosis.length() > 25) {
                    diagnosis = diagnosis.substring(0, 22) + "...";
                }

                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                String dateStr = latest.getConsultationDate().format(fmt);

                System.out.printf(format, p.getId(), p.getName(), diagnosis, dateStr);
            }
        }

        if (!foundAny) {
            System.out.printf("| %-64s |\n", "No patients with unprocessed diagnosed consultations");
        }

        System.out.println(line);
    }

    // Check if patient has unprocessed diagnosed consultation
    private boolean hasUnprocessedDiagnosedConsultation(String patientId) {
        return getLatestUnprocessedDiagnosedConsultation(patientId) != null;
    }

    // Get the latest unprocessed diagnosed consultation for a patient
    private Consultation getLatestUnprocessedDiagnosedConsultation(String patientId) {
        Consultation latest = null;

        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            if (c.getPatientId().equalsIgnoreCase(patientId)) {
                // Check if consultation has valid diagnosis
                String diagnosis = c.getDiagnosis();
                if (diagnosis != null &&
                    !diagnosis.equalsIgnoreCase("To be diagnosed during appointment") &&
                    !diagnosis.equalsIgnoreCase("Pending") &&
                    !diagnosis.trim().isEmpty()) {

                    // Check if this consultation hasn't been processed yet
                    if (!isConsultationProcessedForTreatment(c)) {
                        if (latest == null || c.getConsultationDate().isAfter(latest.getConsultationDate())) {
                            latest = c;
                        }
                    }
                }
            }
        }

        return latest;
    }

    // Check if consultation was already processed for treatment
    private boolean isConsultationProcessedForTreatment(Consultation consultation) {
        // Check if there's already a treatment for this patient with the same diagnosis
        ClinicADT.MyIterator<MedicalTreatment> iterator = treatments.iterator();
        while (iterator.hasNext()) {
            MedicalTreatment t = iterator.next();
            if (t.getPatientId().equalsIgnoreCase(consultation.getPatientId()) &&
                t.getDiagnosis().equals(consultation.getDiagnosis()) &&
                t.getDoctorId().equalsIgnoreCase(consultation.getDoctorId())) {
                return true;
            }
        }
        return false;
    }

    // Mark consultation as processed after treatment creation
    private void markConsultationAsProcessedForTreatment(String patientId) {
        Consultation latest = getLatestUnprocessedDiagnosedConsultation(patientId);
        if (latest != null) {
            // Remove this consultation since it has been used for treatment
            for (int i = 0; i < consultations.size(); i++) {
                Consultation c = consultations.get(i);
                if (c.getId() == latest.getId()) {
                    consultations.remove(i);
                    System.out.println("Processed consultation removed from available list.");
                    break;
                }
            }
        }
    }

    public boolean hasValidDiagnosis(String patientId) {
        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            if (c.getPatientId().equalsIgnoreCase(patientId)) {
                // Check if diagnosis is set and not the default placeholder
                if (c.getDiagnosis() != null &&
                    !c.getDiagnosis().equalsIgnoreCase("To be diagnosed during appointment") &&
                    !c.getDiagnosis().trim().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private LocalDate showCalendarAndSelectDate(int duration) {
        LocalDate today = LocalDate.now();
        System.out.println("\nAvailable Dates (Next 14 Days):");

        String line = "+------+------------+------------+---------------------+";
        String format = "| %-4s | %-10s | %-10s | %-19s |\n";

        System.out.println(line);
        System.out.printf(format, "No.", "Date", "Day", "Available Slots");
        System.out.println(line);

        LocalDate[] optionDates = new LocalDate[14];
        int optionNumber = 1;

        for (int i = 0; i < 14; i++) {
            LocalDate date = today.plusDays(i);
            if (hasAvailableSlots(date, duration)) {
                String formattedDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String dayName = date.getDayOfWeek().toString();
                int availableSlots = countAvailableSlots(date, duration);

                System.out.printf(format, optionNumber, formattedDate, dayName, availableSlots + " slot(s)");
                optionDates[optionNumber - 1] = date;
                optionNumber++;
            }
        }

        System.out.println(line);

        if (optionNumber == 1) {
            System.out.println("No available dates in the next 14 days.");
            return null;
        }

        while (true) {
            System.out.print("Select date option (or 0 to cancel): ");

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice == 0) return null;
                if (choice >= 1 && choice < optionNumber && optionDates[choice - 1] != null) {
                    return optionDates[choice - 1];
                }
                System.out.println("Invalid selection. Please try again.\n");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.\n");
            }
        }
    }

    private LocalDateTime showAvailableTimeSlotsAndSelect(LocalDate date, int duration, Patient patient, boolean isConsultation, String serviceType) {
        System.out.println("\nAvailable Time Slots for " + serviceType);
        System.out.println("Date: " + date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd (EEEE)")));
        System.out.println("==========================================================");

        String line = "+------+-------------------+----------------+---------+";
        String format = "| %-4s | %-17s | %-14s | %-7s |\n";

        System.out.println(line);
        System.out.printf(format, "No.", "Time Slot", "Doctor", "Room");
        System.out.println(line);

        int slotNumber = 1;
        boolean found = false;
        LocalDateTime now = LocalDateTime.now();

        // Map slot number to doctor + time
        class Slot {
            LocalDateTime time;
            Doctor doctor;
            Slot(LocalDateTime t, Doctor d) { time = t; doctor = d; }
        }
        java.util.List<Slot> slotList = new java.util.ArrayList<>();

        ClinicADT.MyIterator<Doctor> doctorIter;

        for (int hour = 8; hour <= 22 - duration; hour++) {
            if (hour == 12) continue; // Skip lunch hour
            LocalDateTime slotStart = LocalDateTime.of(date, LocalTime.of(hour, 0));
            if (slotStart.isBefore(now)) continue; // Skip past time slots

            doctorIter = doctorControl.getAllDoctors().iterator();
            while (doctorIter.hasNext()) {
                Doctor doctor = doctorIter.next();
                if (doctorControl.isDoctorAvailableForAppointment(doctor, slotStart, duration, consultations, treatments)) {
                    String timeRange = slotStart.format(DateTimeFormatter.ofPattern("HH:mm")) +
                            " - " + slotStart.plusHours(duration).format(DateTimeFormatter.ofPattern("HH:mm"));
                    System.out.printf(format, slotNumber, timeRange, "Dr. " + doctor.getName(), doctor.getRoomNumber());
                    slotList.add(new Slot(slotStart, doctor));
                    slotNumber++;
                    found = true;
                }
            }
        }

        System.out.println(line);
        if (!found) {
            System.out.println("No available slots on this date.");
            return null;
        }

        while (true) {
            System.out.print("Select time slot number (or 0 to cancel): ");

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice == 0) return null;

                if (choice < 1 || choice > slotList.size()) {
                    System.out.println("Invalid selection. Please try again.");
                    continue;
                }

                Slot selected = slotList.get(choice - 1);

                // Check patient time conflicts
                if (isPatientTimeClash(patient.getId(), selected.time, duration)) {
                    System.out.println("Patient already has a consultation or treatment at this time.\n");
                    continue;
                }

                // Create the appropriate appointment
                if (isConsultation) {
                    consultationControl.addConsultation(
                        patient.getId(),
                        patient.getName(),
                        selected.doctor.getName(),
                        selected.doctor.getId(),
                        selected.time,
                        "To be diagnosed during appointment"
                    );
                } else {
                    String diagnosis = getMostRecentDiagnosisByPatientId(patient.getId());
                    treatmentControl.addTreatment(new MedicalTreatment(
                        patient.getId(),
                        patient.getName(),
                        selected.doctor.getId(),
                        diagnosis,
                        "To be prescribed during appointment",
                        selected.time,
                        false
                    ));
                }

                System.out.println("Booked with Dr. " + selected.doctor.getName() + " in Room " + selected.doctor.getRoomNumber());
                return selected.time;

            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    /**
     * Gets the most recent valid diagnosis for a patient
     * @param patientId The patient ID to search for
     * @return The most recent diagnosis, or null if none found
     */
    public String getMostRecentDiagnosisByPatientId(String patientId) {
        String diagnosis = null;
        LocalDateTime mostRecentDate = null;

        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            if (c.getPatientId().equalsIgnoreCase(patientId)) {
                // Check if this consultation has a valid diagnosis
                if (c.getDiagnosis() != null &&
                    !c.getDiagnosis().equalsIgnoreCase("To be diagnosed during appointment") &&
                    !c.getDiagnosis().trim().isEmpty()) {

                    // Check if this is the most recent consultation
                    if (mostRecentDate == null || c.getConsultationDate().isAfter(mostRecentDate)) {
                        mostRecentDate = c.getConsultationDate();
                        diagnosis = c.getDiagnosis();
                    }
                }
            }
        }

        if (diagnosis == null) {
            System.out.println("No valid diagnosis found for patient: " + patientId);
        }

        return diagnosis;
    }

    private boolean hasAvailableSlots(LocalDate date, int duration) {
        LocalDateTime now = LocalDateTime.now();
        ClinicADT.MyIterator<Doctor> doctorIter;

        for (int hour = 8; hour <= 22 - duration; hour++) {
            if (hour == 12) continue; // Skip lunch hour
            LocalDateTime slotStart = LocalDateTime.of(date, LocalTime.of(hour, 0));
            if (slotStart.isBefore(now)) continue; // Skip past time slots

            doctorIter = doctorControl.getAllDoctors().iterator();
            while (doctorIter.hasNext()) {
                Doctor doctor = doctorIter.next();
                if (doctorControl.isDoctorAvailableForAppointment(doctor, slotStart, duration, consultations, treatments)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int countAvailableSlots(LocalDate date, int duration) {
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        ClinicADT.MyIterator<Doctor> doctorIter;

        for (int hour = 8; hour <= 22 - duration; hour++) {
            if (hour == 12) continue; // Skip lunch hour
            LocalDateTime slotStart = LocalDateTime.of(date, LocalTime.of(hour, 0));
            if (slotStart.isBefore(now)) continue; // Skip past time slots

            doctorIter = doctorControl.getAllDoctors().iterator();
            while (doctorIter.hasNext()) {
                Doctor doctor = doctorIter.next();
                if (doctorControl.isDoctorAvailableForAppointment(doctor, slotStart, duration, consultations, treatments)) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean isPatientTimeClash(String patientId, LocalDateTime newStart, int duration) {
        LocalDateTime newEnd = newStart.plusHours(duration);

        // Check for consultation conflicts
        ClinicADT.MyIterator<Consultation> itC = consultations.iterator();
        while (itC.hasNext()) {
            Consultation c = itC.next();
            if (c.getPatientId().equalsIgnoreCase(patientId)) {
                LocalDateTime existingStart = c.getConsultationDate();
                LocalDateTime existingEnd = existingStart.plusHours(CONSULTATION_DURATION);

                // Check for time overlap
                if (newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart)) {
                    return true;
                }
            }
        }

        // Check for treatment conflicts
        ClinicADT.MyIterator<MedicalTreatment> itT = treatments.iterator();
        while (itT.hasNext()) {
            MedicalTreatment t = itT.next();
            if (t.getPatientId().equalsIgnoreCase(patientId)) {
                LocalDateTime existingStart = t.getTreatmentDateTime();
                LocalDateTime existingEnd = existingStart.plusHours(TREATMENT_DURATION);

                // Check for time overlap
                if (newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart)) {
                    return true;
                }
            }
        }

        return false;
    }

    // ======== NEW doctor-specific date & slot helpers (for rescheduling) ========

    private LocalDate showCalendarAndSelectDateForDoctor(String doctorId, int duration) {
        LocalDate today = LocalDate.now();
        System.out.println("\nAvailable Dates (Next 14 Days) — Doctor " + doctorId + ":");

        String line = "+------+------------+------------+---------------------+";
        String format = "| %-4s | %-10s | %-10s | %-19s |\n";

        System.out.println(line);
        System.out.printf(format, "No.", "Date", "Day", "Available Slots");
        System.out.println(line);

        LocalDate[] optionDates = new LocalDate[14];
        int optionNumber = 1;

        for (int i = 0; i < 14; i++) {
            LocalDate date = today.plusDays(i);
            int avail = countAvailableSlotsForDoctor(date, duration, doctorId);
            if (avail > 0) {
                String formattedDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String dayName = date.getDayOfWeek().toString();
                System.out.printf(format, optionNumber, formattedDate, dayName, avail + " slot(s)");
                optionDates[optionNumber - 1] = date;
                optionNumber++;
            }
        }

        System.out.println(line);

        if (optionNumber == 1) {
            System.out.println("No available dates in the next 14 days for this doctor.");
            return null;
        }

        while (true) {
            System.out.print("Select date option (or 0 to cancel): ");

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice == 0) return null;
                if (choice >= 1 && choice < optionNumber && optionDates[choice - 1] != null) {
                    return optionDates[choice - 1];
                }
                System.out.println("Invalid selection. Please try again.\n");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.\n");
            }
        }
    }

    private LocalDateTime showAvailableTimeSlotsForDoctorAndSelect(LocalDate date, int duration, Patient patient, String doctorId) {
        System.out.println("\nAvailable Time Slots for Treatment");
        System.out.println("Date: " + date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd (EEEE)")));
        System.out.println("==========================================================");

        String line = "+------+-------------------+----------------+---------+";
        String format = "| %-4s | %-17s | %-14s | %-7s |\n";

        System.out.println(line);
        System.out.printf(format, "No.", "Time Slot", "Doctor", "Room");
        System.out.println(line);

        int slotNumber = 1;
        boolean found = false;
        LocalDateTime now = LocalDateTime.now();

        Doctor theDoctor = findDoctorById(doctorId);
        if (theDoctor == null) {
            System.out.println("Doctor not found.");
            System.out.println(line);
            return null;
        }

        // Map index -> slot time (doctor fixed)
        java.util.List<LocalDateTime> slotStarts = new java.util.ArrayList<>();

        for (int hour = 8; hour <= 22 - duration; hour++) {
            if (hour == 12) continue; // lunch
            LocalDateTime slotStart = LocalDateTime.of(date, LocalTime.of(hour, 0));
            if (slotStart.isBefore(now)) continue;

            if (doctorControl.isDoctorAvailableForAppointment(theDoctor, slotStart, duration, consultations, treatments)) {
                String timeRange = slotStart.format(DateTimeFormatter.ofPattern("HH:mm")) +
                        " - " + slotStart.plusHours(duration).format(DateTimeFormatter.ofPattern("HH:mm"));
                System.out.printf(format, slotNumber, timeRange, "Dr. " + theDoctor.getName(), theDoctor.getRoomNumber());
                slotStarts.add(slotStart);
                slotNumber++;
                found = true;
            }
        }

        System.out.println(line);
        if (!found) {
            System.out.println("No available slots on this date for the selected doctor.");
            return null;
        }

        while (true) {
            System.out.print("Select time slot number (or 0 to cancel): ");

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice == 0) return null;

                if (choice < 1 || choice > slotStarts.size()) {
                    System.out.println("Invalid selection. Please try again.");
                    continue;
                }

                LocalDateTime selectedStart = slotStarts.get(choice - 1);

                // Prevent patient overlap (consultations or treatments)
                if (isPatientTimeClash(patient.getId(), selectedStart, duration)) {
                    System.out.println("Patient already has a consultation or treatment at this time.\n");
                    continue;
                }

                // No side effects here (no new records); just return the chosen slot.
                return selectedStart;

            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    private int countAvailableSlotsForDoctor(LocalDate date, int duration, String doctorId) {
        int count = 0;
        LocalDateTime now = LocalDateTime.now();

        Doctor theDoctor = findDoctorById(doctorId);
        if (theDoctor == null) return 0;

        for (int hour = 8; hour <= 22 - duration; hour++) {
            if (hour == 12) continue; // lunch
            LocalDateTime slotStart = LocalDateTime.of(date, LocalTime.of(hour, 0));
            if (slotStart.isBefore(now)) continue;

            if (doctorControl.isDoctorAvailableForAppointment(theDoctor, slotStart, duration, consultations, treatments)) {
                count++;
            }
        }
        return count;
    }

    private Doctor findDoctorById(String id) {
        ClinicADT.MyIterator<Doctor> it = doctorControl.getAllDoctors().iterator();
        while (it.hasNext()) {
            Doctor d = it.next();
            if (d.getId() != null && d.getId().equalsIgnoreCase(id)) return d;
        }
        return null;
    }
}
