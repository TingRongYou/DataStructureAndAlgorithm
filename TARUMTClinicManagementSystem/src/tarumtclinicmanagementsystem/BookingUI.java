package tarumtclinicmanagementsystem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class BookingUI {
    private static final int CONSULTATION_DURATION = 1;
    private static final int TREATMENT_DURATION = 2;

    private final PatientControl patientControl;
    private final DoctorControl doctorControl;
    private final ClinicADT<Consultation> consultations;
    private final ClinicADT<MedicalTreatment> treatments;
    private final ConsultationControl consultationControl;
    private final Scanner scanner;

    public BookingUI(PatientControl patientControl, DoctorControl doctorControl,
                     ClinicADT<Consultation> consultations, ClinicADT<MedicalTreatment> treatments,
                     ConsultationControl consultationControl) {
        this.patientControl = patientControl;
        this.doctorControl = doctorControl;
        this.consultations = consultations;
        this.treatments = treatments;
        this.consultationControl = consultationControl;
        this.scanner = new Scanner(System.in);
    }

    public void run(boolean isConsultation) {
        System.out.println("\nRegistered Patients:");
        patientControl.displayAllPatients();

        System.out.print("\nEnter Patient ID from the list: ");
        String patientId = scanner.nextLine().trim();
        Patient patient = patientControl.getPatientById(patientId);

        if (patient == null) {
            System.out.println("❌ Patient not found.");
            return;
        }

        int duration = isConsultation ? CONSULTATION_DURATION : TREATMENT_DURATION;
        String serviceType = isConsultation ? "Consultation" : "Medical Treatment";

        LocalDate selectedDate = showCalendarAndSelectDate(duration);
        if (selectedDate == null) return;

        LocalDateTime chosenSlot = showAvailableTimeSlotsAndSelect(selectedDate, duration, patient, isConsultation, serviceType);
        if (chosenSlot == null) return;

        System.out.println("\n" + serviceType + " successfully booked!");
        System.out.println(" Booking Details:");
        System.out.println("   Patient: " + patient.getName() + " (" + patient.getId() + ")");
        System.out.println("   Service: " + serviceType);
        System.out.println("   Date & Time: " + chosenSlot.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        System.out.println("   Duration: " + duration + " hour(s)");
    }

    private LocalDate showCalendarAndSelectDate(int duration) {
        LocalDate today = LocalDate.now();
        System.out.println("\nAvailable Dates (Next 14 days):");
        System.out.println("==================================");

        boolean hasAvailableDates = false;
        int optionNumber = 1;

        for (int i = 0; i < 14; i++) {
            LocalDate date = today.plusDays(i);
            if (hasAvailableSlots(date, duration)) {
                String dayName = date.getDayOfWeek().toString();
                String formattedDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                int availableSlots = countAvailableSlots(date, duration);

                System.out.printf("%2d. %s (%s) - %d slot(s) available\n",
                        optionNumber, formattedDate, dayName, availableSlots);
                hasAvailableDates = true;
                optionNumber++;
            }
        }

        if (!hasAvailableDates) {
            System.out.println("No available dates in the next 14 days.");
            return null;
        }

        System.out.println("====================================");
        System.out.print("Select date option: ");

        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());

            optionNumber = 1;
            for (int i = 0; i < 14; i++) {
                LocalDate date = today.plusDays(i);
                if (hasAvailableSlots(date, duration)) {
                    if (optionNumber == choice) {
                        return date;
                    }
                    optionNumber++;
                }
            }

            System.out.println("Invalid selection.");
            return null;
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
            return null;
        }
    }

    private LocalDateTime showAvailableTimeSlotsAndSelect(LocalDate date, int duration, Patient patient, boolean isConsultation, String serviceType) {
        System.out.println("\nAvailable Time Slots for " + serviceType);
        System.out.println("Date: " + date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd (EEEE)")));
        System.out.println("==========================================================");

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
                    System.out.printf("%2d. %s | Dr. %s | Room %d\n",
                            slotNumber, timeRange, doctor.getName(), doctor.getRoomNumber());
                    slotNumber++;
                    found = true;
                }
            }
        }

        if (!found) {
            System.out.println("No available slots on this date.");
            return null;
        }

        System.out.println("========================================================");
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
                Consultation c = new Consultation(patient.getId(), patient.getName(), selectedDoctor.getName(), selectedSlot);
                consultations.add(c);
            } else {
                if (isPatientTimeClash(patient.getId(), selectedSlot, TREATMENT_DURATION)) {
                    System.out.println("Patient already has a consultation or treatment at this time.");
                    return null;
                }
                treatments.add(new MedicalTreatment(
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
