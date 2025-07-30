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
                     ConsultationControl consultationControl, TreatmentControl treatmentControl ) {
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

        System.out.print("\nEnter Patient ID from the list: ");
        String patientId = scanner.nextLine().trim();
        Patient patient = patientControl.getPatientById(patientId);

        if (patient == null) {
            System.out.println("Patient not found.");
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

    private LocalDate showCalendarAndSelectDate(int duration) {
        LocalDate today = LocalDate.now();
        System.out.println("\nAvailable Dates (Next 14 Days):");

        String line = "+------+------------+------------+---------------------+";
        String format = "| %-4s | %-10s | %-10s | %-19s |\n";

        System.out.println(line);
        System.out.printf(format, "No.", "Date", "Day", "Available Slots");
        System.out.println(line);

        LocalDate[] optionDates = new LocalDate[14]; // max 14 entries
        int optionNumber = 1;

        for (int i = 0; i < 14; i++) {
            LocalDate date = today.plusDays(i);
            if (hasAvailableSlots(date, duration)) {
                String formattedDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String dayName = date.getDayOfWeek().toString(); // keep UPPERCASE
                int availableSlots = countAvailableSlots(date, duration);

                System.out.printf(format, optionNumber, formattedDate, dayName, availableSlots + " slot(s)");
                optionDates[optionNumber - 1] = date; // store at index (optionNumber - 1)
                optionNumber++;
            }
        }

        System.out.println(line);

        if (optionNumber == 1) {
            System.out.println("No available dates in the next 14 days.");
            return null;
        }

        System.out.print("Select date option: ");

        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());

            if (choice >= 1 && choice < optionNumber && optionDates[choice - 1] != null) {
                return optionDates[choice - 1];
            } else {
                System.out.println("Invalid selection.");
                return null;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
            return null;
        }
    }

    private LocalDateTime showAvailableTimeSlotsAndSelect(LocalDate date, int duration, Patient patient, boolean isConsultation, String serviceType) {
        System.out.println("\n?Available Time Slots for " + serviceType);
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

        for (int hour = 8; hour <= 22 - duration; hour++) {
            if (hour == 12) continue;

            LocalDateTime slotStart = LocalDateTime.of(date, LocalTime.of(hour, 0));
            if (slotStart.isBefore(now)) continue;

            for (int i = 0; i < doctorControl.getDoctorCount(); i++) {
                Doctor doctor = doctorControl.getDoctorByIndex(i);
                if (doctor != null && doctorControl.isDoctorAvailableForAppointment(doctor, slotStart, duration, consultations, treatments)) {
                    String timeRange = slotStart.format(DateTimeFormatter.ofPattern("HH:mm")) +
                            " - " + slotStart.plusHours(duration).format(DateTimeFormatter.ofPattern("HH:mm"));
                    System.out.printf(format, slotNumber, timeRange, "Dr. " + doctor.getName(), doctor.getRoomNumber());
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

        System.out.print("Select time slot number: ");

        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            slotNumber = 1;
            LocalDateTime selectedSlot = null;
            Doctor selectedDoctor = null;

            for (int hour = 8; hour <= 22 - duration; hour++) {
                if (hour == 12) continue;

                LocalDateTime slotStart = LocalDateTime.of(date, LocalTime.of(hour, 0));
                if (slotStart.isBefore(LocalDateTime.now())) continue;

                for (int i = 0; i < doctorControl.getDoctorCount(); i++) {
                    Doctor doctor = doctorControl.getDoctorByIndex(i);
                    if (doctor != null && doctorControl.isDoctorAvailableForAppointment(doctor, slotStart, duration, consultations, treatments)) {
                        if (slotNumber == choice) {
                            selectedSlot = slotStart;
                            selectedDoctor = doctor;
                            break;
                        }
                        slotNumber++;
                    }
                }
                if (selectedSlot != null) break;
            }

            if (selectedSlot == null || selectedDoctor == null) {
                System.out.println("Invalid selection or slot no longer available.");
                return null;
            }

            if (isConsultation) {
                if (isPatientTimeClash(patient.getId(), selectedSlot, CONSULTATION_DURATION)) {
                    System.out.println("Patient already has a consultation or treatment at this time.");
                    return null;
                }

                consultationControl.addConsultation(patient.getId(), patient.getName(), selectedDoctor.getName(), selectedDoctor.getId(), selectedSlot);
            } else {
                if (isPatientTimeClash(patient.getId(), selectedSlot, TREATMENT_DURATION)) {
                    System.out.println("Patient already has a consultation or treatment at this time.");
                    return null;
                }

                treatmentControl.addTreatment(new MedicalTreatment(
                        patient.getId(),
                        patient.getName(),
                        selectedDoctor.getId(),
                        "To be diagnosed during appointment",
                        "To be prescribed during appointment",
                        selectedSlot,
                        false
                ));
            }

            System.out.println("Booked with Dr. " + selectedDoctor.getName() + " in Room " + selectedDoctor.getRoomNumber());
            return selectedSlot;

        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
            return null;
        }
    }

    private boolean hasAvailableSlots(LocalDate date, int duration) {
        for (int hour = 8; hour <= 22 - duration; hour++) {
            if (hour == 12) continue;
            LocalDateTime slotStart = LocalDateTime.of(date, LocalTime.of(hour, 0));
            if (slotStart.isBefore(LocalDateTime.now())) continue;

            for (int i = 0; i < doctorControl.getDoctorCount(); i++) {
                Doctor doctor = doctorControl.getDoctorByIndex(i);
                if (doctor != null && doctorControl.isDoctorAvailableForAppointment(doctor, slotStart, duration, consultations, treatments)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int countAvailableSlots(LocalDate date, int duration) {
        int count = 0;
        for (int hour = 8; hour <= 22 - duration; hour++) {
            if (hour == 12) continue;
            LocalDateTime slotStart = LocalDateTime.of(date, LocalTime.of(hour, 0));
            if (slotStart.isBefore(LocalDateTime.now())) continue;

            for (int i = 0; i < doctorControl.getDoctorCount(); i++) {
                Doctor doctor = doctorControl.getDoctorByIndex(i);
                if (doctor != null && doctorControl.isDoctorAvailableForAppointment(doctor, slotStart, duration, consultations, treatments)) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean isPatientTimeClash(String patientId, LocalDateTime newStart, int duration) {
        LocalDateTime newEnd = newStart.plusHours(duration);

        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            if (c.getPatientId().equalsIgnoreCase(patientId)) {
                LocalDateTime existingStart = c.getConsultationDate();
                LocalDateTime existingEnd = existingStart.plusHours(CONSULTATION_DURATION);
                if (newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart)) {
                    return true;
                }
            }
        }

        for (int i = 0; i < treatments.size(); i++) {
            MedicalTreatment t = treatments.get(i);
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