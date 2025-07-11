package tarumtclinicmanagementsystem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Scanner;

public class DoctorControl {
    private ClinicADT<Doctor> doctorList;
    private final String doctorFilePath = "src/textFile/doctor.txt";

    public DoctorControl() {
        doctorList = new MyClinicADT<>();
        loadFromFile(doctorFilePath);
    }

    public void addDoctor(String name, int room, String gender, String icNumber, String phoneNum) {
        if (room < 1 || room > 10) {
            System.out.println("Room must be between 1 and 10.");
            return;
        }

        if (isRoomOccupied(room)) {
            System.out.println("Room already occupied.");
            return;
        }

        DutySchedule schedule = generateWeeklyShift(doctorList.size());
        Doctor doctor = new Doctor(name, room, gender, icNumber, phoneNum, schedule);
        doctorList.add(doctor);

        System.out.println("Doctor registered:");
        System.out.println(doctor);
        
        saveToFile(doctorFilePath);
    }

    private boolean isRoomOccupied(int roomNumber) {
        for (int i = 0; i < doctorList.size(); i++) {
            if (doctorList.get(i).getRoomNumber() == roomNumber) {
                return true;
            }
        }
        return false;
    }

    private DutySchedule generateWeeklyShift(int index) {
        DutySchedule schedule = new DutySchedule();
        DayOfWeek[] days = DayOfWeek.values();
        Session[] shifts = { Session.MORNING, Session.AFTERNOON, Session.NIGHT };

        int rest1 = index % 7;
        int rest2 = (rest1 + 3) % 7;

        for (int i = 0; i < 7; i++) {
            DayOfWeek day = days[i];
            if (i == rest1 || i == rest2) {
                schedule.setDaySession(day, Session.REST);
            } else {
                Session shift = shifts[(index + i) % 3];
                schedule.setDaySession(day, shift);
            }
        }

        return schedule;
    }

    public void displayAllDoctors() {
        if (doctorList.isEmpty()) {
            System.out.println("No doctors available.");
            return;
        }

        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");
        System.out.printf("| %-10s | %-14s | %-6s | %-10s | %-10s | %-16s | %-12s |\n",
                          "Doctor ID", "Name", "Room", "Available", "Gender", "IC Number", "Phone");
        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");

        for (int i = 0; i < doctorList.size(); i++) {
            Doctor doc = doctorList.get(i);
            System.out.printf("| %-10s | %-14s | %-6d | %-10s | %-10s | %-16s | %-12s |\n",
                              doc.getId(), doc.getName(), doc.getRoomNumber(),
                              doc.isAvailable() ? "Yes" : "No",
                              doc.getGender(), doc.getIcNumber(), doc.getPhoneNumber());
        }

        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");
    }

    public void removeDoctorById(String doctorId) {
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor doc = doctorList.get(i);
            if (doc.getId().equalsIgnoreCase(doctorId)) {
                doctorList.remove(i);
                System.out.println("Doctor removed: " + doc.getName() + " (ID: " + doc.getId() + ")");
                saveToFile(doctorFilePath);
                return;
            }
        }
        System.out.println("Doctor ID not found. No doctor removed.");
    }

    public void updateDoctorScheduleById(String doctorId, Scanner scanner) {
        Doctor doctor = getDoctorById(doctorId);
        if (doctor == null) {
            System.out.println("Doctor ID not found.");
            return;
        }

        DutySchedule schedule = doctor.getDutySchedule();
        System.out.println("\nCurrent schedule for Dr. " + doctor.getName() + ":");
        schedule.printScheduleTable(doctor.getName());

        while (true) {
            System.out.print("\nEnter the day to modify (e.g. MONDAY): ");
            String dayInput = scanner.nextLine().trim().toUpperCase();
            DayOfWeek originalDay;
            try {
                originalDay = DayOfWeek.valueOf(dayInput);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid day. Please enter a valid day (e.g., MONDAY).");
                continue;
            }

            System.out.println("Current session on " + originalDay + ": " + schedule.getSessionForDay(originalDay));
            System.out.print("Do you want to retain the session on the same day? (Y/N): ");
            String choice = scanner.nextLine().trim().toUpperCase();

            DayOfWeek targetDay = originalDay;
            if (choice.equals("N")) {
                while (true) {
                    System.out.print("Enter new day to move session to (e.g. TUESDAY): ");
                    String newDayInput = scanner.nextLine().trim().toUpperCase();
                    try {
                        targetDay = DayOfWeek.valueOf(newDayInput);
                        break;
                    } catch (IllegalArgumentException e) {
                        System.out.println("Invalid day. Try again.");
                    }
                }
            }

            Session newSession;
            while (true) {
                System.out.println("Available sessions: MORNING, AFTERNOON, NIGHT, REST");
                System.out.print("Enter new session for " + targetDay + ": ");
                String sessionInput = scanner.nextLine().trim().toUpperCase();
                try {
                    newSession = Session.valueOf(sessionInput);
                    break;
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid session. Try again.");
                }
            }

            // Clear original day session if moved to a new day
            if (!originalDay.equals(targetDay)) {
                schedule.setDaySession(originalDay, Session.REST);
            }

            // Set new session
            schedule.setDaySession(targetDay, newSession);

            System.out.println("\n✅ Schedule updated for Dr. " + doctor.getName() + ":");
            schedule.printScheduleTable(doctor.getName());

            saveToFile(doctorFilePath);

            System.out.print("Do you want to edit another session? (Y/N): ");
            String repeat = scanner.nextLine().trim().toUpperCase();
            if (!repeat.equals("Y")) break;
        }
    }

    public Doctor getDoctorByIndex(int index) {
        if (index >= 0 && index < doctorList.size()) {
            return doctorList.get(index);
        }
        return null;
    }

    public Doctor getDoctorById(String id) {
        for (int i = 0; i < doctorList.size(); i++) {
            if (doctorList.get(i).getId().equalsIgnoreCase(id)) {
                return doctorList.get(i);
            }
        }
        return null;
    }

    public int getDoctorCount() {
        return doctorList.size();
    }

    public void printDoctorsSortedByName() {
        if (doctorList.isEmpty()) {
            System.out.println("No doctors available.");
            return;
        }

        ClinicADT<Doctor> sorted = new MyClinicADT<>();
        for (int i = 0; i < doctorList.size(); i++) {
            sorted.add(doctorList.get(i));
        }

        sorted.sort(Comparator.comparing(Doctor::getName, String::compareToIgnoreCase));

        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");
        System.out.printf("| %-10s | %-14s | %-6s | %-10s | %-10s | %-16s | %-12s |\n",
                          "Doctor ID", "Name", "Room", "Available", "Gender", "IC Number", "Phone");
        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");

        for (int i = 0; i < sorted.size(); i++) {
            Doctor doc = sorted.get(i);
            System.out.printf("| %-10s | %-14s | %-6d | %-10s | %-10s | %-16s | %-12s |\n",
                              doc.getId(), doc.getName(), doc.getRoomNumber(),
                              doc.isAvailable() ? "Yes" : "No",
                              doc.getGender(), doc.getIcNumber(), doc.getPhoneNumber());
        }

        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");
    }
    
    // Fixed: Added separate method for basic availability check
    public boolean isDoctorAvailable(Doctor doctor, LocalDateTime startTime, int durationHours) {
        if (doctor == null) {
            return false;
        }
        
        DayOfWeek dayOfWeek = startTime.getDayOfWeek();
        Session sessionForDay = doctor.getDutySchedule().getSessionForDay(dayOfWeek);
        
        if (sessionForDay == Session.REST) {
            return false;
        }
        
        int hour = startTime.getHour();
        
        // Check if the start time and duration fit within the doctor's session
        switch (sessionForDay) {
            case MORNING:
                return hour >= 8 && (hour + durationHours) <= 12;
            case AFTERNOON:
                return hour >= 12 && (hour + durationHours) <= 18;
            case NIGHT:
                return hour >= 18 && (hour + durationHours) <= 24;
            default:
                return false;
        }
    }

    // Fixed: Renamed method to avoid confusion with the basic availability check
    public boolean isDoctorAvailableForAppointment(
        Doctor doctor,
        LocalDateTime startTime,
        int durationHours,
        ClinicADT<Consultation> consultations,
        ClinicADT<MedicalTreatment> treatments) {

        if (!isDoctorAvailable(doctor, startTime, durationHours)) {
            return false; // Not within working session
        }

        LocalDateTime endTime = startTime.plusHours(durationHours);

        // Check consultations
        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            if (c.getDoctorName().equalsIgnoreCase(doctor.getName())) {
                LocalDateTime cStart = c.getConsultationDate();
                LocalDateTime cEnd = cStart.plusHours(1);
                if (startTime.isBefore(cEnd) && endTime.isAfter(cStart)) {
                    return false; // Overlaps
                }
            }
        }

        // Check treatments
        for (int i = 0; i < treatments.size(); i++) {
            MedicalTreatment t = treatments.get(i);
            if (t.getDoctorId().equalsIgnoreCase(doctor.getId())) {
                LocalDateTime tStart = t.getTreatmentDateTime();
                LocalDateTime tEnd = tStart.plusHours(2);
                if (startTime.isBefore(tEnd) && endTime.isAfter(tStart)) {
                    return false; // Overlaps
                }
            }
        }

        return true; // Available
    }

    public void printAvailableDoctors() {
        boolean found = false;
        System.out.println("=== Available Doctors ===");
        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");
        System.out.printf("| %-10s | %-14s | %-6s | %-10s | %-10s | %-16s | %-12s |\n",
                          "Doctor ID", "Name", "Room", "Available", "Gender", "IC Number", "Phone");
        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");

        for (int i = 0; i < doctorList.size(); i++) {
            Doctor doc = doctorList.get(i);
            if (doc.isAvailable()) {
                System.out.printf("| %-10s | %-14s | %-6d | %-10s | %-10s | %-16s | %-12s |\n",
                                  doc.getId(), doc.getName(), doc.getRoomNumber(),
                                  "Yes", doc.getGender(), doc.getIcNumber(), doc.getPhoneNumber());
                found = true;
            }
        }

        if (!found) {
            System.out.println("|                          No doctors are currently available.                                    |");
        }

        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");
    }
    
    public void printAvailableDoctorsOn(LocalDateTime startTime, int durationHours) {
        System.out.println("=== Available Doctors at " + startTime + " ===");
        boolean found = false;
        
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor doc = doctorList.get(i);
            if (isDoctorAvailable(doc, startTime, durationHours)) {
                System.out.printf("Doctor: %s (%s), Room: %d\n", doc.getName(), doc.getId(), doc.getRoomNumber());
                found = true;
            }
        }
        
        if (!found) {
            System.out.println("No doctors available at the specified time.");
        }
    }

    private void saveToFile(String filePath) {
        try {
            // Create directory if it doesn't exist
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (created) {
                    System.out.println("Created directory: " + parentDir.getAbsolutePath());
                }
            }

            // Write to file
            try (FileWriter writer = new FileWriter(filePath)) {
                for (int i = 0; i < doctorList.size(); i++) {
                    Doctor doc = doctorList.get(i);
                    writer.write(doc.toFileString() + "\n");
                    DutySchedule schedule = doc.getDutySchedule();
                    for (DayOfWeek day : DayOfWeek.values()) {
                        writer.write("  " + day + ": " + schedule.getSessionForDay(day) + "\n");
                    }
                    writer.write("\n");
                }
                System.out.println("✅ Doctor data saved successfully to: " + filePath);
            }
        } catch (IOException e) {
            System.out.println("❌ Error writing to file: " + filePath + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadFromFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("📁 No existing doctor data found at: " + filePath + ". Starting fresh.");
            return;
        }

        try (Scanner fileScanner = new Scanner(file)) {
            Doctor currentDoctor = null;
            DutySchedule currentSchedule = null;
            int loadedCount = 0;

            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                if (line.trim().isEmpty()) {
                    continue;
                }

                if (!line.startsWith("  ")) {
                    // Doctor data line
                    String[] parts = line.split(",", -1);
                    if (parts.length >= 6) {
                        try {
                            String id = parts[0].trim();
                            String name = parts[1].trim();
                            int room = Integer.parseInt(parts[2].trim());
                            String gender = parts[3].trim();
                            String ic = parts[4].trim();
                            String phone = parts[5].trim();

                            currentSchedule = new DutySchedule();
                            currentDoctor = new Doctor(id, name, room, gender, ic, phone, currentSchedule);
                            doctorList.add(currentDoctor);
                            loadedCount++;
                        } catch (NumberFormatException e) {
                            System.out.println("❌ Invalid room number in line: " + line);
                            currentDoctor = null;
                            currentSchedule = null;
                        }
                    } else {
                        System.out.println("❌ Invalid doctor data format in line: " + line);
                        currentDoctor = null;
                        currentSchedule = null;
                    }
                } else if (currentSchedule != null && currentDoctor != null) {
                    // Schedule data line
                    String[] scheduleParts = line.trim().split(":\\s*", 2);
                    if (scheduleParts.length == 2) {
                        try {
                            DayOfWeek day = DayOfWeek.valueOf(scheduleParts[0].trim().toUpperCase());
                            Session session = Session.valueOf(scheduleParts[1].trim().toUpperCase());
                            currentSchedule.setDaySession(day, session);
                        } catch (IllegalArgumentException e) {
                            System.out.println("❌ Invalid schedule entry: " + line);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading from file: " + filePath + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveAllData() {
        saveToFile(doctorFilePath);
    }

    public String getFilePath() {
        return doctorFilePath;
    }

    public boolean fileExists() {
        return new File(doctorFilePath).exists();
    }
} 