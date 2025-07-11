package tarumtclinicmanagementsystem;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class ConsultationControl {
    private ClinicADT<Consultation> consultations;
    private PatientControl patientControl;
    private DoctorControl doctorControl;
    private Scanner sc;
    
    // Working hours for each session
    private static final LocalTime MORNING_START = LocalTime.of(8, 0);
    private static final LocalTime MORNING_END = LocalTime.of(12, 0);
    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
    private static final LocalTime AFTERNOON_END = LocalTime.of(17, 0);
    private static final LocalTime NIGHT_START = LocalTime.of(18, 0);
    private static final LocalTime NIGHT_END = LocalTime.of(22, 0);

    public ConsultationControl(PatientControl patientControl, DoctorControl doctorControl) {
        this.patientControl = patientControl;
        this.doctorControl = doctorControl;
        this.consultations = new MyClinicADT<>();
        this.sc = new Scanner(System.in);
    }

    public void addConsultationFlow() {
        ClinicADT<Patient> allPatients = patientControl.getAllPatients();

        if (allPatients.isEmpty()) {
            System.out.println("❌ No patients registered. Please register a patient first.");
            return;
        }

        System.out.println("\n📋 Registered Patients:");
        displayPatientTable(allPatients);

        System.out.print("Enter Patient ID from the list: ");
        String patientId = sc.nextLine().trim().toUpperCase();

        Patient selectedPatient = patientControl.getPatientById(patientId);
        if (selectedPatient == null) {
            System.out.println("❌ Invalid Patient ID.");
            return;
        }

        LocalDateTime dateTime = getValidDateTime();
        if (dateTime == null) {
            return; // User cancelled or invalid input
        }

        // Get available doctors for the selected time
        List<Doctor> availableDoctors = getAvailableDoctors(dateTime);
        
        if (availableDoctors.isEmpty()) {
            System.out.println("❌ No doctors available at the selected time.");
            System.out.println("Please choose a different time slot.");
            return;
        }

        // Display available doctors
        System.out.println("\n👨‍⚕️ Available Doctors for " + dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + ":");
        displayAvailableDoctors(availableDoctors);

        // Select doctor
        System.out.print("Select Doctor ID: ");
        String doctorId = sc.nextLine().trim().toUpperCase();
        
        Doctor selectedDoctor = null;
        for (Doctor doc : availableDoctors) {
            if (doc.getId().equals(doctorId)) {
                selectedDoctor = doc;
                break;
            }
        }

        if (selectedDoctor == null) {
            System.out.println("❌ Invalid Doctor ID or doctor not available at this time.");
            return;
        }

        // Check if doctor is already booked for this time slot
        if (isDoctorBooked(selectedDoctor.getName(), dateTime)) {
            System.out.println("❌ Doctor " + selectedDoctor.getName() + " is already booked for this time slot.");
            return;
        }

        addConsultation(selectedPatient.getName(), selectedDoctor.getName(), dateTime);
    }

    private LocalDateTime getValidDateTime() {
        while (true) {
            try {
                System.out.print("Enter date and time (yyyy-MM-dd HH:mm): ");
                String input = sc.nextLine().trim();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime dateTime = LocalDateTime.parse(input, formatter);
                
                // Check if the time is in the past
                if (dateTime.isBefore(LocalDateTime.now())) {
                    System.out.println("❌ Cannot schedule consultation in the past.");
                    continue;
                }
                
                // Check if the time is during working hours
                if (!isWorkingHours(dateTime.toLocalTime())) {
                    System.out.println("❌ Time is outside working hours.");
                    System.out.println("Working hours: 08:00-12:00 (Morning), 13:00-17:00 (Afternoon), 18:00-22:00 (Night)");
                    continue;
                }
                
                return dateTime;
            } catch (Exception e) {
                System.out.println("❌ Invalid format. Use 'yyyy-MM-dd HH:mm'.");
            }
        }
    }

    private boolean isWorkingHours(LocalTime time) {
        return (time.isAfter(MORNING_START.minusMinutes(1)) && time.isBefore(MORNING_END)) ||
               (time.isAfter(AFTERNOON_START.minusMinutes(1)) && time.isBefore(AFTERNOON_END)) ||
               (time.isAfter(NIGHT_START.minusMinutes(1)) && time.isBefore(NIGHT_END));
    }

    private Session getSessionForTime(LocalTime time) {
        if (time.isAfter(MORNING_START.minusMinutes(1)) && time.isBefore(MORNING_END)) {
            return Session.MORNING;
        } else if (time.isAfter(AFTERNOON_START.minusMinutes(1)) && time.isBefore(AFTERNOON_END)) {
            return Session.AFTERNOON;
        } else if (time.isAfter(NIGHT_START.minusMinutes(1)) && time.isBefore(NIGHT_END)) {
            return Session.NIGHT;
        }
        return null;
    }

    private List<Doctor> getAvailableDoctors(LocalDateTime dateTime) {
        List<Doctor> availableDoctors = new ArrayList<>();
        Session requiredSession = getSessionForTime(dateTime.toLocalTime());
        
        if (requiredSession == null) {
            return availableDoctors; // No session for this time
        }

        for (int i = 0; i < doctorControl.getDoctorCount(); i++) {
            Doctor doctor = doctorControl.getDoctorByIndex(i);
            if (doctor != null) {
                DutySchedule schedule = doctor.getDutySchedule();
                Session doctorSession = schedule.getSessionForDay(dateTime.getDayOfWeek());
                
                if (doctorSession == requiredSession) {
                    availableDoctors.add(doctor);
                }
            }
        }
        
        return availableDoctors;
    }

    private void displayAvailableDoctors(List<Doctor> doctors) {
        System.out.println("+------------+----------------+--------+------------+------------------+");
        System.out.printf("| %-10s | %-14s | %-6s | %-10s | %-16s |\n",
                          "Doctor ID", "Name", "Room", "Gender", "Phone");
        System.out.println("+------------+----------------+--------+------------+------------------+");

        for (Doctor doc : doctors) {
            System.out.printf("| %-10s | %-14s | %-6d | %-10s | %-16s |\n",
                              doc.getId(), doc.getName(), doc.getRoomNumber(),
                              doc.getGender(), doc.getPhoneNumber());
        }

        System.out.println("+------------+----------------+--------+------------+------------------+");
    }

    private void displayPatientTable(ClinicADT<Patient> patients) {
        System.out.printf("+------------+----------------------+-----+--------+--------------+\n");
        System.out.printf("| %-10s | %-20s | %-3s | %-6s | %-12s |\n", "Patient ID", "Name", "Age", "Gender", "Contact");
        System.out.printf("+------------+----------------------+-----+--------+--------------+\n");

        for (int i = 0; i < patients.size(); i++) {
            Patient p = patients.get(i);
            System.out.printf("| %-10s | %-20s | %-3d | %-6s | %-12s |\n",
                    p.getId(), p.getName(), p.getAge(), p.getGender(), p.getContact());
        }

        System.out.printf("+------------+----------------------+-----+--------+--------------+\n");
    }

    private boolean isDoctorBooked(String doctorName, LocalDateTime dateTime) {
        LocalDateTime consultationEnd = dateTime.plusHours(1); // Each consultation is 1 hour
        
        for (int i = 0; i < consultations.size(); i++) {
            Consultation consultation = consultations.get(i);
            if (consultation.getDoctorName().equalsIgnoreCase(doctorName)) {
                LocalDateTime existingStart = consultation.getConsultationDate();
                LocalDateTime existingEnd = existingStart.plusHours(1);
                
                // Check for time overlap
                if (dateTime.isBefore(existingEnd) && consultationEnd.isAfter(existingStart)) {
                    return true; // Time slot is already booked
                }
            }
        }
        return false;
    }

    public void addConsultation(String patient, String doctor, LocalDateTime date) {
        Consultation c = new Consultation(patient, doctor, date);
        consultations.add(c);
        saveConsultationToFile(c);
        System.out.println("✅ Consultation added successfully!");
        System.out.println("📋 " + c);
        System.out.println("⏰ Duration: 1 hour (ends at " + 
                         date.plusHours(1).format(DateTimeFormatter.ofPattern("HH:mm")) + ")");
    }

    private void saveConsultationToFile(Consultation c) {
        try (FileWriter fw = new FileWriter("consultations.txt", true)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            fw.write(c.getId() + "," + c.getPatientName() + "," + c.getDoctorName() + "," + 
                    c.getConsultationDate().format(formatter) + "\n");
        } catch (IOException e) {
            System.out.println("❌ Error saving consultation: " + e.getMessage());
        }
    }

    public boolean removeConsultationById(int id) {
        for (int i = 0; i < consultations.size(); i++) {
            if (consultations.get(i).getId() == id) {
                Consultation removed = consultations.get(i);
                consultations.remove(i);
                System.out.println("✅ Consultation removed: " + removed);
                return true;
            }
        }
        System.out.println("❌ Consultation not found.");
        return false;
    }

    public void listConsultations() {
        if (consultations.isEmpty()) {
            System.out.println("No consultations scheduled.");
            return;
        }

        System.out.println("\n=== All Consultations ===");
        System.out.printf("%-5s | %-20s | %-20s | %-20s | %-10s\n", "ID", "Patient", "Doctor", "Date & Time", "Duration");
        System.out.println("--------------------------------------------------------------------------------");

        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            System.out.printf("%-5d | %-20s | %-20s | %-20s | %-10s\n",
                    c.getId(), c.getPatientName(), c.getDoctorName(),
                    c.getConsultationDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    "1 hour");
        }
    }

    public void searchByPatient(String patientName) {
        boolean found = false;
        System.out.println("\n=== Consultations for Patient: " + patientName + " ===");
        
        for (int i = 0; i < consultations.size(); i++) {
            if (consultations.get(i).getPatientName().equalsIgnoreCase(patientName)) {
                Consultation c = consultations.get(i);
                System.out.println("📋 " + c);
                System.out.println("   Duration: 1 hour (ends at " + 
                                 c.getConsultationDate().plusHours(1).format(DateTimeFormatter.ofPattern("HH:mm")) + ")");
                found = true;
            }
        }
        
        if (!found) {
            System.out.println("❌ No consultations found for patient: " + patientName);
        }
    }

    public void searchByDoctor(String doctorName) {
        boolean found = false;
        System.out.println("\n=== Consultations for Doctor: " + doctorName + " ===");
        
        for (int i = 0; i < consultations.size(); i++) {
            if (consultations.get(i).getDoctorName().equalsIgnoreCase(doctorName)) {
                Consultation c = consultations.get(i);
                System.out.println("📋 " + c);
                System.out.println("   Duration: 1 hour (ends at " + 
                                 c.getConsultationDate().plusHours(1).format(DateTimeFormatter.ofPattern("HH:mm")) + ")");
                found = true;
            }
        }
        
        if (!found) {
            System.out.println("❌ No consultations found for doctor: " + doctorName);
        }
    }

    public void printConsultationsSortedByDate() {
        if (consultations.isEmpty()) {
            System.out.println("No consultations to sort.");
            return;
        }

        ClinicADT<Consultation> sorted = new MyClinicADT<>();
        for (int i = 0; i < consultations.size(); i++) {
            sorted.add(consultations.get(i));
        }

        sorted.sort(Comparator.comparing(Consultation::getConsultationDate));

        System.out.println("\n=== Consultations (Sorted by Date) ===");
        System.out.printf("%-5s | %-20s | %-20s | %-20s | %-10s\n", "ID", "Patient", "Doctor", "Date & Time", "Duration");
        System.out.println("--------------------------------------------------------------------------------");

        for (int i = 0; i < sorted.size(); i++) {
            Consultation c = sorted.get(i);
            System.out.printf("%-5d | %-20s | %-20s | %-20s | %-10s\n",
                    c.getId(), c.getPatientName(), c.getDoctorName(),
                    c.getConsultationDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    "1 hour");
        }
    }

    public void showDoctorScheduleForDate(LocalDateTime date) {
        System.out.println("\n=== Doctor Availability for " + 
                         date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd (EEEE)")) + " ===");
        
        List<Doctor> morningDoctors = getAvailableDoctorsForSession(date, Session.MORNING);
        List<Doctor> afternoonDoctors = getAvailableDoctorsForSession(date, Session.AFTERNOON);
        List<Doctor> nightDoctors = getAvailableDoctorsForSession(date, Session.NIGHT);

        System.out.println("\n🌅 Morning Shift (08:00-12:00):");
        if (morningDoctors.isEmpty()) {
            System.out.println("   No doctors available");
        } else {
            morningDoctors.forEach(doc -> System.out.println("   " + doc.getName() + " (Room " + doc.getRoomNumber() + ")"));
        }

        System.out.println("\n🌞 Afternoon Shift (13:00-17:00):");
        if (afternoonDoctors.isEmpty()) {
            System.out.println("   No doctors available");
        } else {
            afternoonDoctors.forEach(doc -> System.out.println("   " + doc.getName() + " (Room " + doc.getRoomNumber() + ")"));
        }

        System.out.println("\n🌙 Night Shift (18:00-22:00):");
        if (nightDoctors.isEmpty()) {
            System.out.println("   No doctors available");
        } else {
            nightDoctors.forEach(doc -> System.out.println("   " + doc.getName() + " (Room " + doc.getRoomNumber() + ")"));
        }
    }

    private List<Doctor> getAvailableDoctorsForSession(LocalDateTime date, Session session) {
        List<Doctor> sessionDoctors = new ArrayList<>();
        
        for (int i = 0; i < doctorControl.getDoctorCount(); i++) {
            Doctor doctor = doctorControl.getDoctorByIndex(i);
            if (doctor != null) {
                DutySchedule schedule = doctor.getDutySchedule();
                Session doctorSession = schedule.getSessionForDay(date.getDayOfWeek());
                
                if (doctorSession == session) {
                    sessionDoctors.add(doctor);
                }
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
}