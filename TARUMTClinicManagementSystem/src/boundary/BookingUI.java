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
        patientControl.displayAllPatients();
        String patientId;
        Patient patient;
        while(true){
            System.out.print("\nEnter Patient ID from the list (or 0 to cancel): ");
            patientId = scanner.nextLine().trim();
            
            if (patientId.equals("0")) return;

            patient = patientControl.getPatientById(patientId);

            if (patient != null) break;

            System.out.println("Patient not found. Please try again."); 
        }
        if (!hasValidDiagnosis(patientId)) {
            System.out.println("Cannot add treatment. Patient has no valid consultation diagnosis.");
            return;
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
    }
    
    public boolean hasValidDiagnosis(String patientId) {
        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            if (c.getPatientId().equalsIgnoreCase(patientId)) {
                // if diagnosis is set and not N/A
                if (c.getDiagnosis() != null && !c.getDiagnosis().equalsIgnoreCase("To be diagnosed during appointment")) {
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
            if (hour == 12) continue;
            LocalDateTime slotStart = LocalDateTime.of(date, LocalTime.of(hour, 0));
            if (slotStart.isBefore(now)) continue;

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

                // Check patient clashes
                if (isPatientTimeClash(patient.getId(), selected.time, duration)) {
                    System.out.println("Patient already has a consultation or treatment at this time.\n");
                    continue;
                }
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
                    treatmentControl.addTreatment(new MedicalTreatment(
                        patient.getId(),
                        patient.getName(),
                        selected.doctor.getId(),
                        getDiagnosisByPatientId(patient.getId()),   // have to link with and get from consultation's diagnosis
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
    public String getDiagnosisByPatientId(String patientId) {
        String diagnosis = null;

        ClinicADT.MyIterator<Consultation> iterator = consultations.iterator();
        while (iterator.hasNext()) {
            Consultation c = iterator.next();
            if (c.getPatientId().equalsIgnoreCase(patientId)) {
                diagnosis = c.getDiagnosis();
                break; // stop after finding the first match
            }
        }

        if (diagnosis != null) {
            return diagnosis;
        } else {
            System.out.println("No diagnosis found for patient: " + patientId);
        }
        return diagnosis;
    }


    private boolean hasAvailableSlots(LocalDate date, int duration) {
        LocalDateTime now = LocalDateTime.now();
        ClinicADT.MyIterator<Doctor> doctorIter;

        for (int hour = 8; hour <= 22 - duration; hour++) {
            if (hour == 12) continue;
            LocalDateTime slotStart = LocalDateTime.of(date, LocalTime.of(hour, 0));
            if (slotStart.isBefore(now)) continue;

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
            if (hour == 12) continue;
            LocalDateTime slotStart = LocalDateTime.of(date, LocalTime.of(hour, 0));
            if (slotStart.isBefore(now)) continue;

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

        ClinicADT.MyIterator<Consultation> itC = consultations.iterator();
        while (itC.hasNext()) {
            Consultation c = itC.next();
            if (c.getPatientId().equalsIgnoreCase(patientId)) {
                LocalDateTime existingStart = c.getConsultationDate();
                LocalDateTime existingEnd = existingStart.plusHours(CONSULTATION_DURATION);
                if (newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart)) {
                    return true;
                }
            }
        }

        ClinicADT.MyIterator<MedicalTreatment> itT = treatments.iterator();
        while (itT.hasNext()) {
            MedicalTreatment t = itT.next();
            if (t.getPatientId().equalsIgnoreCase(patientId)) {
                LocalDateTime existingStart = t.getTreatmentDateTime();
                LocalDateTime existingEnd = existingStart.plusHours(TREATMENT_DURATION);
                if (newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart)) {
                    return true;
                }
            }
        }

        return false;
    }
}
