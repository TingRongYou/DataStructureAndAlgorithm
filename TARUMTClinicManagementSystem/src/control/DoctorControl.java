package control;

import adt.ClinicADT;
import adt.MyClinicADT;
import entity.Consultation;
import entity.Doctor;
import entity.MedicalTreatment;
import tarumtclinicmanagementsystem.DutySchedule;
import tarumtclinicmanagementsystem.Session;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Scanner;

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

    private boolean isRoomOccupied(int roomNumber) {
        ClinicADT.MyIterator<Doctor> iterator = doctorList.iterator();
        while (iterator.hasNext()) {
            Doctor doc = iterator.next();
            if (doc.getRoomNumber() == roomNumber) return true;
        }
        return false;
    }

    private DutySchedule generateWeeklyShift(int index) {
        DutySchedule schedule = new DutySchedule();
        DayOfWeek[] days = DayOfWeek.values();
        Session[] shifts = {MORNING, AFTERNOON, NIGHT};

        int rest1 = index % 7;
        int rest2 = (rest1 + 3) % 7;

        for (int i = 0; i < 7; i++) {
            DayOfWeek day = days[i];
            if (i == rest1 || i == rest2) {
                schedule.setDaySession(day, Session.REST);
            } else {
                schedule.setDaySession(day, shifts[(index + i) % 3]);
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

        printDoctorTableHeader();

        ClinicADT.MyIterator<Doctor> iterator = doctorList.iterator();
        while (iterator.hasNext()) {
            printDoctorRow(iterator.next());
        }

        printDoctorTableFooter();
    }

    public void removeDoctorById(String doctorId) {
        ClinicADT.MyIterator<Doctor> iterator = doctorList.iterator();
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

    public Doctor getDoctorById(String id) {
        ClinicADT.MyIterator<Doctor> iterator = doctorList.iterator();
        while (iterator.hasNext()) {
            Doctor doc = iterator.next();
            if (doc.getId().equalsIgnoreCase(id)) return doc;
        }
        return null;
    }
    
    public int getDoctorCount() {
        return doctorList.size();  // assuming doctorList is ClinicADT<Doctor>
    }

    public void printDoctorsSortedByName() {
        if (doctorList.isEmpty()) {
            System.out.println("No doctors available.");
            return;
        }

        ClinicADT<Doctor> sorted = new MyClinicADT<>();
        ClinicADT.MyIterator<Doctor> iter = doctorList.iterator();
        while (iter.hasNext()) sorted.add(iter.next());

        sorted.sort((d1, d2) -> d1.getName().compareToIgnoreCase(d2.getName())); // using MyComparator

        printDoctorTableHeader();
        iter = sorted.iterator();
        while (iter.hasNext()) printDoctorRow(iter.next());
        printDoctorTableFooter();
    }
    
    public boolean checkRoomAvailability(int roomNumber) {
        return !isRoomOccupied(roomNumber);  // returns true if room is free
    }

    public boolean isDoctorAvailable(Doctor doctor, LocalDateTime startTime, int durationHours) {
        if (doctor == null) return false;

        DayOfWeek dayOfWeek = startTime.getDayOfWeek();
        Session sessionForDay = doctor.getDutySchedule().getSessionForDay(dayOfWeek);
        if (sessionForDay == Session.REST) return false;

        int hour = startTime.getHour();
        return switch (sessionForDay) {
            case MORNING -> hour >= 8 && (hour + durationHours) <= 12;
            case AFTERNOON -> hour >= 12 && (hour + durationHours) <= 18;
            case NIGHT -> hour >= 18 && (hour + durationHours) <= 24;
            default -> false;
        };
    }

    public boolean isDoctorAvailableForAppointment(
            Doctor doctor,
            LocalDateTime startTime,
            int durationHours,
            ClinicADT<Consultation> consultations,
            ClinicADT<MedicalTreatment> treatments) {

        if (!isDoctorAvailable(doctor, startTime, durationHours)) return false;
        LocalDateTime endTime = startTime.plusHours(durationHours);

        ClinicADT.MyIterator<Consultation> cIter = consultations.iterator();
        while (cIter.hasNext()) {
            Consultation c = cIter.next();
            if (c.getDoctorName().equalsIgnoreCase(doctor.getName())) {
                LocalDateTime cStart = c.getConsultationDate();
                LocalDateTime cEnd = cStart.plusHours(1);
                if (startTime.isBefore(cEnd) && endTime.isAfter(cStart)) return false;
            }
        }

        ClinicADT.MyIterator<MedicalTreatment> tIter = treatments.iterator();
        while (tIter.hasNext()) {
            MedicalTreatment t = tIter.next();
            if (t.getDoctorId().equalsIgnoreCase(doctor.getId())) {
                LocalDateTime tStart = t.getTreatmentDateTime();
                LocalDateTime tEnd = tStart.plusHours(2);
                if (startTime.isBefore(tEnd) && endTime.isAfter(tStart)) return false;
            }
        }

        return true;
    }

    public void printAvailableDoctors() {
        boolean found = false;
        System.out.println("=== Available Doctors ===");

        printDoctorTableHeader();

        ClinicADT.MyIterator<Doctor> iterator = doctorList.iterator();
        while (iterator.hasNext()) {
            Doctor doc = iterator.next();
            if (doc.isAvailable()) {
                printDoctorRow(doc);
                found = true;
            }
        }

        if (!found) {
            System.out.println("|                          No doctors are currently available.                                    |");
        }

        printDoctorTableFooter();
    }

    public void printAvailableDoctorsOn(LocalDateTime startTime, int durationHours) {
        System.out.println("=== Available Doctors at " + startTime + " ===");
        boolean found = false;

        ClinicADT.MyIterator<Doctor> iterator = doctorList.iterator();
        while (iterator.hasNext()) {
            Doctor doc = iterator.next();
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
            if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();

            try (FileWriter writer = new FileWriter(filePath)) {
                ClinicADT.MyIterator<Doctor> iterator = doctorList.iterator();
                while (iterator.hasNext()) {
                    Doctor doc = iterator.next();
                    writer.write(doc.toFileString() + "\n");
                    DutySchedule schedule = doc.getDutySchedule();
                    for (DayOfWeek day : DayOfWeek.values()) {
                        writer.write("  " + day + ": " + schedule.getSessionForDay(day) + "\n");
                    }
                    writer.write("\n");
                }
            }
        } catch (IOException e) {
            System.out.println("Error writing to file: " + filePath + " - " + e.getMessage());
        }
    }
    
   public void updateDoctorScheduleById(String doctorId, Scanner scanner) {
        Doctor doctor = getDoctorById(doctorId);
        if (doctor == null) {
            System.out.println("Doctor ID not found.");
            return;
        }

        System.out.println("\nUpdating schedule for Dr. " + doctor.getName() + ":");
        // Show current timetable first
        doctor.getDutySchedule().printScheduleTable(doctor.getName());

        // Iterate over days and update shifts
        for (DayOfWeek day : DayOfWeek.values()) {
            String shiftInput;
            while (true) {
                System.out.print("Enter shift for " + day + " (MORNING/AFTERNOON/NIGHT/REST) or leave empty to keep current: ");
                shiftInput = scanner.nextLine().trim().toUpperCase();

                if (shiftInput.isEmpty()) {
                    break; // Keep current shift
                }

                try {
                    Session session = Session.valueOf(shiftInput);
                    doctor.getDutySchedule().setDaySession(day, session);
                    break;
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid session. Please enter MORNING, AFTERNOON, NIGHT, or REST.");
                }
            }
        }

        System.out.println("\nUpdated schedule for Dr. " + doctor.getName() + ":");
        doctor.getDutySchedule().printScheduleTable(doctor.getName());

        saveToFile(doctorFilePath); // persist changes
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

            while (fileScanner.hasNextLine()) {
                String rawLine = fileScanner.nextLine();
                String line = rawLine.trim();
                if (line.isEmpty()) continue;

                // Doctor info line: does NOT start with whitespace
                if (!rawLine.startsWith(" ")) {
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
                        } catch (NumberFormatException e) {
                            System.out.println("Warning: Invalid doctor data line: " + line);
                            currentDoctor = null;
                            currentSchedule = null;
                        }
                    } else {
                        System.out.println("Warning: Incomplete doctor data line: " + line);
                        currentDoctor = null;
                        currentSchedule = null;
                    }
                } 
                // Schedule line: starts with at least one space
                else if (currentSchedule != null && currentDoctor != null) {
                    String[] scheduleParts = line.split(":\\s*", 2);
                    if (scheduleParts.length == 2) {
                        try {
                            DayOfWeek day = DayOfWeek.valueOf(scheduleParts[0].trim().toUpperCase());
                            Session session = Session.valueOf(scheduleParts[1].trim().toUpperCase());
                            currentSchedule.setDaySession(day, session);
                        } catch (IllegalArgumentException e) {
                            System.out.println("Warning: Invalid schedule entry for doctor " 
                                + currentDoctor.getName() + ": " + line);
                        }
                    } else {
                        System.out.println("Warning: Invalid schedule format: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading from file: " + filePath + " - " + e.getMessage());
        }
    }



    // Utility printing methods
    private void printDoctorTableHeader() {
        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");
        System.out.printf("| %-10s | %-14s | %-6s | %-10s | %-10s | %-16s | %-12s |\n",
                "Doctor ID", "Name", "Room", "Available", "Gender", "IC Number", "Phone");
        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");
    }

    private void printDoctorRow(Doctor doc) {
        System.out.printf("| %-10s | %-14s | %-6d | %-10s | %-10s | %-16s | %-12s |\n",
                doc.getId(), doc.getName(), doc.getRoomNumber(),
                doc.isAvailable() ? "Yes" : "No",
                doc.getGender(), doc.getIcNumber(), doc.getPhoneNumber());
    }

    private void printDoctorTableFooter() {
        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");
    }
    
    public int countDutySessions(Doctor doc) {
        DutySchedule schedule = doc.getDutySchedule();
        int dutyCount = 0;

        for (DayOfWeek day : DayOfWeek.values()) {
            Session session = schedule.getSessionForDay(day); // Use Session type
            if (session != null && !session.toString().equalsIgnoreCase("OFF") && !session.toString().trim().isEmpty()) {
                dutyCount++;
            }
        }

        return dutyCount;
    }
}
