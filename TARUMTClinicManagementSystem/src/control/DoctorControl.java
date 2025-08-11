package control;

import adt.ClinicADT;
import adt.MyClinicADT;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Scanner;
import java.util.Iterator;
import entity.Consultation;
import entity.Doctor;
import tarumtclinicmanagementsystem.DutySchedule;
import entity.MedicalTreatment;
import tarumtclinicmanagementsystem.Session;
import static tarumtclinicmanagementsystem.Session.AFTERNOON;
import static tarumtclinicmanagementsystem.Session.MORNING;
import static tarumtclinicmanagementsystem.Session.NIGHT;

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

    public boolean checkRoomAvailability(int room) {
        return !isRoomOccupied(room);
    }

    private boolean isRoomOccupied(int roomNumber) {
        for (Doctor doc : doctorList) {
            if (doc.getRoomNumber() == roomNumber) {
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
    
    public ClinicADT<Doctor> getAllDoctors() {
        return doctorList;
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

        Iterator<Doctor> iterator = doctorList.iterator();
        while (iterator.hasNext()) {
            Doctor doc = iterator.next();
            System.out.printf("| %-10s | %-14s | %-6d | %-10s | %-10s | %-16s | %-12s |\n",
                              doc.getId(), doc.getName(), doc.getRoomNumber(),
                              doc.isAvailable() ? "Yes" : "No",
                              doc.getGender(), doc.getIcNumber(), doc.getPhoneNumber());
        }

        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");
    }   

    public void removeDoctorById(String doctorId) {
        Iterator<Doctor> iterator = doctorList.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            Doctor doc = iterator.next();
            if (doc.getId().equalsIgnoreCase(doctorId)) {
                doctorList.remove(index);
                System.out.println("Doctor removed: " + doc.getName() + " (ID: " + doc.getId() + ")");
                saveToFile(doctorFilePath);
                return;
            }
            index++;
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

        while (true) {
            System.out.println("\nCurrent schedule for Dr. " + doctor.getName() + ":");
            schedule.printScheduleTable(doctor.getName());

            // Ask for day
            System.out.print("\nEnter the day to modify (e.g. MONDAY) or 0 to cancel: ");
            String dayInput = scanner.nextLine().trim().toUpperCase();
            if (dayInput.equals("0")) {
                System.out.println("Update cancelled.");
                return;
            }

            DayOfWeek day;
            try {
                day = DayOfWeek.valueOf(dayInput);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid day. Try again.\n");
                continue;
            }

            // Show current session
            System.out.println("Current session on " + day + ": " + schedule.getSessionForDay(day));

            // Confirm change
            String choice;
            while (true) {
                System.out.print(String.format("Do you want to make changes on %s? (Y/N or 0 to cancel): ", day));
                choice = scanner.nextLine().trim().toUpperCase();
                if (choice.equals("Y") || choice.equals("N") || choice.equals("0")) break;
                System.out.println("Invalid input. Please enter Y, N, or 0.\n");
            }

            if (choice.equals("0")) {
                System.out.println("Update cancelled.");
                return;
            }
            if (choice.equals("N")) continue; // skip to next day

            // Ask for new session
            Session newSession;
            while (true) {
                System.out.println("Available sessions: MORNING, AFTERNOON, NIGHT, REST (or 0 to cancel)");
                System.out.print("Enter new session for " + day + ": ");
                String sessionInput = scanner.nextLine().trim().toUpperCase();
                if (sessionInput.equals("0")) return;
                try {
                    newSession = Session.valueOf(sessionInput);
                    break;
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid session. Try again.\n");
                }
            }

            // Update schedule
            schedule.setDaySession(day, newSession);
            saveToFile(doctorFilePath);
            System.out.println("\nSchedule updated for Dr. " + doctor.getName() + ":");
            schedule.printScheduleTable(doctor.getName());

            // Ask to repeat
            System.out.print("Edit another session? (Y/N): ");
            if (!scanner.nextLine().trim().equalsIgnoreCase("Y")) break;
        }
    }

    public Doctor getDoctorByIndex(int index) {
        if (index >= 0 && index < doctorList.size()) {
            return doctorList.get(index);
        }
        return null;
    }

    public Doctor getDoctorById(String id) {
        for (Doctor doc : doctorList) {
            if (doc.getId().equalsIgnoreCase(id)) {
                return doc;
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
        for (Doctor doc : doctorList) {
            sorted.add(doc);
        }

        sorted.sort(Comparator.comparing(Doctor::getName, String::compareToIgnoreCase));

        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");
        System.out.printf("| %-10s | %-14s | %-6s | %-10s | %-10s | %-16s | %-12s |\n",
                          "Doctor ID", "Name", "Room", "Available", "Gender", "IC Number", "Phone");
        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");

        for (Doctor doc : sorted) {
            System.out.printf("| %-10s | %-14s | %-6d | %-10s | %-10s | %-16s | %-12s |\n",
                              doc.getId(), doc.getName(), doc.getRoomNumber(),
                              doc.isAvailable() ? "Yes" : "No",
                              doc.getGender(), doc.getIcNumber(), doc.getPhoneNumber());
        }

        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");
    }

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

    public boolean isDoctorAvailableForAppointment(
        Doctor doctor,
        LocalDateTime startTime,
        int durationHours,
        ClinicADT<Consultation> consultations,
        ClinicADT<MedicalTreatment> treatments) {

        if (!isDoctorAvailable(doctor, startTime, durationHours)) {
            return false;
        }

        LocalDateTime endTime = startTime.plusHours(durationHours);

        for (Consultation c : consultations) {
            if (c.getDoctorName().equalsIgnoreCase(doctor.getName())) {
                LocalDateTime cStart = c.getConsultationDate();
                LocalDateTime cEnd = cStart.plusHours(1);
                if (startTime.isBefore(cEnd) && endTime.isAfter(cStart)) {
                    return false;
                }
            }
        }

        for (MedicalTreatment t : treatments) {
            if (t.getDoctorId().equalsIgnoreCase(doctor.getId())) {
                LocalDateTime tStart = t.getTreatmentDateTime();
                LocalDateTime tEnd = tStart.plusHours(2);
                if (startTime.isBefore(tEnd) && endTime.isAfter(tStart)) {
                    return false;
                }
            }
        }

        return true;
    }

    public void printAvailableDoctors() {
        boolean found = false;
        System.out.println("=== Available Doctors ===");
        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");
        System.out.printf("| %-10s | %-14s | %-6s | %-10s | %-10s | %-16s | %-12s |\n",
                          "Doctor ID", "Name", "Room", "Available", "Gender", "IC Number", "Phone");
        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");

        for (Doctor doc : doctorList) {
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

        for (Doctor doc : doctorList) {
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
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (created) {
                    System.out.println("Created directory: " + parentDir.getAbsolutePath());
                }
            }

            try (FileWriter writer = new FileWriter(filePath)) {
                for (Doctor doc : doctorList) {
                    writer.write(doc.toFileString() + "\n");
                    DutySchedule schedule = doc.getDutySchedule();
                    for (DayOfWeek day : DayOfWeek.values()) {
                        writer.write("  " + day + ": " + schedule.getSessionForDay(day) + "\n");
                    }
                    writer.write("\n");
                }
                System.out.println("Doctor data saved successfully to: " + filePath);
            }
        } catch (IOException e) {
            System.out.println("Error writing to file: " + filePath + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

        public void loadFromFile(String filePath) {
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("No existing doctor data found at: " + filePath + ". Starting fresh.");
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
                                System.out.println("Invalid room number in line: " + line);
                                currentDoctor = null;
                                currentSchedule = null;
                            }
                        } else {
                            System.out.println("Invalid doctor data format in line: " + line);
                            currentDoctor = null;
                            currentSchedule = null;
                        }
                    } else if (currentSchedule != null && currentDoctor != null) {
                        String[] scheduleParts = line.trim().split(":\\s*", 2);
                        if (scheduleParts.length == 2) {
                            try {
                                DayOfWeek day = DayOfWeek.valueOf(scheduleParts[0].trim().toUpperCase());
                                Session session = Session.valueOf(scheduleParts[1].trim().toUpperCase());
                                currentSchedule.setDaySession(day, session);
                            } catch (IllegalArgumentException e) {
                                System.out.println("Invalid schedule entry: " + line);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Error reading from file: " + filePath + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
}
