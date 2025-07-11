    package tarumtclinicmanagementsystem;

import java.io.BufferedReader;
import java.io.FileReader;
    import java.io.FileWriter;
    import java.io.IOException;
    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.time.LocalTime;
    import java.time.format.DateTimeFormatter;
    import java.util.Comparator;
    import java.util.Scanner;

    public class ConsultationControl {
        private ClinicADT<Consultation> consultations;
        private PatientControl patientControl;
        private DoctorControl doctorControl;
        private Scanner sc;
        private ClinicADT<MedicalTreatment> treatments;
        private String consultationFilePath = "src/textFile/consultations.txt";

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

            // ✅ Check if patient already booked at that time
            if (isPatientAlreadyBooked(selectedPatient.getId(), dateTime.toLocalDate(), true)) {
                System.out.println("Patient already has a consultation booked at that time.");
                return;
            }

            Doctor selectedDoctor = selectAvailableDoctor(dateTime);
            if (selectedDoctor == null) return;

            addConsultation(selectedPatient.getId(),selectedPatient.getName(),selectedDoctor.getName(),dateTime
            );
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

            for (int i = 0; i < availableDoctors.size(); i++) {
                Doctor doc = availableDoctors.get(i);
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

            for (int i = 0; i < doctorControl.getDoctorCount(); i++) {
                Doctor doctor = doctorControl.getDoctorByIndex(i);
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

            for (int i = 0; i < doctors.size(); i++) {
                Doctor doc = doctors.get(i);
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

            for (int i = 0; i < patients.size(); i++) {
                Patient p = patients.get(i);
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

            for (int i = 0; i < consultations.size(); i++) {
                Consultation consultation = consultations.get(i);
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

        public void addConsultation(String patientId, String patientName, String doctorName, LocalDateTime date) {
            
        Consultation consultation = new Consultation(patientId, patientName, doctorName, date);
        consultations.add(consultation);
        saveConsultationToFile(consultation);
        displayConsultationConfirmation(consultation);
    }


        private void displayConsultationConfirmation(Consultation consultation) {
            System.out.println("Consultation added successfully!");
            System.out.println(consultation);
            System.out.println("Duration: " + CONSULTATION_DURATION + " hour (ends at " + 
                             consultation.getConsultationDate().plusHours(CONSULTATION_DURATION)
                             .format(DateTimeFormatter.ofPattern("HH:mm")) + ")");
        }

        private void saveConsultationToFile(Consultation consultation) {
            try (FileWriter fw = new FileWriter(consultationFilePath, true)) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                String line = String.format("%d,%s,%s,%s,%s%n",
                        consultation.getId(),
                        consultation.getPatientId(),
                        consultation.getPatientName(),
                        consultation.getDoctorName(),
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
            System.out.println("Consultation not found.");
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

            for (int i = 0; i < consultations.size(); i++) {
                Consultation c = consultations.get(i);
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

            for (int i = 0; i < consultations.size(); i++) {
                Consultation c = consultations.get(i);
                if (c.getPatientName().equalsIgnoreCase(patientName)) {
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

        public void searchByDoctor(String doctorName) {
            ClinicADT<Consultation> found = new MyClinicADT<>();

            for (int i = 0; i < consultations.size(); i++) {
                Consultation c = consultations.get(i);
                if (c.getDoctorName().equalsIgnoreCase(doctorName)) {
                    found.add(c);
                }
            }

            if (found.isEmpty()) {
                System.out.println("❌ No consultations found for doctor: " + doctorName);
                return;
            }

            System.out.println("\n=== Consultations for Doctor: " + doctorName + " ===");
            displayConsultationDetails(found);
        }

        private ClinicADT<Consultation> SearchByPatient(String patientName) {
            ClinicADT<Consultation> found = new MyClinicADT<>();

            for (int i = 0; i < consultations.size(); i++) {
                Consultation c = consultations.get(i);
                if (c.getPatientName().equalsIgnoreCase(patientName)) {
                    found.add(c);
                }
            }

            return found;
        }

        private ClinicADT<Consultation> findConsultationsByDoctor(String doctorName) {
            ClinicADT<Consultation> found = new MyClinicADT<>();

            for (int i = 0; i < consultations.size(); i++) {
                Consultation c = consultations.get(i);
                if (c.getDoctorName().equalsIgnoreCase(doctorName)) {
                    found.add(c);
                }
            }

            return found;
        }

       private void displayConsultationDetails(ClinicADT<Consultation> consultations) {
            String format = "| %-4s | %-20s | %-15s | %-16s | %-8s | %-8s | %-8s |\n";
            String line = "+------+----------------------+-----------------+------------------+----------+----------+----------+";

            System.out.println(line);
            System.out.printf(format, "ID", "Patient", "Doctor", "Date", "Start", "End", "Duration");
            System.out.println(line);

            for (int i = 0; i < consultations.size(); i++) {
                Consultation c = consultations.get(i);
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

            ClinicADT<Consultation> sorted = new MyClinicADT<>();
            for (int i = 0; i < consultations.size(); i++) {
                sorted.add(consultations.get(i));
            }

            sorted.sort(Comparator.comparing(Consultation::getConsultationDate));

            System.out.println("\n=== Consultations (Sorted by Date) ===");
            displaySortedConsultations(sorted);
        }

        private void displaySortedConsultations(ClinicADT<Consultation> sorted) {
            String format = "| %-4s | %-20s | %-20s | %-20s | %-9s |\n";
            String line = "+------+----------------------+----------------------+----------------------+-----------+";

            System.out.println(line);
            System.out.printf(format, "ID", "Patient", "Doctor", "Date & Time", "Duration");
            System.out.println(line);

            for (int i = 0; i < sorted.size(); i++) {
                Consultation c = sorted.get(i);
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
                for (int i = 0; i < sessionDoctors.size(); i++) {
                    Doctor doc = sessionDoctors.get(i);
                    System.out.printf(format, doc.getName(), doc.getRoomNumber());
                }
            }

            System.out.println(line);
        }

        private ClinicADT<Doctor> getAvailableDoctorsForSession(LocalDateTime date, Session session) {
            ClinicADT<Doctor> sessionDoctors = new MyClinicADT<>();

            for (int i = 0; i < doctorControl.getDoctorCount(); i++) {
                Doctor doctor = doctorControl.getDoctorByIndex(i);
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

            for (int i = 0; i < consultations.size(); i++) {
                Consultation c = consultations.get(i);
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

            for (int i = 0; i < consultations.size(); i++) {
                Consultation c = consultations.get(i);
                String patientId = c.getPatientId(); // use ID for uniqueness

                // Check if already added
                boolean exists = false;
                for (int j = 0; j < result.size(); j++) {
                    if (result.get(j).getId().equalsIgnoreCase(patientId)) {
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
        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            if (c.getPatientId().equalsIgnoreCase(patientId) &&
                c.getConsultationDate().toLocalDate().equals(selectedDate)) {
                consultationCount++;
            }
        }

        // Check treatments
        if (treatments != null) {
            for (int i = 0; i < treatments.size(); i++) {
                MedicalTreatment t = treatments.get(i);
                if (t.getPatientId().equalsIgnoreCase(patientId) &&
                    t.getTreatmentDateTime().toLocalDate().equals(selectedDate)) {
                    treatmentCount++;
                }
            }
        }

        // Rules
        if (isConsultation && (consultationCount > 0 || treatmentCount > 0)) {
            return true;
        }
        if (!isConsultation && (consultationCount > 0 || treatmentCount > 0)) {
            return true;
        }
        return false;
    }
    
    public void loadConsultationsFromFile() {
        consultations.clear(); 
        try (BufferedReader br = new BufferedReader(new FileReader(consultationFilePath))) {
            String line;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            while ((line = br.readLine()) != null) {
                // Assuming file format per line:
                // consultationId,patientName,doctorName,consultationDateTime (yyyy-MM-dd HH:mm)
                // But you want to also save patientId, so change file format to:
                // consultationId,patientId,patientName,doctorName,consultationDateTime
                // Adjust accordingly.

                String[] parts = line.split(",");
                if (parts.length < 5) {
                    System.out.println("Invalid line in consultations file: " + line);
                    continue;
                }

                int consultationId = Integer.parseInt(parts[0].trim());
                String patientId = parts[1].trim();
                String patientName = parts[2].trim();
                String doctorName = parts[3].trim();
                LocalDateTime date = LocalDateTime.parse(parts[4].trim(), formatter);

                Consultation consultation = new Consultation(consultationId, patientId, patientName, doctorName, date);
                consultations.add(consultation);
            }
        } catch (IOException e) {
            System.out.println("Error loading consultations: " + e.getMessage());
        }
    }  
}   